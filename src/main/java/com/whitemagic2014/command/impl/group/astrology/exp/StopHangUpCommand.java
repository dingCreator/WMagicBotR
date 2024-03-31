package com.whitemagic2014.command.impl.group.astrology.exp;

import com.dingCreator.astrology.behavior.ExpBehavior;
import com.dingCreator.astrology.response.BaseResponse;
import com.dingCreator.astrology.vo.HangUpVO;
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
 * @date 2024/3/26
 */
@Command
public class StopHangUpCommand extends NoAuthCommand {
    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        BaseResponse<HangUpVO> response = ExpBehavior.getInstance().stopHangUp(sender.getId());
        HangUpVO vo = response.getContent();
        String msg = "本次挂机时长" + vo.getHangUpTime() + "分钟，获得" + vo.getExp() + "点经验" +
                "\n等级变化 " + vo.getOldLevel() + " >> " + vo.getNewLevel() +
                "\n回复生命值 " + vo.getHp() +
                "\n回复技能值 " + vo.getMp();
        return new PlainText(msg);
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("结束挂机");
    }
}
