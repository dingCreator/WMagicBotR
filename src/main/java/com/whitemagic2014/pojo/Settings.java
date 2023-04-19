package com.whitemagic2014.pojo;

import lombok.Data;

/**
 * 配置信息
 *
 * @author huangkd
 * @date 2023/4/10
 */
@Data
public class Settings {
    /**
     * 自增ID
     */
    private Long id;
    /**
     * 配置名
     */
    private String settingName;
    /**
     * 配置值
     */
    private String value;
}
