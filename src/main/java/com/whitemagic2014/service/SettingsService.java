package com.whitemagic2014.service;

import com.whitemagic2014.pojo.Settings;

/**
 * @author huangkd
 * @date 2023/4/10
 */
public interface SettingsService {

    /**
     * 根据配置名获取配置值
     *
     * @param settingName 配置名
     * @return 配置值
     */
    Settings getSetting(String settingName);

    /**
     * 存在则修改配置，不存在则新增配置
     *
     * @param settings 配置信息
     * @return 影响行数
     */
    int insertOrUpdate(Settings settings);
}
