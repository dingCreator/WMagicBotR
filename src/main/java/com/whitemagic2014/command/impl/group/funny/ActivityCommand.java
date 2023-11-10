package com.whitemagic2014.command.impl.group.funny;

import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.command.impl.group.AdminCommand;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;

import com.whitemagic2014.service.ActivityService;
import com.whitemagic2014.util.ActivityManagerUtil;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ding
 * @date 2023/11/8
 */
@Command
@Slf4j
public class ActivityCommand extends AdminCommand {

    @Autowired
    private GlobalParam globalParam;

    @Autowired
    private ActivityManagerUtil activityManagerUtil;

    private static final String FORMAT_INFO = "格式:%s活动 新增/删除 活动类型/活动ID " +
            "触发关键词(*) 限制类型（无/日/月/年） 限制次数 参与成功回复(*) 参与失败回复(*) 奖品（奖品名|数量|概率（可选））(*) " +
            "开始时间（yyyy-MM-dd） 结束时间(yyyy-MM-dd)\n" +
            "（标(*)表示支持多个，若有多个，用[]包住并用逗号分隔）";

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        String formatInfo = String.format(FORMAT_INFO, globalParam.botName);
        if (args.size() < 2) {
            return new PlainText(formatInfo);
        }
        if ("删除".equals(args.get(0))) {
            long number;
            try {
                number = Long.parseLong(args.get(1));
            } catch (Exception e) {
                log.error("Number Format error", e);
                return new PlainText("活动ID解析错误");
            }
            boolean r = activityManagerUtil.deleteActivity(number);
            if (r) {
                return new PlainText("删除成功");
            }
            return new PlainText("删除失败");
        } else if ("新增".equals(args.get(0))) {
            if (args.size() < 10) {
                return new PlainText(formatInfo);
            }
            String activityType = args.get(1);
            String keywordStr = args.get(2);
            String limitTypeStr = args.get(3);
            int limitType;
            int limitTimes;
            String successReplyStr = args.get(5);
            String failReplyStr = args.get(6);
            String award = args.get(7);
            String startTime = args.get(8);
            String endTime = args.get(9);
            try {
                limitTimes = Integer.parseInt(args.get(4));
            } catch (Exception e) {
                log.error("配置解析失败", e);
                return new PlainText("参与次数限制错误");
            }
            switch (limitTypeStr) {
                case "无":
                    limitType = 0;
                    break;
                case "日":
                    limitType = 1;
                    break;
                case "月":
                    limitType = 2;
                    break;
                case "年":
                    limitType = 3;
                    break;
                case "终身":
                    limitType = 4;
                    break;
                default:
                    return new PlainText("参与限制类型错误");
            }
            int limit = (limitType << 27) + limitTimes;
            Map<String, Object> ruleParam = new HashMap<>();
            List<String> keywords = activityManagerUtil.dealStringList(keywordStr);
            List<String> successReply = activityManagerUtil.dealStringList(successReplyStr);
            List<String> failReply = activityManagerUtil.dealStringList(failReplyStr);
            ruleParam.put("keyword", keywords);
            ruleParam.put("limit", limit);
            ruleParam.put("successReply", successReply);
            ruleParam.put("failReply", failReply);
            String ruleJson = JSONObject.toJSONString(ruleParam);

            List<String> awardStrList = activityManagerUtil.dealStringList(award);
            List<Map<String, String>> awardParam = awardStrList.stream().map(awardStr -> {
                Map<String, String> singleAwardParam = new HashMap<>();
                String[] param = awardStr.split("\\|");
                singleAwardParam.put("awardName", param[0]);
                singleAwardParam.put("awardNum", param[1]);
                if (param.length > 2) {
                    singleAwardParam.put("rate", param[2]);
                }
                return singleAwardParam;
            }).collect(Collectors.toList());
            boolean r = activityManagerUtil.createActivity(activityType, ruleJson, JSONObject.toJSONString(awardParam),
                    startTime, endTime);
            if (r) {
                return new PlainText("新增成功");
            }
            return new PlainText("新增失败");
        }
        return null;
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "活动", globalParam.botNick + "活动");
    }
}
