package com.whitemagic2014.command.impl.group.immortal;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.cache.SettingsCache;
import com.whitemagic2014.command.impl.group.EmptyStringCommand;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import com.whitemagic2014.util.CommandThreadPoolUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.ForwardMessage;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Stack;
import java.util.List;

/**
 * @author ding
 * @date 2023/10/11
 */
@Command
public class ShelvesBuyHerbsCommand extends NoAuthCommand {

    @Autowired
    private GlobalParam globalParam;

    @Autowired
    private CommandThreadPoolUtil commandThreadPoolUtil;

    private static final String TASK_KEY = "shelves-buy-herb";
    private static final String UNIT_PRICE_LIMIT_KEYWORD = "修仙_收草单价上限";

    private long groupId;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        this.groupId = sender.getGroup().getId();
        registerRecognize();
        return new PlainText("坊市查看");
    }

    private void registerRecognize() {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, notice, atMe) -> {
            // 非本群组合信息，忽略
            if (groupId != sender.getGroup().getId() || messageChain.stream()
                    .filter(ForwardMessage.class::isInstance).findFirst().orElse(null) == null) {
                return null;
            }
            ForwardMessage forwardMsg = (ForwardMessage) messageChain.stream().filter(ForwardMessage.class::isInstance)
                    .findFirst().orElse(null);
            if (forwardMsg == null) {
                return null;
            }

            Stack<ItemInfo> itemInfoStack = new Stack<>();
            forwardMsg.component6().forEach(node -> {
                String singleMsg = node.component4().toString().trim();
                String[] lines = singleMsg.split("\n");

                int number = 0, count = 0, unitPrice = 0;
                boolean isHerb = false;
                for (String line : lines) {
                    line = line.trim();
                    try {
                        if (line.startsWith("编号")) {
                            number = Integer.parseInt(line.replace("编号：", ""));
                        }
                        if (line.contains("药材")) {
                            isHerb = true;
                        }
                        if (line.startsWith("数量")) {
                            count = Integer.parseInt(line.replace("数量：", ""));
                        }
                        if (line.startsWith("价格")) {
                            unitPrice = Integer.parseInt(line
                                    .replace("价格：", "")
                                    .replace("枚灵石", ""));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }

                if (number != 0 && count != 0 && unitPrice != 0 && isHerb
                        && unitPrice <= SettingsCache.getInstance().getSettingsAsInt(UNIT_PRICE_LIMIT_KEYWORD, 10000)) {
                    itemInfoStack.push(new ItemInfo(number, count, unitPrice));
                }
            });
            if (itemInfoStack.empty()) {
                return null;
            }

            int index = 0;
            List<BuyMsg> buyMsgList = new ArrayList<>();
            while (!itemInfoStack.empty()) {
                ItemInfo itemInfo = itemInfoStack.pop();
                if (CollectionUtils.isEmpty(buyMsgList)) {
                    buyMsgList.add(new BuyMsg(itemInfo.getNumber(), itemInfo.getNumber(), itemInfo.getCount(),
                            itemInfo.getUnitPrice() *  itemInfo.getCount()));
                } else {
                    BuyMsg present = buyMsgList.get(index);
                    if (present.getStartNumber().equals(itemInfo.getNumber() + 1)) {
                        // 连续数字
                        present.setStartNumber(itemInfo.getNumber());
                        present.setMaxCount(Math.max(itemInfo.getCount(), present.getMaxCount()));
                        present.setTotalPrice(present.getTotalPrice() + itemInfo.getCount() * itemInfo.getUnitPrice());
                    } else {
                        // 非连续数字
                        buyMsgList.add(new BuyMsg(itemInfo.getNumber(), itemInfo.getNumber(), itemInfo.getCount(),
                                itemInfo.getUnitPrice() *  itemInfo.getCount()));
                        index++;
                    }
                }
            }

            List<Message> msgList = new ArrayList<Message>();
            for (BuyMsg buyMsg : buyMsgList) {
                if (buyMsg.getStartNumber().equals(buyMsg.getEndNumber())) {
                    msgList.add(new PlainText(String.format("坊市购买 %s %s", buyMsg.getStartNumber(), buyMsg.getMaxCount())));
                } else {
                    msgList.add(new PlainText(String.format("坊市扫货 %s-%s %s %s",
                            buyMsg.getStartNumber(), buyMsg.getEndNumber(), buyMsg.getMaxCount(), buyMsg.getTotalPrice())));
                }
            }
            commandThreadPoolUtil.addGroupTask(msgList, groupId);
            removeTask();
            return new PlainText("开始收草");
        }, TASK_KEY);
    }

    private void removeTask() {
        EmptyStringCommand.removeLogic(TASK_KEY);
    }

    @Data
    @AllArgsConstructor
    private static class ItemInfo {
        private Integer number;
        private Integer count;
        private Integer unitPrice;
    }

    @Data
    @AllArgsConstructor
    private static class BuyMsg {
        private Integer startNumber;
        private Integer endNumber;
        private Integer maxCount;
        private Integer totalPrice;
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botNick + "收草", globalParam.botName + "收草");
    }
}
