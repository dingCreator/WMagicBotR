package com.whitemagic2014.command.impl.group.immortal.util.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author huangkd
 * @date 2023/5/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FarmDTO implements Serializable {
    public static final String SETTINGS_KEYWORD = "修仙_农场配置";
    /**
     * 群号
     */
    private Long groupId;
    /**
     * 群名字
     */
    private String groupName;
    /**
     * 境界
     */
    private Integer rank;
    /**
     * 境界中文名
     */
    private String rankName;
}
