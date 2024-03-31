package com.whitemagic2014.command.impl.group.immortal;

import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.AdminCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.Activity;
import com.whitemagic2014.pojo.CommandProperties;

import com.whitemagic2014.util.ActivityManagerUtil;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ding
 * @date 2023/11/8
 */
@Command
@Slf4j
public class ActivityCommand extends AdminCommand {

    @Autowired
    private GlobalParam globalParam;

    @Autowired
    private ActivityManagerUtil activityManagerUtil;

    /**
     * 友好提示
     */
    private static final String FORMAT_INFO = "发送“%s活动配置 {新增/删除/查看/编辑/修改}”查看更多帮助";
    /**
     * 新增提示
     */
    private static final String FORMAT_INFO_CREATE = "格式:%s活动配置 {新增} {活动名称} {活动类型}" +
            " {触发关键词}(*)|{单人限制类型（无/日/月/年）}|{单人限制次数}|{总体限制类型（无/日/月/年）}|{总体限制次数}|{参与成功回复}(*)|{参与失败回复}(*)" +
            " {奖品名|数量|其余参数}(*) {开始时间（yyyy-MM-dd）} {结束时间(yyyy-MM-dd)}\n" +
            "（标(*)表示支持多个，若有多个，用[]包住并用逗号分隔）";
    /**
     * 删除提示
     */
    private static final String FORMAT_INFO_DELETE = "格式:%s活动配置 {删除} {活动ID}";
    /**
     * 查询提示
     */
    private static final String FORMAT_INFO_GET = "格式:%s活动配置 {查看} [活动ID/活动类型]";
    /**
     * 修改提示
     */
    private static final String FORMAT_INFO_EDIT = "格式:%s活动配置 {编辑/修改} {活动ID} {修改的配置} {修改后的值}" +
            "\n修改的配置支持：\n触发关键词(*)\n限制类型（无/日/月/年）\n限制次数\n参与成功回复(*)\n参与失败回复(*)" +
            "\n奖品（奖品名|数量|概率（可选））(*)\n开始时间（yyyy-MM-dd）\n结束时间(yyyy-MM-dd)" +
            "\n（标(*)表示支持多个，若有多个，用[]包住并用逗号分隔）";

    private static final String INVALID_ACTIVITY_NAME = "列表";

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        String formatInfo = String.format(FORMAT_INFO, globalParam.botName);
        if (args.size() < 1) {
            return new PlainText(formatInfo);
        }
        String operation = args.remove(0);
        switch (operation) {
            case "新增":
                return createActivity(args);
            case "删除":
                return deleteActivity(args);
            case "查看":
                return listActivity(args);
            case "编辑":
            case "修改":
                return updateActivity(args);
            default:
                return new PlainText("关键词错误");
        }
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "活动配置", globalParam.botNick + "活动配置");
    }

    private Message createActivity(List<String> args) {
        // 0-活动名称 1-活动类型 2-活动规则 3-奖品规则 4-开始时间 5-结束时间
        if (args.size() < 6) {
            return new PlainText(String.format(FORMAT_INFO_CREATE, globalParam.botName));
        }
        // 获取参数
        String activityName = args.get(0);
        String activityType = args.get(1);
        String activityRuleArgStr = args.get(2);
        String awardRuleArg = args.get(3);
        String startTime = args.get(4);
        String endTime = args.get(5);

        // 参数校验与转换
        if (INVALID_ACTIVITY_NAME.equals(activityName)) {
            return new PlainText("活动名称非法");
        }
        String[] activityRuleArgs = activityRuleArgStr.split("\\|");
        List<String> activityRuleArgsList = new ArrayList<>(activityRuleArgs.length);
        activityRuleArgsList.addAll(Arrays.asList(activityRuleArgs));
        if (activityRuleArgsList.size() < 7) {
            return new PlainText("活动规则配置缺少参数");
        }
        // 0-触发关键词 1-单人限制类型 2-单人限制次数 3-总体限制类型 4-总体限制次数 5-参与成功回复 6-参与失败回复
        List<String> keywords = activityManagerUtil.dealStringList(activityRuleArgsList.remove(0));
        int limitType = activityManagerUtil.transLimitTypeStr2Number(activityRuleArgsList.remove(0));
        String limitTimesStr = activityRuleArgsList.remove(0);
        int totalLimitType = activityManagerUtil.transLimitTypeStr2Number(activityRuleArgsList.remove(0));
        String totalLimitTimesStr = activityRuleArgsList.remove(0);

        int limitTimes, totalLimitTimes;
        try {
            limitTimes = Integer.parseInt(limitTimesStr);
            totalLimitTimes = Integer.parseInt(totalLimitTimesStr);
        } catch (Exception e) {
            log.error("配置解析失败", e);
            return new PlainText("参与次数限制错误");
        }

        int limit = (limitType << 27) + limitTimes;
        int totalLimit = (totalLimitType << 27) + totalLimitTimes;
        List<String> successReplies = activityManagerUtil.dealStringList(activityRuleArgsList.remove(0));
        List<String> failReplies = activityManagerUtil.dealStringList(activityRuleArgsList.remove(0));

        // 规则与奖品规则转化为json
        String ruleJson = activityManagerUtil.buildActivityRule(activityType, limit, totalLimit, keywords,
                successReplies, failReplies, activityRuleArgsList);
        List<String> awardStrList = activityManagerUtil.dealStringList(awardRuleArg);
        List<Map<String, String>> awardParam = awardStrList.stream().map(awardStr -> {
            Map<String, String> singleAwardParam = new HashMap<>();
            String[] param = awardStr.split("\\|");
            singleAwardParam.put("awardName", param[0]);
            singleAwardParam.put("awardNum", param[1]);
            if (param.length > 2) {
                singleAwardParam.put("rate", param[2]);
            }
            return singleAwardParam;
        }).collect(Collectors.toList());
        return new PlainText(activityManagerUtil.createActivity(activityName, activityType, ruleJson,
                JSONObject.toJSONString(awardParam), startTime, endTime));
    }

    private Message updateActivity(List<String> args) {
        if (args.size() < 3) {
            return new PlainText(String.format(FORMAT_INFO_EDIT, globalParam.botName));
        }
        long activityId;
        try {
            activityId = Long.parseLong(args.remove(0));
        } catch (Exception e) {
            return new PlainText("活动ID非法");
        }
        return new PlainText(activityManagerUtil.updateActivity(activityId, args.remove(0), args.remove(0)));
    }

    private Message listActivity(List<String> args) {
        return new PlainText("功能开发中");
    }

    private Message deleteActivity(List<String> args) {
        if (args.size() < 2) {
            return new PlainText(String.format(FORMAT_INFO_DELETE, globalParam.botName));
        }
        long number;
        try {
            number = Long.parseLong(args.get(1));
        } catch (Exception e) {
            log.error("Number Format error", e);
            return new PlainText("活动ID解析错误");
        }
        boolean r = activityManagerUtil.deleteActivity(number);
        if (r) {
            return new PlainText("删除成功");
        }
        return new PlainText("删除失败");
    }
}
