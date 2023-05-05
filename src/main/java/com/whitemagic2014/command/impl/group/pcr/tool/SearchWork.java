package com.whitemagic2014.command.impl.group.pcr.tool;

import com.alibaba.fastjson.JSON;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.cache.HomeworkCache;
import com.whitemagic2014.cache.SettingsCache;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import lombok.Data;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.ForwardMessage;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.MessageUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Command
public class SearchWork extends NoAuthCommand {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GlobalParam globalParam;

    private static Date refreshTime = null;
    private static final String[] BOSS;
    private static final Object LOCK = new Object();
    private static final String HOMEWORK_URL = "https://www.caimogu.cc/gzlj/data?date=&lang=zh-cn";
    private static final String HOMEWORK_ICON_URL = "https://www.caimogu.cc/gzlj/data/icon?lang=zh-cn&date=";

    private static final String HOMEWORK_PAGE_SIZE = "会战作业_分页大小";

    private static String getFormatInfo() {
        return "格式： ${botName}会战作业 {a1/a2/b1……} [手动/自动/不限] [页码] [整刀/尾刀] "
                + "[希望包含的角色，若有多个用,分隔] [希望忽略掉的角色，若有多个用,分隔]\n"
                + "默认查询不限自动刀与手动刀，整刀，第一页数据，一页"
                + SettingsCache.getInstance().getSettingsAsInt(HOMEWORK_PAGE_SIZE, 5)
                + "条";
    }

    static {
        BOSS = new String[]{"A1", "A2", "A3", "A4", "A5",
                "B1", "B2", "B3", "B4", "B5",
                "C1", "C2", "C3", "C4", "C5",
                "D1", "D2", "D3", "D4", "D5",
                "E1", "E2", "E3", "E4", "E5"};
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "会战作业",
                globalParam.botNick + "会战作业");
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (CollectionUtils.isEmpty(args)) {
            return new PlainText(getFormatInfo());
        }

        // 检查boss代号
        String bossCode = args.get(0);
        final String finalBossCode = bossCode;
        if (Arrays.stream(BOSS).noneMatch(b -> b.equalsIgnoreCase(finalBossCode))) {
            return new PlainText(getFormatInfo());
        }

        // 初始化作业数据和icon缓存
        initIconDataMap();
        initHomeworkMap();

        // 获取单个boss作业
        bossCode = bossCode.toUpperCase();
        SearchWorkResponse.SearchWorkData data = HomeworkCache.getHomeworkMap().get(bossCode);
        if (data == null) {
            return new PlainText(getFormatInfo());
        }

        // 单个boss作业集
        List<SearchWorkResponse.SearchWorkData.Homework> homework = data.getHomework();
        // 是否是auto刀
        Boolean autoSign = null;
        if (args.size() >= 2) {
            if ("自动".equals(args.get(1)) || "auto".equalsIgnoreCase(args.get(1))) {
                autoSign = true;
            } else if ("手动".equals(args.get(1))) {
                autoSign = false;
            }
        }
        // 页码
        int page = 0;
        if (args.size() >= 3) {
            try {
                page = Integer.parseInt(args.get(2)) - 1;
            } catch (Exception e) {
                return new PlainText("输入的页码不是数字");
            }

            if (page < 0) {
                page = 0;
            }
        }
        // 整刀还是尾刀 默认整刀
        boolean remain = false;
        if (args.size() >= 4 && "尾刀".equals(args.get(3))) {
            remain = true;
        }
        // 包含的角色
        List<String> includes = new ArrayList<>();
        if (args.size() >= 5) {
            if (!StringUtils.isBlank(args.get(4)) && !"无".equals(args.get(4))) {
                includes = JSON.parseArray("[" + args.get(4).replace("，", ",") + "]", String.class);
            }
        }
        // 不包含的角色
        List<String> excludes = new ArrayList<>();
        if (args.size() >= 6) {
            if (!StringUtils.isBlank(args.get(5)) && !"无".equals(args.get(5))) {
                excludes = JSON.parseArray("[" + args.get(5).replace("，", ",") + "]", String.class);
            }
        }

        // 过滤作业
        List<SearchWorkResponse.SearchWorkData.Homework> homeWorkAfterFilter
                = homeworkFilter(autoSign, page, remain, includes, excludes, homework);
        if (CollectionUtils.isEmpty(homeWorkAfterFilter)) {
            return new PlainText("没有作业哦~");
        }

        // 组装返回消息
        List<String> preview = Collections.singletonList("数据来自花舞组作业网，请大家多多支持花舞组");
        String title = "点击查看作业详情";
        String brief = "";
        String source = "";
        String summary = "页数 " + page + 1 + "/"
                + homeWorkAfterFilter.size() / SettingsCache.getInstance().getSettingsAsInt(HOMEWORK_PAGE_SIZE, 5) + 1;

        List<ForwardMessage.Node> nodeList = new ArrayList<>();
        String bossName = HomeworkCache.getIconDataMap().get(Integer.parseInt(data.getId())).getIconValue();
        nodeList.add(buildNode(new PlainText(bossName)));
        homeWorkAfterFilter.forEach(hw -> {
            StringBuilder returnMsgBuilder = new StringBuilder();
            for (Integer id : hw.getUnit()) {
                returnMsgBuilder.append(HomeworkCache.getIconDataMap().get(id).getIconValue());
                returnMsgBuilder.append(" ");
            }
            returnMsgBuilder.append(hw.getAuto() == 1 ? "auto" : "手动").append(" ")
                    .append(hw.getRemain() == 0 ? "整刀" : "尾刀")
                    .append("\n伤害：")
                    .append(hw.getDamage())
                    .append(" w");
            nodeList.add(buildNode(new PlainText(returnMsgBuilder.toString())));

            if (!CollectionUtils.isEmpty(hw.getVideo())) {
                hw.getVideo().forEach(v -> {
                    String videoMsgBuilder = v.getText() + "\n" + v.getUrl() + "\n备注：" + v.getNote();
                    nodeList.add(buildNode(new PlainText(videoMsgBuilder)));
                });
            }

            nodeList.add(buildNode(new PlainText("------------")));
//            bot.getGroup(gId).sendMessage(new ForwardMessage(preview, title, brief, source, summary, nodeList));
//            allWorkNodeList.add(buildNode(new ForwardMessage(preview, title, brief, source, summary, nodeList)));
        });
//        bot.getGroup(gId).sendMessage(new ForwardMessage(preview, title, brief, source, summary, allWorkNodeList));
        return new ForwardMessage(preview, title, brief, source, summary, nodeList);
    }

    public void initIconDataMap() {
        initIconDataMap("");
    }

    public void initIconDataMap(String date) {
        if (CollectionUtils.isEmpty(HomeworkCache.getIconDataMap())) {
            synchronized (LOCK) {
                if (CollectionUtils.isEmpty(HomeworkCache.getIconDataMap())) {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add("x-requested-with", "XMLHttpRequest");
                    String homeworkIconUrl = StringUtils.isEmpty(date) ? HOMEWORK_ICON_URL : HOMEWORK_ICON_URL + date;
                    ResponseEntity<SearchIconResponse> entity = restTemplate.exchange(homeworkIconUrl,
                            HttpMethod.GET, new HttpEntity<>(null, httpHeaders), SearchIconResponse.class);
                    SearchIconResponse response = entity.getBody();
                    response.getData().forEach(iconDatas -> iconDatas.forEach(iconData -> HomeworkCache.getIconDataMap().put(iconData.getId(), iconData)));
                }
            }
        }
    }

    /**
     * 初始化本次会战作业
     */
    public void initHomeworkMap() {
        if (refreshTime == null || refreshTime.getTime() < System.currentTimeMillis()) {
            synchronized (LOCK) {
                if (refreshTime == null || refreshTime.getTime() < System.currentTimeMillis()) {
                    // 缓存有效期1h，避免频繁调用作业网
                    refreshTime = DateUtils.addHours(new Date(), 1);
                    HomeworkCache.getHomeworkMap().clear();

                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add("x-requested-with", "XMLHttpRequest");
                    ResponseEntity<SearchWorkResponse> entity = restTemplate.exchange(HOMEWORK_URL,
                            HttpMethod.GET, new HttpEntity<>(null, httpHeaders), SearchWorkResponse.class);
                    SearchWorkResponse response = entity.getBody();

                    int index = 1;
                    // 数据顺序为A1B1C1D1E1 A2B2C2D2E2 ……
                    for (SearchWorkResponse.SearchWorkData data : response.getData()) {
                        char c = (char) (data.getStage() - 1 + 'A');
                        String key = String.valueOf(c) + index;
                        HomeworkCache.getHomeworkMap().put(key, data);

                        if (c == 'E') {
                            index++;
                        }
                    }
                }
            }
        }
    }

    /**
     * 构建组合消息节点
     *
     * @param msg 信息
     * @return 节点
     */
    public ForwardMessage.Node buildNode(Message msg) {
        return new ForwardMessage.Node(globalParam.botId, (int) (System.currentTimeMillis() / 1000), globalParam.botNick,
                MessageUtils.newChain(msg));
    }

    /**
     * 过滤作业
     *
     * @param auto     是否是auto刀
     * @param page     页码
     * @param remain   是否是尾刀
     * @param includes 包含的角色
     * @param excludes 不包含的角色
     * @param source   全作业集
     * @return 过滤后的作业
     */
    public List<SearchWorkResponse.SearchWorkData.Homework> homeworkFilter(
            Boolean auto, int page, boolean remain, List<String> includes, List<String> excludes,
            List<SearchWorkResponse.SearchWorkData.Homework> source) {
        List<SearchWorkResponse.SearchWorkData.Homework> target = source.stream()
                .sorted(Comparator.comparing(SearchWorkResponse.SearchWorkData.Homework::getDamage).reversed())
                .filter(h -> {
                    // 是否auto
                    if (auto != null) {
                        if (auto) {
                            return h.getAuto() == 1;
                        } else {
                            return h.getAuto() == 2;
                        }
                    }
                    return true;
                }).filter(h -> {
                    // 是否尾刀
                    if (remain) {
                        return h.getRemain() == 1;
                    }
                    return h.getRemain() == 0;
                }).filter(h -> {
                    // 希望包含的角色
                    if (!CollectionUtils.isEmpty(includes)) {
                        List<Integer> roleIds = getRoleIds(includes);
                        if (!CollectionUtils.isEmpty(roleIds)) {
                            List<Integer> roleIdsCopy = new ArrayList<>(roleIds);
                            for (int u : h.getUnit()) {
                                if (roleIdsCopy.isEmpty()) {
                                    return true;
                                }
                                if (roleIds.contains(u)) {
                                    roleIdsCopy.remove(roleIdsCopy.indexOf(u));
                                }
                            }
                            return CollectionUtils.isEmpty(roleIdsCopy);
                        }
                    }
                    return true;
                }).filter(h -> {
                    // 希望排除的角色
                    if (!CollectionUtils.isEmpty(excludes)) {
                        List<Integer> roleIds = getRoleIds(excludes);
                        if (!CollectionUtils.isEmpty(roleIds)) {
                            for (int u : h.getUnit()) {
                                if (roleIds.contains(u)) {
                                    return false;
                                }
                            }
                            return true;
                        }
                    }
                    return true;
                }).collect(Collectors.toList());

        int pageSize = SettingsCache.getInstance().getSettingsAsInt(HOMEWORK_PAGE_SIZE, 5);
        int skip = page * pageSize;
        if (skip >= target.size()) {
            skip = page / pageSize * pageSize;
        }
        return target.stream().skip(skip).limit(pageSize).collect(Collectors.toList());
    }

    /**
     * 角色名转化为ID
     *
     * @param list 中文名
     * @return ids
     */
    private static List<Integer> getRoleIds(List<String> list) {
        return list.stream().map(name -> {
            Optional<SearchIconResponse.IconData> iconData = HomeworkCache.getIconDataMap().values().stream()
                    .filter(i -> i.getIconValue().equals(name)).findAny();
            return iconData.orElse(null);
        }).filter(Objects::nonNull).map(SearchIconResponse.IconData::getId).collect(Collectors.toList());
    }

    /**
     * 作业response
     */
    @Data
    public static class SearchWorkResponse implements Serializable {
        /**
         * 响应状态
         */
        private Integer status;
        /**
         * 作业数据
         */
        private List<SearchWorkData> data;

        @Data
        public static class SearchWorkData implements Serializable {
            private String id;
            /**
             * 阶段
             */
            private Integer stage;
            /**
             * 倍率
             */
            private Float rate;
            /**
             * boss信息
             */
            private String info;
            /**
             * 作业
             */
            private List<Homework> homework;

            @Data
            public static class Homework implements Serializable {

                private Integer id;

                private Integer bossId;

                private String sn;
                /**
                 * 角色Id
                 */
                private Integer[] unit;
                /**
                 * 伤害量
                 */
                private Integer damage;
                /**
                 * 是否是auto刀
                 */
                private Integer auto;
                /**
                 * 是否是尾刀
                 */
                private Integer remain;
                /**
                 *
                 */
                private String info;
                /**
                 * 视频信息
                 */
                private List<Video> video;

                @Data
                public static class Video implements Serializable {
                    /**
                     *
                     */
                    private String text;
                    /**
                     * 视频地址
                     */
                    private String url;
                    /**
                     * 备注
                     */
                    private String note;
                }
            }
        }
    }

    /**
     * icon response
     */
    @Data
    public static class SearchIconResponse {
        private Integer status;
        /**
         * icon数据
         */
        private List<List<IconData>> data;

        @Data
        public static class IconData {
            private Integer id;
            private Integer iconId;
            private String iconValue;
            private String iconFilePath;
        }
    }
}
