package com.whitemagic2014.command.impl.group;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.cache.SettingsCache;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

/**
 * 配置
 *
 * @author ding
 * @date 2023/4/12
 */
@Command
public class SettingsCommand extends OwnerCommand {

    @Autowired
    private GlobalParam globalParam;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (args == null || args.size() == 0) {
            return new PlainText("参数错误，格式：" + globalParam.botName + "设置/" + globalParam.botNick
                    + "设置 {配置名} [新的配置值]，若不填新的配置值则为查看当前配置");
        }

        String settingName = args.get(0);
        SettingsCache cache = SettingsCache.getInstance();
        String oldVal = cache.getSettings(settingName);

        if (StringUtils.isEmpty(oldVal)) {
            if (args.size() < 3 || !"强制".equals(args.get(2).trim())) {
                return new PlainText("该配置不存在");
            }
        }
        if (args.size() == 1) {
            return new PlainText(settingName + "的配置值为：" + oldVal);
        }
        String newVal = args.get(1);
        if (StringUtils.isEmpty(newVal)) {
            return new PlainText("配置无效，配置值不能为空");
        }
        cache.setSettings(settingName, newVal);
        return new PlainText("设置成功");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "设置", globalParam.botNick + "设置");
    }
}
