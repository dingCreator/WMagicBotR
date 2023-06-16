package com.whitemagic2014.cache;

import com.whitemagic2014.pojo.Settings;
import com.whitemagic2014.service.SettingsService;
import com.whitemagic2014.util.spring.SpringApplicationContextUtil;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 统一管理配置
 *
 * @author ding
 * @date 2023/4/10
 */
public class SettingsCache {

    private static SettingsCache SETTINGS = null;

    private final Map<String, String> settingsMap;

    private SettingsCache() {
        settingsMap = new ConcurrentHashMap<>();
    }

    /**
     * 获取单例
     *
     * @return 单例
     */
    public static SettingsCache getInstance() {
        if (SETTINGS == null) {
            synchronized (SettingsCache.class) {
                if (SETTINGS == null) {
                    SETTINGS = new SettingsCache();
                }
            }
        }
        return SETTINGS;
    }

    /**
     * 通过配置名获取配置
     *
     * @param settingName 配置名
     * @return 配置值
     */
    public String getSettings(String settingName) {
        String val;
        if ((val = settingsMap.get(settingName)) == null) {
            val = getFromDb(settingName);
            if (val != null) {
                settingsMap.put(settingName, val);
            }
        }
        return val;
    }

    /**
     * 带类型转换的获取配置
     *
     * @param settingName 配置名
     * @param <T>         配置类型
     * @return 配置值
     */
    public <T> T getSettings(String settingName, Function<String, T> function) {
        return function.apply(getSettings(settingName));
    }

    public int getSettingsAsInt(String settingName, int defaultVal) {
        String str = SettingsCache.getInstance().getSettings(settingName);
        if (!StringUtils.isEmpty(str)) {
            try {
                return Integer.parseInt(str);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return defaultVal;
    }

    public boolean getSettingsAsBoolean(String settingName, boolean defaultVal) {
        return getSettings(settingName, val -> {
            if (StringUtils.isEmpty(val)) {
                return defaultVal;
            }
            return Boolean.valueOf(val);
        });
    }

    /**
     * 从DB获取配置信息
     *
     * @param settingName 配置名
     * @return 配置值
     */
    private String getFromDb(String settingName) {
        SettingsService settingsService = SpringApplicationContextUtil.getBean(SettingsService.class);
        Settings dbVal = settingsService.getSetting(settingName);
        return dbVal == null ? null : dbVal.getValue();
    }

    /**
     * 设置配置
     * @param settingName 配置名
     * @param val 配置值
     * @param <T> 配置值类型
     */
    public <T> void setSettings(String settingName, T val) {
        SettingsService settingsService = SpringApplicationContextUtil.getBean(SettingsService.class);
        Settings settings = new Settings()
                .setSettingName(settingName)
                .setValue(val.toString());

        // 清除缓存
        settingsMap.remove(settingName);
        // 更新DB
        settingsService.insertOrUpdate(settings);
        // 延时双删
        settingsMap.remove(settingName);
    }
}
