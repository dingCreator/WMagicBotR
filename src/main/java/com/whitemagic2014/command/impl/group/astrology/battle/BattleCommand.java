package com.whitemagic2014.command.impl.group.astrology.battle;

import com.dingCreator.astrology.behavior.PlayerBehavior;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.At;

import java.util.ArrayList;
import java.util.Objects;

/**
 * @author ding
 * @date 2024/2/23
 */
@Command
public class BattleCommand extends NoAuthCommand {

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        At at = (At) messageChain.stream().filter(At.class::isInstance).findFirst().orElse(null);
        if (Objects.isNull(at)) {
            return new PlainText("请@你的对手");
        }
        long expireTime = PlayerBehavior.getInstance().createBattle(sender.getId(), at.getTarget());
        return new PlainText("已发起对决，对方若在" + expireTime + "s内无响应，则响应超时。"
                + "被挑战方可发送【接受对决】或【拒绝对决】进行响应");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("对决");
    }
}
