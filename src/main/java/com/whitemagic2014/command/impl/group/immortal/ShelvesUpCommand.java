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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一键上下架
 * 此处严厉批判修仙不人道的上下架系统
 *
 * @author ding
 * @date 2023/3/19
 */
@Command
public class ShelvesUpCommand extends OwnerCommand {

    private static final int DEFAULT_PRICE = 10000;
    /**
     * 是否处于上架模式
     */
    private static volatile boolean upper = false;

    @Autowired
    private GlobalParam globalParam;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        Bot bot = MagicBotR.getBot();
        int price = DEFAULT_PRICE;
        if (args.size() > 0) {
            try {
                price = Integer.parseInt(args.get(0).trim());
            } catch (Exception e) {
                System.out.println("数字解析失败");
                e.printStackTrace();
            }
        }

        String keyword = null;
        if (args.size() > 1) {
            keyword = args.get(1).trim();
        }
        upper = true;
        addUpperEmptyStringLogic(price, keyword, sender.getGroup().getId());
        bot.getGroupOrFail(sender.getGroup().getId()).sendMessage(new PlainText("我的背包"));
        return new PlainText("开始上架");
    }

    private void addUpperEmptyStringLogic(int price, String keyword, long groupId) {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, text) -> {
            if (groupId != sender.getGroup().getId() || messageChain.stream()
                    .filter(ForwardMessage.class::isInstance).findFirst().orElse(null) == null) {
                return null;
            }
            Bot bot = MagicBotR.getBot();

            ForwardMessage forwardMsg = (ForwardMessage) messageChain.stream().filter(ForwardMessage.class::isInstance)
                    .findFirst().orElse(null);
            if (forwardMsg == null) {
                return null;
            }
            if (upper) {
                upper = false;
                final AtomicBoolean start = new AtomicBoolean(keyword == null);
                forwardMsg.component6().forEach(node -> {
                    String singleMsg = node.component4().toString();

                    if (keyword != null) {
                        if (singleMsg.startsWith("☆------")) {
                            start.set(singleMsg.contains(keyword));
                        }
                    }

                    if (!start.get()) {
                        return;
                    }

                    String[] info = singleMsg.split("\n");
                    String name = null;
                    int count = 0;

                    for (String i : info) {
                        i = i.trim();
                        if (i.startsWith("名字：")) {
                            name = i.replace("名字：", "").trim();
                        } else if (i.contains("功法") || i.contains("神通")) {
                            name = i.substring(i.indexOf("-") + 1, i.indexOf("："));
                        } else if (i.startsWith("拥有数量：")) {
                            try {
                                count = Integer.parseInt(i.replace("拥有数量：", "").trim());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (!StringUtils.isEmpty(name) && count > 0) {
                        for (int i = 0; i < count; i++) {
                            bot.getGroupOrFail(groupId).sendMessage(new PlainText("坊市上架" + name + " " + price));
                            try {
                                Thread.sleep(10000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                return new PlainText("上架完毕");
            }
            return null;
        }, "ShelvesUpCommand" + groupId, true);
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "批量上架", globalParam.botNick + "批量上架");
    }
}
