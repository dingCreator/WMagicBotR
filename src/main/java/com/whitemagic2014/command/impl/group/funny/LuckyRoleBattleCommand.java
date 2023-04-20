package com.whitemagic2014.command.impl.group.funny;

import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.cache.LuckyRoleCache;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.command.impl.group.pcr.tool.SearchWork;
import com.whitemagic2014.pojo.CommandProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 幸运角色决斗（条纹袜少女骑士团-佑树 提出的玩法）
 * 纯概率决定胜负，前提是决斗双方必须抽取了幸运角色
 * BOSS有小幅的胜率提升
 *
 * @author ding
 * @author 条纹袜少女骑士团-佑树
 * @see com.whitemagic2014.command.impl.group.funny.LuckyRoleCommand
 */
@Command
public class LuckyRoleBattleCommand extends NoAuthCommand {

    private static final Random RANDOM = new Random();
    private static final List<SpecialBattleRule> SPECIAL_BATTLE_RULES = new ArrayList<>();
    private static final Object RIGGED_LOCK = new Object();
    private static final Object EVENT_LOCK = new Object();
    private static final List<BattleTriggerEvent> EVENTS = new ArrayList<>();
    private static final String NORMAL_BATTLE_DESCRIPTION = "一场激烈的决斗过后";
    private static final List<String> TAGS = Arrays.asList("[@sender]", "[@opponent]", "[@winner]", "[@loser]");
    private static final List<BattleTriggerEvent> SINGLE_RESULT_EVENTS = new ArrayList<>();

    @Value("${specialRule.luckyRoleBattle.rigged:}")
    private String specialRulesJson;

    @Value("${specialRule.luckyRoleBattle.endWhileSpRuleAnalyseFailed:}")
    private boolean endWhileSpRuleAnalyseFailed;

    // 触发事件
    static {
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@loser]临阵脱逃，[@winner]不战而胜！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@loser]中了[@winner]事先布下的陷阱！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@loser]使用了sl，[@winner]不战而胜！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@loser]在前往决斗的路上迷路了，[@winner]不战而胜！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@winner]使用科技，大败[@loser]！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@loser]使用了科技，但被对方举报成功，[@winner]不战而胜！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@winner]假装不敌，趁对方不备杀了一个回马枪击败[@loser]！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@winner]率先发动UB，击败[@loser]！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@loser]与TA的幸运角色约会，咕咕咕~[@winner]不战而胜！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@loser]看错决斗时间，迟到了，[@winner]不战而胜！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@loser]的自残技能对自己造成了暴击，[@winner]获胜！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@loser]发起挑战，手滑使用了小小甜心！[@winner]获胜！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@loser]状态低迷，全程0暴，[@winner]轻松取胜！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@winner]决斗前偷走了[@loser]的专武，在决斗中轻松取胜！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@winner]不讲武德，偷袭[@loser]，击败对手！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@loser]不讲武德，偷袭[@winner]，但被对方识破并反杀！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN, LuckyRoleBattleResult.SENDER_WIN),
                "[@loser]在前往决斗的路上被佩可拉去吃饭，[@winner]不战而胜！"));

        EVENTS.add(new BattleTriggerEvent(1, buildResultSet(LuckyRoleBattleResult.SENDER_WIN),
                "[@sender]作为决斗发起方，主动选择了自己熟悉的地方进行决斗，轻松取胜！"));
        EVENTS.add(new BattleTriggerEvent(1, buildResultSet(LuckyRoleBattleResult.SENDER_WIN),
                "决斗现场大雾弥漫，[@sender]凭借对地形的熟悉，轻松取胜！"));

        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.DRAW),
                "决斗现场大雾弥漫，浓雾之中双方都没有找到对方！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.DRAW),
                "双方都在前往决斗的路上迷路了！"));
        EVENTS.add(new BattleTriggerEvent(3, buildResultSet(LuckyRoleBattleResult.DRAW),
                "双方都与TA的幸运角色约会，无人到达决斗现场"));
    }

    private static Set<LuckyRoleBattleResult> buildResultSet(LuckyRoleBattleResult... rs) {
        Set<LuckyRoleBattleResult> set = new HashSet<>();
        if (rs.length == 0) {
            throw new IllegalArgumentException("result must not be null");
        }
        Collections.addAll(set, rs);
        return set;
    }

    public LuckyRoleBattleCommand() {
        this.like = true;
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("幸运角色决斗");
    }

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        // 校验
        if (!initSpecialRules()) {
            System.out.println("黑幕加载失败");
            if (endWhileSpRuleAnalyseFailed) {
                return new PlainText("黑幕头子因黑幕解析失败终止了此次操作");
            }
        }
        At at = (At) messageChain.stream().filter(At.class::isInstance).findFirst().orElse(null);
        if (at == null) {
            return new At(sender.getId()).plus("请@你的对手");
        }

        Member opponent = subject.get(at.getTarget());
        if (opponent == null) {
            return new At(sender.getId()).plus("请@你的对手");
        }

        if (!LuckyRoleCache.compareLatest(sender.getId())) {
            return new At(sender.getId()).plus("你没有抽取本小时的幸运角色");
        }

        if (!LuckyRoleCache.compareLatest(opponent.getId())) {
            return new At(sender.getId()).plus("你的对手没有抽取本小时的幸运角色");
        }

        if (sender.getId() == opponent.getId()) {
            return new At(sender.getId()).plus("不能和自己决斗");
        }

        // 初始化事件
        initTriggerEvent();
        SearchWork.SearchIconResponse.IconData leftIcon = LuckyRoleCache.getLatestRole(sender.getId());
        SearchWork.SearchIconResponse.IconData rightIcon = LuckyRoleCache.getLatestRole(opponent.getId());

        int leftRate = 90;
        int middleRate = 20;
        int rightRate = 90;
        // BOSS的比较特殊，是区间
        if (leftIcon.getIconId() != 0 && leftIcon.getIconId() < 1000) {
            leftRate += (45 + RANDOM.nextInt(45));
        }
        if (rightIcon.getIconId() != 0 && rightIcon.getIconId() < 1000) {
            rightRate += (45 + RANDOM.nextInt(45));
        }
        // 黑幕计算
        if (!CollectionUtils.isEmpty(SPECIAL_BATTLE_RULES)) {
            for (SpecialBattleRule rule : SPECIAL_BATTLE_RULES) {
                leftRate = getRiggedRate(sender, leftRate, leftIcon, rule);
                rightRate = getRiggedRate(opponent, rightRate, rightIcon, rule);
            }
        }

        List<BattleTriggerEvent> tmpList = new ArrayList<>(SINGLE_RESULT_EVENTS);
        tmpList.add(new BattleTriggerEvent(leftRate, buildResultSet(LuckyRoleBattleResult.SENDER_WIN), NORMAL_BATTLE_DESCRIPTION));
        tmpList.add(new BattleTriggerEvent(middleRate, buildResultSet(LuckyRoleBattleResult.DRAW), NORMAL_BATTLE_DESCRIPTION));
        tmpList.add(new BattleTriggerEvent(rightRate, buildResultSet(LuckyRoleBattleResult.OPPONENT_WIN), NORMAL_BATTLE_DESCRIPTION));

        int randNum = RANDOM.nextInt(tmpList.stream().mapToInt(BattleTriggerEvent::getRate).reduce(Integer::sum).orElse(0));
        int prevNum = 0;
        for (BattleTriggerEvent e : tmpList) {
            if (randNum >= prevNum && randNum < prevNum + e.getRate()) {
                return buildBattleResult(e.getResult().stream().findFirst().orElse(LuckyRoleBattleResult.DRAW),
                        e.getDescription(), subject, sender.getId(), leftIcon, opponent.getId(), rightIcon);
            } else {
                prevNum += e.getRate();
            }
        }
        return buildBattleResult(LuckyRoleBattleResult.DRAW, NORMAL_BATTLE_DESCRIPTION, subject, sender.getId(), leftIcon, opponent.getId(), rightIcon);
    }

    /**
     * 加载黑幕
     *
     * @return 黑幕加载是否成功
     */
    private boolean initSpecialRules() {
        if (CollectionUtils.isEmpty(SPECIAL_BATTLE_RULES) && !StringUtils.isEmpty(specialRulesJson)) {
            synchronized (RIGGED_LOCK) {
                if (CollectionUtils.isEmpty(SPECIAL_BATTLE_RULES) && !StringUtils.isEmpty(specialRulesJson)) {
                    try {
                        SPECIAL_BATTLE_RULES.addAll(JSONObject.parseArray(specialRulesJson, SpecialBattleRule.class));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 加载事件
     */
    private void initTriggerEvent() {
        if (SINGLE_RESULT_EVENTS.size() == 0) {
            synchronized (EVENT_LOCK) {
                if (SINGLE_RESULT_EVENTS.size() == 0) {
                    for (BattleTriggerEvent event : EVENTS) {
                        event.getResult().forEach(r -> SINGLE_RESULT_EVENTS
                                .add(new BattleTriggerEvent(event.getRate(), buildResultSet(r), event.getDescription())));
                    }
                }
            }
        }
    }

    /**
     * 获取黑幕后的胜率
     *
     * @param member 黑幕方
     * @param rate   原胜率
     * @param icon   icon信息
     * @param rule   黑幕规则
     * @return 黑幕后胜率
     */
    private int getRiggedRate(Member member, int rate, SearchWork.SearchIconResponse.IconData icon, SpecialBattleRule rule) {
        if (CollectionUtils.isEmpty(rule.getAffectIds()) || rule.getAffectIds().contains(member.getId())) {
            if (rule.getIconId().equals(icon.getIconId())) {
                rate += rule.getInvariantRate();
                int variantRate;
                if (rule.getVariantRate() < 0) {
                    variantRate = -RANDOM.nextInt(-rule.getVariantRate());
                } else {
                    variantRate = RANDOM.nextInt(rule.getVariantRate());
                }
                rate += variantRate;
            }
        }
        return rate;
    }


    /**
     * 似乎是版本兼容性问题？必须return new才能成功返回，怪
     *
     * @param res        决斗结果
     * @param subject    subject
     * @param senderId   决斗发起者QQ
     * @param leftIcon   决斗发起者幸运角色信息
     * @param opponentId 对手QQ
     * @param rightIcon  对手幸运角色信息
     * @return 组装后的决斗结果
     */
    private Message buildBattleResult(LuckyRoleBattleResult res, String eventDescription, Group subject,
                                      long senderId, SearchWork.SearchIconResponse.IconData leftIcon,
                                      long opponentId, SearchWork.SearchIconResponse.IconData rightIcon) {
        Message message = transferDescription(analyseDescription(eventDescription, true, TAGS),
                senderId, opponentId, res).plus("\n\n");

        if (LuckyRoleBattleResult.SENDER_WIN.equals(res)) {
            return message.plus(getMessage(subject, senderId, leftIcon));
        } else if (LuckyRoleBattleResult.DRAW.equals(res)) {
            boolean leftCanSend, rightCanSend;
            try {
                Contact.uploadImage(subject, new File(leftIcon.getIconFilePath()));
                leftCanSend = true;
            } catch (Exception e) {
                leftCanSend = false;
            }

            try {
                Contact.uploadImage(subject, new File(rightIcon.getIconFilePath()));
                rightCanSend = true;
            } catch (Exception e) {
                rightCanSend = false;
            }

            if (leftCanSend && rightCanSend) {
                return message.plus(new PlainText("决斗结果为：平局\n")
                        .plus(makeAt(senderId))
                        .plus("\n幸运角色：")
                        .plus(Contact.uploadImage(subject, new File(leftIcon.getIconFilePath())))
                        .plus(leftIcon.getIconValue())
                        .plus("\n\n")
                        .plus(makeAt(opponentId))
                        .plus("\n幸运角色：")
                        .plus(Contact.uploadImage(subject, new File(rightIcon.getIconFilePath())))
                        .plus(rightIcon.getIconValue()));
            } else if (!leftCanSend && rightCanSend) {
                return message.plus(new PlainText("决斗结果为：平局\n")
                        .plus(makeAt(senderId))
                        .plus("\n幸运角色：")
                        .plus(new PlainText("\n-----------\n【图片被拦截】\n-----------\n"))
                        .plus(leftIcon.getIconValue())
                        .plus("\n\n")
                        .plus(makeAt(opponentId))
                        .plus("\n幸运角色：")
                        .plus(Contact.uploadImage(subject, new File(rightIcon.getIconFilePath())))
                        .plus(rightIcon.getIconValue()));
            } else if (leftCanSend) {
                return message.plus(new PlainText("决斗结果为：平局\n")
                        .plus(makeAt(senderId))
                        .plus("\n幸运角色：")
                        .plus(Contact.uploadImage(subject, new File(leftIcon.getIconFilePath())))
                        .plus(leftIcon.getIconValue())
                        .plus("\n\n")
                        .plus(makeAt(opponentId))
                        .plus("\n幸运角色：")
                        .plus(new PlainText("\n-----------\n【图片被拦截】\n-----------\n"))
                        .plus(rightIcon.getIconValue()));
            } else {
                return message.plus(new PlainText("决斗结果为：平局\n")
                        .plus(makeAt(senderId))
                        .plus("\n幸运角色：")
                        .plus(new PlainText("\n-----------\n【图片被拦截】\n-----------\n"))
                        .plus(leftIcon.getIconValue())
                        .plus("\n\n")
                        .plus(makeAt(opponentId))
                        .plus("\n幸运角色：")
                        .plus(new PlainText("\n-----------\n【图片被拦截】\n-----------\n"))
                        .plus(rightIcon.getIconValue()));
            }
        } else if (LuckyRoleBattleResult.OPPONENT_WIN.equals(res)) {
            return message.plus(getMessage(subject, opponentId, rightIcon));
        }
        return new PlainText("决斗结果出错");
    }

    /**
     * 获取决斗结果返回值
     *
     * @param subject  subject
     * @param id       qq号
     * @param iconData icon信息
     * @return 结果
     */
    private Message getMessage(Group subject, long id, SearchWork.SearchIconResponse.IconData iconData) {
        try {
            return new PlainText("决斗结果为：")
                    .plus(makeAt(id)).plus(new PlainText(" 获胜\n幸运角色：")
                            .plus(Contact.uploadImage(subject, new File(iconData.getIconFilePath())))
                            .plus(iconData.getIconValue()));
        } catch (Exception e) {
            return new PlainText("决斗结果为：")
                    .plus(makeAt(id)).plus(new PlainText(" 获胜\n幸运角色：")
                            .plus(new PlainText("\n-----------\n【图片被拦截】\n-----------\n"))
                            .plus(iconData.getIconValue()));
        }
    }

    /**
     * 解析特定标签，将整个字符串以标签为分割点分割为多个部分
     *
     * @param str      需要解析的字符串
     * @param keywords 特定标签
     * @return 解析的结果
     */
    private List<String> analyseDescription(final String str, boolean retainKeyword, List<String> keywords) {
        List<String> result = new ArrayList<>();
        if (keywords.size() == 0) {
            result.add(str);
            return result;
        }

        if (keywords.stream().allMatch(kw -> kw.length() >= str.length())) {
            result.add(str);
            return result;
        }

        long[] position = new long[100];
        int strIndex = 0, posIndex = 0;
        Stack<Integer> stack = new Stack<>();

        while (strIndex < str.length()) {
            if ('[' == str.charAt(strIndex)) {
                stack.push(strIndex);
            } else if (']' == str.charAt(strIndex) && stack.size() > 0) {
                int prev = stack.pop();
                position[posIndex] = ((long) prev << 32) + strIndex;
                posIndex++;
            }
            strIndex++;
        }

        int resultIdx = 0, preCharAt = 0;
        for (long pos : position) {
            if (pos == 0) {
                break;
            }
            int left = (int) (pos >> 32), right = (int) pos + 1;

            if (keywords.contains(str.substring(left, right))) {
                result.add(resultIdx, str.substring(preCharAt, left));
                resultIdx++;
                result.add(resultIdx, str.substring(left, right));
                resultIdx++;
                preCharAt = right;
            }
        }
        if (!str.substring(preCharAt).isEmpty()) {
            result.add(str.substring(preCharAt));
        }
        return result.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    private Message transferDescription(List<String> description, long senderId, long opponentId,
                                        LuckyRoleBattleResult battleResult) {
        Message message = new PlainText("");
        for (String str : description) {
            if ("[@winner]".equals(str)) {
                if (battleResult.equals(LuckyRoleBattleResult.DRAW)) {
                    throw new IllegalArgumentException("draw didn't has winner and loser");
                } else if (battleResult.equals(LuckyRoleBattleResult.SENDER_WIN)) {
                    message = message.plus(makeAt(senderId)).plus(" ");
                } else if (battleResult.equals(LuckyRoleBattleResult.OPPONENT_WIN)) {
                    message = message.plus(makeAt(opponentId)).plus(" ");
                }
            } else if ("[@loser]".equals(str)) {
                if (battleResult.equals(LuckyRoleBattleResult.DRAW)) {
                    throw new IllegalArgumentException("draw didn't has winner and loser");
                } else if (battleResult.equals(LuckyRoleBattleResult.SENDER_WIN)) {
                    message = message.plus(makeAt(opponentId)).plus(" ");
                } else if (battleResult.equals(LuckyRoleBattleResult.OPPONENT_WIN)) {
                    message = message.plus(makeAt(senderId)).plus(" ");
                }
            } else if ("[@sender]".equals(str)) {
                message = message.plus(makeAt(senderId)).plus(" ");
            } else if ("[@opponent]".equals(str)) {
                message = message.plus(makeAt(opponentId)).plus(" ");
            } else {
                message = message.plus(str);
            }
        }
        return message;
    }

    /**
     * 黑幕规则
     */
    @Data
    @AllArgsConstructor
    public static class SpecialBattleRule {

        private String ruleName;
        /**
         * if empty -> affect everyone
         */
        private List<Long> affectIds;

        private Integer iconId;
        /**
         * 固定的概率变化
         */
        private Integer invariantRate;
        /**
         * 不固定的概率变化区间
         */
        private Integer variantRate;
    }

    /**
     * 决斗结果
     */
    @Getter
    @AllArgsConstructor
    private enum LuckyRoleBattleResult {
        /**
         *
         */
        SENDER_WIN("SENDER_WIN", "发起方获胜"),
        DRAW("DRAW", "平局"),
        OPPONENT_WIN("OPPONENT_WIN", "对手获胜"),
        ;

        private final String code;
        private final String name;
    }

    /**
     * 触发事件
     */
    @Data
    @AllArgsConstructor
    private static class BattleTriggerEvent {
        /**
         * 事件触发概率值
         * 如果可能造成多个结果
         * 那么每个结果的概率都是这个值
         */
        private Integer rate;
        /**
         * 事件造成的结果
         */
        private Set<LuckyRoleBattleResult> result;
        /**
         * 事件过程描述
         * 提供以下标签对应关系，注意带上中括号
         * [@sender] -> @发起方
         * [@opponent] -> @对手
         * [@winner] -> @胜者 若没有则不@
         * [@loser] -> @败者 若没有则不@
         */
        private String description;
    }
}
