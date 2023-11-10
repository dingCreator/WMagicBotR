package com.whitemagic2014.command.impl.group.immortal;

import com.alibaba.fastjson.JSONObject;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.bot.MagicBotR;
import com.whitemagic2014.cache.SettingsCache;
import com.whitemagic2014.command.impl.group.EmptyStringCommand;
import com.whitemagic2014.command.impl.group.NoAuthCommand;
import com.whitemagic2014.command.impl.group.immortal.util.TimeUtil;
import com.whitemagic2014.config.properties.GlobalParam;
import com.whitemagic2014.pojo.CommandProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.ForwardMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.*;
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
@Slf4j
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
    /**
     * 使用的丹炉
     */
    private static final String ALCHEMY_CONTAINER = "修仙_丹炉";
    /**
     * 分析丹方模式
     */
    private static final AtomicBoolean SHOW_MODE = new AtomicBoolean(true);
    /**
     * 丹药名不存在
     */
    private static final String PROP_NOT_FOUND_RESP = "丹药解析出错";
    /**
     * 药材不足
     */
    private static final String HERB_NOT_ENOUGH = "药材不足";
    /**
     * 产量关键字
     */
    private static final String PRODUCTION_KEYWORD = "修仙_炼丹产量";
    /**
     * 丹药价格关键字
     */
    private static final String PRICE_KEYWORD = "修仙_丹药价格";
    /**
     * 炼完丹后是否立即上架
     */
    private static final String SHELVES_UP_KEYWORD = "修仙_炼丹上架";
    /**
     * 统计次数关键字
     */
    private static final String COUNT_TIMES_KEYWORD = "修仙_炼丹次数统计";

    /**
     * 目标丹药名称
     */
    private String targetName;
    /**
     * 群号（哪个群有人请求炼丹）
     */
    private long groupId;
    /**
     * 炼丹开始时间戳，用于记录加锁时间，超时自动释放锁
     */
    private long startTimeMillis = 0;
    /**
     * 请求炼丹的人的QQ号
     */
    private Long userId;
    /**
     * 药材数量
     */
    private final Map<String, Integer> materialNumMap = new HashMap<>();

    @Autowired
    private GlobalParam globalParam;

    @Override
    protected Message executeHandle(Member sender, ArrayList<String> args, MessageChain messageChain, Group subject) throws Exception {
        if (CollectionUtils.isEmpty(args)) {
            return new PlainText("格式：" + globalParam.botName + "炼丹 {丹药名} [开始]\n第二个参数输入“开始”则自动炼丹，否则为丹方分析模式");
        }
        // 30s超时，释放锁
        if (System.currentTimeMillis() - startTimeMillis > 30 * 1000L) {
            ALCHEMY_ING.set(false);
        }
        // 计算速度可能有一定延迟，加锁防止重复触发
        if (!ALCHEMY_ING.compareAndSet(false, true)) {
            return new PlainText("正在炼丹中，请勿重复操作");
        }
        this.targetName = args.get(0);
        this.userId = sender.getId();
        this.groupId = sender.getGroup().getId();
        this.startTimeMillis = System.currentTimeMillis();

        SHOW_MODE.set(true);
        registerAutoAlchemy();
        if (args.size() > 1 && "开始".equals(args.get(1))) {
            SHOW_MODE.set(false);
            return new PlainText("我的背包");
        }
        return new PlainText("请发送”我的背包“指令");
    }

    @Override
    public CommandProperties properties() {
        return new CommandProperties(globalParam.botName + "炼丹", globalParam.botNick + "炼丹");
    }

    /**
     * 注册自动炼丹命令
     */
    private void registerAutoAlchemy() {
        EmptyStringCommand.addLogic((sender, args, messageChain, subject, notice, atMe) -> {
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
                // 识别背包信息
                List<Material> materialList = recognizeHerbs(forwardMsg);
                long startTime = System.currentTimeMillis();
                // 自动炼丹核心算法
                String order = generatePrescript(materialList, properties);
                System.out.println("cost: " + (System.currentTimeMillis() - startTime) + " ms");

                Bot bot = MagicBotR.getBot();
                if (!PROP_NOT_FOUND_RESP.equals(order) && !SHOW_MODE.get() && !order.startsWith(HERB_NOT_ENOUGH)) {
                    order = "炼丹" + order + "丹炉" + SettingsCache.getInstance().getSettings(ALCHEMY_CONTAINER);
                }
                bot.getGroupOrFail(sender.getGroup().getId()).sendMessage(new PlainText(order));
                TimeUtil.waitRandomMillis(500, 500);
                if (!PROP_NOT_FOUND_RESP.equals(order) && !SHOW_MODE.get() && !order.startsWith(HERB_NOT_ENOUGH)) {
                    int count = SettingsCache.getInstance().getSettingsAsInt(PRODUCTION_KEYWORD, 5);
                    int price = SettingsCache.getInstance().getSettingsAsInt(PRICE_KEYWORD, 10000);
                    if (SettingsCache.getInstance().getSettingsAsBoolean(SHELVES_UP_KEYWORD, true)) {
                        bot.getGroupOrFail(sender.getGroup().getId()).sendMessage(
                                new PlainText("坊市上架 " + targetName + " " + price + " " + count));
                    }
                    return new PlainText("完成炼丹任务");
                }
                return new PlainText("完成分析任务");
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
    private String generatePrescript(List<Material> materialList, List<MaterialEffectProperties> targetList) {
        if (!validateTarget(targetList)) {
            return PROP_NOT_FOUND_RESP;
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
        // 药材数量
        materialNumMap.clear();

        // 1-按冷热顺序排序药材
        List<Material> baseMaterial = new ArrayList<>(materialList.size() * materialList.get(0).getNum());
        for (Material oldM : materialList) {
            materialNumMap.put(oldM.getName(), oldM.getNum());
            for (int i = 1; i <= oldM.getNum(); i++) {
                baseMaterial.add(oldM.deepCopy(i));
            }
        }
        log.info("药材数量【{}】", baseMaterial.size());

        // 主药-冷热排序
        List<Material> mainMaterialSortedByNature = baseMaterial.stream().sorted(Comparator.comparingInt(o ->
                o.getMainMaterial().getNature())).map(Material::deepCopy).collect(Collectors.toList());
        // 药引-冷热排序
        List<Material> associateMaterialSortedByNature = baseMaterial.stream().sorted(Comparator.comparingInt(o ->
                o.getAssociateMaterial().getNature())).map(Material::deepCopy).collect(Collectors.toList());
        // 主药-药力排序
//        List<Material> mainMaterialSortedByEffect = baseMaterial.stream().sorted(Comparator.comparingInt(o ->
//                o.getMainMaterial().getProperties().getEffectNums())).map(Material::deepCopy)
//                .collect(Collectors.toList());
        // 辅药-药力排序
//        List<Material> supportMaterialSortedByEffect = baseMaterial.stream().sorted(Comparator.comparingInt(o ->
//                o.getSupportMaterial().getProperties().getEffectNums())).map(Material::deepCopy)
//                .collect(Collectors.toList());

        // 2-双指针降低冷热调和的时间复杂度
        // 3-匹配辅药
        long calStartTimeMillis = System.currentTimeMillis();
        calAlchemy(mainMaterialSortedByNature, associateMaterialSortedByNature,
//                mainMaterialSortedByEffect, supportMaterialSortedByEffect,
                baseMaterial, result, targetList, suggest);
        log.info("cal alchemy cost:{} ms", System.currentTimeMillis() - calStartTimeMillis);

        // 4-获取最优解
        if (CollectionUtils.isEmpty(result)) {
            StringBuilder reply = new StringBuilder(HERB_NOT_ENOUGH);
            if (!CollectionUtils.isEmpty(suggest)) {
                reply.append("，建议补充以下任意一种药材");
                suggest.forEach(sg -> reply.append("\n").append(sg));
            }
            return reply.toString();
        } else {
            // 优先级计算
            int maxVal = 0;
            List<Material> r = null;
            for (List<Material> ml : result) {
                // 校验数量是否足够的情况下，计算最高得分
                Map<String, Integer> temp = new HashMap<>(materialNumMap);
                Material mainMaterial = ml.get(0);
                Material associateMaterial = ml.get(1);
                Material supportMaterial = ml.get(2);

                temp.put(ml.get(0).getSourceName(),
                        temp.getOrDefault(mainMaterial.getSourceName(), 0) - mainMaterial.getNum());
                temp.put(associateMaterial.getSourceName(),
                        temp.getOrDefault(associateMaterial.getSourceName(), 0) - associateMaterial.getNum());
                temp.put(supportMaterial.getSourceName(),
                        temp.getOrDefault(supportMaterial.getSourceName(), 0) - supportMaterial.getNum());

                boolean valid = temp.getOrDefault(mainMaterial.getSourceName(), -1) >= 0
                        && temp.getOrDefault(associateMaterial.getSourceName(), -1) >= 0
                        && temp.getOrDefault(supportMaterial.getSourceName(), -1) >= 0;
                if (!valid) {
                    continue;
                }

                int point = temp.getOrDefault(mainMaterial.getSourceName(), 0)
                        * mainMaterial.getMainMaterial().getPower()
                        + temp.getOrDefault(associateMaterial.getSourceName(), 0)
                        * associateMaterial.getAssociateMaterial().getPower()
                        + temp.getOrDefault(supportMaterial.getSourceName(), 0)
                        * supportMaterial.getSupportMaterial().getPower();

                if (point > maxVal) {
                    maxVal = point;
                    r = Arrays.asList(ml.get(0), ml.get(1), ml.get(2));
                }
            }
            if (maxVal == 0) {
                return HERB_NOT_ENOUGH;
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
     * @param mainMaterialSortedByNature      冷热排序的主药
     * @param associateMaterialSortedByNature 冷热排序的药引
     *                                        //     * @param mainMaterialSortedByEffect      药力排序的主药
     *                                        //     * @param supportMaterialSortedByEffect   药力排序的辅药
     * @param materialList                    原材料 不可变
     * @param result                          结果
     * @param targetList                      目标
     */
    private void calAlchemy(List<Material> mainMaterialSortedByNature, List<Material> associateMaterialSortedByNature,
//                            List<Material> mainMaterialSortedByEffect, List<Material> supportMaterialSortedByEffect,
                            final List<Material> materialList,
                            List<List<Material>> result, List<MaterialEffectProperties> targetList, Set<String> suggest) {
        MaterialEffect effect1 = targetList.get(0).getEffect();
        MaterialEffect effect2 = targetList.get(2).getEffect();
        int effect1NumFrom = targetList.get(0).getEffectNums();
        int effect1NumTo = targetList.get(1).getEffectNums();
        int effect2NumFrom = targetList.get(2).getEffectNums();
        int effect2NumTo = targetList.get(3).getEffectNums();
        int size = materialList.size();

        // 定义左右指针
        int leftPointer = 0, rightPointer = size - 1;

        // 主药和药引是根据冷热顺序排序的
        while (leftPointer < size - 1 && rightPointer > 0) {
            // 另一种药材的属性
            MaterialEffect anotherEffect;
            int anotherEffectNumFrom, anotherEffectNumTo;
            // 冷热
            int mainNature;
            // 主药副本
            Material mainMaterial = mainMaterialSortedByNature.get(leftPointer).deepCopy();
            // 主药属性
            MaterialProperties mainProp = mainMaterial.getMainMaterial();

            // 药力
            int effectNums = mainProp.getProperties().getEffectNums();

            // 匹配药性 选择主药
            if (mainProp.getProperties().getEffect().equals(effect1)) {
                if (effectNums < effect1NumFrom || effectNums >= effect1NumTo * 5) {
                    leftPointer++;
                    continue;
                }
                anotherEffect = effect2;
                anotherEffectNumFrom = effect2NumFrom;
                anotherEffectNumTo = effect2NumTo;
                mainNature = mainProp.getNature();
                mainProp.setPower(mainProp.getPower() - 100 * (effectNums - effect1NumTo));
            } else if (mainProp.getProperties().getEffect().equals(effect2)) {
                if (effectNums < effect2NumFrom || effectNums >= effect2NumTo * 5) {
                    leftPointer++;
                    continue;
                }
                anotherEffect = effect1;
                anotherEffectNumFrom = effect1NumFrom;
                anotherEffectNumTo = effect1NumTo;
                mainNature = mainProp.getNature();
                mainProp.setPower(mainProp.getPower() - 100 * (effectNums - effect2NumTo));
            } else {
                leftPointer++;
                continue;
            }

            // 选择药引
            Material associateMaterial = associateMaterialSortedByNature.get(rightPointer).deepCopy();
            if (mainNature != 0) {
                // 主药药性不为性平，需要调和
                MaterialProperties associateProp = associateMaterialSortedByNature.get(rightPointer).getAssociateMaterial();
                if (mainNature + associateProp.getNature() < 0) {
                    leftPointer++;
                    continue;
                } else if (mainNature + associateProp.getNature() > 0) {
                    rightPointer--;
                    continue;
                }

                // abyss:主药带寒热的情况下药引优先寒热炼气，再次寒热凝神
                if (associateMaterialSortedByNature.get(rightPointer).getSupportMaterial().getProperties().getEffect()
                        .equals(MaterialEffect.LIAN_QI)
                        || associateMaterialSortedByNature.get(rightPointer).getSupportMaterial().getProperties().getEffect()
                        .equals(MaterialEffect.NING_SHEN)) {
                    associateProp.setPower(1000);
                }
            } else {
                associateMaterial.setName(associateMaterial.getName().replace(String.valueOf(associateMaterial.getNum()), "0"));
                associateMaterial.setNum(0);
            }

            boolean success = false;
            // 选择辅药
            for (Material material : materialList) {
                Material supportMaterial = material.deepCopy();
                MaterialProperties supportProp = supportMaterial.getSupportMaterial();
                int supportEffectNum = supportProp.getProperties().getEffectNums();

                if (supportProp.getProperties().getEffect().equals(anotherEffect)) {
                    if (supportEffectNum >= anotherEffectNumTo * 5) {
                        rightPointer--;
                    }
                    // 辅药可以考虑超出药力
                    if (supportEffectNum >= anotherEffectNumFrom) {
                        result.add(Arrays.asList(mainMaterial, associateMaterial, supportMaterial));
                        success = true;
                        supportProp.setPower(supportProp.getPower() - 100 *
                                (supportProp.getProperties().getEffectNums() - anotherEffectNumTo));
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
    private boolean validateTarget(List<MaterialEffectProperties> targetList) {
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
                } else if (i.startsWith("品级")) {
                    // todo 品级权重
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
                                supportPower = 10000;
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
            setPower(name, mainProp);
            setPower(name, associateProp);
            setPower(name, supportProp);
            result.add(new Material(name, count, mainProp, associateProp, supportProp));
        });
        return result;
    }

    private void setPower(String name, MaterialProperties properties) {
        if (!"恒心草".equals(name)) {
            properties.setPower(100);
        }
    }

    private void countTimes(Member sender) {
        List<String> list = SettingsCache.getInstance()
                .getSettings(COUNT_TIMES_KEYWORD, str -> JSONObject.parseArray(str, String.class));
        // QQ号|昵称|丹药名|次数
        String rec = "";
        if (!CollectionUtils.isEmpty(list)) {
            int index = -1;
            for (int i = 0; i < list.size(); i++) {
                String str = list.get(i);
                String[] timesArgs = str.split("|");
                if (timesArgs[0].equals(String.valueOf(sender.getId()).trim())
                        && timesArgs[2].equals(targetName)) {
                    rec = timesArgs[0] + "|" + sender.getNick() + "|" + targetName + "|"
                            + Integer.parseInt(timesArgs[3]) + 1;
                    index = i;
                    break;
                }
            }
            if (index > 0) {
                list.set(index, rec);
            } else {
                rec = sender.getId() + "|" + sender.getNick() + "|" + targetName + "|" + 1;
                list.add(rec);
            }
        } else {
            list = new ArrayList<>();
            rec = sender.getId() + "|" + sender.getNick() + "|" + targetName + "|" + 1;
            list.add(rec);
        }
        SettingsCache.getInstance().setSettings(COUNT_TIMES_KEYWORD, JSONObject.toJSONString(list));
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
         * 获取原始药材名
         *
         * @return 原始药材名
         */
        public String getSourceName() {
            return this.name.replace(String.valueOf(this.num), "");
        }

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
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 3072),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 1536),
                new MaterialEffectProperties(MaterialEffect.NING_SHEN, 3072)
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
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 3072),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 1536),
                new MaterialEffectProperties(MaterialEffect.LIAN_QI, 3072)
        ));
        // 修为丹
        ALCHEMY_DICT.put("洗髓丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 6),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 12),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 6),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 12)
        ));
        ALCHEMY_DICT.put("养气丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 12),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 24),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 12),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 24)
        ));
        ALCHEMY_DICT.put("九转丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 24),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 48),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 24),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 48)
        ));
        ALCHEMY_DICT.put("易筋丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 48),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 96),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 48),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 96)
        ));
        ALCHEMY_DICT.put("天尘丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 96),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 192),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 96),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 192)
        ));
        ALCHEMY_DICT.put("天元神丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 192),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 384),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 192),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 384)
        ));
        ALCHEMY_DICT.put("太乙碧莹丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 384),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 768),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 384),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 768)
        ));
        ALCHEMY_DICT.put("六阳长生丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 768),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 1536),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 768),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 1536)
        ));
        ALCHEMY_DICT.put("道源丹", Arrays.asList(
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 1536),
                new MaterialEffectProperties(MaterialEffect.SHENG_XI, 3072),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 1536),
                new MaterialEffectProperties(MaterialEffect.YANG_QI, 3072)
        ));
    }
}
