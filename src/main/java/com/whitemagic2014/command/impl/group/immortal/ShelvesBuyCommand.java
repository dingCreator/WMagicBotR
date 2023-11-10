package com.whitemagic2014.command.impl.group.immortal;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.command.impl.group.EmptyStringCommand;
import com.whitemagic2014.command.impl.group.OwnerCommand;
import com.whitemagic2014.util.CommandThreadPoolUtil;
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.List;

/**
 * @author ding
 * @date 2023/4/17
 */
@Command
public class ShelvesBuyCommand extends OwnerCommand {

    @Autowired
    private GlobalParam globalParam;

    @Autowired
    private CommandThreadPoolUtil threadPoolUtil;

    private static final String KEY = "shelvesBuy";

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (args.size() < 2) {
            return new PlainText("请输入参数 {开始下标} {结束下标} [购买数量]");
        }

        Bot bot = MagicBotR.getBot();
        try {
            int args1 = Integer.parseInt(args.get(0).trim());
            int args2 = Integer.parseInt(args.get(1).trim());

            int startIndex = Math.min(args1, args2);
            int endIndex = Math.max(args1, args2);

            int maxCount = 1;
            if (args.size() >= 3) {
                try {
                    maxCount = Integer.parseInt(args.get(2));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            registerShelvesBuyCommand(sender.getGroup().getId(), startIndex, endIndex, maxCount);
        } catch (Exception e) {
            e.printStackTrace();
            return new PlainText("购买行为发生错误");
        }
        return new PlainText("坊市查看");
    }

    private void registerShelvesBuyCommand(long groupId, int startIndex, int endIndex, int maxCount) {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, text, atMe) -> {
            ForwardMessage forwardMsg = (ForwardMessage) messageChain.stream().filter(ForwardMessage.class::isInstance)
                    .findFirst().orElse(null);
            if (groupId != sender.getGroup().getId() || forwardMsg == null) {
                return null;
            }

            AtomicInteger atomicInteger = new AtomicInteger(0);
            List<Message> msgList = forwardMsg.component6().stream().peek(node -> atomicInteger.addAndGet(1))
                    .filter(node -> atomicInteger.get() >= startIndex && atomicInteger.get() <= endIndex)
                    .map(node -> {
                        String singleMsg = node.component4().toString();
                        String[] lines = singleMsg.split("\n");

                        for (String line : lines) {
                            if (line.startsWith("数量")) {
                                String countStr = line.replace("数量：", "").trim();
                                int count;
                                try {
                                    count = Integer.parseInt(countStr);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return null;
                                }
                                return new PlainText("坊市购买 " + startIndex + " " + Math.min(count, maxCount));
                            }
                        }
                        return null;
                    }).filter(Objects::nonNull).collect(Collectors.toList());

            threadPoolUtil.addGroupTask(msgList, groupId);
            EmptyStringCommand.removeLogic(KEY);
            return new PlainText("开始购买");
        }, KEY, true);
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "批量购买", globalParam.botNick + "批量购买");
    }
}
