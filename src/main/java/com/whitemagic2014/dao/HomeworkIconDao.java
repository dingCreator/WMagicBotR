package com.whitemagic2014.dao;

import com.whitemagic2014.pojo.HomeworkIcon;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeworkIconDao {

    int insert(HomeworkIcon icon);

    List<HomeworkIcon> list();

    HomeworkIcon getById(@Param("id") Integer id);

    HomeworkIcon getByName(@Param("iconName") String iconName);

    int deleteAll();

    int updateById(@Param("id") Integer id, @Param("iconUrl") String iconUrl);
}
