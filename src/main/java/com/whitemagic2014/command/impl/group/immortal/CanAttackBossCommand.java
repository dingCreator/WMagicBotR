package com.whitemagic2014.command.impl.group.immortal;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.command.impl.group.immortal.util.CalBossUtil;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.util.CollectionUtils;


import java.util.ArrayList;

/**
 * @author ding
 * @date 2023/4/19
 */
@Command
public class CanAttackBossCommand extends NoAuthCommand {

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (CollectionUtils.isEmpty(args)) {
            return new PlainText("请输入境界");
        }

        String rankStr = args.get(0);
        int rank = CalBossUtil.getRank(rankStr);
        if (rank <= 0) {
            return new PlainText("查询出错");
        }
        return new PlainText("可攻击最低境界：" + CalBossUtil.getRankStr(rank - 8));
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("可攻击boss", "可攻击Boss", "可攻击BOSS");
    }
}
