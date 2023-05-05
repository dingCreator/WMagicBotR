package com.whitemagic2014.command.impl.group.immortal;

import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.command.impl.group.EmptyStringCommand;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.ForwardMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 自动炼丹
 *
 * @author ding
 * @author abyss
 * @date 2023/4/22
 */
@Command
public class AlchemyCommand extends NoAuthCommand {

    /**
     * 是否正在炼丹
     */
    private static final AtomicBoolean ALCHEMY_ING = new AtomicBoolean(false);
    /**
     * 锁
     */
    private static final Lock LOCK = new ReentrantLock();
    /**
     * key
     */
    private static final String KEY = "autoAlchemy";

    private String targetName;
    private long groupId;
    private long startTimeMillis = 0;

    @Autowired
    private GlobalParam globalParam;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (CollectionUtils.isEmpty(args)) {
            return new PlainText("格式：" + globalParam.botName + "炼丹 {丹药名}");
        }
        if (System.currentTimeMillis() - startTimeMillis > 30 * 1000L) {
            ALCHEMY_ING.set(false);
        }
        // 计算速度可能有一定延迟，加锁防止重复触发
        if (!ALCHEMY_ING.compareAndSet(false, true)) {
            return new PlainText("正在炼丹中，请勿重复操作");
        }
        this.targetName = args.get(0);
        this.groupId = sender.getGroup().getId();
        this.startTimeMillis = System.currentTimeMillis();

        registerAutoAlchemy();
        return new PlainText("我的背包");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties("测试炼丹");
    }

    private void registerAutoAlchemy() {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, notice) -> {
            // 非本群组合信息，忽略
            if (groupId != sender.getGroup().getId() || messageChain.stream()
                    .filter(ForwardMessage.class::isInstance).findFirst().orElse(null) == null) {
                return null;
            }
            ForwardMessage forwardMsg = (ForwardMessage) messageChain.stream().filter(ForwardMessage.class::isInstance)
                    .findFirst().orElse(null);
            if (forwardMsg == null) {
                return null;
            }

            // 非炼丹中，忽略
            if (!ALCHEMY_ING.get()) {
                return null;
            }

            // 获取丹药属性
            List<MaterialEffectProperties> properties = getEffectProperties(targetName);
            // 炼丹过程加锁，防止重复触发
            if (!LOCK.tryLock()) {
                return new PlainText("炼丹进行中");
            }
            try {
                // 防止CAS操作自旋进入逻辑
                if (!ALCHEMY_ING.get()) {
                    return null;
                }
                // 识别背包信息
                List<Material> materialList = recognizeHerbs(forwardMsg);
                long startTime = System.currentTimeMillis();
                // 自动炼丹核心算法
                String order = generatePrescript(materialList, properties);
                System.out.println("cost: " + (System.currentTimeMillis() - startTime) + " ms");
                return new PlainText(order);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 无论炼丹结果如何，释放所有锁
                LOCK.unlock();
                ALCHEMY_ING.set(false);
            }
            return null;
        }, KEY, false);
    }

    /**
     * @author ding
     * @date 2023/4/23
     * <p>
     * 1.获取丹药对应的需求属性值
     * 2.加载药材属性
     * 3.剪枝 使用数量最多的药材
     * 4.输出最优解 若不存在相应的解 则返回提示
     */
    private static String generatePrescript(List<Material> materialList, List<MaterialEffectProperties> targetList) {
        if (!validateTarget(targetList)) {
            return "丹药解析出错";
        }
        /*
         1-按冷热顺序排序药材
         2-双指针降低冷热调和的时间复杂度
         3-匹配辅药
         4-得出最优解
         */
        // 结果集
        List<List<Material>> result = new ArrayList<>();
        // 建议
        Set<String> suggest = new HashSet<>();
        // 1-按冷热顺序排序药材
        List<Material> baseMaterial = new ArrayList<>(materialList.size() * materialList.get(0).getNum());
        for (Material oldM : materialList) {
            for (int i = 1; i <= oldM.getNum(); i++) {
//                MaterialEffect supportEffect = oldM.getSupportMaterial().getProperties().getEffect();
//
//                Material m = new Material(oldM.getName() + i, i, oldM.getPower(),
//                        new MaterialProperties(oldM.getMainMaterial().getNature() * i,
//                                new MaterialEffectProperties(oldM.getMainMaterial().getProperties().getEffect(),
//                                        oldM.getMainMaterial().getProperties().getEffectNums() * i)),
//                        new MaterialProperties(oldM.getAssociateMaterial().getNature() * i, null),
//                        new MaterialProperties(0,
//                                new MaterialEffectProperties(supportEffect,
//                                        oldM.getSupportMaterial().getProperties().getEffectNums() * i)));
//                baseMaterial.add(m);
                baseMaterial.add(oldM.deepCopy(i));
            }
        }

        // 主药
        List<Material> mainMaterial = baseMaterial.stream().sorted(Comparator.comparingInt(o ->
                o.getMainMaterial().getNature())).map(Material::deepCopy).collect(Collectors.toList());
        // 药引
        List<Material> associateMaterial = baseMaterial.stream().sorted(Comparator.comparingInt(o ->
                o.getAssociateMaterial().getNature())).map(Material::deepCopy).collect(Collectors.toList());

        // 2-双指针降低冷热调和的时间复杂度
        // 3-匹配辅药
        calAlchemy(mainMaterial, associateMaterial, baseMaterial, result, targetList, suggest);

        // 4-获取最优解
        StringBuilder reply = new StringBuilder("药材不足");
        if (CollectionUtils.isEmpty(result)) {
            if (!CollectionUtils.isEmpty(suggest)) {
                reply.append("，建议补充以下任意一种药材\n");
                suggest.forEach(sg -> reply.append(sg).append("\n"));
            }
            return reply.toString();
        } else {
            // 优先级计算
            int maxVal = 0;
            List<Material> r = null;
            for (List<Material> ml : result) {
                List<Material> temp = new ArrayList<>(materialList.size());
                for (Material material : materialList) {
                    temp.add(material.deepCopy());
                }
                AtomicInteger i = new AtomicInteger(0);
                boolean valid = temp.stream().allMatch(t -> {
                    /*
                     校验数量是否足够的情况下，计算最高得分
                     */
                    if (ml.get(0).getName().startsWith(t.getName())) {
                        int num = t.getNum() - ml.get(0).getNum();
                        if (num < 0) {
                            return false;
                        }
                        t.setNum(num);
                        i.set(i.get() + num * ml.get(0).getMainMaterial().getPower());
                    }
                    if (ml.get(1).getName().startsWith(t.getName())) {
                        int num = t.getNum() - ml.get(1).getNum();
                        if (num < 0) {
                            return false;
                        }
                        t.setNum(num);
                        i.set(i.get() + num * ml.get(1).getAssociateMaterial().getPower());
                    }
                    if (ml.get(2).getName().startsWith(t.getName())) {
                        int num = t.getNum() - ml.get(2).getNum();
                        if (num < 0) {
                            return false;
                        }
                        t.setNum(num);
                        i.set(i.get() + num * ml.get(2).getSupportMaterial().getPower());
                    }
                    return true;
                });

                if (valid && i.get() > maxVal) {
                    maxVal = i.get();
                    r = Arrays.asList(ml.get(0), ml.get(1), ml.get(2));
                }
            }
            if (maxVal == 0) {
                return "药材不足";
            }
            System.out.println("maxVal:[" + maxVal + "] main material power:[" +
                    r.get(0).getMainMaterial().getPower() + "] associate material power:[" +
                    r.get(1).getAssociateMaterial().getPower() + "] support material power:[" +
                    r.get(2).getSupportMaterial().getPower() + "]");
            return "主药" + r.get(0).getName() + "药引" + r.get(1).getName() + "辅药" + r.get(2).getName();
        }
    }

    /**
     * 计算所有可行的组合
     *
     * @param mainMaterial      主药
     * @param associateMaterial 药引
     * @param materialList      原材料
     * @param result            结果
     * @param targetList        目标
     */
    private static void calAlchemy(List<Material> mainMaterial, List<Material> associateMaterial, List<Material> materialList
            , List<List<Material>> result, List<MaterialEffectProperties> targetList, Set<String> suggest) {
        MaterialEffect effect1 = targetList.get(0).getEffect();
        MaterialEffect effect2 = targetList.get(2).getEffect();
        int effect1NumFrom = targetList.get(0).getEffectNums();
        int effect1NumTo = targetList.get(1).getEffectNums();
        int effect2NumFrom = targetList.get(2).getEffectNums();
        int effect2NumTo = targetList.get(3).getEffectNums();
        int size = materialList.size();

        // 定义左右指针
        int leftPointer = 0, rightPointer = size - 1;
        while (leftPointer < size - 1 && rightPointer > 0) {
            // 另一种药材的属性
            MaterialEffect anotherEffect;
            int anotherEffectNumFrom, anotherEffectNumTo;
            // 冷热
            int mainNature;

            if (mainMaterial.get(leftPointer).getMainMaterial().getProperties().getEffect().equals(effect1)) {
                int effectNums = mainMaterial.get(leftPointer).getMainMaterial().getProperties().getEffectNums();
                if (effectNums < effect1NumFrom || effectNums > effect1NumTo) {
                    leftPointer++;
                    continue;
                }
                anotherEffect = effect2;
                anotherEffectNumFrom = effect2NumFrom;
                anotherEffectNumTo = effect2NumTo;
                mainNature = mainMaterial.get(leftPointer).getMainMaterial().getNature();
            } else if (mainMaterial.get(leftPointer).getMainMaterial().getProperties().getEffect().equals(effect2)) {
                int effectNums = mainMaterial.get(leftPointer).getMainMaterial().getProperties().getEffectNums();
                if (effectNums < effect2NumFrom || effectNums > effect2NumTo) {
                    leftPointer++;
                    continue;
                }
                anotherEffect = effect1;
                anotherEffectNumFrom = effect1NumFrom;
                anotherEffectNumTo = effect1NumTo;
                mainNature = mainMaterial.get(leftPointer).getMainMaterial().getNature();
            } else {
                leftPointer++;
                continue;
            }

            if (mainNature != 0) {
                // 主药药性不为性平，需要调和
                MaterialProperties associateProp = associateMaterial.get(rightPointer).getAssociateMaterial();
                if (mainNature + associateProp.getNature() < 0) {
                    leftPointer++;
                    continue;
                } else if (mainMaterial.get(leftPointer).getMainMaterial().getNature() + associateProp.getNature() > 0) {
                    rightPointer--;
                    continue;
                }

                // abyss:主药带寒热的情况下药引优先寒热炼气，再次寒热凝神
                if (associateMaterial.get(rightPointer).getSupportMaterial().getProperties().getEffect()
                        .equals(MaterialEffect.LIAN_QI)
                        || associateMaterial.get(rightPointer).getSupportMaterial().getProperties().getEffect()
                        .equals(MaterialEffect.NING_SHEN)) {
                    associateProp.setPower(100000);
                }
            }

            boolean success = false;
            for (Material material : materialList) {
                if (material.getSupportMaterial().getProperties().getEffect().equals(anotherEffect)) {
                    if (material.getSupportMaterial().getProperties().getEffectNums() >= anotherEffectNumFrom
                            && material.getSupportMaterial().getProperties().getEffectNums() < anotherEffectNumTo) {
                        Material associate = associateMaterial.get(rightPointer).deepCopy();
                        if (mainNature == 0) {
                            associate.setName(associate.getName().replace(String.valueOf(associate.getNum()), "0"));
                            associate.setNum(0);
                        }
                        result.add(Arrays.asList(mainMaterial.get(leftPointer), associate, material));
                        success = true;
                    }
                }
            }

            if (!success) {
                suggest.add("缺少【" + anotherEffect.getEffectTypeName() + anotherEffectNumFrom +
                        "】-【" + anotherEffect.getEffectTypeName() + anotherEffectNumTo + "】的药材");
            }
            leftPointer++;
        }
    }

    /**
     * 目标内容必须为 药性1下限 药性1上限 药性2下限 药性2上限
     *
     * @param targetList 目标
     * @return 目标参数是否合法
     */
    private static boolean validateTarget(List<MaterialEffectProperties> targetList) {
        return !CollectionUtils.isEmpty(targetList)
                && targetList.size() == 4
                && targetList.get(0).getEffect().equals(targetList.get(1).getEffect())
                && targetList.get(0).getEffectNums() < targetList.get(1).getEffectNums()
                && targetList.get(2).getEffect().equals(targetList.get(3).getEffect())
                && targetList.get(2).getEffectNums() < targetList.get(3).getEffectNums();
    }

    /**
     * 识别背包中的药材信息
     *
     * @param forwardMessage 组合信息
     * @return 药材信息
     */
    private List<Material> recognizeHerbs(ForwardMessage forwardMessage) {
        List<Material> result = new ArrayList<>();
        AtomicBoolean herbs = new AtomicBoolean(false);
        forwardMessage.component6().forEach(node -> {
            String singleMsg = node.component4().toString().trim();
            if (singleMsg.startsWith("☆------")) {
                if ("☆------我的药材------☆".equals(singleMsg)) {
                    herbs.set(true);
                } else {
                    herbs.set(false);
                }
                return;
            }

            if (!herbs.get()) {
                return;
            }
            String[] info = singleMsg.split("\n");
            String name = null;
            int count = 0;

            MaterialProperties mainProp = null, associateProp = null, supportProp = null;
            for (String i : info) {
                i = i.trim();
                if (i.startsWith("名字：")) {
                    name = i.replace("名字：", "").trim();
                } else if (i.startsWith("主药")) {
                    String[] prop = i.split(" ");
                    String nature = prop[1].substring(0, 2);

                    MaterialEffect effectType = MaterialEffect.getType(prop[2].substring(0, 2));
                    int effectNum = Integer.parseInt(prop[2].substring(2));
                    switch (nature) {
                        case "性热":
                            mainProp = new MaterialProperties(Integer.parseInt(prop[1].substring(2)), 1,
                                    new MaterialEffectProperties(effectType, effectNum));
                            break;
                        case "性寒":
                            mainProp = new MaterialProperties(-Integer.parseInt(prop[1].substring(2)), 1,
                                    new MaterialEffectProperties(effectType, effectNum));
                            break;
                        case "性平":
                            mainProp = new MaterialProperties(0, 1,
                                    new MaterialEffectProperties(effectType, effectNum));
                            break;
                        default:
                            break;
                    }
                } else if (i.startsWith("药引")) {
                    String[] prop = i.split(" ");
                    String nature = prop[1].substring(0, 2);
                    int supportPower = 1;
                    MaterialEffect effectType = MaterialEffect.getType(prop[2].substring(0, 2));

                    switch (nature) {
                        case "性热":
                            associateProp = new MaterialProperties(Integer.parseInt(prop[1]
                                    .substring(2, prop[1].length() - 2)), 1, null);
                            break;
                        case "性寒":
                            associateProp = new MaterialProperties(-Integer.parseInt(prop[1]
                                    .substring(2, prop[1].length() - 2)), 1, null);
                            break;
                        case "性平":
                            associateProp = new MaterialProperties(0, 1, null);
                            // 炼丹师abyss：辅药优先性平炼气跟性平凝神
                            if (effectType.equals(MaterialEffect.NING_SHEN) || effectType.equals(MaterialEffect.LIAN_QI)) {
                                supportPower = 100000;
                            }
                            break;
                        default:
                            break;
                    }

                    int effectNum = Integer.parseInt(prop[2].substring(2));
                    supportProp = new MaterialProperties(0, supportPower, new MaterialEffectProperties(effectType, effectNum));
                } else if (i.startsWith("拥有数量：")) {
                    try {
                        count = Integer.parseInt(i.replace("拥有数量：", "").trim());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            result.add(new Material(name, count, mainProp, associateProp, supportProp));
        });
        return result;
    }

    /**
     * 药材信息
     * 同一种药材，作为不同药有不同药性
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Material {
        /**
         * 药材名
         */
        private String name;
        /**
         * 数量
         */
        private int num;
        /**
         * 主药
         */
        private MaterialProperties mainMaterial;
        /**
         * 药引
         */
        private MaterialProperties associateMaterial;
        /**
         * 辅药
         */
        private MaterialProperties supportMaterial;

        /**
         * 深拷贝
         *
         * @return another new Material
         */
        public Material deepCopy() {
            return new Material(this.getName(), this.getNum(),
                    new MaterialProperties(this.getMainMaterial().getNature(),
                            this.getMainMaterial().getPower(),
                            new MaterialEffectProperties(this.getMainMaterial().getProperties().getEffect(),
                                    this.getMainMaterial().getProperties().getEffectNums())),
                    new MaterialProperties(this.getAssociateMaterial().getNature(),
                            this.getAssociateMaterial().getPower(), null),
                    new MaterialProperties(0,
                            this.getSupportMaterial().getPower(),
                            new MaterialEffectProperties(this.getSupportMaterial().getProperties().getEffect(),
                                    this.getSupportMaterial().getProperties().getEffectNums())));
        }

        /**
         * 深拷贝
         *
         * @return another new Material
         */
        public Material deepCopy(int count) {
            return new Material(this.getName() + count, count,
                    new MaterialProperties(this.getMainMaterial().getNature() * count,
                            this.getMainMaterial().getPower(),
                            new MaterialEffectProperties(this.getMainMaterial().getProperties().getEffect(),
                                    this.getMainMaterial().getProperties().getEffectNums() * count)),
                    new MaterialProperties(this.getAssociateMaterial().getNature() * count,
                            this.getAssociateMaterial().getPower(), null),
                    new MaterialProperties(0,
                            this.getSupportMaterial().getPower(),
                            new MaterialEffectProperties(this.getSupportMaterial().getProperties().getEffect(),
                                    this.getSupportMaterial().getProperties().getEffectNums() * count)));
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    private static class MaterialProperties {
        /**
         * +性暖 0性平 -性冷
         */
        private int nature;
        /**
         * 权重
         */
        private int power;
        /**
         * 药性
         */
        private MaterialEffectProperties properties;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    private static class MaterialEffectProperties {
        /**
         * 药性
         */
        private MaterialEffect effect;
        /**
         * 数值
         */
        private int effectNums;
    }

    @Getter
    @AllArgsConstructor
    private enum MaterialEffect {
        /**
         * 药性
         */
        SHENG_XI("生息"),
        YANG_QI("养气"),
        LIAN_QI("炼气"),
        JU_YUAN("聚元"),
        NING_SHEN("凝神"),
        ;
        private final String effectTypeName;

        public static MaterialEffect getType(String name) {
            return Arrays.stream(MaterialEffect.values())
                    .filter(e -> name.equals(e.getEffectTypeName())).findFirst().orElse(null);
        }
    }

    private List<MaterialEffectProperties> getEffectProperties(String targetName) {
        return ALCHEMY_DICT.getOrDefault(targetName, null);
    }

    /**
     * 丹药属性配置
     */
    private static final Map<String, List<MaterialEffectProperties>> ALCHEMY_DICT;

    static {
        ALCHEMY_DICT = new HashMap<>();
        // 攻击丹
        ALCHEMY_DICT.put("摄魂鬼丸", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 6),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 12),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 6),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 12)
        ));
        ALCHEMY_DICT.put("化煞魔丸", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 12),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 24),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 12),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 24)
        ));
        ALCHEMY_DICT.put("素心真丸", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 24),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 48),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 24),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 48)
        ));
        ALCHEMY_DICT.put("灭神古丸", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 48),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 96),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 48),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 96)
        ));
        ALCHEMY_DICT.put("静禅魔丸", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 96),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 192),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 96),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 192)
        ));
        ALCHEMY_DICT.put("地仙玄丸", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 192),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 384),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 192),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 384)
        ));
        ALCHEMY_DICT.put("消冰宝丸", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 384),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 768),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 384),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 768)
        ));
        ALCHEMY_DICT.put("无涯鬼丸", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 768),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 1536),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 768),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 1536)
        ));
        ALCHEMY_DICT.put("太一仙丸", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 1536),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, Integer.MAX_VALUE),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 1536),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, Integer.MAX_VALUE)
        ));
        // 突破丹
        ALCHEMY_DICT.put("筑基丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 2),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 4),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 2),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 4)
        ));
        ALCHEMY_DICT.put("朝元丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 4),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 8),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 4),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 8)
        ));
        ALCHEMY_DICT.put("聚顶丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 8),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 12),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 8),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 12)
        ));
        ALCHEMY_DICT.put("锻脉丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 12),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 16),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 12),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 16)
        ));
        ALCHEMY_DICT.put("护脉丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 16),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 20),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 16),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 20)
        ));
        ALCHEMY_DICT.put("天命淬体丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 20),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 24),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 20),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 24)
        ));
        ALCHEMY_DICT.put("澄心塑魂丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 24),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 32),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 24),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 32)
        ));
        ALCHEMY_DICT.put("混元仙体丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 32),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 48),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 32),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 48)
        ));
        ALCHEMY_DICT.put("黑炎丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 48),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 64),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 48),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 64)
        ));
        ALCHEMY_DICT.put("金血丸", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 64),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 96),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 64),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 96)
        ));
        ALCHEMY_DICT.put("虚灵丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 96),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 128),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 96),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 128)
        ));
        ALCHEMY_DICT.put("净明丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 128),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 160),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 128),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 160)
        ));
        ALCHEMY_DICT.put("安神灵液", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 160),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 192),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 160),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 192)
        ));
        ALCHEMY_DICT.put("魇龙之血", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 192),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 256),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 192),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 256)
        ));
        ALCHEMY_DICT.put("化劫丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 256),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 384),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 256),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 384)
        ));
        ALCHEMY_DICT.put("太上玄门丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 384),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 768),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 384),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 768)
        ));
        ALCHEMY_DICT.put("金仙破厄丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 768),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 1536),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 768),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 1536)
        ));
        ALCHEMY_DICT.put("太乙炼髓丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 1536),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, Integer.MAX_VALUE),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 1536),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, Integer.MAX_VALUE)
        ));
        // 修为丹
        ALCHEMY_DICT.put("", Arrays.asList(
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties()
        ));
        ALCHEMY_DICT.put("", Arrays.asList(
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties()
        ));
        ALCHEMY_DICT.put("", Arrays.asList(
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties()
        ));
        ALCHEMY_DICT.put("", Arrays.asList(
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties()
        ));
        ALCHEMY_DICT.put("", Arrays.asList(
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties()
        ));
        ALCHEMY_DICT.put("", Arrays.asList(
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties()
        ));
        ALCHEMY_DICT.put("", Arrays.asList(
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties()
        ));
        ALCHEMY_DICT.put("", Arrays.asList(
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties(),
                new MaterialEffectProperties()
        ));
    }
}
