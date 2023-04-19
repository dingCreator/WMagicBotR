package com.whitemagic2014.command.impl.group.funny;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.util.RecognizeTextUtil;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

import java.util.ArrayList;

/**
 * @author ding
 * @date 2023/2/2
 */
@Command
public class RecognizeTextCommand extends NoAuthCommand {

    public RecognizeTextCommand() {
        this.like = true;
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        return new PlainText(RecognizeTextUtil.getInstance().getImageText(messageChain, sender));
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("识别文字", "文字识别");
    }
}
