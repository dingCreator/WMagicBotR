package com.whitemagic2014.util;

import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageUtils;
import net.mamoe.mirai.message.data.ForwardMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ding
 * @date 2024/3/28
 */
public class ForwardMessageUtil {

    /**
     * 构建组合信息
     *
     * @param nick        昵称（可用于显示简略信息）
     * @param messageList 信息
     * @return 组合信息
     */
    public static ForwardMessage buildForwardMessage(long id, List<Message> messageList) {
        List<ForwardMessage.Node> nodeList = messageList.stream().map(msg -> buildNode(id, msg)).collect(Collectors.toList());
        return buildForwardMessage(Collections.singletonList("点击查看对战详情"), "对战详情", "", "",
                "查看" + nodeList.size() + "条转发消息", nodeList);
    }

    /**
     * 构建组合信息
     *
     * @param messageList 信息
     * @return 组合信息
     */
    public static ForwardMessage buildForwardMessage(long id, String title, List<String> preview, List<Message> messageList) {
        List<ForwardMessage.Node> nodeList = messageList.stream().map(msg -> buildNode(id, msg)).collect(Collectors.toList());
        return buildForwardMessage(preview, title, "", "",
                "查看" + nodeList.size() + "条转发消息", nodeList);
    }

    /**
     * 构建组合信息
     *
     * @param nick        昵称（可用于显示简略信息）
     * @param messageList 信息
     * @return 组合信息
     */
    public static List<ForwardMessage> buildForwardMessageList(long id, List<Message> messageList) {
        List<ForwardMessage> forwardMessages = new ArrayList<>();
        int size = 1 + messageList.size() / 10;
        for (int i = 0; i < size; i += 10) {
            List<Message> sublist = messageList.subList(i, Math.min(i + 10, messageList.size()));
            List<ForwardMessage.Node> nodeList = sublist.stream().map(msg -> buildNode(id, msg)).collect(Collectors.toList());
            forwardMessages.add(buildForwardMessage(Collections.singletonList("点击查看对战详情"), "对战详情", "", "",
                    "查看" + sublist.size() + "条转发消息", nodeList));
        }
        return forwardMessages;
    }

    /**
     * 构建组合信息
     *
     * @param preview  概览
     * @param title    标题
     * @param brief    不知道是啥
     * @param source   不知道是啥
     * @param summary  最下方信息
     * @param nodeList 具体内容
     * @return 组合信息
     */
    public static ForwardMessage buildForwardMessage(List<String> preview, String title, String brief, String source,
                                                     String summary, List<ForwardMessage.Node> nodeList) {
        return new ForwardMessage(preview, title, brief, source, summary, nodeList);
    }

    /**
     * 构建节点
     *
     * @param id      qq号
     * @param message 信息
     * @return 节点
     */
    public static ForwardMessage.Node buildNode(long id, Message message) {
        return buildNode(id, MessageUtils.newChain(message), "System");
    }

    /**
     * 构建节点
     *
     * @param id      qq号
     * @param message 信息
     * @param nick    昵称
     * @return 节点
     */
    public static ForwardMessage.Node buildNode(long id, Message message, String nick) {
        return buildNode(id, message, nick, (int) (System.currentTimeMillis() / 1000));
    }

    /**
     * 构建节点
     *
     * @param id         qq号
     * @param message    信息
     * @param nick       昵称
     * @param timeMillis 时间戳
     * @return 节点
     */
    public static ForwardMessage.Node buildNode(long id, Message message, String nick, int timeMillis) {
        return new ForwardMessage.Node(id, timeMillis, nick, MessageUtils.newChain(message));
    }
}
