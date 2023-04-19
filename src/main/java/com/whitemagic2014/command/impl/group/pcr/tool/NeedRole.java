package com.whitemagic2014.command.impl.group.pcr.tool;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.command.impl.group.pcr.PcrNoAuthCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Objects;

//@Command
public class NeedRole extends PcrNoAuthCommand {

    @Autowired
    private GlobalParam globalParam;

    @Override
    public CommandProperties properties() {
        return new CommandProperties("挂一下", "挂个");
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        Bot bot = MagicBotR.getBot();
        String msg = Objects.requireNonNull(messageChain.get(OnlineMessageSource.Incoming.FromGroup.Key)).getOriginalMessage().toString();
        bot.getFriend(globalParam.ownerId).sendMessage(new PlainText(sender.getNameCard() + "说：" + msg));
        return new At(sender.getId()).plus("已通知" + globalParam.ownerNick);
    }
}
