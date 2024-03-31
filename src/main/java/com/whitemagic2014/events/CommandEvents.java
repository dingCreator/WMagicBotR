package com.whitemagic2014.events;

import com.whitemagic2014.annotate.SpecialCommand;
import com.whitemagic2014.annotate.Switch;
import com.whitemagic2014.command.*;
import com.whitemagic2014.command.impl.group.BaseGroupCommand;
import com.whitemagic2014.config.properties.CommandRule;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.miniGame.MiniGameUtil;
import com.whitemagic2014.miniGame.strategy.BaseMiniGameStrategy;
import com.whitemagic2014.util.MagicSwitch;
import com.whitemagic2014.util.spring.SpringApplicationContextUtil;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.EventPriority;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.GroupTempMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.OnlineMessageSource;
import net.mamoe.mirai.message.data.PlainText;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 指令消息事件, 其实本质是一个消息事件, 在这个消息事件中进一步封装了指令
 * @author: magic chen
 * @date: 2020/8/20 15:46
 **/
public class CommandEvents extends SimpleListenerHost {

    private static final Logger logger = LoggerFactory.getLogger(CommandEvents.class);

    /**
     * 指令头 区分正常消息 和 指令消息
     */
    private final Set<String> commandHeads = new HashSet<>();

    /**
     * 已注册的指令, [指令名, 指令对象]
     */
    private final Map<String, Command> everywhereCommands = new HashMap<>();
    private final Map<String, Command> friendCommands = new HashMap<>();
    private final Map<String, Command> groupCommands = new HashMap<>();
    private final Map<String, Command> tempMsgCommands = new HashMap<>();


    /**
     * @Name: registerCommandHeads
     * @Description: 注册指令头
     * @Param: heads
     * @Return: void
     * @Author: magic chen
     * @Date: 2020/8/21 10:56
     **/
    public void registerCommandHeads(String... heads) {
        commandHeads.addAll(Arrays.asList(heads));
    }

    /**
     * @Name: registerCommands
     * @Description: 批量注册指令
     * @Param: commands
     * @Author: magic chen
     * @Date: 2020/8/21 00:04
     **/
    public void registerCommands(List<Command> commands) {
        for (Command command : commands) {
            registerCommand(command);
        }
    }

    /**
     * @Name: registerCommand
     * @Description: 注册指令
     * @Param: command
     * @Author: magic chen
     * @Date: 2020/8/21 00:02
     **/
    private void registerCommand(Command command) {
        // 构建 临时指令组
        Map<String, Command> tempCommans = new HashMap<>();
        tempCommans.put(command.properties().getName().toLowerCase(), command);
        command.properties().getAlias().forEach(alias -> tempCommans.put(alias.toLowerCase(), command));

        // 根据事件类型分配指令监听组
        if (command instanceof FriendCommand) {
            friendCommands.putAll(tempCommans);
        } else if (command instanceof GroupCommand) {
            groupCommands.putAll(tempCommans);
        } else if (command instanceof TempMessageCommand) {
            tempMsgCommands.putAll(tempCommans);
        } else {
            everywhereCommands.putAll(tempCommans);
        }

    }

    /**
     * 移除指令
     *
     * @param commands 指令
     */
    public void removeCommand(List<Command> commands) {
        for (Command command : commands) {
            List<String> keywords = new ArrayList<>();
            keywords.add(command.properties().getName());
            keywords.addAll(command.properties().getAlias());
            for (String kw : keywords) {
                removeCommand(kw);
            }
        }
    }

    public void removeCommandByKeyword(List<String> keywords) {
        for (String kw : keywords) {
            removeCommand(kw);
        }
    }

    private void removeCommand(String kw) {
        friendCommands.remove(kw);
        groupCommands.remove(kw);
        tempMsgCommands.remove(kw);
        everywhereCommands.remove(kw);
    }

    /**
     * @Name: getArgs
     * @Description: 从消息体中获得 用空格分割的参数
     * @Param: msg
     * @Return: java.util.ArrayList<java.lang.String>
     * @Author: magic chen
     * @Date: 2020/8/21 16:40
     **/
    private ArrayList<String> getArgs(String msg) {
        String[] args = msg.trim().split(" ");
        ArrayList<String> list = new ArrayList<>();
        for (String arg : args) {
            if (StringUtils.isNotBlank(arg)) {
                list.add(arg);
            }
        }
        list.remove(0);
        return list;
    }

    /**
     * @Name: isCommand
     * @Description: 判断是否带有指令头
     * @Param: msg
     * @Return:
     * @Author: magic chen
     * @Date: 2020/8/21 11:10
     **/
    private boolean isCommand(String msg) {
        return commandHeads.stream().anyMatch(msg::startsWith);
    }

    /**
     * @Name: getCommand
     * @Description: 获得指令
     * @Param: msg
     * @Param: commandMap
     * @Return: com.whitemagic2014.command.Command
     * @Author: magic chen
     * @Date: 2020/8/21 11:56
     **/
    private Command getCommand(String msg, Map<String, Command> commandMap) {
        return this.getCommand(msg, commandMap, false, 0);
    }

    private Command getCommand(String msg, Map<String, Command> commandMap, Boolean isLike, int num) {
        String[] temp = msg.split(" ");
        if (temp == null || temp.length == 0) {
            return null;
        }

        // 带头指令
        String headcommand = temp[0];
        // 获得去除指令头的 指令str
        List<String> temps = commandHeads.stream()
                .filter(head -> headcommand.startsWith(head) && StringUtils.isNotBlank(head))
                .map(head -> headcommand.replaceFirst(head, ""))
                .collect(Collectors.toList());

        String commandStr;
        if (temps.isEmpty()) {
            commandStr = headcommand;
        } else {
            commandStr = temps.get(0);
        }

        if (!isLike) {
            return commandMap.getOrDefault(commandStr.toLowerCase(), null);
        } else {
            // 1-过滤掉不支持模糊匹配的命令
            List<String> likeCommandList = commandMap.keySet().stream()
                    .filter(str -> ((BaseGroupCommand) commandMap.get(str)).getLike()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(likeCommandList)) {
                return null;
            }

            // 2-模糊匹配
            List<String> commandList = likeCommandList.stream().filter(commandStr::contains)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(commandList)) {
                return null;
            } else {
                if (num >= commandList.size()) {
                    return null;
                }
                return commandMap.get(commandList.get(num));
            }
        }
    }

    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception) {
        // 貌似无法捕获异常
        logger.error("MessageEvents Error:", exception);
    }


    /**
     * @Name: onMessage
     * @Description: 任何形式的消息处理
     * @Param: event
     * @Return: net.mamoe.mirai.event.ListeningStatus
     * @Author: magic chen
     * @Date: 2020/8/21 15:00
     **/
    @NotNull
    @EventHandler(priority = EventPriority.LOW)
    public ListeningStatus onMessage(@NotNull MessageEvent event) throws Exception { // 可以抛出任何异常, 将在 handleException 处理
        String oriMsg = event.getMessage().contentToString();
        if (isCommand(oriMsg)) {
            EverywhereCommand command = (EverywhereCommand) getCommand(oriMsg, everywhereCommands);
            if (command != null) {
                if (!validCommand(command, event)) {
                    return ListeningStatus.LISTENING;
                }
                Message result = command.execute(event.getSender(), getArgs(oriMsg), event.getMessage(), event.getSubject());
                if (result != null) {
                    event.getSubject().sendMessage(result);
                }
            } else {
                // 单纯的带有指令头的消息 未注册的指令
            }
        } else {
            // 非指令 暂时不处理
        }
        return ListeningStatus.LISTENING; // 表示继续监听事件
    }


    /**
     * @Name: onFriendMessage
     * @Description: 好友私聊消息事件处理 如果是指令则执行 详见 FriendCommand
     * @Param: event  详见 FriendMessageEvent
     * @Return: 是否继续监听 详见 ListeningStatus
     * @Author: magic chen
     * @Date: 2020/8/21 11:59
     **/
    @NotNull
    @EventHandler(priority = EventPriority.NORMAL)
    public ListeningStatus onFriendMessage(@NotNull FriendMessageEvent event) throws Exception {
        String oriMsg = event.getMessage().contentToString();
        if (isCommand(oriMsg)) {
            FriendCommand command = (FriendCommand) getCommand(oriMsg, friendCommands);
            if (command != null) {
                Switch sw = command.getClass().getAnnotation(Switch.class);
                if (sw != null && !MagicSwitch.check(sw.name())) {
                    return ListeningStatus.LISTENING;
                }
                Message result = command.execute(event.getSender(), getArgs(oriMsg), event.getMessage(), event.getSubject());
                if (result != null) {
                    event.getSubject().sendMessage(result);
                }
                //事件拦截 防止公共消息事件再次处理
                event.intercept();
            } else {
                // 单纯的带有指令头的消息 未注册的指令
            }
        } else {
            // 非指令 暂时不处理
        }
        return ListeningStatus.LISTENING;
    }


    /**
     * @Name: onGroupMessage
     * @Description: 群聊消息事件处理 如果是指令则执行 详见 GroupCommand
     * @Param: event
     * @Return: net.mamoe.mirai.event.ListeningStatus
     * @Author: magic chen
     * @Date: 2020/8/21 14:32
     **/
    @NotNull
    @EventHandler(priority = EventPriority.NORMAL)
    public ListeningStatus onGroupMessage(@NotNull GroupMessageEvent event) throws Exception {

        String oriMsg = event.getMessage().contentToString();

        // 消息发送者是否在进行游戏
        if (MiniGameUtil.playerIsPlayingGame(event.getSender().getId())) {
            try {
                BaseMiniGameStrategy strategy = MiniGameUtil.getPlayingGamesById(event.getSender().getId());
                String msg = Objects.requireNonNull(event.getMessage()
                        .get(OnlineMessageSource.Incoming.FromGroup.Key)).getOriginalMessage().toString();
                MiniGameUtil.MiniGameResultEnum r = strategy.play(msg, event.getSender(), event.getSubject());
                if (MiniGameUtil.MiniGameResultEnum.WIN.equals(r)) {
                    event.getSubject().sendMessage(new PlainText("YOU WIN"));
                    MiniGameUtil.stopGame(event.getSender().getId());
                } else if (MiniGameUtil.MiniGameResultEnum.LOSE.equals(r)) {
                    event.getSubject().sendMessage(new PlainText("YOU LOSE"));
                    MiniGameUtil.stopGame(event.getSender().getId());
                } else if (MiniGameUtil.MiniGameResultEnum.CONTINUE.equals(r)) {
                    return ListeningStatus.LISTENING;
                } else if (MiniGameUtil.MiniGameResultEnum.STOP.equals(r)) {
                    MiniGameUtil.stopGame(event.getSender().getId());
                    event.getSubject().sendMessage(new PlainText("游戏中止"));
                    return ListeningStatus.LISTENING;
                }
            } catch (NullPointerException npe) {
                event.getSubject().sendMessage(new PlainText("没有进行中的游戏"));
            } finally {
                //事件拦截 防止公共消息事件再次处理
                event.intercept();
            }
        }

        if (isCommand(oriMsg)) {
            GroupCommand command = (GroupCommand) getCommand(oriMsg, groupCommands);
            if (command != null) {
                if (!validCommand(command, event)) {
                    return ListeningStatus.LISTENING;
                }
                Message result;
                ArrayList<String> args = getArgs(oriMsg);
                com.whitemagic2014.annotate.Command cmd =
                        command.getClass().getAnnotation(com.whitemagic2014.annotate.Command.class);
                if (args.size() < cmd.minArgsSize() || args.size() > cmd.maxArgsSize()) {
                    result = new PlainText(cmd.invalidArgsSizeErrorMsg());
                } else {
                    result = command.execute(event.getSender(), args, event.getMessage(), event.getSubject());
                }

                if (result != null) {
                    event.getSubject().sendMessage(result);
                }
                //事件拦截 防止公共消息事件再次处理
                event.intercept();
            } else {
                // 单纯的带有指令头的消息 未注册的指令 尝试模糊匹配
                int num = 0;
                Message result = null;
                while (result == null) {
                    command = (GroupCommand) getCommand(oriMsg, groupCommands, true, num);
                    if (command != null) {
                        if (validCommand(command, event)) {
                            result = command.execute(event.getSender(), getArgs(oriMsg), event.getMessage(), event.getSubject());
                        }
                        num++;
                    } else {
                        break;
                    }
                }
                if (result != null) {
                    event.getSubject().sendMessage(result);
                    //事件拦截 防止公共消息事件再次处理
                    event.intercept();
                }
            }
        } else {
            // 非指令 暂时不处理
        }
        return ListeningStatus.LISTENING;
    }


    /**
     * @Name: onTempMessage
     * @Description: 群临时消息事件处理 如果是指令则执行 详见 TempMessageCommand
     * @Param: event
     * @Return: net.mamoe.mirai.event.ListeningStatus
     * @Author: magic chen
     * @Date: 2020/8/21 14:57
     **/
    @NotNull
    @EventHandler(priority = EventPriority.NORMAL)
    public ListeningStatus onTempMessage(@NotNull GroupTempMessageEvent event) throws Exception {
        String oriMsg = event.getMessage().contentToString();
        if (isCommand(oriMsg)) {
            TempMessageCommand command = (TempMessageCommand) getCommand(oriMsg, tempMsgCommands);
            if (command != null) {
                Switch sw = command.getClass().getAnnotation(Switch.class);
                if (sw != null && !MagicSwitch.check(sw.name())) {
                    return ListeningStatus.LISTENING;
                }
                Message result = command.execute(event.getSender(), getArgs(oriMsg), event.getMessage(), event.getSubject());
                if (result != null) {
                    event.getSubject().sendMessage(result);
                }
                //事件拦截 防止公共消息事件再次处理
                event.intercept();
            } else {
                // 单纯的带有指令头的消息 未注册的指令
            }
        } else {
            // 非指令 暂时不处理
        }
        return ListeningStatus.LISTENING;
    }

    private boolean validCommand(Command command, MessageEvent event) {
        Switch sw = command.getClass().getAnnotation(Switch.class);
        if (sw != null && !MagicSwitch.check(sw.name())) {
            return false;
        }

        boolean ignoreWhitelist = false;
        boolean ignoreBlacklist = false;
        SpecialCommand sp = command.getClass().getAnnotation(SpecialCommand.class);
        if (sp != null) {
            if (checkSpecialCommand(event.getSender().getId(), sp.affectBotIds(), sp.affectIds())) {
                return false;
            }
            ignoreWhitelist = sp.ignoreWhitelist();
            ignoreBlacklist = sp.ignoreBlacklist();
        }

        if (event instanceof GroupMessageEvent) {
            CommandRule commandRule = SpringApplicationContextUtil.getBean(CommandRule.class);
            long groupId = ((GroupMessageEvent) event).getGroup().getId();

            return (ignoreWhitelist || checkGroupWhitelist(groupId, commandRule))
                    && (ignoreBlacklist || checkGroupBlacklist(groupId, commandRule));
        }
        return true;
    }

    private boolean checkSpecialCommand(long senderId, long[] affectBotIds, long[] affectIds) {
        GlobalParam globalParam = SpringApplicationContextUtil.getBean(GlobalParam.class);
        return Arrays.stream(affectIds).anyMatch(id -> id == senderId)
                && Arrays.stream(affectBotIds).anyMatch(id -> id == globalParam.botId);
    }

    /**
     * 群组白名单检查
     *
     * @param groupId     群号
     * @param commandRule 配置
     * @return true-允许响应 false-禁止响应
     */
    private boolean checkGroupWhitelist(long groupId, CommandRule commandRule) {
        return !commandRule.enabledGroupWhitelist || commandRule.groupWhitelist.contains(groupId);
    }

    /**
     * 群组黑名单检查
     *
     * @param groupId     群号
     * @param commandRule 配置
     * @return true-允许响应 false-禁止响应
     */
    private boolean checkGroupBlacklist(long groupId, CommandRule commandRule) {
        return !commandRule.enabledGroupBlacklist || !commandRule.groupBlacklist.contains(groupId);
    }
}
