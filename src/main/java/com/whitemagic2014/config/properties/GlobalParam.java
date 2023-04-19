package com.whitemagic2014.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 全局变量
 *
 * @author huangkd
 * @date 2023/2/21
 */
@Component
public class GlobalParam {

    @Value("${global.ownerId:}")
    public Long ownerId;

    @Value("${global.ownerNick:}")
    public String ownerNick;

    @Value("${global.botId:}")
    public Long botId;

    @Value("${global.botName:}")
    public String botName;

    @Value("${global.botNick:}")
    public String botNick;
}
