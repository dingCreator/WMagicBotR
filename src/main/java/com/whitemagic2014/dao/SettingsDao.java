package com.whitemagic2014.dao;

import com.whitemagic2014.pojo.Settings;
import org.springframework.stereotype.Repository;

/**
 * @author ding
 * @date 2023/4/10
 */
@Repository
public interface SettingsDao {
    /**
     * 根据配置名获取配置值
     *
     * @param settingName 配置名
     * @return 配置值
     */
    Settings getSetting(String settingName);

    /**
     * 新增配置
     * @param settings 配置
     * @return 影响行数
     */
    int insertSetting(Settings settings);

    /**
     * 新增配置
     * @param settings 配置
     * @return 影响行数
     */
    int updateSettingById(Settings settings);
}
