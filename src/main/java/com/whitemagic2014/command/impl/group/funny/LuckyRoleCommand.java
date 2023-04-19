package com.whitemagic2014.command.impl.group.funny;

import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.cache.HomeworkCache;
import com.whitemagic2014.cache.LuckyRoleCache;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.command.impl.group.pcr.tool.SearchWork;
import com.whitemagic2014.dao.HomeworkIconDao;
import com.whitemagic2014.pojo.CommandProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 抽取幸运角色，每小时只有一个结果
 * icon来源于花舞作业网
 *
 * @author ding
 * @see com.whitemagic2014.command.impl.group.funny.InitIconCommand
 */
@Command
public class LuckyRoleCommand extends NoAuthCommand {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMddHH");
    private static final List<SpecialLucky> SPECIAL_LUCKY_LIST = new ArrayList<>();

    private static final Object LOCK = new Object();

    @Value("${specialRule.luckyRole.rigged:}")
    private String specialRulesJson;

    @Value("${specialRule.luckyRole.endWhileSpRuleAnalyseFailed:}")
    private boolean endWhileSpRuleAnalyseFailed;

    @Autowired
    private HomeworkIconDao homeworkIconDao;

    /**
     * 黑幕
     */
    private boolean initSpecialRules() {
        if (CollectionUtils.isEmpty(SPECIAL_LUCKY_LIST) && !StringUtils.isEmpty(specialRulesJson)) {
            synchronized (LOCK) {
                if (CollectionUtils.isEmpty(SPECIAL_LUCKY_LIST) && !StringUtils.isEmpty(specialRulesJson)) {
                    try {
                        SPECIAL_LUCKY_LIST.addAll(JSONObject.parseArray(specialRulesJson, SpecialLucky.class));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("幸运角色");
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (!initSpecialRules()) {
            System.out.println("黑幕加载失败");
            if (endWhileSpRuleAnalyseFailed) {
                return new PlainText("黑幕头子因黑幕解析失败终止了此次操作");
            }
        }
        long id = sender.getId();
        String yms = SDF.format(new Date());

        Random random = new Random(Long.parseLong(id + yms));
        if (CollectionUtils.isEmpty(HomeworkCache.getIconDataList())) {
            HomeworkCache.getIconDataList().addAll(homeworkIconDao.list().stream().map(icon -> {
                SearchWork.SearchIconResponse.IconData iconData = new SearchWork.SearchIconResponse.IconData();
                iconData.setId(icon.getId());
                iconData.setIconId(icon.getServerId());
                iconData.setIconFilePath(icon.getIconUrl());
                iconData.setIconValue(icon.getIconName());
                return iconData;
            }).collect(Collectors.toList()));
        }

        List<SearchWork.SearchIconResponse.IconData> iconData = new ArrayList<>();
        if (!CollectionUtils.isEmpty(SPECIAL_LUCKY_LIST)) {
            List<SpecialLucky> spList = SPECIAL_LUCKY_LIST.stream()
                    .filter(sp -> sp.getAffectIds().contains(sender.getId())).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(spList)) {
                dealSpecialRule(spList, iconData, random);
            }
        }

        if (CollectionUtils.isEmpty(iconData)) {
            SearchWork.SearchIconResponse.IconData icon
                    = HomeworkCache.getIconDataList().get(random.nextInt(HomeworkCache.getIconDataList().size()));
            iconData.add(icon);
        }
        LuckyRoleCache.putLatest(sender.getId(), iconData.get(0));

        System.out.println("幸运角色【" + iconData.get(0).getIconValue() + "】");
        // 特殊网络环境下图片被拦截的解决方案，正常情况下不需要这个try-catch
        try {
            return new At(id)
                    .plus(new PlainText("你这小时的幸运角色是：\n"))
                    .plus(Contact.uploadImage(subject, new File(iconData.get(0).getIconFilePath())))
                    .plus(new PlainText(iconData.get(0).getIconValue()));
        } catch (Exception e) {
            e.printStackTrace();
            return new At(id)
                    .plus(new PlainText("你这小时的幸运角色是：\n"))
                    .plus(new PlainText("-----------\n"))
                    .plus(new PlainText("【图片被拦截】\n"))
                    .plus(new PlainText("-----------\n"))
                    .plus(new PlainText(iconData.get(0).getIconValue()));
        }
    }

    private void dealSpecialRule(List<SpecialLucky> spList, List<SearchWork.SearchIconResponse.IconData> iconData, Random random) {
        int totalRate = spList.stream().mapToInt(sp -> sp.getRate() + sp.getVariantRate()).reduce(Integer::sum).orElse(0);
        if (totalRate < 0 || totalRate >= 100) {
            throw new IllegalArgumentException("黑幕概率之和不合法，应在[0,100)区间内");
        }

        int randNum = random.nextInt(100);
        System.out.println("【生成的随机数：" + randNum + "】【最大黑幕数：" + totalRate  + "】");
        if (randNum < totalRate) {
            int rate = 0;
            SpecialLucky aimedSp = null;
            for (SpecialLucky sp : spList) {
                rate += sp.getRate();
                if (sp.getVariantRate() > 0) {
                    rate += random.nextInt(sp.getVariantRate());
                }
                if (rate > randNum) {
                    aimedSp = sp;
                    break;
                }
            }

            if (aimedSp != null) {
                SearchWork.SearchIconResponse.IconData icon = new SearchWork.SearchIconResponse.IconData();
                icon.setId(aimedSp.getSpecialId());
                icon.setIconId(aimedSp.getSpecialId());
                icon.setIconValue(aimedSp.getSpecialName());
                icon.setIconFilePath(aimedSp.getSpecialIconFilePath());

                iconData.add(icon);
            }
        }
    }


    @Data
    @AllArgsConstructor
    public static class SpecialLucky {
        /**
         * 此规则生效的QQ号
         */
        private List<Long> affectIds;
        /**
         * 概率
         */
        private Integer rate;
        /**
         * 在原有概率基础上，浮动的概率
         */
        private Integer variantRate;
        /**
         * 幸运角色ID
         */
        private Integer specialId;
        /**
         * 幸运角色名称
         */
        private String specialName;
        /**
         * 幸运角色icon路径
         */
        private String specialIconFilePath;
    }
}
