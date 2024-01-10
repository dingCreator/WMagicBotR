package com.whitemagic2014.command.impl.group.immortal;

import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.Activity;
import com.whitemagic2014.pojo.CommandProperties;

import com.whitemagic2014.util.ActivityManagerUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ding
 * @date 2023/12/24
 */
@Command
public class NoAuthActivityCommand extends NoAuthCommand {

    @Autowired
    private GlobalParam globalParam;

    @Autowired
    private ActivityManagerUtil activityManagerUtil;

    private static final String FORMAT_INFO = "发送“%s活动 列表”查看正在进行中的活动\n发送“%s活动 活动名称”查看活动详情";
    private static final String LIST = "列表";

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        String formatInfo = String.format(FORMAT_INFO, globalParam.botNick, globalParam.botNick);
        if (CollectionUtils.isEmpty(args)) {
            return new PlainText(formatInfo);
        }

        String arg = args.get(0);
        if (LIST.equals(arg)) {
            List<Activity> activityList = activityManagerUtil.queryActivityList(true);
            StringBuilder builder = new StringBuilder("进行中的活动：\n活动名称|触发指令|限制类型|限制次数|开始时间|结束时间");
            activityList.forEach(activity ->  {
                Activity.ActivityRule rule = JSONObject.parseObject(activity.getActivityRule(), Activity.ActivityRule.class);
                List<String> keywordAfterFormat = activityManagerUtil.keywordFormat(rule.getKeywords());
                builder.append("\n").append(activity.getActivityName())
                        .append("|").append(keywordAfterFormat.stream().reduce((k1, k2) -> k1 + "、" + k2).orElse(null))
                        .append("|").append(activityManagerUtil.transLimitTypeNumber2Str(rule.getLimit()))
                        .append("|").append(activityManagerUtil.getLimitTimes(rule.getLimit()))
                        .append("|").append(activity.getStartTime())
                        .append("|").append(activity.getEndTime());
            });
            return new PlainText(builder.toString());
        } else {
            return new PlainText(activityManagerUtil.getActivityDetailByName(args.get(0)));
        }
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "活动", globalParam.botNick + "活动");
    }
}
