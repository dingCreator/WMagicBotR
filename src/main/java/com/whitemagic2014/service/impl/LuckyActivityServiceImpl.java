package com.whitemagic2014.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.dao.ActivityDao;
import com.whitemagic2014.pojo.Activity;
import com.whitemagic2014.service.ActivityService;
import com.whitemagic2014.util.ActivityManagerUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

import java.util.List;

/**
 * @author ding
 * @date 2023/10/23
 */
@Service
@Slf4j
public class LuckyActivityServiceImpl implements ActivityService<LuckyActivityServiceImpl.LuckyActivityRule,
        LuckyActivityServiceImpl.LuckyAwardRule> {

    @Autowired
    private ActivityDao activityDao;

    @Autowired
    private ActivityManagerUtil activityManagerUtil;

    private static final int RATE = 100;

    @Override
    public boolean support(String type) {
        return Activity.ActivityType.LUCKY.getName().equals(type);
    }

    @Override
    public List<Message> joinActivity(long activityId, long userId) {
        // 抽奖功能
        Activity activity = activityDao.getById(activityId, true);
        if (activity == null) {
            return null;
        }
        List<LuckyAwardRule> luckyAwardRuleList = JSONObject.parseArray(activity.getAwardRule(), LuckyAwardRule.class);
        int r = RandomUtil.randomInt(RATE);
        System.out.println("抽奖活动生成的随机数：" + r);
        int index = 0;
        String awardName = "谢谢惠顾";
        int awardNum = 0;
        for (LuckyAwardRule rule : luckyAwardRuleList) {
            if (r >= index && r < index + rule.getRate()) {
                awardName = rule.getAwardName();
                awardNum = rule.getAwardNum();
                break;
            }
            index += rule.getRate();
        }
        return activityManagerUtil.buildAwardMessage(awardName, awardNum, userId);
    }

    @Override
    public boolean updateById(Activity activity) {
        Activity old = activityDao.getById(activity.getId(), false);
        if (old == null) {
            return false;
        }
        BeanUtils.copyProperties(activity, old);
        activityDao.updateById(old);
        return true;
    }

    @Override
    public LuckyActivityRule buildRule(String rule, List<String> args) {
        return JSONObject.parseObject(rule, LuckyActivityRule.class);
    }

    @Override
    public List<LuckyAwardRule> buildAwardRule(String awardRuleJson) {
        try {
            List<LuckyAwardRule> awardRule = JSONObject.parseArray(awardRuleJson, LuckyAwardRule.class);
            int rate = awardRule.stream().mapToInt(LuckyAwardRule::getRate).sum();
            // 概率之和应为100
            if (rate != RATE) {
                return null;
            }
            return awardRule;
        } catch (Exception e) {
            log.error("奖品规则构建失败", e);
        }
        return null;
    }

    @Override
    public String getActivityDetail(Activity activity) {
        StringBuilder builder = new StringBuilder();
        List<LuckyAwardRule> awardRuleList = JSONObject.parseArray(activity.getAwardRule(), LuckyAwardRule.class);
        builder.append("\n").append("奖品列表:");
        awardRuleList.forEach(award -> {
            builder.append("\n").append("奖品名称:").append(award.getAwardName());
            builder.append(",").append("奖品数量:").append(award.getAwardNum());
            builder.append(",").append("中奖概率:").append(award.getRate()).append("%");
        });
        return builder.toString();
    }

    @Data
    public static class LuckyActivityRule extends Activity.ActivityRule {

    }

    @Data
    public static class LuckyAwardRule extends Activity.ActivityAwardRule {
        /**
         * 中奖率
         */
        private Integer rate;
    }
}
