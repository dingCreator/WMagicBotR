package com.whitemagic2014.command.impl.group.immortal;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.cache.SettingsCache;
import com.whitemagic2014.command.impl.group.EmptyStringCommand;
import com.whitemagic2014.command.impl.group.OwnerCommand;
import com.whitemagic2014.command.impl.group.immortal.util.CalBossUtil;
import com.whitemagic2014.command.impl.group.immortal.util.DTO.FarmDTO;
import com.whitemagic2014.command.impl.group.immortal.util.ImmortalConstants;
import com.whitemagic2014.command.impl.group.immortal.util.TimeUtil;
import com.whitemagic2014.pojo.ImmortalFarmRecord;
import com.whitemagic2014.service.ImmortalFarmRecordService;
import com.whitemagic2014.util.CommandThreadPoolUtil;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import com.whitemagic2014.util.MagicMd5;
import com.whitemagic2014.util.NumberFormatUtil;
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
import java.util.stream.Collectors;

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
    @Value("${immortal.mainGroup:0}")
    private long mainGroupId;

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
            new SearchRankKeyword(),
            new ExploreKeyword(),
            new SetSettingsKeyword()
    );

    // 中间过程变量
    /**
     * 是否继续打boss
     */
    private static final AtomicBoolean CONTINUE_ATTACK = new AtomicBoolean(true);
    /**
     * 是否有正在进行的日常周期任务
     */
    private boolean hasDailyPeriod;
    /**
     * 上次攻击boss时间
     */
    private long attackStartTimeMillis;
    /**
     * 上次完成宗门任务的时间
     */
    private final AtomicLong COMPLETE_TASK_TIME_MILLIS = new AtomicLong(0);
    /**
     * 宗门任务完成次数
     */
    private final AtomicInteger COMPLETE_TASK_TIMES = new AtomicInteger(0);
    /**
     * 是否是需要刷新
     */
    private final AtomicBoolean NEED_REFRESH = new AtomicBoolean(false);
    /**
     * 上次探索秘境时间
     */
    private long lastExploreTimeMillis;
    /**
     * 探索所需时间
     */
    private int exploreTime;
    /**
     * 是否正在探索秘境
     */
    private boolean exploring;
    /**
     * 是否在处理秘境过程中
     */
    private AtomicBoolean doingExplore = new AtomicBoolean(false);

    private static final Random RANDOM = new Random();

    @Autowired
    private GlobalParam globalParam;

    @Autowired
    private CommandThreadPoolUtil commandThreadPoolUtil;

    @Autowired
    private ImmortalFarmRecordService immortalFarmRecordService;

    @Override
    public CommandProperties properties() {
        return new CommandProperties("修仙");
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (CollectionUtils.isEmpty(args)) {
            return null;
        }
        String msg = args.remove(0).trim();
        ICommandKeyword cmd = COMMAND_KEY_WORD_LIST.stream().filter(c -> c.support(msg)).findFirst().orElse(null);
        if (cmd != null) {
            return cmd.execute(msg, args);
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
        Message execute(String keyword, List<String> args);
    }

    /**
     * 开始修仙指令
     */
    private class StartCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return ImmortalConstants.START_KEYWORD.equals(keyword);
        }

        @Override
        public Message execute(String keyword, List<String> args) {
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
            return ImmortalConstants.STOP_KEYWORD1.equals(keyword) || ImmortalConstants.STOP_KEYWORD2.equals(keyword);
        }

        @Override
        public Message execute(String keyword, List<String> args) {
            stopAuto();
            return new PlainText("停止自动修仙");
        }
    }

    /**
     * 切换修炼模式指令
     */
    private class SwitchCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return keyword.startsWith(ImmortalConstants.SWITCH_KEYWORD);
        }

        @Override
        public Message execute(String keyword, List<String> args) {
            if (CollectionUtils.isEmpty(args)) {
                return new PlainText("错误的模式");
            }
            if ("0".equals(args.get(0))) {
                SettingsCache.getInstance().setSettings(ImmortalConstants.MODE_KEYWORD, 0);
                return new PlainText("模式切换为 正常修炼");
            } else if ("1".equals(args.get(0))) {
                SettingsCache.getInstance().setSettings(ImmortalConstants.MODE_KEYWORD, 1);
                return new PlainText("模式切换为 左右互搏");
            } else if ("2".equals(args.get(0))) {
                SettingsCache.getInstance().setSettings(ImmortalConstants.MODE_KEYWORD, 2);
                return new PlainText("模式切换为 打boss");
            } else if ("3".equals(args.get(0))) {
                SettingsCache.getInstance().setSettings(ImmortalConstants.MODE_KEYWORD, 3);
                return new PlainText("模式切换为 秘境");
            }
            return new PlainText("错误的模式");
        }
    }

    /**
     * 查看状态指令
     */
    private class StatusCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return ImmortalConstants.STATUS_KEYWORD.equals(keyword);
        }

        @Override
        public Message execute(String keyword, List<String> args) {
            StringBuilder stringBuilder = new StringBuilder("当前修仙状态 ");
            stringBuilder.append(PERIOD_KEYS.size() > 0 ? "自动修仙" : "停止修仙");
            stringBuilder.append("\n当前修仙模式 ");

            switch (SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.MODE_KEYWORD, 0)) {
                case 0:
                    stringBuilder.append("正常修炼");
                    break;
                case 1:
                    stringBuilder.append("左右互搏");
                    break;
                case 2:
                    stringBuilder.append("打boss");
                    break;
                case 3:
                    stringBuilder.append("秘境");
                    stringBuilder.append("\n进入时间 ").append(new Date(lastExploreTimeMillis));
                    stringBuilder.append("\n持续时间 ").append(exploreTime).append("分钟");
                    break;
                default:
                    stringBuilder.append("错误的模式");
                    break;
            }

            stringBuilder.append("\n当前悬赏模式 ");
            boolean autoWanted = SettingsCache.getInstance().getSettingsAsBoolean(ImmortalConstants.AUTO_WANTED_KEYWORD, false);
            stringBuilder.append(autoWanted ? "接取" : "不接取");
            stringBuilder.append("\n当前突破模式 ");

            switch (SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.AUTO_UPGRADE_KEYWORD, 0)) {
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
                    .append(SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.ATTACK_PERIOD_KEYWORD, 1))
                    .append("分钟")
                    .append("\n随机攻击周期 ")
                    .append(SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.ATTACK_RANDOM_PERIOD_KEYWORD, 1))
                    .append("分钟")
                    .append("\n随机突破周期 ")
                    .append(SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.LEVEL_UP_RANDOM_PERIOD_KEYWORD, 1))
                    .append("分钟")
                    .append("\n当前设置的境界 ")
                    .append(SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.BOT_RANK_KEYWORD, 1))
                    .append("\n上次收取灵田时间 ")
                    .append(SettingsCache.getInstance().getSettings(ImmortalConstants.LAST_COLLECT_HERBS_TIME_KEYWORD))
                    .append("\n上次突破时间：")
                    .append(SettingsCache.getInstance().getSettings(ImmortalConstants.LAST_LEVEL_UP_TIME_KEYWORD))
            ;
            return new PlainText(stringBuilder.toString());
        }
    }

    /**
     * 做日常指令
     */
    private class DailyCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return ImmortalConstants.DAILY_KEYWORD.equals(keyword);
        }

        @Override
        public Message execute(String keyword, List<String> args) {
            registerTask();

            Bot bot = MagicBotR.getBot();
            bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("修仙签到"));
            bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("宗门丹药领取"));

            if (!hasDailyPeriod) {
                MagicPeriodTask.build(ImmortalConstants.TASK_PERIOD_KEY, () -> {
                    TaskStatusEnum taskStatusEnum = tryCompleteTask(System.currentTimeMillis());
                    if (TaskStatusEnum.DO_IT.equals(taskStatusEnum)) {
                        if (NEED_REFRESH.get()) {
                            bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("宗门任务刷新"));
                        } else {
                            bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("宗门任务接取"));
                            NEED_REFRESH.set(true);
                        }
                    }
                }).schedule(ImmortalConstants.TASK_DELAY, ImmortalConstants.TASK_PERIOD);
                hasDailyPeriod = true;
            }
            return new PlainText("开始做日常");
        }
    }

    /**
     * 自动接取悬赏令指令
     */
    private class WantedCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return keyword.startsWith(ImmortalConstants.WANTED_KEYWORD);
        }

        @Override
        public Message execute(String keyword, List<String> args) {
            if (CollectionUtils.isEmpty(args)) {
                return new PlainText("关键词错误");
            }
            if (ImmortalConstants.START_KEYWORD.equals(args.get(0))) {
                SettingsCache.getInstance().setSettings(ImmortalConstants.AUTO_WANTED_KEYWORD, true);
                return new PlainText("开始自动接悬赏");
            } else if (ImmortalConstants.STOP_KEYWORD1.equals(args.get(0)) || ImmortalConstants.STOP_KEYWORD2.equals(args.get(0))) {
                SettingsCache.getInstance().setSettings(ImmortalConstants.AUTO_WANTED_KEYWORD, false);
                return new PlainText("停止自动接悬赏");
            } else {
                return new PlainText("关键词错误");
            }
        }
    }

    /**
     * 突破模式指令
     */
    private class UpgradeCommandKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return keyword.startsWith(ImmortalConstants.UPGRADE_KEYWORD);
        }

        @Override
        public Message execute(String keyword, List<String> args) {
            if (CollectionUtils.isEmpty(args)) {
                return new PlainText("模式错误");
            }
            if ("0".equals(args.get(0))) {
                SettingsCache.getInstance().setSettings(ImmortalConstants.AUTO_UPGRADE_KEYWORD, 0);
                stopAutoUpgradeRank();
                return new PlainText("不自动突破");
            } else if ("1".equals(args.get(0))) {
                SettingsCache.getInstance().setSettings(ImmortalConstants.AUTO_UPGRADE_KEYWORD, 1);
                registerAutoUpgradeRank();
                return new PlainText("不使用丹药突破");
            } else if ("2".equals(args.get(0))) {
                SettingsCache.getInstance().setSettings(ImmortalConstants.AUTO_UPGRADE_KEYWORD, 2);
                registerAutoUpgradeRank();
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
            return ImmortalConstants.NOW_ATTACK_BOSS_KEYWORD.equalsIgnoreCase(keyword);
        }

        @Override
        public Message execute(String keyword, List<String> args) {
            if (SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.MODE_KEYWORD, 0) != 2) {
                return new PlainText("现在的模式不为打boss");
            }
            if (CollectionUtils.isEmpty(args)) {
                attackAllBoss(getCollectGroupIds());
            } else {
                List<Long> list;
                try {
                    list = args.stream().map(Long::parseLong).collect(Collectors.toList());
                } catch (Exception e) {
                    e.printStackTrace();
                    return new PlainText("输入的群号非数字");
                }
                attackAllBoss(list);
            }
            return new PlainText("开始打boss");
        }
    }

    /**
     * 文字等级对应数字等级
     */
    private class SearchRankKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return keyword.startsWith(ImmortalConstants.SEARCH_RANK_KEYWORD);
        }

        @Override
        public Message execute(String keyword, List<String> args) {
            String str = CollectionUtils.isEmpty(args) ? "" : args.get(0);
            return new PlainText("等级对应的数字为：" + CalBossUtil.getRank(str));
        }
    }

    /**
     * 探索秘境关键字
     */
    private class ExploreKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return keyword.equals(ImmortalConstants.EXPLORE_KEYWORD);
        }

        @Override
        public Message execute(String keyword, List<String> args) {
            if (CollectionUtils.isEmpty(args)) {
                return new PlainText("格式错误");
            }

            switch (args.get(0)) {
                case ImmortalConstants.PREVIEW_KEYWORD:
                    String config = SettingsCache.getInstance().getSettings(ImmortalConstants.EXPLORE_GROUPS_KEYWORD, preview -> {
                        List<Long> list = JSONObject.parseArray(preview, Long.class);
                        if (CollectionUtils.isEmpty(list)) {
                            return "";
                        }
                        StringBuilder builder = new StringBuilder("编号|群号");
                        int i = 1;
                        for (Long groupId : list) {
                            builder.append("\n").append(i).append("|").append(groupId);
                            i++;
                        }
                        return builder.toString();
                    });
                    return StringUtils.isEmpty(config) ? new PlainText("当前没有配置") : new PlainText(config);
                case ImmortalConstants.ADD_KEYWORD1:
                case ImmortalConstants.ADD_KEYWORD2:
                    if (args.size() <= 1) {
                        return new PlainText("请输入群号");
                    }
                    long groupId;
                    try {
                        groupId = Long.parseLong(args.get(1));
                    } catch (Exception e) {
                        return new PlainText("输入的群号非数字");
                    }

                    Bot bot = MagicBotR.getBot();
                    Group group = bot.getGroups().stream().filter(g -> g.getId() == groupId).findFirst().orElse(null);
                    if (group == null) {
                        return new PlainText(globalParam.botNick + "没有加入该群");
                    }

                    String str = SettingsCache.getInstance().getSettings(ImmortalConstants.EXPLORE_GROUPS_KEYWORD);
                    List<Long> list;
                    if (StringUtils.isEmpty(str)) {
                        list = new ArrayList<>();
                    } else {
                        list = JSONObject.parseArray(str, Long.class);
                    }
                    if (list.stream().anyMatch(g -> g.equals(groupId))) {
                        return new PlainText("该配置已存在");
                    }
                    list.add(groupId);
                    SettingsCache.getInstance().setSettings(ImmortalConstants.EXPLORE_GROUPS_KEYWORD, JSONObject.toJSONString(list));
                    return new PlainText("增加成功");
                case ImmortalConstants.DEL_KEYWORD:
                    if (args.size() <= 1) {
                        return new PlainText("请输入编号");
                    }
                    // 解析编号
                    List<Integer> nums = NumberFormatUtil.analyseNum(args.get(1));
                    if (CollectionUtils.isEmpty(nums)) {
                        return new PlainText("所有的编号均非法");
                    }

                    List<Long> groupList = SettingsCache.getInstance()
                            .getSettings(ImmortalConstants.EXPLORE_GROUPS_KEYWORD, g -> JSONObject.parseArray(g, Long.class));
                    List<Integer> sortedNums = nums.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
                    int count = 0;
                    for (int num : sortedNums) {
                        if (num <= groupList.size() && num > 0) {
                            groupList.remove(num - 1);
                            count++;
                        }
                    }
                    SettingsCache.getInstance().setSettings(ImmortalConstants.EXPLORE_GROUPS_KEYWORD, JSONObject.toJSONString(groupList));
                    return new PlainText("成功删除【" + count + "】条配置");
                default:
                    break;
            }
            return null;
        }
    }

    /**
     * 设置时间周期指令
     */
    private class SetSettingsKeyword implements ICommandKeyword {

        @Override
        public boolean support(String keyword) {
            return keyword.equals(ImmortalConstants.SET_SETTINGS_KEYWORD);
        }

        @Override
        public Message execute(String keyword, List<String> args) {
            if (args.size() < 2) {
                return new PlainText("参数不足");
            }
            int val;
            try {
                val = Integer.parseInt(args.get(1));
            } catch (Exception e) {
                return new PlainText("参数格式错误");
            }

            switch (args.get(0)) {
                case "攻击周期":
                    SettingsCache.getInstance().setSettings(ImmortalConstants.ATTACK_PERIOD_KEYWORD, val);
                    return new PlainText("更新成功");
                case "攻击随机周期":
                    SettingsCache.getInstance().setSettings(ImmortalConstants.ATTACK_RANDOM_PERIOD_KEYWORD, val);
                    return new PlainText("更新成功");
                case "突破随机周期":
                    SettingsCache.getInstance().setSettings(ImmortalConstants.LEVEL_UP_RANDOM_PERIOD_KEYWORD, val);
                    return new PlainText("更新成功");
                default:
                    return new PlainText("参数关键字错误");
            }
        }
    }

    /**
     * 获取收菜群号
     *
     * @return 群号
     */
    private List<Long> getCollectGroupIds() {
        List<FarmDTO> list = SettingsCache.getInstance().getSettings(FarmDTO.SETTINGS_KEYWORD, str ->
                JSONObject.parseArray(str, FarmDTO.class));
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        return list.stream().map(FarmDTO::getGroupId).collect(Collectors.toList());
    }

    /**
     * 开始自动修仙
     */
    public void startAuto() {
        Bot bot = MagicBotR.getBot();
        bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("闭关"));

        // 注册统计打boss指令
        registerFarmStatic();

        // 定时闭关出关
        String key = MagicMd5.getMd5String("exercise-inOut");
        PERIOD_KEYS.add(key);
        MagicPeriodTask.build(key, () -> {
            TimeUtil.waitRandomMillis(1000, 2000);
            switch (SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.MODE_KEYWORD, 0)) {
                case 0:
                    return;
                case 1:
                    if (System.currentTimeMillis() >= attackStartTimeMillis
                            + SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.ATTACK_PERIOD_KEYWORD, 2) * 60 * 1000) {
                        bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("抢劫").plus(makeAt(globalParam.botId)));
                        attackStartTimeMillis = System.currentTimeMillis();

                        bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("灵石出关"));
                        bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("闭关"));
                    }
                    break;
                case 2:
                    int cycle = 2 * SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.ATTACK_PERIOD_KEYWORD, 2) * 60 * 1000;
                    int randomCycle = RANDOM.nextInt(SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.ATTACK_RANDOM_PERIOD_KEYWORD, 2));
                    if (System.currentTimeMillis() >= attackStartTimeMillis + cycle) {
                        attackAllBoss(getCollectGroupIds());
                        attackStartTimeMillis = System.currentTimeMillis();
                    }
                    break;
                case 3:
                    if (doingExplore.get()) {
                        break;
                    }

                    if (System.currentTimeMillis() >= lastExploreTimeMillis + exploreTime * 60 * 1000 + 60 * 1000) {
                        if (exploring) {
                            bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("秘境结算"));
                            TimeUtil.waitRandomMillis(10 * 1000, 0);
                            exploring = false;
                        } else {
                            registerExplore();
                            doingExplore.set(true);

                            bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("灵石出关"));
                            commandThreadPoolUtil.addGroupTask(new PlainText("探索秘境"),
                                    JSONObject.parseArray(SettingsCache.getInstance().getSettings(ImmortalConstants.EXPLORE_GROUPS_KEYWORD), Long.class),
                                    botR -> {
                                        botR.getGroupOrFail(mainGroupId).sendMessage(new PlainText("秘境结算"));
                                        doingExplore.set(false);
                                    });
                        }
                    }
                    break;
                default:
                    break;
            }
        }).schedule(ImmortalConstants.IN_OUT_DELAY, ImmortalConstants.IN_OUT_PERIOD);

        key = MagicMd5.getMd5String("exercise-wanted");
        PERIOD_KEYS.add(key);
        MagicPeriodTask.build(key, () -> {
            if (!SettingsCache.getInstance().getSettingsAsBoolean(ImmortalConstants.AUTO_WANTED_KEYWORD, false)) {
                return;
            }
            TimeUtil.waitRandomMillis(5000, 5000);
            if (!WantedCommand.wanted || WantedCommand.wantedEndTimeMillis == 0) {
                bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("灵石出关"));
                if (SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.BOT_RANK_KEYWORD, 1) < 29) {
                    bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("悬赏令"));
                } else {
                    bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("渡劫境悬赏令"));
                }
                registerWanted();
            } else if (System.currentTimeMillis() > WantedCommand.wantedEndTimeMillis + 10000L) {
                bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("渡劫境悬赏令结算"));
                WantedCommand.wanted = false;
                WantedCommand.wantedEndTimeMillis = 0;
                bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("闭关"));
            }
        }).schedule(ImmortalConstants.WANTED_DELAY, ImmortalConstants.WANTED_PERIOD);

        // 自动突破
        key = MagicMd5.getMd5String("exercise-improve");
        PERIOD_KEYS.add(key);
        MagicPeriodTask.build(key, () -> {
            TimeUtil.waitRandomMillis(1000, 3000);
            int upgradeMode = SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.AUTO_UPGRADE_KEYWORD, 0);
            if (upgradeMode > 1) {
                registerAutoUpgradeRank();
                bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("突破 使用"));
            }
            if (upgradeMode > 0) {
                registerAutoUpgradeRank();
                bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("突破 不使用"));
            }
        }).schedule(ImmortalConstants.IMPROVE_DELAY, ImmortalConstants.IMPROVE_PERIOD);

        // 自动收取药草
        key = MagicMd5.getMd5String("exercise-collectHerbs");
        PERIOD_KEYS.add(key);
        MagicPeriodTask.build(key, () -> {
            Date date = new Date(0);
            String lastCollectTimeStr = SettingsCache.getInstance().getSettings(ImmortalConstants.LAST_COLLECT_HERBS_TIME_KEYWORD);
            if (!StringUtils.isEmpty(lastCollectTimeStr)) {
                date = DateUtil.parse(lastCollectTimeStr, "yyyy-MM-dd HH:mm:ss");
            }

            int collectHerbsPeriod = SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.COLLECT_HERBS_PERIOD_KEYWORD, 24);
            date = DateUtil.offsetHour(date, collectHerbsPeriod);
            date = DateUtil.offsetSecond(date, (int) ImmortalConstants.COLLECT_HERBS_DELAY);

            Date now = new Date();
            if (now.after(date)) {
                SettingsCache.getInstance().setSettings(ImmortalConstants.LAST_COLLECT_HERBS_TIME_KEYWORD, DateUtil.format(now, "yyyy-MM-dd HH:mm:ss"));
                bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("灵田收取"));
            }
        }).schedule(ImmortalConstants.COLLECT_HERBS_DELAY, ImmortalConstants.COLLECT_HERBS_PERIOD);
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
    public void attackAllBoss(List<Long> groupIds) {
        if (CollectionUtils.isEmpty(groupIds)) {
            return;
        }

        Bot bot = MagicBotR.getBot();
        Map<Long, GroupBossStatus> bossMap = initBossMap(groupIds);
        GROUP_BOSS_MAP.clear();
        GROUP_BOSS_MAP.putAll(bossMap);
        registerCollect();

        // 只出关一次即可
        bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("灵石出关"));
        bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("闭关"));

        CONTINUE_ATTACK.set(true);
        List<GroupBossStatus> randomSortBossList = new ArrayList<>(bossMap.values());
        Collections.shuffle(randomSortBossList);
        randomSortBossList.forEach(g -> {
            if (!CONTINUE_ATTACK.get()) {
                return;
            }
            if (!g.getBossAllDefeated()) {
                try {
                    bot.getGroupOrFail(g.getGroupId()).sendMessage(new PlainText("查询世界BOSS"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            TimeUtil.waitRandomMillis(3000, 3000);
        });
    }

    private Map<Long, GroupBossStatus> initBossMap(List<Long> groupIds) {
        Map<Long, GroupBossStatus> map = new HashMap<>();
        groupIds.forEach(id -> map.put(id, new GroupBossStatus(id)));
        return map;
    }

    /**
     * 自动打boss逻辑
     */
    private void registerCollect() {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, notice, atMe) -> {
            if (!atMe) {
                return null;
            }

            if (SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.MODE_KEYWORD, 0) != 2) {
                return null;
            }

            if (StringUtils.isEmpty(notice)) {
                return null;
            }

            if (!CONTINUE_ATTACK.get()) {
                return null;
            }
            long gId = sender.getGroup().getId();

            TimeUtil.waitRandomMillis(500, 500);
            Bot bot = MagicBotR.getBot();
            if (notice.contains(ImmortalConstants.NO_BOSS_KEYWORD)) {
                // 没有boss了
                GROUP_BOSS_MAP.get(gId).setBossAllDefeated(true);
            } else if (notice.contains(ImmortalConstants.NUMBER_KEYWORD) && notice.contains(ImmortalConstants.HAS_BOSS_KEYWORD)) {
                // 避免踩雷
                String[] bossInfoArray = notice.split("\n");
                List<String> bossInfo = new ArrayList<>();
                for (String info : bossInfoArray) {
                    if (info.contains(ImmortalConstants.HAS_BOSS_KEYWORD)) {
                        bossInfo.add(info);
                    }
                }

                if (CollectionUtils.isEmpty(bossInfo)) {
                    return null;
                }
                Stack<Integer> bossIndex = new Stack<>();
                for (int i = 0; i < bossInfo.size(); i++) {
                    if (CalBossUtil.canAttackBoss(bossInfo.get(i),
                            SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.BOT_RANK_KEYWORD, 1), gId)) {
                        bossIndex.push(i + 1);
                    }
                }
                if (bossIndex.empty()) {
                    return null;
                }
                GROUP_BOSS_MAP.get(gId).setBossAllDefeated(false);

                while (!GROUP_BOSS_MAP.get(gId).getBossAllDefeated() && CONTINUE_ATTACK.get() && !bossIndex.empty()) {
                    bot.getGroupOrFail(gId).sendMessage(new PlainText("感化首领" + bossIndex.pop()));
                    TimeUtil.waitRandomMillis(ImmortalConstants.ATTACK_BOSS_PERIOD, 3000);
                }
            } else if (notice.contains(ImmortalConstants.CANT_ATTACK_KEYWORD1)
                    || notice.contains(ImmortalConstants.CANT_ATTACK_KEYWORD2)
                    || notice.contains(ImmortalConstants.CANT_ATTACK_KEYWORD3)) {
                // 重伤
                CONTINUE_ATTACK.set(false);
                List<Message> msgList = new ArrayList<>();
                msgList.add(new PlainText("感化【" + sender.getGroup().getName() + "】boss失败"));

                Healing healing = Healing.getRandom(SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.BOT_RANK_KEYWORD, 1));
                if (healing != null) {
                    int i = 0;
                    do {
                        if (healing.getNumber() > 0) {
                            msgList.add(new PlainText("百货购买" + healing.getNumber()));
                        }
                        msgList.add(new PlainText("使用" + healing.getName()));
                        i++;
                    } while (i * healing.getEffect() < 30);
                }
                commandThreadPoolUtil.addGroupTask(msgList, mainGroupId);
            }
            return null;
        }, ImmortalConstants.AUTO_COLLECT_KEY);
    }

    /**
     * 统计农场数据
     */
    private void registerFarmStatic() {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, notice, atMe) -> {
            if (!atMe) {
                return null;
            }
            boolean farmMoneyStaticsKeyword = notice.contains(ImmortalConstants.FARM_MONEY_STATICS_KEYWORD1)
                    || notice.contains(ImmortalConstants.FARM_MONEY_STATICS_KEYWORD2)
                    || notice.contains(ImmortalConstants.FARM_MONEY_STATICS_KEYWORD3);

            if (farmMoneyStaticsKeyword && notice.contains(ImmortalConstants.FARM_POINT_STATICS_KEYWORD)) {
                String[] info = notice.replace("，", ",").split(",");
                int point = 0, money = 0;
                String moneyStr = "0", pointStr = "0";
                List<Message> err = new ArrayList<>();
                for (String i : info) {
                    if (i.startsWith(ImmortalConstants.FARM_MONEY_STATICS_KEYWORD1)) {
                        moneyStr = i.replace(ImmortalConstants.FARM_MONEY_STATICS_KEYWORD1, "")
                                .replace("枚", "").trim();
                    } else if (i.startsWith(ImmortalConstants.FARM_MONEY_STATICS_KEYWORD2)) {
                        moneyStr = i.replace(ImmortalConstants.FARM_MONEY_STATICS_KEYWORD2, "")
                                .replace("枚赠送给你发展事业", "").trim();
                    } else if (i.startsWith(ImmortalConstants.FARM_MONEY_STATICS_KEYWORD3)) {
                        moneyStr = i.replace(ImmortalConstants.FARM_MONEY_STATICS_KEYWORD3, "")
                                .replace("枚", "").trim();
                    } else if (i.startsWith(ImmortalConstants.FARM_POINT_STATICS_KEYWORD)) {
                        pointStr = i.replace(ImmortalConstants.FARM_POINT_STATICS_KEYWORD, "")
                                .replace("：", "").replace(":", "")
                                .replace("点", "").trim();
                    }
                }
                try {
                    money = Integer.parseInt(moneyStr);
                    point = Integer.parseInt(pointStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    err.add(new PlainText("解析灵石或积分失败"));
                }
                if (err.size() > 0) {
                    commandThreadPoolUtil.addGroupTask(err, sender.getGroup().getId());
                    return null;
                }
                ImmortalFarmRecord immortalFarmRecord = new ImmortalFarmRecord();
                immortalFarmRecord.setGroupId(sender.getGroup().getId());
                immortalFarmRecord.setMoney(money);
                immortalFarmRecord.setPoint(point);
                immortalFarmRecord.setDate(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
                immortalFarmRecordService.createRecord(immortalFarmRecord);
            }
            return null;
        }, ImmortalConstants.FARM_STATICS_KEY);
    }


    /**
     * 悬赏令识别处理逻辑
     * 由于使用的文字识别算法有一定的错误率，建议关键词列举多种情况且不要轻信自动结算功能
     */
    private void registerWanted() {
        // 悬赏令
        EmptyStringCommand.addLogic(((sender, args, messageChain, subject, notice, atMe) -> {
            if (!atMe || WantedCommand.wanted || notice == null || !notice.contains("个人悬赏令")) {
                return null;
            }

            removeWanted();
            String[] notices = notice.split("!");
            if (notices.length == 1) {
                notices = notice.split("！");
            }

            Bot bot = MagicBotR.getBot();
            if (notices.length != 4 && needAward(notice)) {
                bot.getGroupOrFail(sender.getGroup().getId()).sendMessage(new At(globalParam.ownerId).plus("识别悬赏令失败"));
            } else {
                for (int i = 0; i < 3; i++) {
                    if (needAward(notices[i].trim().replace(" ", ""))) {
                        String[] sentences = notices[i].split(",");
                        for (String sentence : sentences) {
                            if (sentence.contains("，")) {
                                sentence = sentence.split("，")[0];
                            }
                            if (sentence.startsWith("预计需")) {
                                String minuteStr = sentence.replace("预计需", "").replace("分钟", "").trim();
                                try {
                                    WantedCommand.wantedEndTimeMillis = System.currentTimeMillis()
                                            + Long.parseLong(minuteStr) * 60 * 1000L;
                                    WantedCommand.wanted = true;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    bot.getGroupOrFail(sender.getGroup().getId())
                                            .sendMessage(new At(globalParam.ownerId).plus("接取悬赏令失败，识别时间失败"));
                                }
                            }
                        }
                        if (SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.BOT_RANK_KEYWORD, 1) < 29) {
                            return new PlainText("悬赏令接取" + (i + 1));
                        } else {
                            return new PlainText("渡劫境悬赏令接取" + (i + 1));
                        }
                    }
                }

                if (SettingsCache.getInstance().getSettingsAsInt(ImmortalConstants.BOT_RANK_KEYWORD, 1) < 29) {
                    if (RANDOM.nextInt(100) < 30) {
                        return new PlainText("悬赏令刷新");
                    }
                }
            }
            return new PlainText("闭关");
        }), "AutoExerciseCommand");
    }

    /**
     * 取消悬赏令识别
     */
    private void removeWanted() {
        EmptyStringCommand.removeLogic("AutoExerciseCommand");
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

    // 自动更新当前境界
    private void registerAutoUpgradeRank() {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, notice, atMe) -> {
            if (!atMe) {
                return null;
            }
            if (CollectionUtils.isEmpty(args)) {
                return null;
            }
            String content = args.get(0);
            if (!content.contains("恭喜道友突破") || !content.contains("成功")) {
                return null;
            }
            String rankStr = content.replace("恭喜道友突破", "").replace("成功", "").trim();
            int rank = CalBossUtil.getRank(rankStr);

            List<Message> msgList = new ArrayList<>();

            switch (rank) {
                case 4:
                    // 炼气圆满
                    msgList.add(new PlainText("使用筑基丹"));
                    break;
                case 5:
                    // 筑基初期
                    for (int i = 0; i < 6; i++) {
                        msgList.add(new PlainText("使用摄魂鬼丸"));
                        msgList.add(new PlainText("使用健身丹"));
                    }
                    break;
                case 7:
                    // 筑基圆满
                    msgList.add(new PlainText("使用朝元丹"));
                    msgList.add(new PlainText("使用聚顶丹"));
                    break;
                case 9:
                    // 金丹初期
                    for (int i = 0; i < 6; i++) {
                        msgList.add(new PlainText("使用摄魂鬼丸"));
                        msgList.add(new PlainText("使用生命丹"));
                    }
                case 10:
                    // 金丹圆满
                    msgList.add(new PlainText("使用锻脉丹"));
                    msgList.add(new PlainText("使用护脉丹"));
                    break;
                case 11:
                    // 元婴初期
                    for (int i = 0; i < 6; i++) {
                        msgList.add(new PlainText("使用摄魂鬼丸"));
                        msgList.add(new PlainText("使用养怡丹"));
                    }
                    break;
                case 13:
                    // 元婴圆满
                    msgList.add(new PlainText("使用天命淬体丹"));
                    msgList.add(new PlainText("使用澄心塑魂丹"));
                    msgList.add(new PlainText("使用混元仙体丹"));
                    break;
                case 14:
                    // 化神初期
                    for (int i = 0; i < 6; i++) {
                        msgList.add(new PlainText("使用摄魂鬼丸"));
                        msgList.add(new PlainText("使用活仙心丹"));
                    }
                    break;
                case 16:
                    // 化神圆满
                    msgList.add(new PlainText("使用黑炎丹"));
                    msgList.add(new PlainText("使用金血丸"));
                    break;
                case 17:
                    // 炼虚初期
                    for (int i = 0; i < 6; i++) {
                        msgList.add(new PlainText("使用摄魂鬼丸"));
                        msgList.add(new PlainText("使用三纹凝血丹"));
                    }
                    break;
                case 19:
                    // 炼虚圆满
                    msgList.add(new PlainText("使用虚灵丹"));
                    msgList.add(new PlainText("使用净明丹"));
                    break;
                case 20:
                    // 合体初期
                    for (int i = 0; i < 6; i++) {
                        msgList.add(new PlainText("使用摄魂鬼丸"));
                        msgList.add(new PlainText("使用复灵红丹"));
                    }
                    break;
                case 22:
                    // 合体圆满
                    msgList.add(new PlainText("使用安神灵液"));
                    msgList.add(new PlainText("使用魇龙之血"));
                    break;
                case 23:
                    // 大乘初期
                    for (int i = 0; i < 6; i++) {
                        msgList.add(new PlainText("使用幽冥寿丹"));
                        msgList.add(new PlainText("使用幽冥寿丹"));
                    }
                    break;
                case 25:
                    // 大乘圆满
                    msgList.add(new PlainText("使用化劫丹"));
                    break;
                case 26:
                    // 渡劫初期
                    for (int i = 0; i < 6; i++) {
                        msgList.add(new PlainText("使用摄魂鬼丸"));
                        msgList.add(new PlainText("使用九阳大还丹"));
                    }
                    break;
                case 29:
                    // 半步真仙
                    msgList.add(new PlainText("使用太上玄门丹"));
                    break;
                case 32:
                    // 真仙圆满
                    msgList.add(new PlainText("使用金仙破厄丹"));
                    break;
                case 33:
                    // 金仙初期
                    for (int i = 0; i < 6; i++) {
                        msgList.add(new PlainText("使用摄魂鬼丸"));
                        msgList.add(new PlainText("使用九阳大还丹"));
                    }
                    break;
                case 35:
                    // 金仙圆满
                    msgList.add(new PlainText("使用太乙炼髓丹"));
                    break;
                case 36:
                    // 太乙初期
                    for (int i = 0; i < 6; i++) {
                        msgList.add(new PlainText("使用摄魂鬼丸"));
                        msgList.add(new PlainText("使用混沌天罗丹"));
                    }
                    break;
                default:
                    break;
            }
            SettingsCache.getInstance().setSettings(ImmortalConstants.BOT_RANK_KEYWORD, rank);
            return new PlainText("更新当前境界成功");
        }, "immortal-auto-upgrade-rank");
    }

    private void stopAutoUpgradeRank() {
        EmptyStringCommand.removeLogic("immortal-auto-upgrade-rank");
    }

    /**
     * 宗门任务处理逻辑
     */
    private void registerTask() {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, notice, atMe) -> {
            if (!atMe) {
                return null;
            }
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
                try {
                    MagicTaskObserver.cancelTask(ImmortalConstants.TASK_PERIOD_KEY);
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                }
            }
            return null;
        }, "AutoTaskCommand");
    }

    private void registerExplore() {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, text, atMe) -> {
            if (!atMe) {
                return null;
            }

            String[] contents = text.split("，");
            if (contents.length < 2) {
                contents = text.split(",");
            }
            if (contents.length < 2 || !contents[1].contains("预计")) {
                removeExplore();
                SettingsCache.getInstance().setSettings(ImmortalConstants.MODE_KEYWORD, 2);

                Bot bot = MagicBotR.getBot();
                bot.getGroupOrFail(mainGroupId).sendMessage(new PlainText("闭关"));
                doingExplore.set(false);
                return new PlainText("当前无法探索秘境，已切换模式为打boss");
            }

            String minStr = contents[1].replace("预计", "").replace("分钟后可结束", "").trim();
            int minute;
            try {
                minute = Integer.parseInt(minStr);
            } catch (Exception e) {
                removeExplore();
                doingExplore.set(false);
                return new At(globalParam.ownerId).plus("秘境时间识别失败");
            }


            removeExplore();
            exploreTime = minute;
            lastExploreTimeMillis = System.currentTimeMillis();
            exploring = true;
            doingExplore.set(false);
            return new PlainText("识别秘境时间完毕");
        }, "AutoExplore");
    }

    private void removeExplore() {
        EmptyStringCommand.removeLogic("AutoExplore");
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

        if (msg.contains(ImmortalConstants.TASK_COMPLETED_KEYWORD)) {
            return TaskStatusEnum.COMPLETE;
        }

        if (msg.contains(ImmortalConstants.DAILY_PERIOD_KEYWORD)) {
            hasDailyPeriod = false;
            return TaskStatusEnum.IN_CD;
        }

        if (ImmortalConstants.TASK_KEYWORD.stream().anyMatch(msg::contains)) {
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

        public GroupBossStatus(Long groupId) {
            this.groupId = groupId;
            this.bossAllDefeated = false;
        }
    }

    @AllArgsConstructor
    @Getter
    private enum Healing {
        /**
         * 回血丹药
         */
        SHENG_GU("生骨丹", 5, 8, 2),
        HUA_YU("化瘀丹", 10, 9, 5),
        GU_YUAN("固元丹", 15, 10, 8),
        PEI_YUAN("培元丹", 20, 11, 11),
        HUANG_LONG("黄龙丹", 25, 12, 14),
        HUI_YUAN("回元丹", 30, 13, 17),
        HUI_CHUN("回春丹", 35, 14, 20),
        YANG_YUAN("养元丹", 40, 15, 23),
        TAI_YUAN("太元真丹", 45, 16, 26),
        JIU_YANG("九阳真丹", 50, 17, 29),
        GUI_ZANG("归藏灵丹", 60, 18, 33),
        TIAN_MING("天命血凝丹", 100, -1, 33),
        ;

        private final String name;
        private final Integer effect;
        private final Integer number;
        private final Integer lowestRank;

        public static Healing getRandom(int rank) {
            if (rank <= 1) {
                return null;
            }
            int length = Healing.values().length;
            int rand = RANDOM.nextInt(length);
            return Healing.values()[rand];
        }
    }
}
