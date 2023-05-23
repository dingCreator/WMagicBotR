package com.whitemagic2014.command.impl.group.immortal;

import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.cache.SettingsCache;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.command.impl.group.immortal.util.CalBossUtil;
import com.whitemagic2014.command.impl.group.immortal.util.DTO.FarmDTO;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ding
 * @date 2023/2/27
 */
@Command
public class CollectConfigCommand extends NoAuthCommand {

    private static final String PREVIEW_KEYWORD = "查看";
    private static final String ADD_KEYWORD1 = "增加";
    private static final String ADD_KEYWORD2 = "新增";
    private static final String DEL_KEYWORD = "删除";
    private static final String EDIT_KEYWORD1 = "编辑";
    private static final String EDIT_KEYWORD2 = "修改";
    private static final String FORMAT_INFO = "格式：修仙农场配置 {查看/增加/删除/编辑} [群号/编号] [打boss等级上限（含）]";

    @Autowired
    private GlobalParam globalParam;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (CollectionUtils.isEmpty(args)) {
            return new PlainText(FORMAT_INFO);
        }

        if (PREVIEW_KEYWORD.equals(args.get(0))) {
            String config = SettingsCache.getInstance().getSettings(FarmDTO.SETTINGS_KEYWORD, preview -> {
                List<FarmDTO> list = JSONObject.parseArray(preview, FarmDTO.class);
                if (CollectionUtils.isEmpty(list)) {
                    return "";
                }
                StringBuilder builder = new StringBuilder("编号|群号|群名|境界");
                int i = 1;
                for (FarmDTO farm : list) {
                    builder.append("\n").append(i)
                            .append("|").append(farm.getGroupId())
                            .append("|").append(farm.getGroupName())
                            .append("|").append(farm.getRankName());
                    i++;
                }
                return builder.toString();
            });
            return StringUtils.isEmpty(config) ? new PlainText("当前没有配置") : new PlainText(config);
        } else if (ADD_KEYWORD1.equals(args.get(0)) || ADD_KEYWORD2.equals(args.get(0))) {
            if (args.size() < 3) {
                return new PlainText("请输入群号和可打boss境界上限（含）");
            }
            long groupId;
            try {
                groupId = Long.parseLong(args.get(1));
            } catch (Exception e) {
                return new PlainText("输入的群号非数字");
            }

            Bot bot = MagicBotR.getBot();
            Group group = bot.getGroups().stream().filter(g -> g.getId() == groupId).findFirst().orElse(null);
            if (group == null) {
                return new PlainText(globalParam.botNick + "没有加入该群");
            }

            int rank = CalBossUtil.getRank(args.get(2));
            if (rank == 0) {
                return new PlainText("输入的境界有误");
            }

            String str = SettingsCache.getInstance().getSettings(FarmDTO.SETTINGS_KEYWORD);
            List<FarmDTO> list;
            if (StringUtils.isEmpty(str)) {
                list = new ArrayList<>();
            } else {
                list = JSONObject.parseArray(str, FarmDTO.class);
            }
            if (list.stream().anyMatch(farm -> farm.getGroupId().equals(groupId))) {
                return new PlainText("该配置已存在");
            }
            list.add(new FarmDTO(groupId, group.getName(), rank, args.get(2)));
            SettingsCache.getInstance().setSettings(FarmDTO.SETTINGS_KEYWORD, JSONObject.toJSONString(list));
            return new PlainText("增加成功");
        } else if (DEL_KEYWORD.equals(args.get(0))) {
            if (args.size() < 2) {
                return new PlainText("请输入编号");
            }
            // 解析编号
            List<Integer> nums = analyseNum(args.get(1));
            if (CollectionUtils.isEmpty(nums)) {
                return new PlainText("所有的编号均非法");
            }

            String str = SettingsCache.getInstance().getSettings(FarmDTO.SETTINGS_KEYWORD);
            List<FarmDTO> list = JSONObject.parseArray(str, FarmDTO.class);
            List<Integer> sortedNums = nums.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            int count = 0;
            for (int num : sortedNums) {
                if (num <= list.size() && num > 0) {
                    list.remove(num - 1);
                    count++;
                }
            }
            SettingsCache.getInstance().setSettings(FarmDTO.SETTINGS_KEYWORD, JSONObject.toJSONString(list));
            return new PlainText("成功删除【" + count + "】条配置");
        } else if (EDIT_KEYWORD1.equals(args.get(0)) || EDIT_KEYWORD2.equals(args.get(0))) {
            if (args.size() < 3) {
                return new PlainText("请输入编号和打boss等级上限（含）");
            }
            // 解析境界
            int rank = CalBossUtil.getRank(args.get(2));
            if (rank == 0) {
                return new PlainText("输入的境界有误");
            }
            // 解析编号
            List<Integer> nums = analyseNum(args.get(1));
            if (CollectionUtils.isEmpty(nums)) {
                return new PlainText("所有的编号均非法");
            }
            String str = SettingsCache.getInstance().getSettings(FarmDTO.SETTINGS_KEYWORD);
            List<FarmDTO> list = JSONObject.parseArray(str, FarmDTO.class);
            int count = 0;

            for (int num : nums) {
                if (num <= list.size() && num > 0) {
                    list.set(num - 1, new FarmDTO(
                            list.get(num - 1).getGroupId(),
                            list.get(num - 1).getGroupName(),
                            rank, args.get(2)));
                    count++;
                }
            }
            SettingsCache.getInstance().setSettings(FarmDTO.SETTINGS_KEYWORD, JSONObject.toJSONString(list));
            return new PlainText("成功编辑【" + count +"】条配置");
        }
        return new PlainText(FORMAT_INFO);
    }

    private List<Integer> analyseNum(String str) {
        List<Integer> nums = new ArrayList<>();
        String numsStr = str.replace("，", ",");
        for (String numStr : numsStr.split(",")) {
            try {
                nums.add(Integer.parseInt(numStr));
            } catch (Exception e) {
                // do nothing
            }
        }
        return nums;
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("修仙农场配置");
    }
}