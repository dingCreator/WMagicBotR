package com.whitemagic2014.command.impl.group.immortal.util;

import cn.hutool.core.util.RandomUtil;

/**
 * @author huangkd
 * @date 2023/5/9
 */
public class TimeUtil {
    /**
     * 随机延时防止tx检测
     */
    public static void waitRandomMillis(int base, int range) {
        try {
            Thread.sleep(base + RandomUtil.randomInt(range));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
