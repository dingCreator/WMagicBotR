package com.whitemagic2014.dao;

import com.whitemagic2014.pojo.ImmortalFarmRecord;
import org.springframework.stereotype.Repository;

/**
 * @author ding
 * @date 2023/12/18
 */
@Repository
public interface ImmortalFarmRecordDao {

    void createRecord(ImmortalFarmRecord record);
}
