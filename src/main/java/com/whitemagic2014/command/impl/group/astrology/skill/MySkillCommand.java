package com.whitemagic2014.command.impl.group.astrology.skill;

import com.dingCreator.astrology.behavior.SkillBehavior;
import com.dingCreator.astrology.entity.SkillBelongTo;
import com.dingCreator.astrology.enums.job.JobEnum;
import com.dingCreator.astrology.enums.skill.SkillEnum;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import com.whitemagic2014.util.CommandThreadPoolUtil;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ding
 * @date 2024/3/31
 */
@Command
public class MySkillCommand extends NoAuthCommand {

    @Autowired
    private GlobalParam globalParam;

    @Autowired
    private CommandThreadPoolUtil commandThreadPoolUtil;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        List<SkillBelongTo> belongToList = SkillBehavior.getInstance().getSkillBelongTo(sender.getId());
        List<String> info = belongToList.stream().map(skill -> {
            SkillEnum skillEnum = SkillEnum.getById(skill.getSkillId());
            return SkillFormatUtil.skillDetailFormat(skillEnum);
        }).collect(Collectors.toList());

        commandThreadPoolUtil.sendForwardMessage(info, "技能列表", Collections.singletonList("点击查看技能信息")
                , sender.getGroup().getId(), globalParam.botId);
        return new PlainText("技能信息");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("我的技能列表");
    }
}
