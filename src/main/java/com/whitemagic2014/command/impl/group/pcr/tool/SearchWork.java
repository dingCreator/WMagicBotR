package com.whitemagic2014.command.impl.group.pcr.tool;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.cache.HomeworkCache;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.pojo.CommandProperties;
import lombok.Data;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.*;
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
    private static Date refreshTime = null;
    private static final String[] BOSS;
    private static final Object LOCK = new Object();
    private static final String HOMEWORK_URL = "https://www.caimogu.cc/gzlj/data?date=&lang=zh-cn";
    private static final String HOMEWORK_ICON_URL = "https://www.caimogu.cc/gzlj/data/icon?lang=zh-cn&date=";
    private static final String FORMAT_INFO = "格式： 初音作业(a1/a2/b1……)(手动/自动)(希望包含的角色，用[]包住，用空格分隔)(希望忽略掉的角色，用{}包住，用空分隔)(<页数>)。其中BOSS代号必填，其余选填";

    static {
        BOSS = new String[]{"A1", "A2", "A3", "A4", "A5", "a1", "a2", "a3", "a4", "a5",
                "B1", "B2", "B3", "B4", "B5", "b1", "b2", "b3", "b4", "b5",
                "C1", "C2", "C3", "C4", "C5", "c1", "c2", "c3", "c4", "c5",
                "D1", "D2", "D3", "D4", "D5", "d1", "d2", "d3", "d4", "d5",
                "E1", "E2", "E3", "E4", "E5", "e1", "e2", "e3", "e4", "e5"};
    }

    public SearchWork() {
        this.like = true;
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("初音作业");
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        String msg = Objects.requireNonNull(messageChain.get(OnlineMessageSource.Incoming.FromGroup.Key)).getOriginalMessage().toString();
        boolean flag = false;
        for (String bossAlias : BOSS) {
            if (msg.contains(bossAlias)) {
                flag = true;
                break;
            }
        }

        if (!flag) {
            return new PlainText(FORMAT_INFO);
        }

        initIconDataMap();
        initHomeworkMap();

        SearchWorkResponse.SearchWorkData data;
        if (msg.contains("auto")) {
            msg = msg.replace("auto", "自动");
        }

        if (msg.contains("A") || msg.contains("a")) {
            String msg1 = msg.replace("a", "A");
            if (msg1.contains("A1")) {
                data = HomeworkCache.getHomeworkMap().get("A1");
            } else if (msg1.contains("A2")) {
                data = HomeworkCache.getHomeworkMap().get("A2");
            } else if (msg1.contains("A3")) {
                data = HomeworkCache.getHomeworkMap().get("A3");
            } else if (msg1.contains("A4")) {
                data = HomeworkCache.getHomeworkMap().get("A4");
            } else if (msg1.contains("A5")) {
                data = HomeworkCache.getHomeworkMap().get("A5");
            } else {
                return new PlainText(FORMAT_INFO);
            }
        } else if (msg.contains("B") || msg.contains("b")) {
            String msg1 = msg.replace("b", "B");
            if (msg1.contains("B1")) {
                data = HomeworkCache.getHomeworkMap().get("B1");
            } else if (msg1.contains("B2")) {
                data = HomeworkCache.getHomeworkMap().get("B2");
            } else if (msg1.contains("B3")) {
                data = HomeworkCache.getHomeworkMap().get("B3");
            } else if (msg1.contains("B4")) {
                data = HomeworkCache.getHomeworkMap().get("B4");
            } else if (msg1.contains("B5")) {
                data = HomeworkCache.getHomeworkMap().get("B5");
            } else {
                return new PlainText(FORMAT_INFO);
            }
        } else if (msg.contains("C") || msg.contains("c")) {
            String msg1 = msg.replace("c", "C");
            if (msg1.contains("C1")) {
                data = HomeworkCache.getHomeworkMap().get("C1");
            } else if (msg1.contains("C2")) {
                data = HomeworkCache.getHomeworkMap().get("C2");
            } else if (msg1.contains("C3")) {
                data = HomeworkCache.getHomeworkMap().get("C3");
            } else if (msg1.contains("C4")) {
                data = HomeworkCache.getHomeworkMap().get("C4");
            } else if (msg1.contains("C5")) {
                data = HomeworkCache.getHomeworkMap().get("C5");
            } else {
                return new PlainText(FORMAT_INFO);
            }
        } else if (msg.contains("D") || msg.contains("d")) {
            String msg1 = msg.replace("d", "D");
            if (msg1.contains("D1")) {
                data = HomeworkCache.getHomeworkMap().get("D1");
            } else if (msg1.contains("D2")) {
                data = HomeworkCache.getHomeworkMap().get("D2");
            } else if (msg1.contains("D3")) {
                data = HomeworkCache.getHomeworkMap().get("D3");
            } else if (msg1.contains("D4")) {
                data = HomeworkCache.getHomeworkMap().get("D4");
            } else if (msg1.contains("D5")) {
                data = HomeworkCache.getHomeworkMap().get("D5");
            } else {
                return new PlainText(FORMAT_INFO);
            }
        } else if (msg.contains("E") || msg.contains("e")) {
            return new PlainText("E面还没开放呢！这么想坐牢吗？");
        } else {
            return new PlainText(FORMAT_INFO);
        }

        List<SearchWorkResponse.SearchWorkData.Homework> homework = data.getHomework();
        List<SearchWorkResponse.SearchWorkData.Homework> homeWorkAfterFilter = homeworkFilter(msg, homework);
        if (CollectionUtils.isEmpty(homeWorkAfterFilter)) {
            return new PlainText("没有作业哦~");
        }

        List<String> preview = Collections.singletonList("数据来自花舞组作业网，请大家多多支持花舞组");
        String title = "作业来啦~ Kira☆";
        String brief = "";
        String source = "";
        String summary = "点击查看作业详情";

        long gId = sender.getGroup().getId();
        Bot bot = MagicBotR.getBot();

        int page = 1;
        if (msg.contains("<") && msg.contains(">")) {
            String pageStr = msg.substring(msg.indexOf("<") + 1, msg.indexOf(">"));
            try {
                page = Integer.parseInt(pageStr);
            } catch (Exception e) {
                return new PlainText("请输入正确的页码");
            }
        }
        homeWorkAfterFilter.stream().sorted(Comparator.comparing(SearchWorkResponse.SearchWorkData.Homework::getDamage).reversed())
                .skip((page - 1) * 5L).limit(5).forEach(hw -> {
                    List<ForwardMessage.Node> nodeList = new ArrayList<>();
                    StringBuilder returnMsgBuilder = new StringBuilder();
                    for (Integer id : hw.getUnit()) {
                        returnMsgBuilder.append(HomeworkCache.getIconDataMap().get(id).getIconValue());
                        returnMsgBuilder.append(" ");
                    }
                    returnMsgBuilder.append(hw.getAuto() == 1 ? "auto" : "手动").append(" ")
                            .append(hw.getRemain() == 0 ? "整刀" : "尾刀")
                            .append("\n伤害：")
                            .append("w");
                    nodeList.add(buildNode(new PlainText(returnMsgBuilder.toString())));

                    if (!CollectionUtils.isEmpty(hw.getVideo())) {
                        hw.getVideo().forEach(v -> {
                            StringBuilder videoMsgBuilder = new StringBuilder();
                            videoMsgBuilder.append(v.getText());
                            videoMsgBuilder.append("\n");
                            videoMsgBuilder.append(v.getUrl());
                            videoMsgBuilder.append("\n备注：");
                            videoMsgBuilder.append(v.getNote());
                            nodeList.add(buildNode(new PlainText(videoMsgBuilder.toString())));
                        });
                    }
                    bot.getGroup(gId).sendMessage(new ForwardMessage(preview, title, brief, source, summary, nodeList));
//            allWorkNodeList.add(buildNode(new ForwardMessage(preview, title, brief, source, summary, nodeList)));
                });
//        bot.getGroup(gId).sendMessage(new ForwardMessage(preview, title, brief, source, summary, allWorkNodeList));
//        return new ForwardMessage(preview, title, brief, source, summary, nodeList);
        return null;
    }

    @Data
    public static class SearchWorkResponse implements Serializable {
        private Integer status;
        private List<SearchWorkData> data;

        @Data
        public static class SearchWorkData implements Serializable {
            private String id;
            private Integer stage;
            private Float rate;
            private String info;
            private List<Homework> homework;

            @Data
            public static class Homework implements Serializable {
                private Integer id;
                private String sn;
                private Integer[] unit;
                private Integer damage;
                private Integer auto;
                private Integer remain;
                private String info;
                private List<Video> video;

                @Data
                public static class Video implements Serializable {
                    private String text;
                    private String url;
                    private String note;
                }
            }
        }
    }

    @Data
    public static class SearchIconResponse {
        private Integer status;
        private List<List<IconData>> data;

        @Data
        public static class IconData {
            private Integer id;
            private Integer iconId;
            private String iconValue;
            private String iconFilePath;
        }
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

                    List<String> bossIds = new ArrayList<>(5);
                    for (SearchWorkResponse.SearchWorkData data : response.getData()) {
                        if (!bossIds.contains(data.getId())) {
                            bossIds.add(data.getId());
                        }
                    }

                    bossIds = bossIds.stream().map(Integer::parseInt).sorted(Integer::compareTo).map(Object::toString).collect(Collectors.toList());
                    for (SearchWorkResponse.SearchWorkData data : response.getData()) {
                        int index = bossIds.indexOf(data.getId()) + 1;
                        char c = (char) (data.getStage() + 'A' - 1);
                        String key = String.valueOf(c) + index;
                        HomeworkCache.getHomeworkMap().put(key, data);
                    }
                }
            }
        }
    }

    public ForwardMessage.Node buildNode(Message msg) {
        return new ForwardMessage.Node(1586197314L, (int) (System.currentTimeMillis() / 1000), "小初音",
                MessageUtils.newChain(msg));
    }

    public List<SearchWorkResponse.SearchWorkData.Homework> homeworkFilter(String msg, List<SearchWorkResponse.SearchWorkData.Homework> source) {
        List<SearchWorkResponse.SearchWorkData.Homework> target = source;
        if (msg.contains("自动")) {
            target = target.stream().filter(h -> h.getAuto() == 1).collect(Collectors.toList());
        } else if (msg.contains("手动")) {
            target = target.stream().filter(h -> h.getAuto() == 2).collect(Collectors.toList());
        }

        if (msg.contains("[") && msg.contains("]")) {
            List<Integer> roleIds = getRoleIds(msg, false);
            if (!CollectionUtils.isEmpty(roleIds)) {
                target = target.stream().filter(h -> {
                    for (int u : h.getUnit()) {
                        if (roleIds.contains(u)) {
                            return false;
                        }
                    }
                    return true;
                }).collect(Collectors.toList());
            }
        }

        if (msg.contains("(") && msg.contains(")")) {
            List<Integer> roleIds = getRoleIds(msg, true);
            if (!CollectionUtils.isEmpty(roleIds)) {
                target = target.stream().filter(h -> {
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
                }).collect(Collectors.toList());
            }
        }
        return target;
    }

    private static List<Integer> getRoleIds(String str, boolean isInclude) {
        String roleStr;
        if (isInclude) {
            roleStr = str.substring(str.indexOf("(") + 1, str.lastIndexOf(")"));
        } else {
            roleStr = str.substring(str.indexOf("[") + 1, str.lastIndexOf("]"));
        }
        System.out.println("roleStr ======= " + roleStr);
        String[] roleNameArray = roleStr.split(" ");
        return Arrays.stream(roleNameArray).map(name -> {
            Optional<SearchIconResponse.IconData> iconData = HomeworkCache.getIconDataMap().values().stream()
                    .filter(i -> i.getIconValue().equals(name)).findAny();
            return iconData.orElse(null);
        }).filter(Objects::nonNull).map(SearchIconResponse.IconData::getId).collect(Collectors.toList());
    }
}
