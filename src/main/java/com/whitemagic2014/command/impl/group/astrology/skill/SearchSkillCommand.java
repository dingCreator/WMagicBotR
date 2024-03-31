package com.whitemagic2014.command.impl.group.astrology.skill;

import com.dingCreator.astrology.behavior.SkillBehavior;
import com.dingCreator.astrology.enums.job.JobEnum;
import com.dingCreator.astrology.enums.skill.SkillEnum;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

import java.util.ArrayList;

/**
 * @author ding
 * @date 2024/3/31
 */
@Command(minArgsSize = 1)
public class SearchSkillCommand extends NoAuthCommand {

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        SkillEnum skillEnum = SkillBehavior.getInstance().getSkillEnumByName(args.get(0));
        return new PlainText(SkillFormatUtil.skillDetailFormat(skillEnum));
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("查询技能");
    }
}
