package com.whitemagic2014.command.impl.group.astrology.skill;
import com.dingCreator.astrology.behavior.SkillBehavior;
import com.dingCreator.astrology.dto.skill.SkillBarDTO;
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
import java.util.Objects;

/**
 * @author ding
 * @date 2024/3/31
 */
@Command
public class MySkillBatItemCommand extends NoAuthCommand {

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        SkillBarDTO skillBarDTO = SkillBehavior.getInstance().getSkillBarDTO(sender.getId());
        StringBuilder builder = new StringBuilder(SkillEnum.getById(skillBarDTO.getSkillId()).getName());
        while (Objects.nonNull(skillBarDTO.getNext())) {
            skillBarDTO = skillBarDTO.getNext();
            builder.append(" >> ").append(SkillEnum.getById(skillBarDTO.getSkillId()).getName());
        }
        return new PlainText(builder.toString());
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("我的技能栏", "查看技能栏");
    }
}
