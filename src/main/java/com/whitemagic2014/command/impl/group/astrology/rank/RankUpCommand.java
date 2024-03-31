package com.whitemagic2014.command.impl.group.astrology.rank;

import com.dingCreator.astrology.behavior.RankBehavior;
import com.dingCreator.astrology.cache.PlayerCache;
import com.dingCreator.astrology.response.BaseResponse;
import com.dingCreator.astrology.vo.BattleResultVO;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import com.whitemagic2014.util.CommandThreadPoolUtil;
import com.whitemagic2014.util.ForwardMessageUtil;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.ForwardMessage;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author ding
 * @date 2024/3/29
 */
@Command
public class RankUpCommand extends NoAuthCommand {

    @Autowired
    private CommandThreadPoolUtil commandThreadPoolUtil;

    @Autowired
    private GlobalParam globalParam;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        BattleResultVO vo = RankBehavior.getInstance().rankUp(sender.getId());

//        new PlainText(vo.getInfo().stream().reduce((s1,s2) -> s1 + "\n\n" + s2).orElse(""));
        ForwardMessage forwardMessage = ForwardMessageUtil.buildForwardMessage(globalParam.botId,
                vo.getInfo().stream().map(i -> new PlainText(i)).collect(Collectors.toList()));
        commandThreadPoolUtil.addGroupTask(Collections.singletonList(forwardMessage), sender.getGroup().getId());

        String tip;
        if (BattleResultVO.BattleResult.WIN.equals(vo.getBattleResult())) {
            tip = "恭喜击败突破boss，突破成功";
        } else if (BattleResultVO.BattleResult.LOSE.equals(vo.getBattleResult())) {
            tip = "很遗憾，被突破boss击败，突破失败";
        } else {
            tip = "恭喜你，突破boss拿你毫无办法，突破成功";
        }
        return new PlainText(tip);
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("突破");
    }
}
