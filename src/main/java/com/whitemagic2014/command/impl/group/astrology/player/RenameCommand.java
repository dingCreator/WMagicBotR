package com.whitemagic2014.command.impl.group.astrology.player;

import com.dingCreator.astrology.behavior.PlayerBehavior;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.pojo.CommandProperties;

import java.util.ArrayList;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

/**
 * @author ding
 * @date 2024/3/27
 */
@Command(minArgsSize = 1)
public class RenameCommand extends NoAuthCommand {

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        PlayerBehavior.getInstance().rename(sender.getId(), args.get(0));
        return new PlainText("改名成功");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("改名");
    }
}
