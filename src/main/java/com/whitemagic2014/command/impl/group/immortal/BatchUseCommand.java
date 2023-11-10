package com.whitemagic2014.command.impl.group.immortal;

import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.command.impl.group.OwnerCommand;
import com.whitemagic2014.util.CommandThreadPoolUtil;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import org.springframework.beans.factory.annotation.Autowired;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author huangkd
 * @date 2023/5/9
 */
@Command
public class BatchUseCommand extends OwnerCommand {

    @Autowired
    private GlobalParam globalParam;

    @Autowired
    private CommandThreadPoolUtil commandThreadPoolUtil;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (CollectionUtils.isEmpty(args)) {
            return new PlainText("缺少参数");
        }
        Bot bot = MagicBotR.getBot();
        List<String> list = JSONObject.parseArray("['" +
                args.get(0).replace("，", ",").replace(",", "','") + "']", String.class);
        int count = 1;
        if (args.size() >= 2) {
            try {
                count = Integer.parseInt(args.get(1));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (count > 100) {
            count = 100;
        }

        List<Message> messageList = new ArrayList<Message>(count * list.size());
        for (int i = 0; i < count; i++) {
            for (String article : list) {
                messageList.add(new PlainText("使用 " + article));
            }
        }
        commandThreadPoolUtil.addGroupTask(messageList, sender.getGroup().getId());
        return new PlainText("开始使用");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "批量使用", globalParam.botNick + "批量使用");
    }
}
