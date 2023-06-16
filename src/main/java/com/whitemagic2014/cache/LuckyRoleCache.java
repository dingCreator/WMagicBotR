package com.whitemagic2014.cache;

import com.whitemagic2014.command.impl.group.pcr.tool.SearchWork;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 幸运角色缓存
 *
 * @author ding
 */
public class LuckyRoleCache {

    private static final Map<String, SearchWork.SearchIconResponse.IconData> LATEST_ROLE = new HashMap<>();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMddHH");
    private static final Object LOCK = new Object();

    private static String format(Date date) {
        synchronized (LOCK) {
            return SDF.format(date);
        }
    }

    public static boolean compareLatest(Long id) {
        String timeStr = format(new Date());
        return LATEST_ROLE.containsKey(id + timeStr);
    }

    public static void putLatest(Long id, SearchWork.SearchIconResponse.IconData iconData) {
        LATEST_ROLE.put(id + SDF.format(new Date()), iconData);
    }

    public static SearchWork.SearchIconResponse.IconData getLatestRole(Long id) {
        return LATEST_ROLE.get(id + SDF.format(new Date()));
    }
}
