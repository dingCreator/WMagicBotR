package com.whitemagic2014.miniGame;

import java.util.ArrayList;
import java.util.List;

/**
 * 牌堆类
 *
 * @author huangkd
 * @date 2023/3/22
 */
public class CardLep {
    /**
     * 牌堆
     */
    private List<CardUtil.Card> cardList;
    /**
     * 已使用牌堆（不在玩家手上也不在牌堆的牌）
     */
    private final List<CardUtil.Card> usedCardList;

    public CardLep() {
        usedCardList = new ArrayList<>();
    }

    /**
     * 发一张牌
     *
     * @return 牌
     */
    public List<CardUtil.Card> deal() {
        return deal(1);
    }

    /**
     * 发多张牌
     *
     * @param num 牌数量
     * @return 牌
     */
    public List<CardUtil.Card> deal(int num) {
        if (num <= 0) {
            throw new IllegalArgumentException("deal nums must positive");
        }

        List<CardUtil.Card> list = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            list.add(cardList.remove(0));
        }
        return list;
    }

    /**
     * 初始化牌堆
     *
     * @return 初始化后的牌堆
     */
    public List<CardUtil.Card> initCards() {
        this.cardList = CardUtil.initCardsAsArray();
        CardUtil.shuffle(cardList);
        return this.cardList;
    }

    /**
     * 使用/弃置 卡牌
     *
     * @param list 牌
     */
    public void useCard(List<CardUtil.Card> list) {
        this.usedCardList.addAll(list);
    }

    /**
     * 清空已使用牌堆
     *
     * @return 清理前已使用牌堆的牌
     */
    public List<CardUtil.Card> clearUsedCards() {
        List<CardUtil.Card> r = new ArrayList<>(this.usedCardList);
        this.usedCardList.clear();
        return r;
    }
}
