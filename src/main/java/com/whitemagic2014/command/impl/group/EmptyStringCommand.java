package com.whitemagic2014.command.impl.group;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import com.whitemagic2014.util.RecognizeTextUtil;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.OnlineMessageSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 无条件匹配指令
 * 需要增加新的无条件指令请调用此类中的addLogic方法
 *
 * @author ding
 * @date 2023/2/16
 * @see this#addLogic(EmptyStringLogic, String)
 * @see this#addLogic(EmptyStringLogic, String, boolean)
 */
@Command
public class EmptyStringCommand extends NoAuthCommand {

    private static final Map<String, EmptyStringLogic> LOGIC_MAP = new ConcurrentHashMap<>();
    private static final Lock LOCK = new ReentrantLock();

    @Autowired
    private GlobalParam globalParam;

    public EmptyStringCommand() {
        this.like = true;
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        String notice = null;
        At at = (At) messageChain.stream().filter(At.class::isInstance).findFirst().orElse(null);
        boolean atMe = at != null && globalParam.botId == at.getTarget();

        LOCK.lock();
        try {
            notice = RecognizeTextUtil.getInstance().getImageText(messageChain, sender);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LOCK.unlock();
        }

        Message message;
        if (notice != null) {
            final String imgText = notice;
            message = LOGIC_MAP.values().stream()
                    .map(l -> l.executeLogic(sender, args, messageChain, subject, imgText, atMe))
                    .filter(Objects::nonNull).findFirst().orElse(null);
        } else {
            final String text = Objects.requireNonNull(messageChain.get(OnlineMessageSource.Incoming.FromGroup.Key))
                    .getOriginalMessage().toString();
            message = LOGIC_MAP.values().stream()
                    .map(l -> l.executeLogic(sender, args, messageChain, subject, text, atMe))
                    .filter(Objects::nonNull).findFirst().orElse(null);
        }
        return message;
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("");
    }

    /**
     * 用key区分逻辑是否已存在
     *
     * @param logic 逻辑
     * @param key   键
     */
    public static void addLogic(EmptyStringLogic logic, String key) {
        addLogic(logic, key, false);
    }

    /**
     * 用key区分逻辑是否已存在
     *
     * @param logic    逻辑
     * @param key      键
     * @param override 是否覆盖原逻辑
     */
    public static void addLogic(EmptyStringLogic logic, String key, boolean override) {
        if (!LOGIC_MAP.containsKey(key) || override) {
            LOGIC_MAP.put(key, logic);
        }
    }

    public static void removeLogic(String key) {
        LOGIC_MAP.remove(key);
    }

    @FunctionalInterface
    public interface EmptyStringLogic {
        /**
         * 执行逻辑
         *
         * @param sender       sender
         * @param args         args
         * @param messageChain messageChain
         * @param subject      subject
         * @param notice       图片文字识别结果（如果有图片的话）
         * @param atMe         是否艾特我
         * @return bot msg
         */
        Message executeLogic(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject, String notice,
                             Boolean atMe);
    }
}
