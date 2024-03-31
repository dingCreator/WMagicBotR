package com.whitemagic2014.command.impl.group.astrology.player;

import com.dingCreator.astrology.behavior.JobBehavior;
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
public class StartGameCommand extends NoAuthCommand {

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        BaseResponse<List<String>> response = JobBehavior.getInstance().getJob();
        String jobMsg = response.getContent().stream().reduce((s1, s2) -> s1 + "\n" + s2).orElse("");
        return new PlainText(jobMsg);
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("我要启动");
    }
}
