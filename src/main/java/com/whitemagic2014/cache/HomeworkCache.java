package com.whitemagic2014.cache;

import com.whitemagic2014.command.impl.group.pcr.tool.SearchWork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作业网缓存
 *
 * @author ding
 */
public class HomeworkCache {
    private static final Map<String, SearchWork.SearchWorkResponse.SearchWorkData> HOMEWORK_MAP = new HashMap<>();
    private static final Map<Integer, SearchWork.SearchIconResponse.IconData> ICON_DATA_MAP = new HashMap<>();
    private static final List<SearchWork.SearchIconResponse.IconData> ICON_DATA_LIST = new ArrayList<>();

    public static Map<String, SearchWork.SearchWorkResponse.SearchWorkData> getHomeworkMap() {
        return HOMEWORK_MAP;
    }

    public static Map<Integer, SearchWork.SearchIconResponse.IconData> getIconDataMap() {
        return ICON_DATA_MAP;
    }

    public static List<SearchWork.SearchIconResponse.IconData> getIconDataList() {
        return ICON_DATA_LIST;
    }
}
