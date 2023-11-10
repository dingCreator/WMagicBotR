package com.whitemagic2014.dao;

import com.whitemagic2014.pojo.ActivityReceiveRecord;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * @author ding
 * @date 2023/9/24
 */
@Repository
public interface ActivityReceiveRecordDao {

    /**
     * 获取领取记录
     *
     * @param receiverId 领取人QQ号
     * @param date       领取时间
     * @param activityId 活动ID
     * @return 领取记录 若无返回null
     */
    ActivityReceiveRecord getOne(@Param("receiverId") Long receiverId, @Param("date") String date, @Param("activityId") Long activityId);

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
     * @param date       日期 精确到日
     * @param receiverId 领取人QQ
     * @return 领取记录数
     */
    int selectCount(@Param("date") String date, @Param("receiverId") Long receiverId, @Param("activityId") Long activityId);
}
