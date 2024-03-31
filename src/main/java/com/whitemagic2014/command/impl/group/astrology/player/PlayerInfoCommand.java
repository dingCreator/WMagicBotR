package com.whitemagic2014.command.impl.group.astrology.player;

import com.dingCreator.astrology.behavior.PlayerBehavior;
import com.dingCreator.astrology.response.BaseResponse;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ding
 * @date 2024/2/22
 */
@Command
public class PlayerInfoCommand extends NoAuthCommand {

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        BaseResponse<List<String>> response = PlayerBehavior.getInstance().getPlayerInfoById(sender.getId());
        StringBuilder builder = new StringBuilder("人物信息");
        response.getContent().forEach(info -> builder.append("\n").append(info));
        return new PlainText(builder.toString());
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("我的存档");
    }
}
