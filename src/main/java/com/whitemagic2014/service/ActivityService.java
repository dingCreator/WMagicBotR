package com.whitemagic2014.service;

import com.whitemagic2014.pojo.Activity;

import net.mamoe.mirai.message.data.Message;

import java.util.List;

/**
 * @author ding
 * @date 2023/10/23
 */
public interface ActivityService<Rule extends Activity.ActivityRule, AwardRule extends Activity.ActivityAwardRule> {

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
     * @param rule 公共规则
     * @param args 扩展参数
     * @return 构建结果 null-构建失败
     */
    Rule buildRule(Activity.ActivityRule rule, List<String> args);

    /**
     * 构建奖品规则
     *
     * @param awardRuleJson 奖品规则json
     * @return 构建结果 null-构建失败
     */
    List<AwardRule> buildAwardRule(String awardRuleJson);

    /**
     * 根据活动名称查询活动详情
     *
     * @param activity
     * @return 活动详情
     */
    String getActivityDetail(Activity activity);

    /**
     * 通用方法，由通用活动实现类实现，具体活动实现类不需要实现此方法
     *
     * @param id 活动ID
     * @return 具体活动
     */
    default Activity getById(Long id) {
        throw new UnsupportedOperationException("不支持的操作");
    }

    /**
     * 通用方法，由通用活动实现类实现，具体活动实现类不需要实现此方法
     *
     * @param id    活动ID
     * @param valid 是否有效
     * @return 具体活动
     */
    default Activity getById(Long id, Boolean valid) {
        throw new UnsupportedOperationException("不支持的操作");
    }

    /**
     * 通用方法，由通用活动实现类实现，具体活动实现类不需要实现此方法
     *
     * @param id 活动ID
     */
    default void deleteById(Long id) {
        throw new UnsupportedOperationException("不支持的操作");
    }

    /**
     * 通用方法，由通用活动实现类实现，具体活动实现类不需要实现此方法
     *
     * @return 活动列表
     */
    default List<Activity> queryActivityList() {
        throw new UnsupportedOperationException("不支持的操作");
    }

    /**
     * 通用方法，由通用活动实现类实现，具体活动实现类不需要实现此方法
     *
     * @param valid 是否有效
     * @return 活动列表
     */
    default List<Activity> queryActivityList(Boolean valid) {
        throw new UnsupportedOperationException("不支持的操作");
    }
}
