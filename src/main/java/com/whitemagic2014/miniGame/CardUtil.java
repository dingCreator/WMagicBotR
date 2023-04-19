package com.whitemagic2014.miniGame;

import cn.hutool.core.util.RandomUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author huangkd
 * @date 2023/3/21
 */
public class CardUtil {

    @Getter
    @AllArgsConstructor
    public enum Suits {
        /**
         * 花色
         */
        DIAMOND(1, "DIAMOND", "♦", false),
        CLUB(2, "CLUB", "♣", false),
        HEART(3, "HEART", "♥", false),
        SPADE(4, "SPADE", "♠", false),
        BLACK(5, "SPADE", "小王", true),
        RED(6, "SPADE", "大王", true),
        ;

        private final int num;
        private final String name;
        private final String character;
        private final boolean joker;
    }

    @Getter
    @AllArgsConstructor
    public enum Points {
        /**
         * 点数
         */
        CARD_A(1, "A"),
        CARD_2(2, "2"),
        CARD_3(3, "3"),
        CARD_4(4, "4"),
        CARD_5(5, "5"),
        CARD_6(6, "6"),
        CARD_7(7, "7"),
        CARD_8(8, "8"),
        CARD_9(9, "9"),
        CARD_10(10, "10"),
        CARD_J(11, "J"),
        CARD_Q(12, "Q"),
        CARD_K(13, "K"),
        CARD_JOKER(14, "JOKER"),
        ;

        private final int point;
        private final String name;
    }

    @AllArgsConstructor
    @Data
    public static class Card {
        private Suits suits;
        private Points points;

        @Override
        public String toString() {
            return suits.getCharacter() + points.getName();
        }
    }

    private static final Random RANDOM = new Random();

    /**
     * 以数组形式返回顺序牌堆
     * 不包含joker
     *
     * @return card arrays
     */
    public static List<Card> initCardsAsArray() {
        return initCardsAsArray(false);
    }

    /**
     * 以数组形式返回顺序牌堆
     * 可指定是否包含joker
     *
     * @param includeJoker 是否包含joker
     * @return card arrays
     */
    public static List<Card> initCardsAsArray(boolean includeJoker) {
        List<Card> list = new ArrayList<>();
        for (Points p : Points.values()) {
            if ("JOKER".equals(p.getName()) && !includeJoker) {
                continue;
            }
            for (Suits s : Suits.values()) {
                if ("JOKER".equals(p.getName())) {
                    if (!s.isJoker()) {
                        continue;
                    }
                } else {
                    if (s.isJoker()) {
                        continue;
                    }
                }
                list.add(new Card(s, p));
            }
        }
        return list;
    }

    /**
     * 洗牌
     *
     * @param list 牌堆
     */
    public static void shuffle(List<Card> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        int lastIndex = list.size() - 1;
        for (; lastIndex > 0; lastIndex--) {
            int randIndex = RANDOM.nextInt(lastIndex);
            Card tmp = list.get(randIndex);
            list.set(randIndex, list.get(lastIndex));
            list.set(lastIndex, tmp);
        }
    }

    public static void main(String[] args) {
        List<Card> list = initCardsAsArray();
        shuffle(list);
        System.out.println(list);
    }
}
