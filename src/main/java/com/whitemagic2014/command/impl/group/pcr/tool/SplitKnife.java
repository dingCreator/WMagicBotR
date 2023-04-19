package com.whitemagic2014.command.impl.group.pcr.tool;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.cache.HomeworkCache;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.command.impl.group.pcr.tool.SearchWork;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.*;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Command
public class SplitKnife extends NoAuthCommand {

    @Autowired
    private RestTemplate restTemplate;
    private static Date refreshTime = null;
    private static final Object LOCK = new Object();
    private static final String HOMEWORK_URL = "https://www.caimogu.cc/gzlj/data?date=&lang=zh-cn";
    private static final String HOMEWORK_ICON_URL = "https://www.caimogu.cc/gzlj/data/icon?date=&lang=zh-cn";

    private static Integer maxDamage = 0;
    private static List<SearchWork.SearchWorkResponse.SearchWorkData.Homework> homeworkList;

    public SplitKnife() {
        this.like = true;
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("分刀计算");
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        String msg = Objects.requireNonNull(messageChain.get(OnlineMessageSource.Incoming.FromGroup.Key)).getOriginalMessage().toString();
        initIconDataMap();
        initHomeworkMap();
        maxDamage = 0;
        homeworkList = new ArrayList<>();

        List<SearchWork.SearchWorkResponse.SearchWorkData.Homework> temp = new ArrayList<>();
        if (msg.contains("A") || msg.contains("a")) {
            temp.addAll(HomeworkCache.getHomeworkMap().get("A1").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("A2").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("A3").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("A4").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("A5").getHomework());
        }
        if (msg.contains("B") || msg.contains("b")) {
            temp.addAll(HomeworkCache.getHomeworkMap().get("B1").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("B2").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("B3").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("B4").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("B5").getHomework());
        }
        if (msg.contains("C") || msg.contains("c")) {
            temp.addAll(HomeworkCache.getHomeworkMap().get("C1").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("C2").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("C3").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("C4").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("C5").getHomework());
        }
        if (msg.contains("D") || msg.contains("d")) {
            temp.addAll(HomeworkCache.getHomeworkMap().get("D1").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("D2").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("D3").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("D4").getHomework());
            temp.addAll(HomeworkCache.getHomeworkMap().get("D5").getHomework());
        }
        if (msg.contains("E") || msg.contains("e")) {
            return new PlainText("E面还没开放呢！这么想坐牢吗？");
        }
        splitKnife(temp, 0, 0, new ArrayList<>());

        List<String> preview = Collections.singletonList("最高伤害");
        String title = "Kira☆";
        String brief = "";
        String source = "";
        String summary = "分刀";

        homeworkList.forEach(hw -> {
            List<ForwardMessage.Node> nodeList = new ArrayList<>();
            StringBuilder returnMsgBuilder = new StringBuilder();
            for (Integer id : hw.getUnit()) {
                returnMsgBuilder.append(HomeworkCache.getIconDataMap().get(id).getIconValue());
                returnMsgBuilder.append(" ");
            }
            returnMsgBuilder.append(hw.getAuto() == 1 ? "auto" : "手动");
            returnMsgBuilder.append("\n伤害：");
            returnMsgBuilder.append(hw.getDamage()).append("w");
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

            long gId = sender.getGroup().getId();
            Bot bot = MagicBotR.getBot();
            bot.getGroup(gId).sendMessage(new ForwardMessage(preview, title, brief, source, summary, nodeList));
        });
        return null;
    }

    public void initIconDataMap() {
        if (CollectionUtils.isEmpty(HomeworkCache.getIconDataMap())) {
            synchronized (LOCK) {
                if (CollectionUtils.isEmpty(HomeworkCache.getIconDataMap())) {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add("x-requested-with", "XMLHttpRequest");
                    ResponseEntity<SearchWork.SearchIconResponse> entity = restTemplate.exchange(HOMEWORK_ICON_URL,
                            HttpMethod.GET, new HttpEntity<>(null, httpHeaders), SearchWork.SearchIconResponse.class);
                    SearchWork.SearchIconResponse response = entity.getBody();
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
                    ResponseEntity<SearchWork.SearchWorkResponse> entity = restTemplate.exchange(HOMEWORK_URL,
                            HttpMethod.GET, new HttpEntity<>(null, httpHeaders), SearchWork.SearchWorkResponse.class);
                    SearchWork.SearchWorkResponse response = entity.getBody();

                    List<String> bossIds = new ArrayList<>(5);
                    for (SearchWork.SearchWorkResponse.SearchWorkData data : response.getData()) {
                        if (!bossIds.contains(data.getId())) {
                            bossIds.add(data.getId());
                        }
                    }

                    bossIds = bossIds.stream().map(Integer::parseInt).sorted(Integer::compareTo).map(Object::toString).collect(Collectors.toList());
                    for (SearchWork.SearchWorkResponse.SearchWorkData data : response.getData()) {
                        int index = bossIds.indexOf(data.getId()) + 1;
                        char c = (char) (data.getStage() + 'A' - 1);
                        String key =  String.valueOf(c) + index;
                        HomeworkCache.getHomeworkMap().put(key, data);
                    }
                }
            }
        }
    }

    public void splitKnife(List<SearchWork.SearchWorkResponse.SearchWorkData.Homework> homework, int index, int damage,
                           List<SearchWork.SearchWorkResponse.SearchWorkData.Homework> result) {
        if (homework.size() < 3) {
            return;
        }
        for (int i = index; i < homework.size() - 3; i++) {
            if (checkDuplicate(result, homework.get(i))) {
                continue;
            }
            List<SearchWork.SearchWorkResponse.SearchWorkData.Homework> nextResult = new ArrayList<>(result);
            nextResult.add(homework.get(i));
            int nextDamage = damage + homework.get(i).getDamage();
            if (nextResult.size() >= 3) {
                if (nextDamage > maxDamage) {
                    maxDamage = nextDamage;
                    homeworkList = nextResult;
                }
                continue;
            }
            splitKnife(homework, i + 1, nextDamage, nextResult);
        }
    }

    public boolean checkDuplicate(List<SearchWork.SearchWorkResponse.SearchWorkData.Homework> result,
                                  SearchWork.SearchWorkResponse.SearchWorkData.Homework homework) {
        if (CollectionUtils.isEmpty(result)) {
            return false;
        }
        for (SearchWork.SearchWorkResponse.SearchWorkData.Homework r : result) {
            boolean oneDuplicate = false;
            for (int u : r.getUnit()) {
                for (int h : homework.getUnit()) {
                    if (u == h) {
                        if (!oneDuplicate) {
                            oneDuplicate = true;
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public ForwardMessage.Node buildNode(Message msg) {
        return new ForwardMessage.Node(1586197314L, (int) (System.currentTimeMillis() / 1000), "小初音",
                MessageUtils.newChain(msg));
    }
}
