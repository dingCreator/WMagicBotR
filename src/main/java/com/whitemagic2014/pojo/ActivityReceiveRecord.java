package com.whitemagic2014.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ding
 * @date 2023/9/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityReceiveRecord {
    /**
     * 主键
     */
    private Long id;
    /**
     * 领取时间 精确到日
     */
    private String date;
    /**
     * 领取人QQ号
     */
    private Long receiverId;
    /**
     * 活动ID
     */
    private Long activityId;

    public ActivityReceiveRecord(String date, Long receiverId, Long activityId) {
        this.date = date;
        this.receiverId = receiverId;
        this.activityId = activityId;
    }
}