package com.whitemagic2014.command.impl.group.immortal;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.command.impl.group.AdminCommand;
import com.whitemagic2014.command.impl.group.EmptyStringCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;

import com.whitemagic2014.util.CommandThreadPoolUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.ForwardMessage;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ding
 * @date 2023/9/18
 */
@Command
public class BatchSendCommand extends AdminCommand {

    @Autowired
    private GlobalParam globalParam;

    @Autowired
    private CommandThreadPoolUtil commandThreadPoolUtil;

    private Long receiverId;
    private Long groupId;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        At at = (At) messageChain.stream().filter(At.class::isInstance).findFirst().orElse(null);
        if (at == null) {
            return new PlainText("请@送礼对象");
        }
        receiverId = at.getTarget();
        groupId = sender.getGroup().getId();
        registerRecognizePresent();
        return new PlainText("我的背包");
    }

    private void registerRecognizePresent() {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, text, atMe) -> {
            if (groupId != sender.getGroup().getId() || messageChain.stream()
                    .filter(ForwardMessage.class::isInstance).findFirst().orElse(null) == null) {
                return null;
            }
            ForwardMessage forwardMsg = (ForwardMessage) messageChain.stream().filter(ForwardMessage.class::isInstance)
                    .findFirst().orElse(null);
            if (forwardMsg == null) {
                return null;
            }
            recognizePresent(forwardMsg);
            return new PlainText("开始送礼物");
        }, "batch-send-present");
    }

    private synchronized void recognizePresent(ForwardMessage forwardMsg) {
        AtomicBoolean start = new AtomicBoolean(false);
        List<Message> msgList = new ArrayList<Message>();
        forwardMsg.component6().forEach(node -> {
            String singleMsg = node.component4().toString();
            if (singleMsg.startsWith("☆------")) {
                start.set(singleMsg.contains("礼物"));
            }
            if (!start.get()) {
                return;
            }
            String[] info = singleMsg.split("\n");
            String name = "";
            int effect = 0, num = 0;
            try {
                for (String singleInfo : info) {
                    singleInfo = singleInfo.trim();
                    if (singleInfo.startsWith("名字")) {
                        name = singleInfo.replace("名字：", "");
                    } else if (singleInfo.startsWith("效果")) {
                        effect = Integer.parseInt(singleInfo.replace("效果：", "").replace("好感", ""));
                    } else if (singleInfo.startsWith("拥有数量")) {
                        num = Integer.parseInt(singleInfo.replace("拥有数量：", ""));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            if (effect < 0 || "ibanana R18s Pro Max".equalsIgnoreCase(name)) {
                return;
            }

            for (int i = 0; i < num; i++) {
                msgList.add(new PlainText("送礼物" + name).plus(makeAt(receiverId)));
            }
        });
        if (!CollectionUtils.isEmpty(msgList)) {
            commandThreadPoolUtil.addGroupTask(msgList, groupId);
        }
        cancelRegister();
    }

    private void cancelRegister() {
        EmptyStringCommand.removeLogic("batch-send-present");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "批量送礼物", globalParam.botNick + "批量送礼物");
    }
}
