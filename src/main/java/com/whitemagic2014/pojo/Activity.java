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
     * 活动名称
     */
    private String activityName;

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
         * 通用
         */
        GENERAL("GENERAL", false),
        /**
         * 抽奖
         */
        LUCKY("LUCKY", true),
        /**
         * 签到
         */
        SIGN("SIGN", true),
        ;
        /**
         * 类型名称
         */
        private final String name;
        /**
         * 是否可创建此类活动（区分是真实活动还是通用规范）
         */
        private final Boolean canCreate;
    }

    @Data
    public static class ActivityRule implements Serializable {
        /**
         * 触发关键词
         */
        protected List<String> keywords;
        /**
         * 限制单人参与次数 高5位-限制类型/其余位-次数限制
         */
        protected Integer limit;
        /**
         * 限制总体参与次数 高5位-限制类型/其余位-次数限制
         */
        protected Integer totalLimit;
        /**
         * 参与成功回复
         */
        protected List<String> successReplies;
        /**
         * 参与失败回复
         */
        protected List<String> failReplies;
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
