package com.whitemagic2014.command.impl.group.immortal;

import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.cache.SettingsCache;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ding
 * @date 2023/2/21
 */
@Command
public class WantedCommand extends NoAuthCommand {

    private static final String ADD_KEYWORD = "增加";
    private static final String REMOVE_KEYWORD = "删除";
    private static final String VIEW_KEYWORD = "查看";

    static final String WANTED_AWARD_KEYWORD = "修仙_悬赏令_关键字";
    /**
     * 是否已接取悬赏令
     */
    static boolean wanted = false;
    /**
     * 悬赏令结束时间戳 0-未接取悬赏令
     */
    static long wantedEndTimeMillis;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (CollectionUtils.isEmpty(args)) {
            return new PlainText("格式：初音悬赏令 {增加/删除/查看} [关键词]\n参数1必填，参数2在增加/删除时必填，注意空格");
        }

        List<String> wantedAwardKeyword = SettingsCache.getInstance().getSettings(WANTED_AWARD_KEYWORD,
                str -> JSONObject.parseArray(str, String.class));
        if (wantedAwardKeyword == null) {
            wantedAwardKeyword = new ArrayList<>();
        }

        String msg = args.get(0).trim();
        if (msg.equals(VIEW_KEYWORD)) {
            if (CollectionUtils.isEmpty(wantedAwardKeyword)) {
                return new PlainText("暂无关键词");
            }
            StringBuilder builder = new StringBuilder("关键词[");
            wantedAwardKeyword.forEach(k -> builder.append(k).append("|"));
            builder.replace(builder.lastIndexOf("|"), builder.lastIndexOf("|") + 1, "").append("]");
            return new PlainText(builder.toString());
        }

        if (args.size() < 2) {
            return new PlainText("请输入关键词");
        }
        String keyword = args.get(1).trim();
        if (msg.endsWith(ADD_KEYWORD)) {
            if (!CollectionUtils.isEmpty(wantedAwardKeyword) && wantedAwardKeyword.stream().anyMatch(keyword::equals)) {
                return new PlainText("关键词[" + keyword + "]已存在");
            }
            wantedAwardKeyword.add(args.get(1).trim());
            SettingsCache.getInstance().setSettings(WANTED_AWARD_KEYWORD, JSONObject.toJSONString(wantedAwardKeyword));
            return new PlainText("关键词[" + keyword + "]添加成功");
        } else if (msg.endsWith(REMOVE_KEYWORD)) {
            if (wantedAwardKeyword.remove(args.get(1).trim())) {
                SettingsCache.getInstance().setSettings(WANTED_AWARD_KEYWORD, JSONObject.toJSONString(wantedAwardKeyword));
                return new PlainText("关键词[" + keyword + "]删除成功");
            } else {
                return new PlainText("关键词[" + keyword + "]删除失败，请检查关键词是否存在");
            }
        }
        return new PlainText("指令错误");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("初音悬赏令");
    }
}
