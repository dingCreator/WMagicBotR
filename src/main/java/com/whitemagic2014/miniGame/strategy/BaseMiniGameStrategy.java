package com.whitemagic2014.miniGame.strategy;

import com.whitemagic2014.miniGame.MiniGameUtil;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;

/**
 * 游戏规则接口
 *
 * @author huangkd
 */
public interface BaseMiniGameStrategy {

    /**
     * 玩家是否正在进行此游戏
     *
     * @param id QQ号
     * @return 是/否
     */
    boolean support(Long id);

    /**
     * 游玩游戏
     *
     * @param msg     玩家输入的信息
     * @param sender  发送者
     * @param subject subject
     * @return 游戏状态
     */
    MiniGameUtil.MiniGameResultEnum play(String msg, Member sender, Group subject);
}
