package com.whitemagic2014.command.impl.group.astrology.admin;

import com.dingCreator.astrology.behavior.ExpBehavior;
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
 * @date 2024/3/29
 */
@Command(minArgsSize = 2)
public class GiveExpCommand extends AdminCommand {

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        ExpBehavior.getInstance().getExp(Long.parseLong(args.get(0)), Long.parseLong(args.get(1)));
        return new PlainText("增加成功");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("增加经验值");
    }
}
