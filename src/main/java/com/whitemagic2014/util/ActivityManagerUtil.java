package com.whitemagic2014.util;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.command.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.dao.ActivityDao;
import com.whitemagic2014.events.CommandEvents;
import com.whitemagic2014.pojo.Activity;
import com.whitemagic2014.pojo.CommandProperties;
import com.whitemagic2014.service.ActivityReceiveRecordService;
import com.whitemagic2014.service.ActivityService;
import com.whitemagic2014.util.spring.SpringApplicationContextUtil;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.event.GlobalEventChannel;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ding
 * @date 2023/10/25
 */
@Component
public class ActivityManagerUtil {

    @Autowired
    @SuppressWarnings("rawtypes")
    private List<ActivityService> activityServiceList;

    @Autowired
    private ActivityDao activityDao;

    @Autowired
    private GlobalParam globalParam;

    @Autowired
    private CommandThreadPoolUtil commandThreadPoolUtil;

    @Autowired
    private ActivityReceiveRecordService activityReceiveRecordService;

    private final String botNameReplacement = "${botName}";
    private final String botNickReplacement = "${botNick}";
    private final String ownerNickReplacement = "${ownerNick}";

    /**
     * 新增活动
     *
     * @param activityType  活动类型
     * @param ruleJson      活动规则
     * @param awardRuleJson 奖品规则
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @return 是否创建成功
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean createActivity(String activityType, String ruleJson, String awardRuleJson, String startTime, String endTime) {
        ActivityService activityService = activityServiceList.stream().filter(actService ->
                        actService.support(activityType)).findFirst().orElse(null);
        if (activityService == null) {
            return false;
        }
        Activity.ActivityRule rule = activityService.buildRule(ruleJson);
        if (rule == null) {
            return false;
        }
        List<Activity.ActivityAwardRule> awardRule = activityService.buildAwardRule(awardRuleJson);
        if (CollectionUtils.isEmpty(awardRule)) {
            return false;
        }

        Activity activity = new Activity();
        activity.setActivityType(activityType);
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        activity.setActivityRule(ruleJson);
        activity.setAwardRule(awardRuleJson);
        activity.setEnabled(1);

        activityDao.createActivity(activity);
        CommandEvents commandEvents = (CommandEvents) SpringApplicationContextUtil.getBean("CommandEvents");
        commandEvents.registerCommands(buildActivityCommand(Collections.singletonList(activity)));
        return true;
    }

    /**
     * 参加活动
     *
     * @param activityId 活动ID
     * @param userId     参与者ID
     * @return 参与结果 null-参与失败 not null-参与结果
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized List<Message> joinActivity(long activityId, long userId) {
        Activity activity = activityDao.getById(activityId);
        if (activity == null) {
            return null;
        }

        Activity.ActivityRule activityRule = JSONObject.parseObject(activity.getActivityRule(), Activity.ActivityRule.class);
        // 时间范围，是否启用判断
        if (new Date().before(DateUtil.parse(activity.getStartTime(), "yyyy-MM-dd"))
                || new Date().after(DateUtil.parse(activity.getEndTime(), "yyyy-MM-dd"))
                || activity.getEnabled() == 0) {
            return null;
        }

        // 参与次数判断
        int limitType = activityRule.getLimit() >> 27;
        int limitTimes = activityRule.getLimit() & ((1 << 27) - 1);
        int joinTimes = 0;
        switch (limitType) {
            case 0:
                // 无限制
                break;
            case 1:
                // 日维度
                joinTimes = activityReceiveRecordService.selectCount(DateUtil.format(new Date(), "yyyy-MM-dd"), userId, activityId);
                break;
            case 2:
                // 月维度
                joinTimes = activityReceiveRecordService.selectCount(DateUtil.format(new Date(), "yyyy-MM"), userId, activityId);
                break;
            case 3:
                // 年维度
                joinTimes = activityReceiveRecordService.selectCount(DateUtil.format(new Date(), "yyyy"), userId, activityId);
                break;
            case 4:
                // 终身
                joinTimes = activityReceiveRecordService.selectCount(null, userId, activityId);
                break;
            default:
                return null;
        }
        if (joinTimes >= limitTimes) {
            return null;
        }
        ActivityService activityService = activityServiceList.stream().filter(act -> act.support(activity.getActivityType()))
                .findFirst().orElse(null);
        if (activityService == null) {
            return null;
        }
        return activityService.joinActivity(activityId, userId);
    }

    /**
     * 删除活动
     *
     * @param activityId 活动ID
     * @return 删除结果
     */
    public boolean deleteActivity(long activityId) {
        Activity old = activityDao.getById(activityId);
        if (old == null) {
            return false;
        }
        activityDao.deleteById(activityId);

        Activity.ActivityRule rule = JSONObject.parseObject(old.getActivityRule(), Activity.ActivityRule.class);
        CommandEvents commandEvents = (CommandEvents) SpringApplicationContextUtil.getBean("CommandEvents");
        commandEvents.removeCommandByKeyword(rule.getKeyword());
        return true;
    }

    /**
     * 初始化活动指令
     *
     * @return 活动指令集
     */
    public List<Command> initActivityCommand() {
        List<Activity> actList = activityDao.queryActivityList();
        if (!CollectionUtils.isEmpty(actList)) {
            return buildActivityCommand(actList);
        }
        return null;
    }

    /**
     * 构建活动指令
     *
     * @return 活动指令
     */
    public List<Command> buildActivityCommand(List<Activity> actList) {
        return actList.stream().map(act -> {
                    Activity.ActivityRule activityRule = JSONObject.parseObject(act.getActivityRule(), Activity.ActivityRule.class);
                    return new NoAuthCommand() {

                        @Override
                        protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
                            List<Message> msgList = joinActivity(act.getId(), sender.getId());
                            if (CollectionUtils.isEmpty(msgList)) {
                                return new PlainText(randomText(activityRule.getFailReply()));
                            }
                            commandThreadPoolUtil.addGroupTask(msgList, sender.getGroup().getId());
                            return new PlainText(randomText(activityRule.getSuccessReply()));
                        }

                        @Override
                        public CommandProperties properties() {
                            return explainKeyword(activityRule.getKeyword());
                        }
                    };
                }
        ).collect(Collectors.toList());
    }

    /**
     * 关键词构建为指令属性
     *
     * @param keyword 关键词列表
     * @return 指令属性
     */
    public CommandProperties explainKeyword(List<String> keyword) {
        if (CollectionUtils.isEmpty(keyword)) {
            throw new NullPointerException("activity keyword must not be null");
        }
        keyword.forEach(k -> {
            if (k.contains(botNameReplacement)) {
                k = k.replace(botNameReplacement, globalParam.botName);
            }
            if (k.contains(botNickReplacement)) {
                k = k.replace(botNickReplacement, globalParam.botNick);
            }
            if (k.contains(ownerNickReplacement)) {
                k = k.replace(ownerNickReplacement, globalParam.ownerNick);
            }
        });
        if (keyword.size() == 1) {
            return new CommandProperties(keyword.get(0));
        }
        return new CommandProperties(keyword.remove(0), keyword.toArray(new String[0]));
    }

    public String randomText(List<String> list) {
        return list.get(RandomUtils.nextInt(0, list.size()));
    }

    public List<Message> buildAwardMessage(String awardName, Integer awardNum, long userId) {
        switch (awardName) {
            case "谢谢惠顾":
                return Collections.singletonList(new PlainText(awardName));
            case "灵石":
                return Collections.singletonList(new PlainText("送" + awardName + awardNum));
            case "渡厄丹":
                List<Message> messageList = new ArrayList<Message>();
                for (int i = 0; i < awardNum; i++) {
                    messageList.add(new PlainText("世界积分兑换1"));
                }
                messageList.add(new PlainText("坊市上架 " + awardName + " 10000 " + awardNum));
                return messageList;
            default:
                return Collections.singletonList(new PlainText("坊市上架 " + awardName + " 10000 " + awardNum));
        }
    }

    public List<String> dealStringList(String src) {
        if (src.startsWith("[") && src.endsWith("]")) {
            String content = src.replace("[","").replace("]","");
            String[] contents = content.replace("，", ",").split(",");
            return Arrays.stream(contents).collect(Collectors.toList());
        }
        List<String> list = new ArrayList<>();
        list.add(src);
        return list;
    }
}
