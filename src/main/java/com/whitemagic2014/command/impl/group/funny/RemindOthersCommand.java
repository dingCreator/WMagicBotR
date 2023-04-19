package com.whitemagic2014.command.impl.group.funny;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.command.impl.group.EmptyStringCommand;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.pojo.CommandProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.*;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author ding
 * @date 2023/2/14
 */
@Command
public class RemindOthersCommand extends NoAuthCommand {

    private static final Map<Long, Map<Long, RemindBody>> REMIND_BODY_MAP = new HashMap<>();
    private static final String ADD_REMIND_KEYWORD = "提醒";
    private Bot bot;

    @Data
    @AllArgsConstructor
    private static class RemindBody {
        /**
         * QQ群号
         */
        private Long group;
        /**
         * 提醒人
         */
        private Long reminder;
        /**
         * 提醒人昵称
         */
        private String reminderNick;
        /**
         * 被提醒人
         */
        private Long reminded;
        /**
         * 提醒内容
         */
        private String msg;
    }

    /**
     * 防止滥用，限制频次，暂时不用，如果后续发现有滥用的再启用
     */
    private static final Map<Long, Integer> TIMES_MAP = new HashMap<>();
    private static final int TIMES_LIMIT = 5;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        bot = MagicBotR.getBot();
        if (CollectionUtils.isEmpty(args)) {
            return new PlainText("请输入提醒内容");
        }
        addEmptyStringLogic();

        long groupId = sender.getGroup().getId();
        long senderId = sender.getId();

        At at = (At) messageChain.stream().filter(At.class::isInstance).findFirst().orElse(null);
        String msg = args.stream().filter(m -> !m.startsWith("@")).reduce((a1, a2) -> a1 + a2).orElse("");
        if (at != null) {
            Map<Long, RemindBody> newRemind = REMIND_BODY_MAP.getOrDefault(at.getTarget(), new HashMap<>());
            RemindBody newRemindBody = newRemind.get(groupId);
            if (newRemindBody == null) {
                newRemind.put(groupId, new RemindBody(groupId, senderId, sender.getNameCard(), at.getTarget(), msg));
                REMIND_BODY_MAP.put(at.getTarget(), newRemind);
            } else {
                newRemindBody.setMsg(newRemindBody.getMsg() + "\n" + msg);
            }
            return new At(senderId).plus("添加提醒成功");
        }
        return null;
    }

    private void addEmptyStringLogic() {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, text) -> {
            long groupId = sender.getGroup().getId();
            long senderId = sender.getId();
            if (REMIND_BODY_MAP.containsKey(senderId) && REMIND_BODY_MAP.get(senderId).containsKey(groupId)) {
                RemindBody remindBody = REMIND_BODY_MAP.get(senderId).get(groupId);
                REMIND_BODY_MAP.remove(senderId);

                bot.getGroupOrFail(groupId).sendMessage(new At(remindBody.getReminded())
                        .plus(remindBody.getReminderNick())
                        .plus("提醒你：\n")
                        .plus(remindBody.getMsg()));
                return new At(remindBody.getReminder()).plus("你想要提醒的对象已出现");
            }
            return null;
        }, "RemindOthersCommand");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("提醒");
    }
}
