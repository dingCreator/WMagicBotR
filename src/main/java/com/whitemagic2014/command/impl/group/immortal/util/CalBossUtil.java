package com.whitemagic2014.command.impl.group.immortal.util;

import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.cache.SettingsCache;
import com.whitemagic2014.command.impl.group.immortal.CollectConfigCommand;
import com.whitemagic2014.command.impl.group.immortal.util.DTO.FarmDTO;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ding
 * @date 2023/4/19
 */
public class CalBossUtil {
    /**
     * 境界
     */
    private static final Map<String, Integer> RANK_MAP;

    static {
        RANK_MAP = new HashMap<>(32);
        RANK_MAP.put("江湖好手", 1);
        RANK_MAP.put("练气境", 2);
        RANK_MAP.put("筑基境", 5);
        RANK_MAP.put("结丹境", 8);
        RANK_MAP.put("元婴境", 11);
        RANK_MAP.put("元姗境", 11);
        RANK_MAP.put("化神境", 14);
        RANK_MAP.put("炼虚境", 17);
        RANK_MAP.put("合体境", 20);
        RANK_MAP.put("大乘境", 23);
        RANK_MAP.put("渡劫境", 26);
        RANK_MAP.put("半步真仙", 29);
        RANK_MAP.put("真仙境", 30);
        RANK_MAP.put("金仙境", 33);
        RANK_MAP.put("太乙境", 36);
    }

    private static final String[] MINOR_RANK = new String[]{"初期", "中期", "圆满"};
    private static final String BOSS_RANK_LIMIT = "修仙_攻击boss境界上限";

    public static int getRank(String info) {
        // 获取大境界
        String key = RANK_MAP.keySet().stream().filter(info::contains).findFirst().orElse(null);
        if (key == null) {
            return 0;
        }
        // 获取小境界
        int baseRank = RANK_MAP.get(key);
        int minorRank = 0;
        for (int i = 0; i < MINOR_RANK.length; i++) {
            if (info.contains(MINOR_RANK[i])) {
                minorRank = i;
                break;
            }
        }
        return baseRank + minorRank;
    }

    /**
     * 获取境界中文名
     *
     * @param rank 境界数字
     * @return 中文名
     */
    public static String getRankStr(int rank) {
        if (rank == 1) {
            return "江湖好手";
        }
        if (rank == 29) {
            return "半步真仙";
        }

        String key = RANK_MAP.entrySet().stream().filter(r -> (rank - r.getValue()) >= 0 && (rank - r.getValue()) < 3)
                .map(Map.Entry::getKey).findFirst().orElse("查询出错");
        if (!RANK_MAP.containsKey(key)) {
            return key;
        }
        return key + MINOR_RANK[rank - RANK_MAP.get(key)];
    }

    /**
     * 判断boss能不能打
     *
     * @param info 信息
     * @return 是否能打
     */
    public static boolean canAttackBoss(String info, int nowRank, long groupId) {
        int rank = CalBossUtil.getRank(info);
        if (rank <= 0) {
            return false;
        }
        // 低9个小境界以内（不包括9）可以打
        // 根据配置留点boss
        List<FarmDTO> list = SettingsCache.getInstance().getSettings(FarmDTO.SETTINGS_KEYWORD, str ->
                        JSONObject.parseArray(str, FarmDTO.class));
        if (CollectionUtils.isEmpty(list)) {
            return false;
        }
        int maxRank = list.stream().filter(farm -> farm.getGroupId().equals(groupId)).mapToInt(FarmDTO::getRank)
                .findFirst().orElse(0);
        return nowRank - rank <= 8 && rank <= maxRank;
    }
}
