package com.whitemagic2014.service;

import com.whitemagic2014.pojo.ActivityReceiveRecord;

/**
 * @author ding
 * @date 2023/9/23
 */
public interface ActivityReceiveRecordService {

    /**
     * 获取领取记录
     *
     * @param receiverId 领取人QQ号
     * @param date       领取时间
     * @param activityId 活动ID
     * @return 领取记录 若无返回null
     */
    ActivityReceiveRecord getOne(Long receiverId, String date, Long activityId);

    /**
     * 插入一条领取记录
     *
     * @param activityReceiveRecord 领取记录
     * @return 影响行数
     */
    int insert(ActivityReceiveRecord activityReceiveRecord);

    /**
     * 查询某天的领取记录条数
     *
     * @param date 日期 精确到日
     * @return 领取记录数
     */
    int selectCount(String date);

    /**
     * 查询某天的领取记录条数
     *
     * @param date       日期 精确到日
     * @param receiverId 获奖者ID
     * @param activityId 活动ID
     * @return 领取记录数
     */
    int selectCount(String date, Long receiverId, Long activityId);
}
