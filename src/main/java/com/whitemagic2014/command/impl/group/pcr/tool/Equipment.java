package com.whitemagic2014.command.impl.group.pcr.tool;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.ArrayList;

@Command
public class Equipment extends NoAuthCommand {

    @Override
    public CommandProperties properties() {
        return new CommandProperties("孤儿装");
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        At at = new At(sender.getId());
        return at.plus("https://www.bilibili.com/read/cv21697824");
    }
}
