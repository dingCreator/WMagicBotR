package com.whitemagic2014.command.impl.group.astrology.skill;

import com.dingCreator.astrology.behavior.SkillBehavior;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.pojo.CommandProperties;
import com.whitemagic2014.util.NumberFormatUtil;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ding
 * @date 2024/3/31
 */
@Command(minArgsSize = 1, maxArgsSize = 10)
public class SetSkillBarItemCommand extends NoAuthCommand {
    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        List<Long> skillIds = NumberFormatUtil.numberListFormat(args);
        if (skillIds.size() == 0) {
            return new PlainText("输入的编号有误");
        }
        SkillBehavior.getInstance().createSkillBarItem(sender.getId(), skillIds);
        return new PlainText("设置成功");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("设置技能栏");
    }
}
