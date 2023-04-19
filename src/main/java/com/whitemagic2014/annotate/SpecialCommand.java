package com.whitemagic2014.annotate;

import java.lang.annotation.*;

/**
 *
 * @author huangkd
 * @date 2023/2/27
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpecialCommand {
    /**
     * 生效bot账号
     * @return
     */
    long[] affectBotIds();
    /**
     * 生效触发者账号
     * @return
     */
    long[] affectIds();
    /**
     * 是否忽略黑名单限制
     * @return
     */
    boolean ignoreBlacklist() default false;
    /**
     * 是否忽略白名单限制
     * @return
     */
    boolean ignoreWhitelist() default false;
}
