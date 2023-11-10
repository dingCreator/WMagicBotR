package com.whitemagic2014.command.impl.group.currency;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.OwnerCommand;
import com.whitemagic2014.util.CommandThreadPoolUtil;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * @author ding
 * @date 2023/2/13
 */
@Command
public class FastCommand extends OwnerCommand {

    @Autowired
    private GlobalParam globalParam;

    @Autowired
    private CommandThreadPoolUtil commandThreadPoolUtil;

    private static final String TIMES_KEYWORD = "次";
    private static final String AT_KEYWORD = "@";

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        String emptyCommandReply = "要" + globalParam.botNick + "说什么呢";
        if (CollectionUtils.isEmpty(args)) {
            return new PlainText(emptyCommandReply);
        }

        List<String> tmpList = new ArrayList<>(args.size());
        tmpList.addAll(args);
        int times = 1, maxTimes = 100;
        if (tmpList.get(tmpList.size() - 1).endsWith(TIMES_KEYWORD)) {
            String timesStr = tmpList.remove(tmpList.size() - 1).replace(TIMES_KEYWORD, "").trim();
            try {
                times = Math.max(Integer.parseInt(timesStr), times);
                times = Math.min(times, maxTimes);
            } catch (Exception e) {
                return new PlainText(args.stream().reduce((s1, s2) -> s1 + " " + s2).orElse(emptyCommandReply));
            }
        }

        List<Message> msgList = tmpList.stream().map(arg -> {
            if (arg.contains(AT_KEYWORD)) {
                long atId;
                try {
                    atId = Long.parseLong(arg.substring(arg.indexOf(AT_KEYWORD) + 1));
                } catch (Exception e) {
                    e.printStackTrace();
                    return new PlainText(arg);
                }
                MessageChain chain = makeAt(atId);
                return new PlainText(arg.substring(0, arg.indexOf(AT_KEYWORD))).plus(chain);
            }
            return new PlainText(arg);
        }).collect(Collectors.toList());

        Message reply = msgList.stream().reduce((msg1, msg2) -> {
            msg1 = msg1.plus(new PlainText(" ")).plus(msg2);
            return msg1;
        }).orElse(new PlainText(emptyCommandReply));

        if (times > 1) {
            List<Message> messageList = new ArrayList<Message>(times - 1);
            for (int i = 0; i < times - 1; i++) {
                messageList.add(reply);
            }
            commandThreadPoolUtil.addGroupTask(messageList, sender.getGroup().getId());
        }
        return reply;
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "说", globalParam.botNick + "说");
    }
}
