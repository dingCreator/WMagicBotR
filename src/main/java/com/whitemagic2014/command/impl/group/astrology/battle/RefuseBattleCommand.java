package com.whitemagic2014.command.impl.group.astrology.battle;

import com.dingCreator.astrology.behavior.PlayerBehavior;
import com.dingCreator.astrology.cache.PlayerCache;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

import java.util.ArrayList;

/**
 * @author ding
 * @date 2024/3/26
 */
@Command
public class RefuseBattleCommand extends NoAuthCommand {
    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        PlayerBehavior.getInstance().refuseBattle(sender.getId());
        return new PlainText(PlayerCache.getPlayerById(sender.getId()).getPlayer().getName() + "拒绝了对决");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("拒绝对决");
    }
}
