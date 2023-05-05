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
 * @author huangkd
 * @date 2023/3/19
 */
//@Command
public class ShelvesCommand extends OwnerCommand {

    private static final int DEFAULT_PRICE = 10000;
    /**
     * 是否处于上架模式
     */
    private static volatile boolean upper = false;
    /**
     * 是否处于下架模式
     */
    private static volatile boolean down = false;

    @Autowired
    private GlobalParam globalParam;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (CollectionUtils.isEmpty(args)) {
            return new PlainText("请输入参数 上架/下架 [价格/开始下标] [种类名/结束下标]");
        }

        Bot bot = MagicBotR.getBot();
        if ("下架".equals(args.get(0).trim())) {
            int startIndex = 100;
            if (args.size() > 1) {
                try {
                    startIndex = Math.min(Integer.parseInt(args.get(1).trim()), startIndex);
                    int endIndex = 0;
                    if (args.size() > 2) {
                        endIndex = Math.max(Integer.parseInt(args.get(2).trim()), endIndex);
                    }
                    for (int i = startIndex; i > endIndex; i--) {
                        bot.getGroupOrFail(sender.getGroup().getId()).sendMessage("坊市下架" + i);
                        Thread.sleep(500);
                    }
                } catch (Exception e) {
                    System.out.println("数字解析失败");
                    e.printStackTrace();
                }
                return new PlainText("下架完毕");
            }

            addDownEmptyStringLogic(sender.getGroup().getId());
            down = true;
            bot.getGroupOrFail(sender.getGroup().getId()).sendMessage(new PlainText("坊市查看"));
        } else if ("上架".equals(args.get(0).trim())) {
            int price = DEFAULT_PRICE;
            if (args.size() > 1) {
                try {
                    price = Integer.parseInt(args.get(1).trim());
                } catch (Exception e) {
                    System.out.println("数字解析失败");
                    e.printStackTrace();
                }
            }

            String keyword = null;
            if (args.size() > 2) {
                keyword = args.get(2).trim();
            }
            upper = true;
            addUpperEmptyStringLogic(price, keyword, sender.getGroup().getId());
            bot.getGroupOrFail(sender.getGroup().getId()).sendMessage(new PlainText("我的背包"));
        }
        return new PlainText("开始处理");
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
                                Thread.sleep(500);
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

    private void addDownEmptyStringLogic(long groupId) {
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
            if (down) {
                down = false;
                try {
                    int count = Integer.parseInt(forwardMsg.component5().split(" ")[1].trim());
                    if (count > 0) {
                        for (int i = count; i > 0; i--) {
                            bot.getGroupOrFail(groupId).sendMessage("坊市下架" + i);
                            Thread.sleep(500);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return new PlainText("下架完毕");
            }
            return null;
        }, "ShelvesDownCommand" + groupId);
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "一键", globalParam.botNick + "一键");
    }
}
