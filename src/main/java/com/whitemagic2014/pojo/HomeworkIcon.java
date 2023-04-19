package com.whitemagic2014.pojo;

import lombok.Data;

@Data
public class HomeworkIcon {
    /**
     * ID
     */
    private Integer id;
    /**
     * icon地址
     */
    private String iconUrl;
    /**
     * icon名称
     */
    private String iconName;
    /**
     * icon在作业网服务器中的ID
     */
    private Integer serverId;
    /**
     * 作业网服务器类型 0-国服 1-台服
     */
    private Integer serverType;
}
