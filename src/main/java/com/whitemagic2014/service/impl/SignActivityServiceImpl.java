package com.whitemagic2014.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.dao.ActivityDao;
import com.whitemagic2014.pojo.Activity;
import com.whitemagic2014.service.ActivityService;

import com.whitemagic2014.util.ActivityManagerUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ding
 * @date 2023/10/26
 */
@Service
@Slf4j
public class SignActivityServiceImpl implements ActivityService<Activity.ActivityAwardRule> {

    @Autowired
    private ActivityDao activityDao;

    @Autowired
    private ActivityManagerUtil activityManagerUtil;

    @Override
    public boolean support(String type) {
        return Activity.ActivityType.SIGN.getName().equals(type);
    }

    @Override
    public List<Message> joinActivity(long activityId, long userId) {
        Activity activity = activityDao.getById(activityId);
        if (activity == null) {
            return null;
        }
        SignActivityRule signActivityRule = JSONObject.parseObject(activity.getActivityRule(), SignActivityRule.class);

        Activity.ActivityAwardRule awardRule = JSONObject.parseObject(activity.getAwardRule(), Activity.ActivityAwardRule.class);
        return activityManagerUtil.buildAwardMessage(awardRule.getAwardName(), awardRule.getAwardNum(), userId);
    }

    @Override
    public boolean updateById(Activity activity) {

        return false;
    }

    @Override
    public SignActivityRule buildRule(String ruleJson) {
        try {
            return JSONObject.parseObject(ruleJson, SignActivityRule.class);
        } catch (Exception e) {
            log.error("活动规则构建失败", e);
        }
        return null;
    }

    @Override
    public List<Activity.ActivityAwardRule> buildAwardRule(String awardRuleJson) {
        try {
            return JSONObject.parseArray(awardRuleJson, Activity.ActivityAwardRule.class);
        } catch (Exception e) {
            log.error("奖品规则构建失败", e);
        }
        return null;
    }

    @Override
    public List<Activity> queryActivity() {
        return activityDao.queryActivity(Activity.ActivityType.SIGN.getName());
    }

    @Data
    public static class SignActivityRule extends Activity.ActivityRule {
        /**
         * 日签到总数上限
         */
        private Integer dailyLimit;
    }
}
