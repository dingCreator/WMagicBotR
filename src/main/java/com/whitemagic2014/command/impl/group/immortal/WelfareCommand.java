package com.whitemagic2014.command.impl.group.immortal;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.cache.SettingsCache;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;

import com.whitemagic2014.pojo.ActivityReceiveRecord;
import com.whitemagic2014.service.ActivityReceiveRecordService;
import com.whitemagic2014.util.CommandThreadPoolUtil;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ding
 * @date 2023/9/23
 */
@Command
public class WelfareCommand extends NoAuthCommand {

    @Autowired
    private GlobalParam globalParam;

    @Autowired
    private ActivityReceiveRecordService activityReceiveRecordService;

    @Autowired
    private CommandThreadPoolUtil commandThreadPoolUtil;

    private static final Lock LOCK = new ReentrantLock();
    private static final String DAILY_WELFARE_LIMIT_KEYWORD = "修仙_每日福利上限";

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        String outOfLimitedReply = "唔姆~zzz\n("+ globalParam.botNick + "似乎睡着了，明天再来吧)";
        if (!LOCK.tryLock()) {
            return null;
        }
        String todayStr = DateUtil.format(new Date(), "yyyy-MM-dd");
        try {
            ActivityReceiveRecord receiveRecord = activityReceiveRecordService.getOne(sender.getId(), todayStr, 1L);
            if (receiveRecord != null) {
                return new PlainText(outOfLimitedReply);
            }

            int receiveCount = activityReceiveRecordService.selectCount(todayStr, null, 1L);
            if (receiveCount >= SettingsCache.getInstance().getSettingsAsInt(DAILY_WELFARE_LIMIT_KEYWORD, 10)) {
                return new PlainText(outOfLimitedReply);
            }

            activityReceiveRecordService.insert(new ActivityReceiveRecord(todayStr, sender.getId(), 1L));
            List<Message> msgList = new ArrayList<Message>();
            msgList.add(new PlainText("世界积分兑换1"));
            msgList.add(new PlainText("坊市上架 渡厄丹 " + RandomUtil.randomInt(10000, 500_000)));
            commandThreadPoolUtil.addGroupTask(msgList, sender.getGroup().getId());
            return new PlainText("啊咧~☆ 是你啊！给你点好东西吧Kira~☆");
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("叩拜" + globalParam.botName, "叩拜" + globalParam.botNick, "向繁星许愿");
    }
}
