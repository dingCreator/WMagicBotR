package com.whitemagic2014.util;

import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.command.impl.group.immortal.util.TimeUtil;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.ForwardMessage;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

/**
 * @author ding
 * @date 2023/8/24
 */
@Component
public class CommandThreadPoolUtil {

    private final ThreadPoolExecutor executor;

    public CommandThreadPoolUtil() {
        final int corePoolSize = 5;
        final int maxPoolSize = 10;
        final int keepAliveTime = 30;
        final int poolSize = Integer.MAX_VALUE >>> 8;

        this.executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(poolSize), r -> {
            Thread thread = new Thread(r);
            thread.setName("处理指令线程");
            thread.setDaemon(false);
            return thread;
        });
    }

    public void addGroupTask(List<Message> messages, long groupId) {
        Bot bot = MagicBotR.getBot();
        executor.execute(() -> {
            for (Message message : messages) {
                bot.getGroupOrFail(groupId).sendMessage(message);
                TimeUtil.waitRandomMillis(2000, 3000);
            }
        });
    }

    public void addGroupTask(Message message, List<Long> groupIds, Consumer<Bot> consumer) {
        Bot bot = MagicBotR.getBot();
        Future<?> future = executor.submit(() -> {
            for (Long groupId : groupIds) {
                bot.getGroupOrFail(groupId).sendMessage(message);
                TimeUtil.waitRandomMillis(2000, 3000);
            }
        });
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            consumer.accept(bot);
        }
    }

    public void sendForwardMessage(List<String> msg, String title, List<String> preview, long groupId, long senderId) {
        Bot bot = MagicBotR.getBot();

        List<List<String>> subMsgList = splitPartition(100, msg);
        // 如果被拦截的解决方案
        executor.execute(() -> {
            try {
                for (List<String> list : subMsgList) {
                    ForwardMessage message = ForwardMessageUtil.buildForwardMessage(senderId, title, preview,
                            list.stream().map(m -> new PlainText(m)).collect(Collectors.toList()));
                    bot.getGroupOrFail(groupId).sendMessage(message);
                    TimeUtil.waitRandomMillis(2000, 3000);
                }
            } catch (Exception e) {
                List<List<String>> subMsgList1 = splitPartition(20, msg);
                for (List<String> list : subMsgList1) {
                    bot.getGroupOrFail(groupId).sendMessage(list.stream().reduce((s1, s2) -> s1 + "\n\n" + s2).orElse(""));
                    TimeUtil.waitRandomMillis(2000, 3000);
                }
            }
        });
    }

    private List<List<String>> splitPartition(int partitionSize, List<String> list) {
        int partitionIndex = 0;
        List<List<String>> result = new ArrayList<>(list.size() / partitionSize + 1);
        while (partitionIndex * partitionSize < list.size()) {
            List<String> subList = new ArrayList<>(partitionSize);
            for (int i = partitionIndex; i < Math.min(partitionIndex + partitionSize, list.size()); i++) {
                subList.add(list.get(i));
            }
            result.add(subList);
            partitionIndex += partitionSize;
        }
        return result;
    }

    public void interrupt() {
        executor.getQueue().clear();
    }
}
