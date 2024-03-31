package com.whitemagic2014.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.dao.ActivityDao;
import com.whitemagic2014.pojo.Activity;
import com.whitemagic2014.service.ActivityService;

import com.whitemagic2014.util.ActivityManagerUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ding
 * @date 2023/10/26
 */
@Service
@Slf4j
public class SignActivityServiceImpl implements ActivityService<SignActivityServiceImpl.SignActivityRule,
        SignActivityServiceImpl.SignAwardRule> {

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
        Activity activity = activityDao.getById(activityId, false);
        if (activity == null) {
            return null;
        }
        SignActivityRule signActivityRule = JSONObject.parseObject(activity.getActivityRule(), SignActivityRule.class);

        List<Activity.ActivityAwardRule> awardRule = JSONObject.parseArray(activity.getAwardRule(), Activity.ActivityAwardRule.class);
        List<Message> msgList = new ArrayList<>();
        awardRule.forEach(award -> msgList.addAll(activityManagerUtil
                .buildAwardMessage(award.getAwardName(), award.getAwardNum(), userId)));
        return msgList;
    }

    @Override
    public boolean updateById(Activity activity) {

        return false;
    }

    @Override
    public SignActivityRule buildRule(String rule, List<String> args) {
        return JSONObject.parseObject(rule, SignActivityRule.class);
    }

    @Override
    public List<SignAwardRule> buildAwardRule(String awardRuleJson) {
        try {
            return JSONObject.parseArray(awardRuleJson, SignAwardRule.class);
        } catch (Exception e) {
            log.error("奖品规则构建失败", e);
        }
        return null;
    }

    @Override
    public String getActivityDetail(Activity activity) {
        return null;
    }

    @Data
    public static class SignActivityRule extends Activity.ActivityRule {

    }

    @Data
    public static class SignAwardRule extends Activity.ActivityAwardRule {
        /**
         * 奖品类型
         */
        private String signAwardType;
        /**
         * 规则值
         */
        private Integer val;

        @Getter
        @AllArgsConstructor
        public enum signAwardType {
            CONTINUOUS("CONTINUOUS", "连续签到"),
            ACCUMULATIVE("ACCUMULATIVE", "累计签到"),
            NORMAL("NORMAL", "普通签到"),
            ;
            /**
             * 类型名称
             */
            private final String name;
            /**
             * 类型描述
             */
            private final String desc;
        }
    }
}
