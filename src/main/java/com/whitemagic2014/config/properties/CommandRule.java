package com.whitemagic2014.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bot指令规则
 *
 * @author ding
 * @date 2023/2/1
 */
@Component
public class CommandRule {

    /**
     * 是否开启群白名单
     */
    @Value("${commandRule.enabledGroupWhitelist:false}")
    public boolean enabledGroupWhitelist;
    /**
     * 群白名单列表
     */
    @Value("#{'${commandRule.groupWhitelist:}'.split(',')}")
    public List<Long> groupWhitelist;
    /**
     * 是否开启群黑名单
     */
    @Value("${commandRule.enabledGroupBlacklist:false}")
    public boolean enabledGroupBlacklist;
    /**
     * 群黑名单列表
     */
    @Value("#{'${commandRule.groupBlacklist:}'.split(',')}")
    public List<Long> groupBlacklist;
    /**
     * 是否开启机器人账号白名单
     */
    @Value("${commandRule.enabledBotAccountWhitelist:false}")
    public boolean enabledBotAccountWhitelist;
    /**
     * 机器人账号白名单列表
     */
    @Value("#{'${commandRule.botAccountWhitelist:}'.split(',')}")
    public List<Long> botAccountWhitelist;
    /**
     * 是否开启机器人账号黑名单
     */
    @Value("${commandRule.enabledBotAccountBlacklist:false}")
    public boolean enabledBotAccountBlacklist;
    /**
     * 机器人账号黑名单列表
     */
    @Value("#{'${commandRule.botAccountBlacklist:}'.split(',')}")
    public List<Long> botAccountBlacklist;
}
