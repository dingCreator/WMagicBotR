package com.whitemagic2014.command.impl.group.immortal;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.command.impl.group.OwnerCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.ForwardMessage;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

/**
 * @author ding
 * @date 2023/4/17
 */
@Command
public class ShelvesBuyCommand extends OwnerCommand {

    @Autowired
    private GlobalParam globalParam;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (args.size() < 2) {
            return new PlainText("请输入参数 {开始下标} {结束下标}");
        }

        Bot bot = MagicBotR.getBot();
        try {
            int args1 = Integer.parseInt(args.get(0).trim());
            int args2 = Integer.parseInt(args.get(1).trim());

            int startIndex = Math.min(args1, args2);
            int endIndex = Math.max(args1, args2);

            for (int i = startIndex; i <= endIndex; i++) {
                bot.getGroupOrFail(sender.getGroup().getId()).sendMessage(new PlainText("坊市购买" + startIndex));
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new PlainText("购买行为发生错误");
        }
        return new PlainText("开始购买");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "批量购买", globalParam.botNick + "批量购买");
    }
}
