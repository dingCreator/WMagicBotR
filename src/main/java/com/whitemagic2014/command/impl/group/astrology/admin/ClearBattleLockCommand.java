package com.whitemagic2014.command.impl.group.astrology.admin;

import com.dingCreator.astrology.behavior.PlayerBehavior;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.AdminCommand;
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
public class ClearBattleLockCommand extends AdminCommand {

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {

        return new PlainText("清理成功");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("清理对战缓存");
    }
}
