package com.whitemagic2014.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理数字工具类
 *
 * @author ding
 * @date 2023/5/8
 */
public class NumberFormatUtil {

    /**
     * 数字转中文数字
     *
     * @param num 数字
     * @return 汉字数字
     */
    public String getChnNum(int num) {

        return "";
    }

    /**
     * 中文数字转数字
     *
     * @param chnNum 汉字数字
     * @return 数字
     */
    public int getNum(String chnNum) {

        return 0;
    }

    @Getter
    @AllArgsConstructor
    private static enum ChineseNumber {
        /**
         * 数字列表
         */
        ZERO("零", 0),
        ONE("一", 1),
        TWO("二", 2),
        THREE("三", 3),
        FOUR("四", 4),
        FIVE("五", 5),
        SIX("六", 6),
        SEVEN("七", 7),
        EIGHT("八", 8),
        NINE("九", 9),
        TEN("十", 10),
        HUNDRED("百", 100),
        THOUSAND("千", 1_000),
        TEN_THOUSAND("万", 10_000),
        HUNDRED_MILLION("亿", 100_000_000),
        ;

        private final String chnNum;
        private final int val;
    }


    public static List<Integer> analyseNum(String str) {
        List<Integer> nums = new ArrayList<>();
        String numsStr = str.replace("，", ",");
        for (String numStr : numsStr.split(",")) {
            if (numStr.contains("-")) {
                String[] intervals = numsStr.split("-");
                if (intervals.length > 2) {
                    continue;
                }
                try {
                    int left = Integer.parseInt(intervals[0]);
                    int right = Integer.parseInt(intervals[1]);
                    for (int i = left; i <= right; i++) {
                        nums.add(i);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                try {
                    nums.add(Integer.parseInt(numStr));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return nums.stream().distinct().collect(Collectors.toList());
    }

    public static List<Long> numberListFormat(List<String> list) {
        try {
            return list.stream().map(Long::parseLong).collect(Collectors.toList());
        } catch (Exception e) {
        }
        return new ArrayList<>();
    }
}
