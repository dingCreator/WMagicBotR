package com.whitemagic2014.service;

import com.whitemagic2014.pojo.Activity;

import net.mamoe.mirai.message.data.Message;

import java.util.List;

/**
 * @author ding
 * @date 2023/10/23
 */
public interface ActivityService<Rule extends Activity.ActivityAwardRule> {

    /**
     * 模板是否支持处理此类活动
     *
     * @param type 活动类型
     * @return 是否支持
     * @see com.whitemagic2014.pojo.Activity.ActivityType
     */
    boolean support(String type);

    /**
     * 参加活动
     *
     * @param activityId 活动ID
     * @param userId     参与者ID
     * @return 是否参加成功
     */
    List<Message> joinActivity(long activityId, long userId);

    /**
     * 根据活动ID编辑活动
     *
     * @param activity 活动
     * @return 编辑是否成功
     */
    boolean updateById(Activity activity);

    /**
     * 构建活动规则
     *
     * @param ruleJson 规则json
     * @return 构建结果 null-构建失败
     */
    Activity.ActivityRule buildRule(String ruleJson);

    /**
     * 构建奖品规则
     *
     * @param awardRuleJson 奖品规则json
     * @return 构建结果 null-构建失败
     */
    List<Rule> buildAwardRule(String awardRuleJson);

    /**
     * 查询活动列表
     * 先暂时不分页，暂时不会很多
     *
     * @return 活动列表
     */
    List<Activity> queryActivity();
}
