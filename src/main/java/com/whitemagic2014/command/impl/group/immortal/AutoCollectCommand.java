package com.whitemagic2014.command.impl.group.immortal;

import cn.hutool.core.util.RandomUtil;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.annotate.SpecialCommand;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.command.impl.group.EmptyStringCommand;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.command.impl.group.OwnerCommand;
import com.whitemagic2014.pojo.CommandProperties;
import com.whitemagic2014.util.MagicMd5;
import com.whitemagic2014.util.time.MagicPeriodTask;
import com.whitemagic2014.util.time.MagicTaskObserver;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author huangkd
 * @date 2023/2/27
 */
//@Command
public class AutoCollectCommand extends OwnerCommand {

    private static final List<Long> GROUP_IDS = Arrays.asList();
    private static final long PERIOD = 5 * 60 * 1000L;
    private static final long DELAY = 5 * 1000L;
    private static final long BOSS_BORN_PERIOD = 60 * 60 * 1000L;
    private static final String NO_BOSS_KEYWORD = "本群尚未生成";
    private static final String CANT_ATTACK_KEYWORD = "重伤未愈";
    private static final String NUMBER_KEYWORD = "编号";

    private static final String START_KEYWORD = "开始";
    private static final String STOP_KEYWORD1 = "结束";
    private static final String STOP_KEYWORD2 = "停止";
    private static final String STATUS_KEYWORD = "状态";

    private static final String KEY = "autoCollect";
    private static final String PERIOD_KEY = MagicMd5.getMd5String(KEY);
    private static final Map<Long, GroupBossStatus> GROUP_BOSS_MAP = new ConcurrentHashMap<>();
    private static final AtomicBoolean CONTINUE_ATTACK = new AtomicBoolean(true);

    private static boolean auto;

    @Data
    @AllArgsConstructor
    private static class GroupBossStatus {
        private Long groupId;
        private Long previousCollectTimeMillis;
        private Boolean bossAllDefeated;

        GroupBossStatus(Long groupId) {
            this.groupId = groupId;
            this.previousCollectTimeMillis = 0L;
            this.bossAllDefeated = false;
        }
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (CollectionUtils.isEmpty(args)) {
            return new PlainText("未输入参数：" + START_KEYWORD + "/" + STOP_KEYWORD1 + "/" + STOP_KEYWORD2);
        }

        if (CollectionUtils.isEmpty(GROUP_IDS)) {
            return new PlainText("未配置收菜群");
        }

        switch (args.get(0)) {
            case START_KEYWORD:
                initBossMap();
                registerCollect();

                Bot bot = MagicBotR.getBot();
                // 在一个群闭关就可以了
                bot.getGroupOrFail(GROUP_IDS.get(0)).sendMessage(new PlainText("闭关"));

                MagicPeriodTask.build(PERIOD_KEY, () -> {
                    CONTINUE_ATTACK.set(true);
                    GROUP_BOSS_MAP.values().forEach(g -> {
                        if (!g.getBossAllDefeated()
                                || System.currentTimeMillis() > g.getPreviousCollectTimeMillis() + BOSS_BORN_PERIOD) {
                            bot.getGroupOrFail(g.getGroupId()).sendMessage(new PlainText("灵石出关"));
                            bot.getGroupOrFail(g.getGroupId()).sendMessage(new PlainText("闭关"));
                            bot.getGroupOrFail(g.getGroupId()).sendMessage(new PlainText("查询世界BOSS"));
                        }
                        sleep(2000);
                    });
                }).schedule(DELAY, PERIOD);

                auto = true;
                return new PlainText("开始自动收菜");
            case STOP_KEYWORD1:
            case STOP_KEYWORD2:
                MagicTaskObserver.cancelTask(PERIOD_KEY);
                auto = false;
                return new PlainText("停止自动收菜");
            case STATUS_KEYWORD:
                return new PlainText(auto ? "自动收菜中" : "休息中");
            default:
                return null;
        }
    }

    private void initBossMap() {
        GROUP_IDS.forEach(id -> GROUP_BOSS_MAP.put(id, new GroupBossStatus(id)));
    }

    private void registerCollect() {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, notice) -> {
            Bot bot = MagicBotR.getBot();
            long gId = sender.getGroup().getId();

            if (notice.contains(NO_BOSS_KEYWORD)) {
                GROUP_BOSS_MAP.get(gId).setBossAllDefeated(true);
            } else if (notice.contains(NUMBER_KEYWORD) && notice.contains("Boss")) {
                GROUP_BOSS_MAP.get(gId).setBossAllDefeated(false);
                bot.getGroupOrFail(gId).sendMessage(new PlainText("讨伐世界BOSS1"));
                sleep(2000 + RandomUtil.randomLong(3000));
            } else if (notice.contains(CANT_ATTACK_KEYWORD)) {
                CONTINUE_ATTACK.set(false);
            }
            return null;
        }, KEY);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("收菜");
    }
}
