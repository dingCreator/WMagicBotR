package com.whitemagic2014.command.impl.group.astrology.battle;

import com.dingCreator.astrology.behavior.PlayerBehavior;
import com.dingCreator.astrology.cache.PlayerCache;
import com.dingCreator.astrology.response.BaseResponse;
import com.dingCreator.astrology.util.BattleUtil;
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
 * @date 2024/3/26
 */
@Command
public class AcceptBattleCommand extends NoAuthCommand {

    @Autowired
    private CommandThreadPoolUtil commandThreadPoolUtil;

    @Autowired
    private GlobalParam globalParam;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        BaseResponse<BattleResultVO> response = PlayerBehavior.getInstance().acceptBattle(sender.getId());
        BattleResultVO vo = response.getContent();

        ForwardMessage forwardMessage = ForwardMessageUtil.buildForwardMessage(globalParam.botId,
                vo.getInfo().stream().map(i -> new PlainText(i)).collect(Collectors.toList()));
        commandThreadPoolUtil.addGroupTask(Collections.singletonList(forwardMessage), sender.getGroup().getId());

        StringBuilder builder = new StringBuilder("决斗结果：");
        if (BattleResultVO.BattleResult.WIN.equals(vo.getBattleResult())) {
            builder.append(PlayerCache.getPlayerById(vo.getInitiatorId()).getPlayer().getName()).append("获胜");
        } else if (BattleResultVO.BattleResult.LOSE.equals(vo.getBattleResult())) {
            builder.append(PlayerCache.getPlayerById(vo.getRecipientId()).getPlayer().getName()).append("获胜");
        } else if (BattleResultVO.BattleResult.DRAW.equals(vo.getBattleResult())) {
            builder.append("平局");
        } else {
            builder.append("超时");
        }
        return new PlainText(builder.toString());
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("接受对决");
    }
}
