package com.whitemagic2014.dao;

import com.whitemagic2014.pojo.Activity;
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
     * @return 活动详情
     */
    Activity getById(long activityId);

    /**
     * 查询活动列表
     * 先暂时不分页，暂时不会很多
     *
     * @return 活动列表
     */
    List<Activity> queryActivityList();

    /**
     * 查询活动列表
     * 先暂时不分页，暂时不会很多
     *
     * @return 活动列表
     */
    List<Activity> queryActivity(String type);
}
