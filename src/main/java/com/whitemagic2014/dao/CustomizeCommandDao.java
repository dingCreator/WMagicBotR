package com.whitemagic2014.dao;

import com.whitemagic2014.pojo.CustomizeCommandText;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomizeCommandDao {
    int insert(CustomizeCommandText command);

}
