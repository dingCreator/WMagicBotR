package com.whitemagic2014.command.impl.group.funny;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.miniGame.MiniGameUtil;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.OnlineMessageSource;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Objects;

@Command
public class MineSweeperCommand extends NoAuthCommand {

    @Autowired
    private GlobalParam globalParam;

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "开始扫雷", globalParam.botNick + "开始扫雷",
                globalParam.botName + "结束扫雷", globalParam.botNick + "结束扫雷");
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        String msg = Objects.requireNonNull(messageChain.get(OnlineMessageSource.Incoming.FromGroup.Key)).getOriginalMessage().toString();
        if (msg.contains("开始")) {
            if (MiniGameUtil.startGame(sender.getId(), MiniGameUtil.MiniGameEnum.MINE_SWEEPER)) {
                Bot bot = MagicBotR.getBot();
                bot.getGroupOrFail(sender.getGroup().getId()).sendMessage(new PlainText("请输入地雷的长，宽，雷的数量，用空格隔开"));
                return new PlainText("开始扫雷游戏");
            } else {
                return new PlainText("无法开始扫雷游戏，可能正在进行其他游戏");
            }
        } else {
            if (MiniGameUtil.stopGame(sender.getId())) {
                return new PlainText("扫雷游戏结束");
            } else {
                return new PlainText("无法结束扫雷游戏，可能你没有在游玩扫雷游戏");
            }
        }
    }
}
