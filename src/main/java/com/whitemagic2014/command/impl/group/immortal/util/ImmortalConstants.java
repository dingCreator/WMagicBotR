package com.whitemagic2014.command.impl.group.immortal.util;

import com.whitemagic2014.util.MagicMd5;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 常量
 * 
 * @author ding
 * @date 2023/12/15
 */
public class ImmortalConstants {

    // 指令关键字
    public static final String START_KEYWORD = "开始";
    public static final String STOP_KEYWORD1 = "结束";
    public static final String STOP_KEYWORD2 = "停止";
    public static final String SWITCH_KEYWORD = "切换";
    public static final String STATUS_KEYWORD = "状态";
    public static final String DAILY_KEYWORD = "日常";
    public static final String WANTED_KEYWORD = "悬赏令";
    public static final String UPGRADE_KEYWORD = "突破";
    public static final String NOW_ATTACK_BOSS_KEYWORD = "打boss";
    public static final String SEARCH_RANK_KEYWORD = "查询境界";
    public static final String EXPLORE_KEYWORD = "秘境";
    public static final String SET_SETTINGS_KEYWORD = "设置";

    public static final String PREVIEW_KEYWORD = "查看";
    public static final String ADD_KEYWORD1 = "增加";
    public static final String ADD_KEYWORD2 = "新增";
    public static final String DEL_KEYWORD = "删除";

    // 识别关键字
    public static final String TASK_COMPLETED_KEYWORD = "今日无法再获取";
    public static final String DAILY_PERIOD_KEYWORD = "还没有宗门任务";
    public static final String NO_BOSS_KEYWORD = "本群尚未生成";
    public static final String CANT_ATTACK_KEYWORD1 = "重伤未愈";
    public static final String CANT_ATTACK_KEYWORD2 = "重伤逃遁";
    public static final String CANT_ATTACK_KEYWORD3 = "吐血身亡";
    public static final String NUMBER_KEYWORD = "编号";
    public static final String HAS_BOSS_KEYWORD = "Boss";
    public static final List<String> TASK_KEYWORD = Arrays.asList("灰尘", "罕见", "不要多问", "工厂", "耗材");
    public static final String FARM_MONEY_STATICS_KEYWORD1 = "收获灵石";
    public static final String FARM_MONEY_STATICS_KEYWORD2 = "首领把灵石";
    public static final String FARM_MONEY_STATICS_KEYWORD3 = "首领看你可怜赠送灵石";
    public static final String FARM_POINT_STATICS_KEYWORD = "获得世界积分";

    // 周期任务key
    public static final String TASK_PERIOD_KEY = MagicMd5.getMd5String("autoTask");

    // 无内容识别注册key
    public static final String AUTO_COLLECT_KEY = "autoCollect";
    public static final String FARM_STATICS_KEY = "farmStatics";

    // 配置关键字
    public static final String MODE_KEYWORD = "修仙_修炼模式";
    public static final String AUTO_WANTED_KEYWORD = "修仙_悬赏令";
    public static final String AUTO_UPGRADE_KEYWORD = "修仙_突破模式";
    public static final String ATTACK_PERIOD_KEYWORD = "修仙_攻击间歇";
    public static final String ATTACK_RANDOM_PERIOD_KEYWORD = "修仙_随机攻击间歇";
    public static final String BOT_RANK_KEYWORD = "修仙_当前境界";
    public static final String LAST_LEVEL_UP_TIME_KEYWORD = "修仙_上次突破时间";
    public static final String LEVEL_UP_RANDOM_PERIOD_KEYWORD = "修仙_随机突破间歇";
    public static final String LAST_COLLECT_HERBS_TIME_KEYWORD = "修仙_前次收取药草时间";
    public static final String COLLECT_HERBS_PERIOD_KEYWORD = "修仙_收取药草间隔";
    public static final String EXPLORE_GROUPS_KEYWORD = "修仙_秘境群号";

    /**
     * 时间参数
     */
    public static final long IN_OUT_PERIOD = 60 * 1000L;
    public static final long IN_OUT_DELAY = 5 * 1000L;

    public static final long WANTED_PERIOD = 61 * 60 * 1000L;
    public static final long WANTED_DELAY = 10 * 1000L;

    public static final long IMPROVE_PERIOD = 61 * 60 * 1000L;
    public static final long IMPROVE_DELAY = 15 * 1000L;

    public static final long TASK_PERIOD = 6 * 60 * 1000L;
    public static final long TASK_DELAY = 2 * 1000L;

    public static final long COLLECT_HERBS_PERIOD = 30 * 1000L;
    public static final long COLLECT_HERBS_DELAY = 10 * 1000L;
    
    public static final int ATTACK_BOSS_PERIOD = 5 * 1000;
}
