package com.whitemagic2014.service.impl;

import com.whitemagic2014.dao.ImmortalFarmRecordDao;
import com.whitemagic2014.pojo.ImmortalFarmRecord;
import com.whitemagic2014.service.ImmortalFarmRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author ding
 * @date 2023/12/19
 */
@Service
public class ImmortalFarmRecordServiceImpl implements ImmortalFarmRecordService {

    @Autowired
    private ImmortalFarmRecordDao immortalFarmRecordDao;

    @Override
    public void createRecord(ImmortalFarmRecord record) {
        immortalFarmRecordDao.createRecord(record);
    }
}
