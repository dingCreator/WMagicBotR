package com.whitemagic2014.command.impl.group.immortal;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.OwnerCommand;
import com.whitemagic2014.config.properties.CommandRule;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

/**
 * @author huangkd
 * @date 2023/2/13
 */
@Command
public class FastCommand extends OwnerCommand {

    @Autowired
    private GlobalParam globalParam;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        return new PlainText(args.stream().reduce((s1, s2) -> s1 + " " + s2).orElse("要" + globalParam.botNick + "说什么呢"));
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "说", globalParam.botNick + "说");
    }
}
