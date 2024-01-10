package com.whitemagic2014.dao;

import com.whitemagic2014.pojo.Activity;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author ding
 * @date 2023/10/23
 */
@Repository
public interface ActivityDao {
    /**
     * 新增活动
     *
     * @param activity 活动pojo
     */
    void createActivity(Activity activity);

    /**
     * 根据活动ID编辑活动
     *
     * @param activity 活动
     */
    void updateById(Activity activity);

    /**
     * 根据活动ID删除活动
     *
     * @param activityId 活动ID
     */
    void deleteById(long activityId);

    /**
     * 根据ID获取活动详情
     *
     * @param activityId 活动ID
     * @param valid 是否有效
     * @return 活动详情
     */
    Activity getById(@Param("activityId") Long activityId, @Param("valid") Boolean valid);

    /**
     * 查询活动列表
     * 先暂时不分页，暂时不会很多
     *
     * @param valid 是否有效
     * @return 活动列表
     */
    List<Activity> queryActivityList(@Param("valid") Boolean valid);

    /**
     * 根据活动名称获取活动详情
     *
     * @param name 活动名称
     * @param valid 是否可用
     * @return 活动列表
     */
    Activity getActivityByName(@Param("name") String name, @Param("valid") Boolean valid);

    /**
     * 查询最新ID
     * @return ID
     */
    Long getLastActivityId();
}
