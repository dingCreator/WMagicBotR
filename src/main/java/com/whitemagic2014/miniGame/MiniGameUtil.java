package com.whitemagic2014.miniGame;

import com.whitemagic2014.miniGame.strategy.BaseMiniGameStrategy;
import com.whitemagic2014.miniGame.strategy.MineSweeperStrategy;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 小游戏工具
 *
 * @author ding
 */
public class MiniGameUtil {
    /**
     * 正在进行中的小游戏
     */
    private static final Map<Long, BaseMiniGameStrategy> PLAYING_GAME = new ConcurrentHashMap<>();
    /**
     * 锁
     */
    private static final Object LOCK = new Object();

    /**
     * 开始游戏
     *
     * @param id           QQ号
     * @param miniGameEnum 游戏类型
     * @return 是否开始成功
     */
    public static boolean startGame(Long id, MiniGameEnum miniGameEnum) {
        try {
            if (!PLAYING_GAME.containsKey(id)) {
                PLAYING_GAME.put(id, miniGameEnum.getStrategy().newInstance());
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 结束游戏
     *
     * @param id QQ号
     * @return 是否结束成功
     */
    public static boolean stopGame(Long id) {
        if (PLAYING_GAME.containsKey(id)) {
            PLAYING_GAME.remove(id);
            return true;
        }
        return false;
    }

    /**
     * 判断该QQ号是否处于游戏状态
     *
     * @param id QQ号
     * @return 是/否
     */
    public static boolean playerIsPlayingGame(Long id) {
        return PLAYING_GAME.containsKey(id);
    }

    /**
     * 获取该QQ账号正在进行的游戏
     *
     * @param id QQ号
     * @return 正在进行的游戏逻辑
     * @throws NullPointerException npe
     */
    public static BaseMiniGameStrategy getPlayingGamesById(Long id) throws NullPointerException {
        return PLAYING_GAME.get(id);
    }

    /**
     * 是否有游戏进行中
     *
     * @return TRUE-无 FALSE-有
     */
    public static boolean noPlayingGames() {
        return PLAYING_GAME.isEmpty();
    }

    @Getter
    @AllArgsConstructor
    public enum MiniGameEnum {
        /**
         * 游戏类型
         */
        MINE_SWEEPER("MINE_SWEEPER", "扫雷", MineSweeperStrategy.class),
        BLACK_JACK("BLACK_JACK", "黑杰克", MineSweeperStrategy.class),
        ;

        private final String gameCode;
        private final String gameName;
        private final Class<? extends BaseMiniGameStrategy> strategy;
    }

    @Getter
    @AllArgsConstructor
    public enum MiniGameResultEnum {
        /**
         * 胜利
         */
        WIN("WIN"),
        /**
         * 游戏继续
         */
        CONTINUE("CONTINUE"),
        /**
         * 失败
         */
        LOSE("LOSE"),
        /**
         * 非游戏指令
         */
        NOT_GAME_COMMAND("NOT_GAME_COMMAND"),
        /**
         * 中止
         */
        STOP("STOP"),
        ;
        private final String name;
    }
}
