package com.whitemagic2014.command.impl.group.immortal;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.cache.SettingsCache;
import com.whitemagic2014.command.impl.group.EmptyStringCommand;
import com.whitemagic2014.command.impl.group.OwnerCommand;
import com.whitemagic2014.command.impl.group.immortal.util.CalBossUtil;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import com.whitemagic2014.util.MagicMd5;
import com.whitemagic2014.util.time.MagicPeriodTask;
import com.whitemagic2014.util.time.MagicTaskObserver;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自动修仙
 * 适用于修仙小游戏
 *
 * @author ding
 */
@Command
public class AutoExerciseCommand extends OwnerCommand {

    /**
     * 修仙主群
     */
    private static final long GROUP_ID = 256326696;

    // 指令关键字
    private static final String START_KEYWORD = "开始";
    private static final String STOP_KEYWORD1 = "结束";
    private static final String STOP_KEYWORD2 = "停止";
    private static final String SWITCH_KEYWORD = "切换";
    private static final String STATUS_KEYWORD = "状态";
    private static final String DAILY_KEYWORD = "日常";
    private static final String WANTED_KEYWORD = "悬赏令";
    private static final String UPGRADE_KEYWORD = "突破";
    private static final String NOW_ATTACK_BOSS_KEYWORD = "打boss";
    private static final String EDIT_ATTACK_PERIOD_KEYWORD = "攻击周期";
    private static final String SEARCH_RANK_KEYWORD = "查询境界";

    // 识别关键字
    private static final String AUTO_COLLECT_KEY = "autoCollect";
    private static final String TASK_PERIOD_KEY = MagicMd5.getMd5String("autoTask");
    private static final String TASK_COMPLETED_KEYWORD = "今日无法再获取";
    private static final String DAILY_PERIOD_KEYWORD = "还没有宗门任务";
    private static final String NO_BOSS_KEYWORD = "本群尚未生成";
    private static final String CANT_ATTACK_KEYWORD = "重伤";
    private static final String NUMBER_KEYWORD = "编号";
    private static final String HAS_BOSS_KEYWORD = "Boss";
    private static final List<String> TASK_KEYWORD = Arrays.asList("灰尘", "罕见", "不敬", "工厂");

    // 配置关键字
    private static final String MODE_KEYWORD = "修仙_修炼模式";
    private static final String AUTO_WANTED_KEYWORD = "修仙_悬赏令";
    private static final String AUTO_UPGRADE_KEYWORD = "修仙_突破模式";
    private static final String ATTACK_PERIOD_KEYWORD = "修仙_攻击间歇";
    private static final String BOT_RANK_KEYWORD = "修仙_当前境界";

    /**
     * 容器
     */
    private static final Set<String> PERIOD_KEYS = new HashSet<>();
    private static final Map<Long, GroupBossStatus> GROUP_BOSS_MAP = new ConcurrentHashMap<>();

    private final List<ICommandKeyword> COMMAND_KEY_WORD_LIST = Arrays.asList(
            new StartCommandKeyword(),
            new StopCommandKeyword(),
            new SwitchCommandKeyword(),
            new StatusCommandKeyword(),
            new DailyCommandKeyword(),
            new WantedCommandKeyword(),
            new UpgradeCommandKeyword(),
            new NowAttackBossCommandKeyword(),
            new EditAttackPeriodKeyword(),
            new SearchRankKeyword()
    );

    @Value("${immortal.collectGroups:}")
    private List<Long> collectGroupIds;

    /**
     * 时间参数
     */
    private static final long IN_OUT_PERIOD = 60 * 1000L;
    private static final long IN_OUT_DELAY = 5 * 1000L;

    private static final long WANTED_PERIOD = 61 * 60 * 1000L;
    private static final long WANTED_DELAY = 10 * 1000L;

    private static final long IMPROVE_PERIOD = 61 * 60 * 1000L;
    private static final long IMPROVE_DELAY = 15 * 1000L;

    private static final long TASK_PERIOD = 6 * 60 * 1000L;
    private static final long TASK_DELAY = 2 * 1000L;

    private static final AtomicBoolean CONTINUE_ATTACK = new AtomicBoolean(true);
    private static final long ATTACK_BOSS_PERIOD = 20 * 1000L;

    // 中间过程变量
    /**
     * 是否有正在进行的日常周期任务
     */
    private static boolean hasDailyPeriod;
    /**
     * 上次攻击boss时间
     */
    private static long attackStartTimeMillis;
    /**
     * 上次完成宗门任务的时间
     */
    private static final AtomicLong COMPLETE_TASK_TIME_MILLIS = new AtomicLong(0);
    /**
     * 宗门任务完成次数
     */
    private static final AtomicInteger COMPLETE_TASK_TIMES = new AtomicInteger(0);
    /**
     * 是否是需要刷新
     */
    private static final AtomicBoolean NEED_REFRESH = new AtomicBoolean(false);

    private static final Random RANDOM = new Random();

    @Autowired
    private GlobalParam globalParam;

    private Bot bot;

    @Override
    public CommandProperties properties() {
        return new CommandProperties("修仙");
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (CollectionUtils.isEmpty(args)) {
            return null;
        }
        String msg = args.get(0).trim();
        ICommandKeyword cmd = COMMAND_KEY_WORD_LIST.stream().filter(c -> c.support(msg)).findFirst().orElse(null);
        if (cmd != null) {
            return cmd.execute(msg);
        }
        return null;
    }

    /**
     * 参数响应
     */
    private interface ICommandKeyword {
        /**
         * 是否执行此逻辑
         *
         * @param keyword 关键词
         * @return 是否支持
         */
        boolean support(String keyword);

        /**
         * 执行相应逻辑
         *
         * @param keyword 关键字
         * @return 返回信息
         */
        Message execute(String keyword);
    }

    /**
     * 开始修仙指令
     */
    private class StartCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return START_KEYWORD.equals(keyword);
        }

        @Override
        public Message execute(String keyword) {
            startAuto();
            return new PlainText("开始自动修仙");
        }
    }

    /**
     * 停止修仙指令
     */
    private class StopCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return STOP_KEYWORD1.equals(keyword) || STOP_KEYWORD2.equals(keyword);
        }

        @Override
        public Message execute(String keyword) {
            stopAuto();
            return new PlainText("停止自动修仙");
        }
    }

    /**
     * 切换修炼模式指令
     */
    private static class SwitchCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return keyword.startsWith(SWITCH_KEYWORD);
        }

        @Override
        public Message execute(String keyword) {
            if (keyword.endsWith("0")) {
                SettingsCache.getInstance().setSettings(MODE_KEYWORD, 0);
                return new PlainText("模式切换为 正常修炼");
            } else if (keyword.endsWith("1")) {
                SettingsCache.getInstance().setSettings(MODE_KEYWORD, 1);
                return new PlainText("模式切换为 左右互搏");
            } else if (keyword.endsWith("2")) {
                SettingsCache.getInstance().setSettings(MODE_KEYWORD, 2);
                return new PlainText("模式切换为 打boss");
            }
            return new PlainText("错误的模式");
        }
    }

    /**
     * 查看状态指令
     */
    private static class StatusCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return STATUS_KEYWORD.equals(keyword);
        }

        @Override
        public Message execute(String keyword) {
            StringBuilder stringBuilder = new StringBuilder("当前修仙状态 ");
            stringBuilder.append(PERIOD_KEYS.size() > 0 ? "自动修仙" : "停止修仙");
            stringBuilder.append("\n当前修仙模式 ");

            switch (SettingsCache.getInstance().getSettingsAsInt(MODE_KEYWORD, 0)) {
                case 0:
                    stringBuilder.append("正常修炼");
                    break;
                case 1:
                    stringBuilder.append("左右互搏");
                    break;
                case 2:
                    stringBuilder.append("打boss");
                    break;
                default:
                    stringBuilder.append("错误的模式");
                    break;
            }

            stringBuilder.append("\n当前悬赏模式 ");
            boolean autoWanted = SettingsCache.getInstance().getSettingsAsBoolean(AUTO_WANTED_KEYWORD, false);
            stringBuilder.append(autoWanted ? "接取" : "不接取");
            stringBuilder.append("\n当前突破模式 ");

            switch (SettingsCache.getInstance().getSettingsAsInt(AUTO_UPGRADE_KEYWORD, 0)) {
                case 0:
                    stringBuilder.append("不突破");
                    break;
                case 1:
                    stringBuilder.append("不使用丹药突破");
                    break;
                case 2:
                    stringBuilder.append("使用丹药突破");
                    break;
                default:
                    stringBuilder.append("错误的模式");
                    break;
            }
            stringBuilder.append("\n攻击周期 ")
                    .append(SettingsCache.getInstance().getSettingsAsInt(ATTACK_PERIOD_KEYWORD, 1))
                    .append("分钟")
                    .append("\n当前设置的境界 ")
                    .append(SettingsCache.getInstance().getSettingsAsInt(BOT_RANK_KEYWORD, 1));
            return new PlainText(stringBuilder.toString());
        }
    }

    /**
     * 做日常指令
     */
    private class DailyCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return DAILY_KEYWORD.equals(keyword);
        }

        @Override
        public Message execute(String keyword) {
            registerTask();

            bot = MagicBotR.getBot();
            bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("修仙签到"));
            bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("宗门丹药领取"));

            if (!hasDailyPeriod) {
                MagicPeriodTask.build(TASK_PERIOD_KEY, () -> {
                    TaskStatusEnum taskStatusEnum = tryCompleteTask(System.currentTimeMillis());
                    if (TaskStatusEnum.DO_IT.equals(taskStatusEnum)) {
                        if (NEED_REFRESH.get()) {
                            bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("宗门任务刷新"));
                        } else {
                            bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("宗门任务接取"));
                            NEED_REFRESH.set(true);
                        }
                    }
                }).schedule(TASK_DELAY, TASK_PERIOD);
                hasDailyPeriod = true;
            }
            return new PlainText("开始做日常");
        }
    }

    /**
     * 自动接取悬赏令指令
     */
    private static class WantedCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return keyword.startsWith(WANTED_KEYWORD);
        }

        @Override
        public Message execute(String keyword) {
            if (keyword.endsWith(START_KEYWORD)) {
                SettingsCache.getInstance().setSettings(AUTO_WANTED_KEYWORD, true);
                return new PlainText("开始自动接悬赏");
            } else if (keyword.endsWith(STOP_KEYWORD1) || keyword.endsWith(STOP_KEYWORD2)) {
                SettingsCache.getInstance().setSettings(AUTO_WANTED_KEYWORD, false);
                return new PlainText("停止自动接悬赏");
            } else {
                return new PlainText("关键词错误");
            }
        }
    }

    /**
     * 突破模式指令
     */
    private static class UpgradeCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return keyword.startsWith(UPGRADE_KEYWORD);
        }

        @Override
        public Message execute(String keyword) {
            if (keyword.endsWith("0")) {
                SettingsCache.getInstance().setSettings(AUTO_UPGRADE_KEYWORD, 0);
                return new PlainText("不自动突破");
            } else if (keyword.endsWith("1")) {
                SettingsCache.getInstance().setSettings(AUTO_UPGRADE_KEYWORD, 1);
                return new PlainText("不使用丹药突破");
            } else if (keyword.endsWith("2")) {
                SettingsCache.getInstance().setSettings(AUTO_UPGRADE_KEYWORD, 2);
                return new PlainText("丹药突破");
            } else {
                return new PlainText("模式错误");
            }
        }
    }

    /**
     * 立即打boss指令
     */
    private class NowAttackBossCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return NOW_ATTACK_BOSS_KEYWORD.equalsIgnoreCase(keyword);
        }

        @Override
        public Message execute(String keyword) {
            if (SettingsCache.getInstance().getSettingsAsInt(MODE_KEYWORD, 0) != 2) {
                return new PlainText("现在的模式不为打boss");
            }
            attackAllBoss();
            return new PlainText("开始打boss");
        }
    }

    /**
     * 修改攻击周期
     */
    private static class EditAttackPeriodKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return keyword.startsWith(EDIT_ATTACK_PERIOD_KEYWORD);
        }

        @Override
        public Message execute(String keyword) {
            String str = keyword.replace(EDIT_ATTACK_PERIOD_KEYWORD, "").trim();
            try {
                Integer.parseInt(str);
            } catch (Exception e) {
                return new PlainText("输入的不为数字");
            }
            SettingsCache.getInstance().setSettings(ATTACK_PERIOD_KEYWORD, str);
            return new PlainText("更新攻击周期成功！");
        }
    }

    /**
     * 文字等级对应数字等级
     */
    private class SearchRankKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return keyword.startsWith(SEARCH_RANK_KEYWORD);
        }

        @Override
        public Message execute(String keyword) {
            String str = keyword.replace(SEARCH_RANK_KEYWORD, "").trim();
            return new PlainText("等级对应的数字为：" + CalBossUtil.getRank(str));
        }
    }

    /**
     * 随机延时
     */
    private static void waitRandomMillis(long base, int range) {
        try {
            Thread.sleep(base + RANDOM.nextInt(range));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始自动修仙
     */
    public void startAuto() {
        bot = MagicBotR.getBot();
        bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("闭关"));
        // 定时闭关出关
        String key = MagicMd5.getMd5String("g-" + GROUP_ID + "exercise-inOut");
        PERIOD_KEYS.add(key);
        MagicPeriodTask.build(key, () -> {
            waitRandomMillis(1000, 3000);
            switch (SettingsCache.getInstance().getSettingsAsInt(MODE_KEYWORD, 0)) {
                case 0:
                    return;
                case 1:
                    if (System.currentTimeMillis() >= attackStartTimeMillis
                            + SettingsCache.getInstance().getSettingsAsInt(ATTACK_PERIOD_KEYWORD, 2) * 60 * 1000) {
                        bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("抢劫").plus(makeAt(globalParam.botId)));
                        attackStartTimeMillis = System.currentTimeMillis();

                        bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("灵石出关"));
                        bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("闭关"));
                    }
                    break;
                case 2:
                    if (System.currentTimeMillis() >= attackStartTimeMillis
                            + 2 * SettingsCache.getInstance().getSettingsAsInt(ATTACK_PERIOD_KEYWORD, 2) * 60 * 1000) {
//                        bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("讨伐世界BOSS1"));
                        attackAllBoss();
                        attackStartTimeMillis = System.currentTimeMillis();
                    }
                    break;
                default:
                    break;
            }
        }).schedule(IN_OUT_DELAY, IN_OUT_PERIOD);

        key = MagicMd5.getMd5String("g-" + GROUP_ID + "exercise-wanted");
        PERIOD_KEYS.add(key);
        MagicPeriodTask.build(key, () -> {
            if (!SettingsCache.getInstance().getSettingsAsBoolean(AUTO_WANTED_KEYWORD, false)) {
                return;
            }
            waitRandomMillis(1000, 3000);
            if (!WantedCommand.wanted || WantedCommand.wantedEndTimeMillis == 0) {
                bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("灵石出关"));
                if (SettingsCache.getInstance().getSettingsAsInt(BOT_RANK_KEYWORD, 1) < 29) {
                    bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("悬赏令"));
                } else {
                    bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("渡劫境悬赏令"));
                }
            } else if (System.currentTimeMillis() > WantedCommand.wantedEndTimeMillis + 10000L) {
                bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("渡劫境悬赏令结算"));
                WantedCommand.wanted = false;
                WantedCommand.wantedEndTimeMillis = 0;
            }
        }).schedule(WANTED_DELAY, WANTED_PERIOD);

        // 自动突破
        key = MagicMd5.getMd5String("g-" + GROUP_ID + "exercise-improve");
        PERIOD_KEYS.add(key);
        MagicPeriodTask.build(key, () -> {
            waitRandomMillis(1000, 3000);
            int upgradeMode = SettingsCache.getInstance().getSettingsAsInt(AUTO_UPGRADE_KEYWORD, 0);
            if (upgradeMode > 1) {
                bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("突破 使用"));
            }
            if (upgradeMode > 0) {
                bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("突破 不使用"));
            }
        }).schedule(IMPROVE_DELAY, IMPROVE_PERIOD);

        registerWanted();
    }

    /**
     * 停止自动修仙
     */
    public void stopAuto() {
        if (!CollectionUtils.isEmpty(PERIOD_KEYS)) {
            PERIOD_KEYS.forEach(MagicTaskObserver::cancelTask);
        }
        PERIOD_KEYS.clear();
    }

    /**
     * 打boss逻辑
     */
    public void attackAllBoss() {
        if (CollectionUtils.isEmpty(this.collectGroupIds)) {
            return;
        }

        bot = MagicBotR.getBot();
        initBossMap();
        registerCollect();

        // 只出关一次即可
        bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("灵石出关"));
        bot.getGroupOrFail(GROUP_ID).sendMessage(new PlainText("闭关"));

        CONTINUE_ATTACK.set(true);
        GROUP_BOSS_MAP.values().forEach(g -> {
            if (!CONTINUE_ATTACK.get()) {
                return;
            }
            if (!g.getBossAllDefeated()) {
                bot.getGroupOrFail(g.getGroupId()).sendMessage(new PlainText("查询世界BOSS"));
            }
            waitRandomMillis(3000, 3000);
        });
    }

    private void initBossMap() {
        collectGroupIds.forEach(id -> GROUP_BOSS_MAP.put(id, new GroupBossStatus(id)));
    }

    /**
     * 自动打boss逻辑
     */
    private void registerCollect() {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, notice) -> {
            if (SettingsCache.getInstance().getSettingsAsInt(MODE_KEYWORD, 0) != 2) {
                return null;
            }

            if (StringUtils.isEmpty(notice)) {
                return null;
            }

            if (!CONTINUE_ATTACK.get()) {
                return null;
            }
            long gId = sender.getGroup().getId();

            waitRandomMillis(500, 1000);
            if (notice.contains(NO_BOSS_KEYWORD)) {
                // 没有boss了
                GROUP_BOSS_MAP.get(gId).setBossAllDefeated(true);
            } else if (notice.contains(NUMBER_KEYWORD) && notice.contains(HAS_BOSS_KEYWORD)) {
                // 避免踩雷
                String[] bossInfoArray = notice.split("\n");
                List<String> bossInfo = new ArrayList<>();
                for (String info : bossInfoArray) {
                    if (info.contains(HAS_BOSS_KEYWORD)) {
                        bossInfo.add(info);
                    }
                }

                if (CollectionUtils.isEmpty(bossInfo)) {
                    return null;
                }
                Stack<Integer> bossIndex = new Stack<>();
                for (int i = 0; i < bossInfo.size(); i++) {
                    if (CalBossUtil.canAttackBoss(bossInfo.get(i),
                            SettingsCache.getInstance().getSettingsAsInt(BOT_RANK_KEYWORD, 1))) {
                        bossIndex.push(i + 1);
                    }
                }
                if (bossIndex.empty()) {
                    return null;
                }
                GROUP_BOSS_MAP.get(gId).setBossAllDefeated(false);

                while (!GROUP_BOSS_MAP.get(gId).getBossAllDefeated() && CONTINUE_ATTACK.get() && !bossIndex.empty()) {
                    bot.getGroupOrFail(gId).sendMessage(new PlainText("讨伐世界BOSS" + bossIndex.pop()));
                    waitRandomMillis(ATTACK_BOSS_PERIOD, 1000);
                }
            } else if (notice.contains(CANT_ATTACK_KEYWORD)) {
                // 重伤
                CONTINUE_ATTACK.set(false);
            }
            return null;
        }, AUTO_COLLECT_KEY);
    }

    /**
     * 悬赏令识别处理逻辑
     * 由于使用的文字识别算法有一定的错误率，建议关键词列举多种情况且不要轻信自动结算功能
     */
    private void registerWanted() {
        // 悬赏令
        EmptyStringCommand.addLogic(((sender, args, messageChain, subject, notice) -> {
            if (WantedCommand.wanted) {
                return null;
            }
            if (notice == null || !notice.contains("个人悬赏令")) {
                return null;
            }
            String[] notices = notice.split("!");
            System.out.println("悬赏令【" + Arrays.toString(notices) + "】");
            for (int i = 0; i < 3; i++) {
                if (needAward(notices[i].trim().replace(" ", ""))) {
                    String[] sentences = notices[i].split(",");
                    for (String sentence : sentences) {
                        if (sentence.startsWith("预计需")) {
                            String minuteStr = sentence.replace("预计需", "").replace("分钟", "").trim();
                            try {
                                WantedCommand.wantedEndTimeMillis = System.currentTimeMillis() + Long.parseLong(minuteStr);
                                WantedCommand.wanted = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                                return new At(globalParam.ownerId).plus("悬赏令接取失败，识别时间失败");
                            }
                        }
                    }
                    if (SettingsCache.getInstance().getSettingsAsInt(BOT_RANK_KEYWORD, 1) < 29) {
                        return new PlainText("悬赏令接取" + (i + 1));
                    } else {
                        return new PlainText("渡劫境悬赏令接取" + (i + 1));
                    }
                }
            }

            if (SettingsCache.getInstance().getSettingsAsInt(BOT_RANK_KEYWORD, 1) < 29) {
                if (RANDOM.nextInt(100) < 50) {
                    return new PlainText("悬赏令刷新");
                }
            }
            return new PlainText("闭关");
        }), "AutoExerciseCommand");
    }

    /**
     * 是否接取悬赏令关键字判断
     *
     * @param str 悬赏令内容
     * @return 是否接取此悬赏令
     */
    private boolean needAward(String str) {
        List<String> wantedAwardKeyword = SettingsCache.getInstance().getSettings(WantedCommand.WANTED_AWARD_KEYWORD,
                s -> JSONObject.parseArray(s, String.class));
        return !CollectionUtils.isEmpty(wantedAwardKeyword)
                && wantedAwardKeyword.stream().anyMatch(str::contains);
    }

    /**
     * 宗门任务处理逻辑
     */
    private void registerTask() {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, notice) -> {
            if (notice == null) {
                return null;
            }
            if (!NEED_REFRESH.get()) {
                return null;
            }

            TaskStatusEnum taskStatusEnum = completeTask(System.currentTimeMillis(), notice);
            if (TaskStatusEnum.DO_IT.equals(taskStatusEnum)) {
                NEED_REFRESH.set(false);
                return new PlainText("宗门任务完成");
            }
            if (TaskStatusEnum.COMPLETE.equals(taskStatusEnum)) {
                COMPLETE_TASK_TIMES.set(3);
                COMPLETE_TASK_TIME_MILLIS.set(System.currentTimeMillis());
                MagicTaskObserver.cancelTask(TASK_PERIOD_KEY);
            }
            return null;
        }, "AutoTaskCommand");
    }

    /**
     * 某个时间点是否可以完成宗门任务
     *
     * @param timeMillis 时间点
     * @return 任务状态
     */
    private TaskStatusEnum tryCompleteTask(long timeMillis) {
        if (COMPLETE_TASK_TIMES.get() >= 3) {
            if (new Date(COMPLETE_TASK_TIME_MILLIS.get()).before(DateUtil.beginOfDay(new Date()))) {
                COMPLETE_TASK_TIMES.set(0);
                return TaskStatusEnum.DO_IT;
            } else {
                return TaskStatusEnum.COMPLETE;
            }
        }
        if (timeMillis - COMPLETE_TASK_TIME_MILLIS.get() <= 30 * 60 * 1000) {
            return TaskStatusEnum.IN_CD;
        }
        return TaskStatusEnum.DO_IT;
    }

    /**
     * 完成宗门任务
     *
     * @param timeMillis 时间点
     * @param msg        宗门任务内容
     * @return 任务状态
     */
    private TaskStatusEnum completeTask(long timeMillis, String msg) {
        TaskStatusEnum taskStatusEnum;
        if (!TaskStatusEnum.DO_IT.equals((taskStatusEnum = tryCompleteTask(timeMillis)))) {
            return taskStatusEnum;
        }

        if (msg.contains(TASK_COMPLETED_KEYWORD)) {
            return TaskStatusEnum.COMPLETE;
        }

        if (msg.contains(DAILY_PERIOD_KEYWORD)) {
            hasDailyPeriod = false;
            return TaskStatusEnum.IN_CD;
        }

        if (TASK_KEYWORD.stream().anyMatch(msg::contains)) {
            COMPLETE_TASK_TIME_MILLIS.set(timeMillis);
            COMPLETE_TASK_TIMES.addAndGet(1);
            return TaskStatusEnum.DO_IT;
        }

        return TaskStatusEnum.IGNORE;
    }

    @Getter
    @AllArgsConstructor
    public enum TaskStatusEnum {
        /**
         * 宗门任务有限状态机
         */
        IN_CD("IN_CD", "CD中"),
        COMPLETE("COMPLETE", "已完成"),
        IGNORE("IGNORE", "忽略此任务"),
        DO_IT("DO_IT", "完成任务"),
        ;
        private final String code;
        private final String name;
    }

    /**
     * 群boss状态
     */
    @Data
    @AllArgsConstructor
    private static class GroupBossStatus {
        private Long groupId;
        private Boolean bossAllDefeated;

        GroupBossStatus(Long groupId) {
            this.groupId = groupId;
            this.bossAllDefeated = false;
        }
    }
}
