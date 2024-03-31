package com.whitemagic2014.util;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.command.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.dao.ActivityDao;
import com.whitemagic2014.events.CommandEvents;
import com.whitemagic2014.pojo.Activity;
import com.whitemagic2014.pojo.ActivityReceiveRecord;
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
import net.mamoe.mirai.message.data.MessageUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
    private final String activityIdIncrement = "活动_自增ID";

    private static final Lock CREATE_LOCK = new ReentrantLock();

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
    @Transactional(rollbackFor = Exception.class)
    public String createActivity(String activityName, String activityType, String ruleJson, String awardRuleJson,
                                 String startTime, String endTime) {
        ActivityService<?, ?> activityService = activityServiceList.stream().filter(actService ->
                actService.support(activityType)).findFirst().orElse(null);
        if (activityService == null) {
            return "不支持的活动类型";
        }

        CREATE_LOCK.lock();
        try {
            Long lastId = activityDao.getLastActivityId();
            lastId = lastId == null ? 0L : lastId;

            Activity activity = new Activity();
            activity.setId(lastId + 1);
            activity.setActivityName(activityName);
            activity.setActivityType(activityType);
            activity.setStartTime(startTime);
            activity.setEndTime(endTime);
            activity.setActivityRule(ruleJson);
            activity.setAwardRule(awardRuleJson);
            activity.setEnabled(1);

            activityDao.createActivity(activity);
            CommandEvents commandEvents = (CommandEvents) SpringApplicationContextUtil.getBean("CommandEvents");
            commandEvents.registerCommands(buildActivityCommand(Collections.singletonList(activity)));
            return "创建成功";
        } finally {
            CREATE_LOCK.unlock();
        }
    }

    public String updateActivity(long activityId, String settingName, String settingVal) {
        Activity old = getById(activityId, false);
        if (Objects.isNull(old)) {
            return "活动不存在";
        }
        switch (settingName) {
            case "规则":

                break;
            case "奖品":

                break;
            case "开始时间":

                break;
            case "结束时间":

                break;

            default:
                return "配置项错误";
        }
        ActivityService<?, ?> activityService = activityServiceList.stream().filter(actService ->
                actService.support(old.getActivityType())).findFirst().orElse(null);
        if (Objects.isNull(activityService)) {
            return "活动类型有误";
        }
        activityService.updateById(old);
        return null;
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
        // 获取有效活动
        Activity activity = activityDao.getById(activityId, true);
        if (activity == null) {
            return null;
        }

        Activity.ActivityRule activityRule = JSONObject.parseObject(activity.getActivityRule(), Activity.ActivityRule.class);
        // 参与次数判断
        int limitType = activityRule.getLimit() >> 27;
        int limitTimes = getLimitTimes(activityRule.getLimit());
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
                // 终生
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

        List<Message> msgList = activityService.joinActivity(activityId, userId);
        if (!CollectionUtils.isEmpty(msgList)) {
            ActivityReceiveRecord activityReceiveRecord = new ActivityReceiveRecord();
            activityReceiveRecord.setActivityId(activityId);
            activityReceiveRecord.setDate(DateUtil.format(new Date(), "yyyy-MM-dd"));
            activityReceiveRecord.setReceiverId(userId);
            activityReceiveRecordService.insert(activityReceiveRecord);
        }
        return msgList;
    }

    /**
     * 删除活动
     *
     * @param activityId 活动ID
     * @return 删除结果
     */
    public boolean deleteActivity(long activityId) {
        Activity old = activityDao.getById(activityId, false);
        if (old == null) {
            return false;
        }
        activityDao.deleteById(activityId);

        Activity.ActivityRule rule = JSONObject.parseObject(old.getActivityRule(), Activity.ActivityRule.class);
        CommandEvents commandEvents = (CommandEvents) SpringApplicationContextUtil.getBean("CommandEvents");
        commandEvents.removeCommandByKeyword(rule.getKeywords());
        return true;
    }

    /**
     * 初始化活动指令
     *
     * @return 活动指令集
     */
    public List<Command> initActivityCommand() {
        List<Activity> actList = activityDao.queryActivityList(true);
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
                                return new PlainText(randomText(activityRule.getFailReplies()));
                            }
                            commandThreadPoolUtil.addGroupTask(msgList, sender.getGroup().getId());
                            return new PlainText(randomText(activityRule.getSuccessReplies()));
                        }

                        @Override
                        public CommandProperties properties() {
                            return explainKeyword(activityRule.getKeywords());
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
        List<String> keywordAfterFormat = keywordFormat(keyword);
        if (keywordAfterFormat.size() == 1) {
            return new CommandProperties(keywordAfterFormat.get(0));
        }
        return new CommandProperties(keywordAfterFormat.remove(0), keywordAfterFormat.toArray(new String[0]));
    }

    /**
     * 变量转化
     *
     * @param keywords 转化前
     * @return 转化后结果
     */
    public List<String> keywordFormat(List<String> keywords) {
        return keywords.stream().map(k -> {
            if (k.contains(botNameReplacement)) {
                k = k.replace(botNameReplacement, globalParam.botName);
            }
            if (k.contains(botNickReplacement)) {
                k = k.replace(botNickReplacement, globalParam.botNick);
            }
            if (k.contains(ownerNickReplacement)) {
                k = k.replace(ownerNickReplacement, globalParam.ownerNick);
            }
            return k;
        }).collect(Collectors.toList());
    }

    /**
     * 随机获取回复
     *
     * @param list 随机回复集
     * @return 随机回复结果
     */
    public String randomText(List<String> list) {
        return list.get(RandomUtils.nextInt(0, list.size()));
    }

    /**
     * 构建获奖回复信息
     *
     * @param awardName 奖品名称
     * @param awardNum  奖品数量
     * @param userId    获奖者
     * @return 获奖回复信息
     */
    public List<Message> buildAwardMessage(String awardName, Integer awardNum, long userId) {
        List<Message> msgList = new ArrayList<>();
        msgList.add(new At(userId).plus(new PlainText(" 恭喜获得奖品:" + awardName + "*" + awardNum)));
        switch (awardName) {
            case "谢谢惠顾":
                msgList.add(new PlainText(awardName));
                break;
            case "灵石":
                Message message = new PlainText("送" + awardName + awardNum);
                MessageChain chain = MessageUtils.newChain();
                At temp = new At(userId);
                chain = chain.plus(temp);
                message = message.plus(chain);
                msgList.add(message);
                break;
            case "渡厄丹":
                for (int i = 0; i < awardNum; i++) {
                    msgList.add(new PlainText("世界积分兑换1"));
                }
                msgList.add(new PlainText("坊市上架 " + awardName + " 10000 " + awardNum));
                break;
            default:
                msgList.add(new PlainText("坊市上架 " + awardName + " 10000 " + awardNum));
                break;
        }
        return msgList;
    }

    /**
     * 将多条记录处理为List
     *
     * @param src 原始数据
     * @return List
     */
    public List<String> dealStringList(String src) {
        if (src.startsWith("[") && src.endsWith("]")) {
            String content = src.replace("[", "").replace("]", "");
            String[] contents = content.replace("，", ",").split(",");
            return Arrays.stream(contents).collect(Collectors.toList());
        }
        List<String> list = new ArrayList<>();
        list.add(src);
        return list;
    }

    /**
     * 获取活动列表
     *
     * @param valid 是否有效
     * @return 活动列表
     */
    public List<Activity> queryActivityList(boolean valid) {
        return activityDao.queryActivityList(valid);
    }

    /**
     * 根据ID获取活动
     *
     * @param activityId 活动ID
     * @param valid      是否有效
     * @return 活动
     */
    public Activity getById(Long activityId, Boolean valid) {
        return activityDao.getById(activityId, valid);
    }

    /**
     * 根据活动名称获取活动详情
     *
     * @param name 活动名称
     * @return 活动详情
     */
    public String getActivityDetailByName(String name) {
        Activity activity = activityDao.getActivityByName(name, true);
        if (Objects.isNull(activity)) {
            return "没有该活动哦";
        }
        Activity.ActivityRule rule = JSONObject.parseObject(activity.getActivityRule(), Activity.ActivityRule.class);

        StringBuilder builder = new StringBuilder("活动名称:").append(activity.getActivityName());
        builder.append("\n").append("触发指令:").append(this.keywordFormat(rule.getKeywords())
                .stream().reduce((s1, s2) -> s1 + "、" + s2).orElse(null));
        builder.append("\n").append("限制类型:").append(this.transLimitTypeNumber2Str(rule.getLimit()));
        builder.append("\n").append("限制次数:").append(this.getLimitTimes(rule.getLimit()));
        builder.append("\n").append("开始时间:").append(activity.getStartTime());
        builder.append("\n").append("结束时间:").append(activity.getEndTime());

        activityServiceList.stream()
                .filter(service -> service.support(activity.getActivityType()))
                .filter(service -> Objects.nonNull(service.getActivityDetail(activity)))
                .map(service -> service.getActivityDetail(activity)).findFirst().ifPresent(builder::append);
        return builder.toString();
    }

    /**
     * 限制类型数字转化为字符串
     *
     * @param limit 位运算后限制数据
     * @return 限制类型
     */
    public String transLimitTypeNumber2Str(int limit) {
        int limitType = limit >> 27;
        String limitTypeStr = "无";
        switch (limitType) {
            case 0:
                limitTypeStr = "无";
                break;
            case 1:
                limitTypeStr = "日";
                break;
            case 2:
                limitTypeStr = "月";
                break;
            case 3:
                limitTypeStr = "年";
                break;
            case 4:
                limitTypeStr = "终生";
                break;
            default:
                break;
        }
        return limitTypeStr;
    }

    /**
     * 限制类型字符串转化为数字
     *
     * @param limitTypeStr 限制类型
     * @return 限制类型
     */
    public int transLimitTypeStr2Number(String limitTypeStr) {
        int limitType = 0;
        switch (limitTypeStr) {
            case "无":
                limitType = 0;
                break;
            case "日":
                limitType = 1;
                break;
            case "月":
                limitType = 2;
                break;
            case "年":
                limitType = 3;
                break;
            case "终生":
                limitType = 4;
                break;
            default:
                break;
        }
        return limitType;
    }

    /**
     * 获取限制次数
     *
     * @param limit 加密后限制数据
     * @return 限制次数
     */
    public int getLimitTimes(int limit) {
        return limit & ((1 << 27) - 1);
    }

    /**
     * 根据活动类型获取相应service
     *
     * @param activityType 活动类型
     * @return service
     */
    public ActivityService<?, ?> getActivityServiceByActivityType(String activityType) {
        return activityServiceList.stream().filter(act -> act.support(activityType)).findFirst().orElse(null);
    }

    /**
     * 构建公共规则
     *
     * @param limit          个人限制
     * @param totalLimit     总体限制
     * @param keywords       关键词
     * @param successReplies 参与成功回复
     * @param failReplies    参与失败回复
     * @return 公共规则
     */
    public String buildCommonRuleJsonString(int limit, int totalLimit, List<String> keywords,
                                            List<String> successReplies, List<String> failReplies) {
        Activity.ActivityRule ruleParam = new Activity.ActivityRule();
        ruleParam.setLimit(limit);
        ruleParam.setTotalLimit(totalLimit);
        ruleParam.setKeywords(keywords);
        ruleParam.setSuccessReplies(successReplies);
        ruleParam.setFailReplies(failReplies);
        return JSONObject.toJSONString(ruleParam);
    }

    /**
     * 构建具体活动规则
     *
     * @param activityType   活动类型
     * @param limit          个人限制
     * @param totalLimit     总体限制
     * @param keywords       关键词
     * @param successReplies 参与成功回复
     * @param failReplies    参与失败回复
     * @param otherArgs      扩展参数
     * @return 具体某类活动的规则
     */
    public String buildActivityRule(String activityType, int limit, int totalLimit, List<String> keywords,
                                    List<String> successReplies, List<String> failReplies, List<String> otherArgs) {
        String params = this.buildCommonRuleJsonString(limit, totalLimit, keywords, successReplies, failReplies);
        ActivityService<?, ?> activityService = this.getActivityServiceByActivityType(activityType);
        if (Objects.nonNull(activityService)) {
            return JSONObject.toJSONString(activityService.buildRule(params, otherArgs));
        }
        return JSONObject.toJSONString(params);
    }

}
