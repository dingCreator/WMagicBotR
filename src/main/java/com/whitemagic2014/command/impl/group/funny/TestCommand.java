package com.whitemagic2014.command.impl.group.funny;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author huangkd
 * @date 2023/3/17
 */
//@Command
public class TestCommand extends NoAuthCommand {

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {

        return transferDescription(Arrays.asList("11", "22", "33"));
    }

    private Message transferDescription(List<String> description) {
        Message message = new PlainText("");
        for (String str : description) {
            if ("[@winner]".equals(str)) {
                message = message.plus(makeAt(1072065168L));
            } else if ("[@loser]".equals(str)) {

            } else if ("[@sender]".equals(str)) {

            } else if ("[@opponent]".equals(str)) {

            } else {
                message = message.plus(str);
            }
        }
        return message;
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("测试专用指令");
    }
}
