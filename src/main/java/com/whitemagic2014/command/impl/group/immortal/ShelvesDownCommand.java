package com.whitemagic2014.command.impl.group.immortal;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.command.impl.group.EmptyStringCommand;
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
public class ShelvesDownCommand extends OwnerCommand {

    @Autowired
    private GlobalParam globalParam;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (args.size() < 1) {
            return new PlainText("请输入参数 {开始下标} [结束下标]");
        }
        int startIndex = 200;
        int endIndex = 1;

        Bot bot = MagicBotR.getBot();
        try {
            // 下标从大至小
            int right = Integer.parseInt(args.get(0).trim());
            if (args.size() == 2) {
                int args1 = Integer.parseInt(args.get(0).trim());
                int args2 = Integer.parseInt(args.get(1).trim());

                right = Math.max(args1, args2);
                int left = Math.min(args1, args2);

                endIndex = Math.max(left, endIndex);
            }
            startIndex = Math.min(right, startIndex);

            for (int i = startIndex; i >= endIndex; i--) {
                bot.getGroupOrFail(sender.getGroup().getId()).sendMessage("坊市下架" + i);
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            System.out.println("数字解析失败");
            e.printStackTrace();
        }
        return new PlainText("下架完毕");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "批量下架", globalParam.botNick + "批量下架");
    }
}
