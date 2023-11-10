package com.whitemagic2014.service.impl;

import com.whitemagic2014.dao.ActivityReceiveRecordDao;
import com.whitemagic2014.pojo.ActivityReceiveRecord;
import com.whitemagic2014.service.ActivityReceiveRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author ding
 * @date 2023/9/24
 */
@Service
public class ActivityReceiveRecordServiceImpl implements ActivityReceiveRecordService {

    @Autowired
    private ActivityReceiveRecordDao activityReceiveRecordDao;

    @Override
    public ActivityReceiveRecord getOne(Long receiverId, String date, Long activityId) {
        return activityReceiveRecordDao.getOne(receiverId, date, activityId);
    }

    @Override
    public int insert(ActivityReceiveRecord activityReceiveRecord) {
        return activityReceiveRecordDao.insert(activityReceiveRecord);
    }

    @Override
    public int selectCount(String date) {
        return selectCount(date, null, null);
    }

    @Override
    public int selectCount(String date, Long receiverId, Long activityId) {
        return activityReceiveRecordDao.selectCount(date, receiverId, activityId);
    }
}
