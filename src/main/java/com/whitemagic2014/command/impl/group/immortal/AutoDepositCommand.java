package com.whitemagic2014.command.impl.group.immortal;

import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.util.RecognizeTextUtil;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.*;

import java.util.ArrayList;
import java.util.Objects;

/**
 * @author huangkd
 * @date 2023/2/3
 */
//@Command
public class AutoDepositCommand extends NoAuthCommand {

    private static final long BOT_ID = 1586197314;
    private static final String DEPOSIT_KEYWORD = "送灵石";
    private static final String AMOUNT_KEYWORD = "当前拥有灵石:";
    private static boolean deposit;

    public AutoDepositCommand() {
        super();
        this.like = true;
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        String msg = Objects.requireNonNull(messageChain.get(OnlineMessageSource.Incoming.FromGroup.Key)).getOriginalMessage().toString();
        At at = (At) messageChain.stream().filter(At.class::isInstance).findFirst().orElse(null);

        if (!deposit) {
            if (!msg.startsWith(DEPOSIT_KEYWORD)) {
                return null;
            }

            if (at == null || BOT_ID != at.getTarget()) {
                return null;
            }
            deposit = true;
            return new PlainText("灵庄信息");
        } else {
            if (at == null || BOT_ID != at.getTarget()) {
                return null;
            }

            String notice = RecognizeTextUtil.getInstance().getImageText(messageChain, sender);
            if (notice == null) {
                return null;
            }
            String[] notices = notice.split("\n");
            int amount = 0;
            for (String no : notices) {
                if (no.startsWith(AMOUNT_KEYWORD)) {
                    String amountStr = no.replace(AMOUNT_KEYWORD, "").trim();
                    // 因为8和0识别有问题，所以所有的8都改成0至少不会余额不足
                    amountStr = amountStr.replace("8", "0");
                    if (amountStr.startsWith("0")) {
                        // 首位数字不可能是0，转回来
                        amountStr = amountStr.replaceFirst("0", "8");
                    }
                    amount = Integer.parseInt(amountStr.trim());
                }
            }
            deposit = false;
            if (amount <= 0) {
                return null;
            }
            return new PlainText("灵庄存灵石" + amount);
        }
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("");
    }
}
