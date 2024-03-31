package com.whitemagic2014.command.impl.group.astrology.player;

import com.dingCreator.astrology.behavior.JobBehavior;
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
 * @date 2024/2/22
 */
@Command(minArgsSize = 1)
public class ChooseJobCommand extends NoAuthCommand {
    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        PlayerBehavior.getInstance().createPlayer(sender.getId(), sender.getNick(), args.get(0));
        return new PlainText("创建角色成功");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("职业选择", "选择职业");
    }
}
