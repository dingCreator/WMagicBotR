package com.whitemagic2014.command.impl.group.astrology.skill;

import com.dingCreator.astrology.enums.job.JobEnum;
import com.dingCreator.astrology.enums.skill.SkillEnum;

/**
 * @author ding
 * @date 2024/3/31
 */
public class SkillFormatUtil {

    public static String skillDetailFormat(SkillEnum skillEnum) {
        StringBuilder builder = new StringBuilder("技能编号：").append(skillEnum.getId());
        builder.append("\n技能名称：").append(skillEnum.getName());
        builder.append("\n技能消耗：").append(skillEnum.getMp());
        builder.append("\n限定职业：");
        if (skillEnum.getJobCode().contains("None")) {
            builder.append("所有职业均不可用");
        } else if (skillEnum.getJobCode().contains("All")) {
            builder.append("所有职业均可使用");
        } else {
            String job = skillEnum.getJobCode().stream().filter(j -> !"None".equals(j) && !"All".equals(j))
                    .map(j -> JobEnum.getByCode(j).getJobName()).reduce((s1, s2) -> s1 + "," + s2)
                    .orElse("所有职业均不可用");
            builder.append(job);
        }
        builder.append("\n技能描述：").append(skillEnum.getDesc());
        return builder.toString();
    }
}
