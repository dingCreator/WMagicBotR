package com.whitemagic2014.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 配置信息
 *
 * @author ding
 * @date 2023/4/10
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
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
