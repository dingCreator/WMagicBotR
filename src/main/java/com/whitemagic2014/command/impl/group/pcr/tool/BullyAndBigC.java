package com.whitemagic2014.command.impl.group.pcr.tool;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;

/**
 * @author huangkd
 * @date 2023/2/22
 */
@Command
public class BullyAndBigC extends NoAuthCommand {

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (CollectionUtils.isEmpty(args)) {
            return null;
        }
        String str = args.get(0).trim();
        if ("恶霸".equals(str)) {
            return new PlainText("赢，绿毛，喵良，杨老板，樱");
        }
        if ("C".equals(str) || "c".equals(str)) {
            return new PlainText("绿毛，f，盒子哥，铃鹿御前，佑树");
        }
        return null;
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("五大");
    }
}
