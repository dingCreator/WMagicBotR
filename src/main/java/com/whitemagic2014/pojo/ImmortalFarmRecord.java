package com.whitemagic2014.pojo;

import lombok.Data;

/**
 * @author ding
 * @date 2023/12/18
 */
@Data
public class ImmortalFarmRecord {
    /**
     * ID
     */
    private Integer id;
    /**
     * 群号
     */
    private Long groupId;
    /**
     * 积分
     */
    private Integer point;
    /**
     * 灵石
     */
    private Integer money;
    /**
     * 时间
     */
    private String date;
    /**
     * 农场主QQ号
     */
    private Long ownerId;
}
