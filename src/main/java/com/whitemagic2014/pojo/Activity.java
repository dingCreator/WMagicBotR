package com.whitemagic2014.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * @author ding
 * @date 2023/10/23
 */
@Data
public class Activity {
    /**
     * 活动ID
     */
    private Long id;

    /**
     * 活动类型
     */
    private String activityType;

    /**
     * 活动规则
     */
    private String activityRule;

    /**
     * 奖品规则
     */
    private String awardRule;

    /**
     * 活动开始时间
     */
    private String startTime;

    /**
     * 活动结束时间
     */
    private String endTime;

    /**
     * 是否启用活动
     */
    private Integer enabled;

    @Getter
    @AllArgsConstructor
    public enum ActivityType {
        /**
         * 抽奖
         */
        LUCKY("LUCKY"),
        /**
         * 签到
         */
        SIGN("SIGN"),
        ;
        private final String name;
    }

    @Data
    public static class ActivityRule implements Serializable {
        /**
         * 触发关键词
         */
        protected List<String> keyword;
        /**
         * 限制参与 高5位-限制类型/其余位-次数限制
         */
        protected Integer limit;
        /**
         * 参与成功回复
         */
        protected List<String> successReply;
        /**
         * 参与失败回复
         */
        protected List<String> failReply;
    }

    @Data
    public static class ActivityAwardRule implements Serializable {
        /**
         * 奖品名称
         */
        protected String awardName;
        /**
         * 奖品数量
         */
        protected Integer awardNum;
    }
}
