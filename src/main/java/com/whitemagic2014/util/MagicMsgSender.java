package com.whitemagic2014.util;

import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.util.time.MagicOnceTask;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

import java.util.Date;

/**
 * @Description: 用来发送消息的工具类
 * @author: magic chen
 * @date: 2020/9/29 16:31
 **/
public class MagicMsgSender {

    /**
     * @Name: sendGroupMsg
     * @Description: 发送群消息
     * @Param: groupId
     * @Param: msg
     * @Return: void
     * @Author: magic chen
     * @Date: 2020/9/29 17:02
     **/
    public static void sendGroupMsg(Long groupId, Message msg) {
        MagicBotR.getBot().getGroupOrFail(groupId).sendMessage(msg);
    }

    /**
     * @Name: sendGroupMsgDelay
     * @Description: 发送延时群消息
     * @Param: groupId
     * @Param: msg
     * @Param: delay  秒
     * @Return: taskkey 可用来取消延时任务
     * @Author: magic chen
     * @Date: 2020/9/30 17:07
     **/
    public static String sendGroupMsgDelay(Long groupId, Message msg, Long delay) {
        String key = MagicMd5.getMd5String("g" + groupId + msg.toString() + System.currentTimeMillis());
        MagicOnceTask.build(key, () -> MagicBotR.getBot().getGroupOrFail(groupId).sendMessage(msg)).schedule(delay * 1000L);
        return key;
    }


    /**
     * 同上
     *
     * @param groupId
     * @param msg
     * @param delay
     * @param taskkey 由外部传入
     * @return
     */
    public static String sendGroupMsgDelay(Long groupId, Message msg, Long delay, String taskkey) {
        MagicOnceTask.build(taskkey, () -> MagicBotR.getBot().getGroupOrFail(groupId).sendMessage(msg)).schedule(delay * 1000L);
        return taskkey;
    }


    /**
     * @Name: sendGroupMsgTiming
     * @Description: 发送定时群消息
     * @Param: groupId
     * @Param: msg
     * @Param: time
     * @Return: taskkey 可用来取消延时任务
     * @Author: magic chen
     * @Date: 2020/9/30 17:07
     **/
    public static String sendGroupMsgTiming(Long groupId, Message msg, Date time) {
        String key = MagicMd5.getMd5String("g" + groupId + msg.toString() + System.currentTimeMillis());
        MagicOnceTask.build(key, () -> MagicBotR.getBot().getGroupOrFail(groupId).sendMessage(msg)).schedule(time);
        return key;
    }


    /**
     * 同上
     *
     * @param groupId
     * @param msg
     * @param time
     * @param taskkey 由外部传入
     * @return
     */
    public static String sendGroupMsgTiming(Long groupId, Message msg, Date time, String taskkey) {
        MagicOnceTask.build(taskkey, () -> MagicBotR.getBot().getGroupOrFail(groupId).sendMessage(msg)).schedule(time);
        return taskkey;
    }


    /**
     * @Name: sendFriendMsg
     * @Description: 发送私聊消息
     * @Param: uid
     * @Param: msg
     * @Return: void
     * @Author: magic chen
     * @Date: 2020/9/29 17:12
     **/
    public static void sendFriendMsg(Long uid, Message msg) {
        MagicBotR.getBot().getFriendOrFail(uid).sendMessage(msg);
    }


    /**
     * @Name: sendFriendMsgDelay
     * @Description: 发送延时私聊消息
     * @Param: uid
     * @Param: msg
     * @Param: delay 秒
     * @Return: taskkey 可用来取消延时任务
     * @Author: magic chen
     * @Date: 2020/9/30 17:10
     **/
    public static String sendFriendMsgDelay(Long uid, Message msg, Long delay) {
        String key = MagicMd5.getMd5String("u" + uid + msg.toString() + System.currentTimeMillis());
        MagicOnceTask.build(key, () -> MagicBotR.getBot().getFriendOrFail(uid).sendMessage(msg)).schedule(delay * 1000L);
        return key;
    }


    /**
     * 同上
     *
     * @param uid
     * @param msg
     * @param delay
     * @param taskkey 由外部传入
     * @return
     */
    public static String sendFriendMsgDelay(Long uid, Message msg, Long delay, String taskkey) {
        MagicOnceTask.build(taskkey, () -> MagicBotR.getBot().getFriendOrFail(uid).sendMessage(msg)).schedule(delay * 1000L);
        return taskkey;
    }

    /**
     * @Name: sendFriendMsgTiming
     * @Description: 发送定时私聊消息
     * @Param: uid
     * @Param: msg
     * @Param: time
     * @Return: taskkey 可用来取消延时任务
     * @Author: magic chen
     * @Date: 2020/9/30 17:10
     **/
    public static String sendFriendMsgTiming(Long uid, Message msg, Date time) {
        String key = MagicMd5.getMd5String("u" + uid + msg.toString() + System.currentTimeMillis());
        MagicOnceTask.build(key, () -> MagicBotR.getBot().getFriendOrFail(uid).sendMessage(msg)).schedule(time);
        return key;
    }

    /**
     * 同上
     *
     * @param uid
     * @param msg
     * @param time
     * @param taskkey 由外部传入
     * @return
     */
    public static String sendFriendMsgTiming(Long uid, Message msg, Date time, String taskkey) {
        MagicOnceTask.build(taskkey, () -> MagicBotR.getBot().getFriendOrFail(uid).sendMessage(msg)).schedule(time);
        return taskkey;
    }


    /**
     * @Name: sendBroadcast
     * @Description: 发送广播消息
     * @Param: msg
     * @Return: void
     * @Author: magic chen
     * @Date: 2020/9/29 17:02
     **/
    public static void sendBroadcast(Message msg) {
        Bot bot = MagicBotR.getBot();
        MessageChain messageChain = new PlainText("公告通知:\n").plus(msg);
        for (Group g : bot.getGroups()) {
            g.sendMessage(messageChain);
            try {
                Thread.sleep(5000L);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Friend f : bot.getFriends()) {
            f.sendMessage(messageChain);
            try {
                Thread.sleep(5000L);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
