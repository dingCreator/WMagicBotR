package com.whitemagic2014.util;

import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.command.impl.group.immortal.util.TimeUtil;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.List;
import java.util.function.Consumer;

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

//    @PreDestroy
//    private void preDestroy() {
//        bot.getGroupOrFail().sendMessage(new PlainText("嘶哑 ~☆"));
//    }
}
