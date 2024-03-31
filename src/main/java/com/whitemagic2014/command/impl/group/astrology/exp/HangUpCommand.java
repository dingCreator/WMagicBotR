package com.whitemagic2014.command.impl.group.astrology.exp;

import com.dingCreator.astrology.behavior.ExpBehavior;
import com.dingCreator.astrology.behavior.PlayerBehavior;
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
public class HangUpCommand extends NoAuthCommand {
    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        ExpBehavior.getInstance().hangUp(sender.getId());
        return new PlainText("进入挂机状态");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("开始挂机");
    }
}
