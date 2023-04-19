package com.whitemagic2014.service.impl;

import com.whitemagic2014.dao.SettingsDao;
import com.whitemagic2014.pojo.Settings;
import com.whitemagic2014.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author huangkd
 * @date 2023/4/10
 */
@Service
public class SettingsServiceImpl implements SettingsService {

    @Autowired
    private SettingsDao settingsDao;

    @Override
    public Settings getSetting(String settingName) {
        return settingsDao.getSetting(settingName);
    }

    @Override
    public int insertOrUpdate(Settings settings) {
        Assert.notNull(settings.getSettingName(), "setting name must not be null");
        Settings entity = settingsDao.getSetting(settings.getSettingName());
        if (entity != null) {
            entity.setValue(settings.getValue());
            return settingsDao.updateSettingById(entity);
        } else {
            return settingsDao.insertSetting(settings);
        }
    }
}
