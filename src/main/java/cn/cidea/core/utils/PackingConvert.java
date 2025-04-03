package cn.cidea.core.utils;

import cn.cidea.core.utils.math.Fraction;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 解析规格串，生成规格串包含的单位和转化系数，最后根据当前单位数量、当前单位、目标单位，获取目标单位数量
 * 规格串没什么规范，比较五花八门，以适配性为主，相应的适配处理会注释上案例
 *
 * @author: CIdea
 */
@Slf4j
public class PackingConvert {

    // 单位
    private static final String UNIT_REG = "(mm|cm|m|μg|ug|mg|g|kg|t|kcal|s|ca|kit|react|reactions|run|nmol|mlar|mgx|pack|p|r|ke|b|微克|合|毫克|平方米|毫居|卡|小袋|克|吸|千克|反应|吨|片|#|粒|片瓶|单|支|ml|iul|l|vl|ul|nl|mci|管|毫升|升|件|泡|复方片|复方栓|复方粒|复方丸|复方胶囊|复方膏|复方|瓶|袋|盒|枚|张|只|罐|包|抽|帖|散|板|套|本|听|桶|个|卷|份|条|根|扳|筒|掀|箱|贴|滴|批|喷|次|对|块|人次|人份|孔|每人|位|揿|付|种|a\\.b|rxns|丸|miu|kiu|au|iu|su|ui|mu|u|ku|lsu|bu|eu|pe|d|单位|pna|pna单位|万u|万iu|百万单位|万单位|国际单位|unit|units|亿活菌|axaiu)";
    private static final String UNIT_REG_END = UNIT_REG + "$";
    // 数字正则
    private static final String NUMBER_REG_END = RegexStr.NUMBER + "$";

    // 运算符
    private static final char[] multiplyOpt = new char[]{'*', '/', 'x', '×', '／', '＊', '\\'};
    // 默认参数
    private static final String DEF_UNIT = "件";
    private static final RoundingMode ROUNDING_MODE = RoundingMode.CEILING;
    private static final int ROUNDING_SCALE = 8;
    /**
     * K1-规格值；K2-单位；V-规格节点
     */
    private static Map<String, Map<String, Node>> cache = new HashMap<>();
    /**
     * 通用单位
     */
    private static final List<Node> GENERAL_UNIT_TREE = new ArrayList<>();

    static {
        Node kg = buildTree("1000000mg:1000000毫克:1000g:1000克:1kg:1千克", "kg");
        GENERAL_UNIT_TREE.add(kg);

        Node l = new Node("1000ml:1000毫升:1l:1升", "l");
        GENERAL_UNIT_TREE.add(l);
    }

    /**
     * 返回当前计量转为目标单位时对应的数量
     * 即return=cnt*cntUnit/targetUnit
     * eg: 1盒 -> 片，100片/盒，返回100，即1盒等于100片
     * eg: 1kg -> g, 返回1000
     *
     * @param cnt        当前数量
     * @param cntUnit    当前单位
     * @param targetUnit 目标单位
     * @param spec       规格
     * @param unit       单位，对应规格
     */
    public static BigDecimal convert(BigDecimal cnt, String cntUnit, String targetUnit, String spec, String unit) {
        cntUnit = StringUtils.lowerCase(cntUnit);
        targetUnit = StringUtils.lowerCase(targetUnit);
        spec = StringUtils.lowerCase(spec);
        unit = StringUtils.lowerCase(unit);
        log.info("cnt = {}, cntUnit = {}, targetUnit = {}, spec = {}, unit = {}", cnt, cntUnit, targetUnit, spec, unit);
        if (cnt == null || StringUtils.isAnyBlank(cntUnit, targetUnit, spec)) {
            log.warn("primary param can't be null");
            return null;
        }
        if (StringUtils.isBlank(unit)) {
            unit = DEF_UNIT;
        }
        if (cntUnit.equals(targetUnit)) {
            // 单位相同，不转换
            log.info("unit equals");
            return cnt;
        }
        Node root = buildTree(spec, unit);
        if (root == null) {
            return null;
        }
        Node cur = null;
        // 当前单位或同义词查找
        for (String term : Synonyms.get(cntUnit)) {
            cur = Optional.ofNullable(root.getEdges().get(term)).map(Edge::getAccess).orElse(null);
            if (cur != null) {
                log.info("cntUnit = {}", term);
                break;
            }
        }
        // 通用单位查找
        Fraction curWeight = Fraction.ONE;
        if (cur == null) {
            for (Node generalUnit : GENERAL_UNIT_TREE) {
                Edge edge = generalUnit.getEdges().get(cntUnit);
                if (edge == null) {
                    continue;
                }
                // 含通用单位
                for (Map.Entry<String, Edge> entry : generalUnit.getEdges().entrySet()) {
                    Edge convertUnitEdge = root.getEdges().get(entry.getKey());
                    if (convertUnitEdge != null) {
                        // 转化单位命中，用转化后单位做cur，并加上权重
                        cur = convertUnitEdge.getAccess();
                        // curWeight = entry.getValue().getAccess().getEdges().get(entry.getKey()).getWeight();
                        curWeight = generalUnit.getEdges().get(cntUnit).getAccess().getEdges().get(entry.getKey()).getWeight();
                        break;
                    }
                    // cur = Optional.ofNullable(value).map(Edge::getAccess).orElse(null);
                    // if (cur != null) {
                    //     // 命中通用单位
                    //     log.info("cntUnit = {}", entry.getKey());
                    //     break;
                    // }
                }
                if (cur != null) {
                    break;
                }
            }
        }
        if (cur == null) {
            log.error("spec {} can't find cntUnit {}", spec, cntUnit);
            log.info("root edges = {}", JSONObject.toJSONString(root.getEdges().keySet()));
            return null;
        }
        Fraction targetWeight = null;
        for (String term : Synonyms.get(targetUnit)) {
            Edge targetEdge = cur.getEdges().get(term);
            if (targetEdge != null) {
                log.info("targetUnit = {}", term);
                targetWeight = targetEdge.getWeight();
                break;
            }
        }
        if (targetWeight == null) {
            for (Node generalUnit : GENERAL_UNIT_TREE) {
                Edge edge = generalUnit.getEdges().get(targetUnit);
                if (edge != null) {
                    // 通用组中含有目标单位
                    targetWeight = edge.getAccess().getEdges().get(cntUnit).getWeight().reverse();
                    break;
                }
            }
        }
        if (targetWeight == null) {
            log.error("spec {} can't find targetUnit {}", spec, targetUnit);
            log.info("cur edges = {}", JSONObject.toJSONString(cur.getEdges().keySet()));
            return null;
        }
        BigDecimal targetCnt = new Fraction(cnt)
                .multiply(curWeight)
                .multiply(targetWeight)
                .val(ROUNDING_SCALE, ROUNDING_MODE);

        if (targetCnt.scale() > 0) {
            // 小数移除尾0
            targetCnt = targetCnt.stripTrailingZeros();
        }
        if (targetCnt.scale() < 0) {
            // 取消科学计数法，避免一些参数传递toString时出问题
            targetCnt = targetCnt.setScale(0);
        }
        log.info("target = {}{}", targetCnt, targetUnit);
        return targetCnt;
    }

    public static Node buildTree(String spec, String unit) {
        Map<String, Node> cacheNode = cache.computeIfAbsent(spec, s -> new HashMap<>());
        if (cacheNode.containsKey(unit)) {
            return cacheNode.get(unit);
        }
        StringBuilder seq = new StringBuilder(spec);
        parse(seq);
        Node tree = buildNode(seq, unit);
        cacheNode.put(unit, tree);
        return tree;
    }

    /**
     * dfs，先解析子节点，再通过递归把子节点连接到层层父代
     *
     * @param seq
     * @param pkg
     * @return
     */
    private static Node buildNode(String seq, String pkg) {
        return buildNode(new StringBuilder(seq), pkg);
    }

    private static Node buildNode(StringBuilder seq, String pkg) {
        String key = seq.toString();
        if (StringUtils.isBlank(key) || StringUtils.equals(pkg, key)) {
            return new Node(pkg);
        }
        if (StringUtils.isBlank(pkg)) {
            // 默认包装
            pkg = DEF_UNIT;
        }
        Node node = new Node(seq.toString(), pkg);
        Node bracketRight = null;
        if (StringSeqUtils.endWithBracket(seq)) {
            // 如果是括号闭符，不管什么含义按整单独处理，另外，有的开闭符不严格匹配，一半中文一半英文
            // 例：10ml(100mg)；100ml(A)；50ug(0.05%)60掀/支；10ml(含碘(I)480mg/ml)/支，(I)是注释；1000ml(0.75kcal/ml)/袋；10mg(枢衡)*20片/盒，全中文，注释
            bracketRight = buildNode(StringSeqUtils.bracket(seq), pkg);
            if (bracketRight == null) {
                // 兼容处理，如果括号里面是非法或无法解析的串，去掉多余的乘符
                // 例：6袋*（A剂+B剂）
                deleteTailWithMultiplyOpt(seq);
            } else if (seq.length() == 0) {
                // 如果括号外没有内容了，直接返回
                return bracketRight;
            }
        }
        if (StringUtils.endsWithAny(seq.toString(), "%")) {
            // 多余的百分比和符号，移除掉，目前无含义不用管
            // 例：250ml*20%/盒
            seq.deleteCharAt(seq.length() - 1);
            StringSeqUtils.pattern(seq, NUMBER_REG_END, true);
            deleteTailWithMultiplyOpt(seq);
            deleteTailWithParOpt(seq);
        }

        // 部分联装规格拆解后无单位尾缀，用下级的单位，如：100片/瓶*5，`100片/瓶`用`100片`的单位`瓶`
        String unit = StringUtils.defaultString(StringSeqUtils.pattern(seq, UNIT_REG_END, true), DEF_UNIT);
        Fraction weight;
        if (seq.length() != 0 && !StringSeqUtils.endWithAny(seq, multiplyOpt)) {
            // 下一组的权重等于当前组的单位系数
            // '/'结尾串和空串（纯单位）按默认权重1，否则从串里取
            // 取出权重数值
            String numberStr = StringSeqUtils.pattern(seq, NUMBER_REG_END, true);
            if (StringUtils.isNotBlank(numberStr)) {
                weight = new Fraction(new BigDecimal(numberStr));
            } else {
                log.warn("非法规格串：{}", node.getVal());
                // TODO CIdea: 移除末尾的中文串再试一次
                // 例：0.1%（5g:5mg）
                return bracketRight;
            }
        } else {
            weight = Fraction.ONE;
        }
        Node par = null;
        if (seq.length() > 0) {
            // 获取当前规格串与下一串之间的关系
            // 取出操作符
            if (deleteTailWithMultiplyOpt(seq)) {
                // 乘
                // 3g*15袋 -》 3g*
            } else if (deleteTailWithParOpt(seq)) {
                // 同义并联
                // 把右式提取出来作并联节点
                par = buildNode(node.getVal().substring(seq.length() + 1), pkg);
                weight = Fraction.ONE;
                unit = pkg;
            } else if (StringSeqUtils.endWithBracket(seq)) {
                // 无乘号间隔的左括号代表与右式规格同义，即包装相等
                // 如：(3ml)300u；(1ml)5mg；(2ml)5mg；(3ml)3.6IU
                par = buildNode(StringSeqUtils.bracket(seq), pkg);
            } else if (bracketRight != null) {
                // 有右括号了，没有操作符
            } else if (StringSeqUtils.pattern(seq, UNIT_REG_END, false) != null) {
                // 有些不搞分隔
                // 例：2mg14片
            } else {
                log.warn("非法规格串，无法识别的操作符：{}", seq);
            }
        }
        Node next = buildNode(seq, unit);
        if (next == null) {
            if (!weight.eqOne()) {
                next = new Node(unit);
            } else if (par != null) {
                // 左边非法
                // 例：a：10ml；b：10ml
                next = par;
                par = null;
            }
        }
        if (par != null) {
            Collection<String> intersection = CollectionUtils.intersection(next.getEdges().keySet(), par.getEdges().keySet());
            boolean error = false;
            for (String dup : intersection) {
                Edge sync1 = next.getEdges().get(dup);
                Edge sync2 = par.getEdges().get(dup);
                if (!sync1.getWeight().equals(sync2.getWeight())) {
                    log.warn("并行单位重复。l = {}, r = {}", next.getVal(), par.getVal());
                    error = true;
                    break;
                }
            }
            if (!error) {
                // 左右如果单位相等，需要权重不同，否则无视
                // 反例：10g:0.2g
                node.addSuffix(Fraction.ONE, par);
            }
        }
        node.addSuffix(weight, next);
        if (bracketRight != null) {
            // 右括号为左式补充
            Edge sync = bracketRight.getEdges().get(unit);
            Fraction pw = weight;
            if (sync != null) {
                // 找出左右单位相等的节点，为左式的下一个节点
                // 例：10ml(含碘(I)480mg/ml)/支；56喷/瓶(8.1ml/瓶)；5ml(5ml:50mg(1%))
                // 有一些右边疑似单次剂量
                // 例：10g (1g)；20g (0.2g)
                Fraction aw = accessWeight(sync.getAccess());
                pw = pw.multiply(aw);
                if (pw.eqOne()) {
                    // 左右节点相同单位的系数相等，判断为同一个单位
                    // 例：56喷/瓶(8.1ml/瓶)、5ml(5ml:50mg(1%))
                    bracketRight.setPkg(node.getPkg());
                } else {
                    // 左右不等，系数需要处理一下
                    if (weight.eqOne()) {
                        // WARN: 待测试
                        // 左右不等且主单位系数为1，副串才是主规格
                        // 例：130mg/g（16g*5袋）
                        node.setPkg(pw + bracketRight.getPkg());
                        pw = pw.reverse();
                    } else {
                        // 例：10ml(含碘(I)480mg/ml)/支
                        bracketRight.setPkg(pw.reverse() + node.getPkg());
                    }
                }
            } else {
                // 单位不等时，与左式同义
                // 例：20ml：200万IU(240喷)；5.5ug(75IU)
                bracketRight.setPkg(unit);
            }
            node.addSuffix(pw, bracketRight);
        }
        crossSupply(node);
        node.link(Fraction.ONE, node);
        return node;
    }

    private static void crossSupply(Node node) {
        if (node == null || node.getSuffixes().size() < 2) {
            return;
        }
        for (Edge suffix : node.getSuffixes()) {
            // 当前链路没有的单位
            Collection<String> absentPkgs = CollectionUtils.subtract(node.getEdges().keySet(), suffix.getAccess().getEdges().keySet());
            for (String absentPkg : absentPkgs) {
                // 中心节点到缺席单位的系数
                Edge le = node.getEdges().get(absentPkg);
                // 当前链路所有节点连接这个缺席的单位
                for (Edge re : new HashSet<>(suffix.getAccess().getEdges().values())) {
                    if (re.getAccess() == node) {
                        continue;
                    }
                    Fraction aw = le.getWeight().divide(re.getWeight()).divide(suffix.getWeight());
                    if (re.getAccess().getEdges().containsKey(absentPkg) && !aw.equals(re.getAccess().getEdges().get(absentPkg).getWeight())) {
                        log.warn("并行单位重复。pkg = {}", absentPkg);
                    }
                    re.getAccess().link(aw, le.getAccess());
                }
            }
        }
    }

    /**
     * 获取root到该节点的权重
     *
     * @param mid
     * @return
     */
    private static Fraction accessWeight(Node mid) {
        Fraction aw = Fraction.ONE;
        Edge pre = mid.getPre();
        while (pre != null) {
            aw = aw.multiply(pre.getWeight());
            pre = pre.getAccess().getPre();
        }
        return aw;
    }

    public static void main(String[] args) {
        BigDecimal cnt;
        Node node;
        String spec;
        // 完成
        // 通用单位
        // node = buildTree("散", "散");
        // 单例
        // node = buildTree("散", "散");
        // node = buildTree("1管", "管");
        // // 标准
        // node = buildTree("3g", "袋");
        // node = buildTree("10mg*24粒", "瓶");
        // node = buildTree("10mg*6片*3板/瓶", "瓶");
        // node = buildTree("80万IU x4粒", "盒");
        // node = buildTree("100g（2.5%）", "支");
        // node = buildTree("1贴(1mg/片，1.25cm2)", "盒");
        // node = buildTree("6袋*（A剂+B剂）", "盒");
        // node = buildTree("4000Axaiu*2支", "盒");
        // node = buildTree("0.5亿活菌*20粒/盒", "盒");
        // node = buildTree("2500ml／桶", "桶");
        // node = buildTree("15mg x36片", "桶");
        // node = buildTree("3g\\管", "管");
        // node = buildTree("60粒\\瓶", "瓶");
        // 并行
        // node = buildTree("200ml:134g", "支");
        // node = buildTree("5ml：5mg（0.1%）", "支");
        // node = buildTree("60ml：3.0g（5%）", "支");
        // // 左括号
        // node = buildTree("(3ml)300u*1支", "支");
        // node = buildTree("(4.5g)500ml*1袋", "袋");
        // // 右括号，'/单位'格式的
        // node = buildTree("56喷/瓶(8.1ml/瓶)", "瓶");
        // node = buildTree("10ml(含碘(I)480mg/ml)/支", "支");
        // 右括号，并行格式的，左右式包装相等
        // node = buildTree("5ml(5ml:50mg(1%))", "支");
        // // 右括号，并行格式的，左右式单位不等，对左式单位的补充
        // node = buildTree("2ml*10支(2ml:100mg)", "盒");
        // node = buildTree("2ml*20支（2ml:5mg)", "盒");
        // node = buildTree("3ml*5支(3ml:0.3g)", "盒");
        // // 右括号，左边非法，实际规格是括号内容的
        // node = buildTree("0.005%(30ml：1.50mg)", "支");
        // // 警告，规则不明，需要实际支撑
        // // 双并行
        // node = buildTree("3ml:300单位(10.4mg)", null);
        // // 系数不明确，有两组系数不同的单位
        // node = buildTree("1500IU:500IU*20粒", null);
        // node = buildTree("10g（10g:0.1g）", "支");
        // node = buildTree("7片（150mg:12.5mg）", "盒");
        // node = buildTree("50ug/250ug/泡*60泡", "支");
        // node = buildTree("15g(10g:0.2g)", "支");
        // node = buildTree("10片（5mg：10mg）", "盒");
        // node = buildTree("2.5g：2.4275g*40袋", "盒");
        // node = buildTree("(80μg:4.5μg)*60吸", "盒");
        // node = buildTree("50g(1g:0.025g)", "盒");
        // node = buildTree("5g:0.015g", "支");
        // 暂不支持
        // 联装，具体实例找不到了
        // node = buildTree("0.3g*100片/瓶*5", null);
        // 括号右式是'/2ml'，没有实例，应该不会有
        // node = buildTree("10ml(含碘(I)480mg/2ml)/支", "支");
        // 字符不等的单位冲突
        // node = buildTree("0.1%（5g:5mg）", "支");
        // 非法，不要抛异常出去就行
        // node = buildTree("5ml:100mg铁与1600mg蔗糖", null);
        // node = buildTree("6.0*10E6IU(100μg)/0.6ml", null);
        // node = buildTree("1.35×10^8IU(3.0mg):1.0ml", null);
        // node = buildTree("A：10ml；B：10ml/盒", null);
        // node = buildTree("0.173:10ml/支", null);
        // node = buildTree("10:89.4mg/支", null);
        // node = buildTree("1500:500 30粒/盒", null);
        // node = buildTree("0.1%：50ml/瓶", null);
        // node = buildTree("1.6:250ml", null);
        // node = buildTree("1%:15g/支", null);
        // node = buildTree("50:850mg*28片/盒", null);
        node = buildTree("1000g/(10g)t", null);
        node = buildTree("50g+60gl/支", null);
        // 测试
        System.out.println();
        // //
        // 值列表
        // 0.3g*100片/瓶
        // 3g*15袋
        // 200ml:134g
        // 20ml/支
        // 1500IU:500IU*20粒
        // 支持示例
        cnt = convert(new BigDecimal("1"), "kg", "g", "1kg", "盒");
        Assert.isTrue(new BigDecimal("1000").compareTo(cnt) == 0, String.valueOf(cnt));
        cnt = convert(new BigDecimal("1"), "kg", "g", "1000g", "盒");
        Assert.isTrue(new BigDecimal("1000").compareTo(cnt) == 0, String.valueOf(cnt));

        cnt = convert(new BigDecimal("1"), "g", "kg", "1kg", "盒");
        Assert.isTrue(new BigDecimal("0.001").compareTo(cnt) == 0, String.valueOf(cnt));
        cnt = convert(new BigDecimal("1"), "g", "kg", "1000g", "盒");
        Assert.isTrue(new BigDecimal("0.001").compareTo(cnt) == 0, String.valueOf(cnt));

        cnt = convert(new BigDecimal("1"), "克", "千克", "1000g", "盒");
        Assert.isTrue(new BigDecimal("0.001").compareTo(cnt) == 0, String.valueOf(cnt));
        cnt = convert(new BigDecimal("1"), "克", "kg", "1000g", "盒");
        Assert.isTrue(new BigDecimal("0.001").compareTo(cnt) == 0, String.valueOf(cnt));
        // 1盒:5g:5000mg
        cnt = convert(new BigDecimal("1"), "毫克", "盒", "2ml：0.5g*10支/盒", "盒");
        Assert.isTrue(new BigDecimal("0.0002").compareTo(cnt) == 0, String.valueOf(cnt));


        cnt = convert(new BigDecimal("100"), "片", "盒", "5mg*100片", "盒");
        // 0.02瓶
        cnt = convert(new BigDecimal("2"), "片", "瓶", "100片", "瓶");
        Assert.isTrue(new BigDecimal("0.02").compareTo(cnt) == 0, String.valueOf(cnt));
        // 0.02瓶
        cnt = convert(new BigDecimal("2"), "片", "瓶", "100片/瓶", "瓶");
        Assert.isTrue(new BigDecimal("0.02").compareTo(cnt) == 0, String.valueOf(cnt));
        // 0.003瓶
        cnt = convert(new BigDecimal("0.3"), "片", "瓶", "0.3g*100片", "瓶");
        Assert.isTrue(new BigDecimal("0.003").compareTo(cnt) == 0, String.valueOf(cnt));
        // 3.33333334片
        cnt = convert(new BigDecimal("1"), "g", "片", "0.3g", "片");
        Assert.isTrue(BigDecimal.ONE.divide(new BigDecimal("0.3"), ROUNDING_SCALE, ROUNDING_MODE).compareTo(cnt) == 0, String.valueOf(cnt));
        // // 0.03333334瓶
        cnt = convert(new BigDecimal("1"), "g", "瓶", "0.3g*100片/瓶", "瓶");
        Assert.isTrue(BigDecimal.ONE.divide(new BigDecimal("0.3"), ROUNDING_SCALE, ROUNDING_MODE).divide(new BigDecimal("100"), ROUNDING_SCALE, ROUNDING_MODE).compareTo(cnt) == 0, String.valueOf(cnt));
        // 0.0030瓶
        cnt = convert(new BigDecimal("0.3"), "片", "瓶", "0.3g*100片/瓶", "瓶");
        Assert.isTrue(new BigDecimal("0.0030").compareTo(cnt) == 0, String.valueOf(cnt));
        // // 0.01瓶
        // cnt = convert(new BigDecimal("1"), "片", "瓶", "0.3g*100片/瓶*5", "5瓶");
        // Assert.isTrue(new BigDecimal("0.01").compareTo(cnt) == 0, String.valueOf(cnt));
        // // 0.002 * 5瓶
        // cnt = convert(new BigDecimal("1"), "片", "5瓶", "0.3g*100片/瓶*5", "5瓶");
        // Assert.isTrue(new BigDecimal("0.002").compareTo(cnt) == 0, String.valueOf(cnt));
        // cnt = convert(new BigDecimal("0.3"), "g", "瓶", "0.3g*100片/瓶*5", "5瓶");
        // Assert.isTrue(new BigDecimal("0.01").compareTo(cnt) == 0, String.valueOf(cnt));
        // 0.04瓶
        cnt = convert(new BigDecimal("20"), "mg", "瓶", "5mg*100片", "瓶");
        Assert.isTrue(new BigDecimal("0.04").compareTo(cnt) == 0, String.valueOf(cnt));
        // 0.02瓶
        cnt = convert(new BigDecimal("20"), "mg", "瓶", "10mg*100片/瓶", "瓶");
        Assert.isTrue(new BigDecimal("0.02").compareTo(cnt) == 0, String.valueOf(cnt));
        // 0.5支
        cnt = convert(new BigDecimal("10"), "ml", "支", "20ml/支", "支");
        Assert.isTrue(new BigDecimal("0.5").compareTo(cnt) == 0, String.valueOf(cnt));
        // 3.33333334片，1.1111瓶
        cnt = convert(new BigDecimal("1"), "g", "片", "0.3g*3片", "瓶");
        Assert.isTrue(new BigDecimal("3.33333334").compareTo(cnt) == 0, String.valueOf(cnt));

        cnt = convert(new BigDecimal("100"), "ml", "瓶", "200ml:134g", "瓶");
        Assert.isTrue(new BigDecimal("0.5").compareTo(cnt) == 0, String.valueOf(cnt));
        cnt = convert(new BigDecimal("1"), "g", "kg", "1kg", "kg");
        Assert.isTrue(new BigDecimal("0.001").compareTo(cnt) == 0, String.valueOf(cnt));

        cnt = convert(new BigDecimal("228.5"), "mg", "袋", "228.5mgx28袋", "袋");
        Assert.isTrue(BigDecimal.ONE.divide(new BigDecimal("28"), ROUNDING_SCALE, ROUNDING_MODE).compareTo(cnt) == 0, String.valueOf(cnt));
        cnt = convert(new BigDecimal("67"), "g", "瓶", "200ml:134g", "瓶");
        Assert.isTrue(new BigDecimal("0.5").compareTo(cnt) == 0, String.valueOf(cnt));

        cnt = convert(new BigDecimal("56"), "喷", "瓶", "56喷/瓶(8.1ml/瓶)", "瓶");
        Assert.isTrue(new BigDecimal("1").compareTo(cnt) == 0, String.valueOf(cnt));
        cnt = convert(new BigDecimal("81"), "ml", "瓶", "56喷/瓶(8.1ml/瓶)", "瓶");
        Assert.isTrue(new BigDecimal("10").compareTo(cnt) == 0, String.valueOf(cnt));

        cnt = convert(new BigDecimal("14"), "片", "瓶", "7片（150mg:12.5mg）", "瓶");
        Assert.isTrue(new BigDecimal("2").compareTo(cnt) == 0, String.valueOf(cnt));

        // 右括号，并行格式的，左右式包装相等
        spec = "5ml(5ml:50mg(1%))";
        cnt = convert(new BigDecimal("7.5"), "ml", "支", spec, "支");
        Assert.isTrue(new BigDecimal("1.5").compareTo(cnt) == 0, String.valueOf(cnt));
        spec = "7片（150mg:12.5mg）";
        cnt = convert(new BigDecimal("14"), "片", "瓶", spec, "瓶");
        Assert.isTrue(new BigDecimal("2").compareTo(cnt) == 0, String.valueOf(cnt));
        // // 右括号，并行格式的，左右式单位不等，对左式单位的补充
        spec = "10ml(含碘(I)480mg/ml)/支";
        cnt = convert(new BigDecimal("15"), "ml", "瓶", spec, "瓶");
        Assert.isTrue(new BigDecimal("1.5").compareTo(cnt) == 0, String.valueOf(cnt));
        spec = "2ml*10支(2ml:100mg)";
        cnt = convert(new BigDecimal("20"), "ml", "瓶", spec, "瓶");
        Assert.isTrue(new BigDecimal("1").compareTo(cnt) == 0, String.valueOf(cnt));


        spec = "130mg/g（16g*5袋）";
        cnt = convert(new BigDecimal("80"), "g", "盒", spec, "盒");
        Assert.isTrue(new BigDecimal("1").compareTo(cnt) == 0, String.valueOf(cnt));
        cnt = convert(new BigDecimal("5"), "袋", "盒", spec, "盒");
        Assert.isTrue(new BigDecimal("1").compareTo(cnt) == 0, String.valueOf(cnt));


        // 规格不合法
        // node = buildNode("85g（气雾剂）+60g（保险液）", null);
        // // 自己扩展的，实际是否有未知，是否为设想的规则未知。并行时/的含义不同，外面的'/支'代表总共几只，并行括号里的'480mg/2ml'代表每2ml有480mg，也就是240mg/ml
        // node = buildNode("10ml(含碘(I)480mg/2ml)/支", "支");


        // 实际的一些案例测试
        cnt = convert(new BigDecimal("100"), "粒", "盒", "0.1g*100片", "瓶");
        Assert.isTrue(new BigDecimal("1").compareTo(cnt) == 0, String.valueOf(cnt));
        return;
    }

    private static boolean deleteTailWithMultiplyOpt(StringBuilder seq) {
        boolean deleted = StringSeqUtils.deleteTails(seq, false, multiplyOpt);
        if (deleted) {
            // 兼容处理，有的乘符前面会多空格
            StringSeqUtils.deleteTails(seq, true, ' ');
        }
        return deleted;
    }

    private static boolean deleteTailWithParOpt(StringBuilder seq) {
        boolean deleted = StringSeqUtils.deleteTails(seq, false, ':', '：', ',', '，');
        return deleted;
    }

    private static void parse(StringBuilder seq) {
        StringSeqUtils.removeAll(seq, "\n");
        StringSeqUtils.deleteTails(seq, true, ' ');
        // StringSeqUtils.lowerCase(seq);
        // TODO CIdea: 从科学计数法转成常规计数
        StringSeqUtils.replaceAll(seq, "10^8", "100000000");
        // TODO CIdea: 从中文转成常规计数
        // TODO CIdea: 常规单位换算，不会输入完整规格式
        if ("1kg".equals(seq.toString())) {
            StringSeqUtils.reset(seq, "1000g*1kg");
        } else if ("1t".equals(seq.toString())) {
            StringSeqUtils.reset(seq, "1000kg*1t");
        } else if ("1l".equals(seq.toString())) {
            StringSeqUtils.reset(seq, "1000ml*1l");
        }
    }


    /**
     * 有向图节点
     */
    @Accessors(chain = true)
    public static class Node implements Serializable {

        /**
         * 规格值
         */
        @Getter
        private final String val;
        /**
         * 规格包装单位
         */
        @Getter
        @Setter
        private String pkg;

        // @Getter
        // private BigDecimal multiply;
        /**
         * 拆装单位
         */
        // @Getter
        // private String unit;
        @Getter
        @Setter
        private Edge pre;
        @Getter
        private final Set<Edge> suffixes = new HashSet<>();

        /**
         * 边，K-{@link #pkg}
         */
        @Getter
        private final Map<String, Edge> edges = new HashMap<>();

        public Node(String val, String pkg) {
            Assert.hasText(pkg, "pkg is not be null");
            this.val = val;
            this.pkg = pkg;
        }

        public Node(String pkg) {
            this(1 + pkg, pkg);
            link(Fraction.ONE, this);
        }

        public Edge addSuffix(Fraction weight, Node node) {
            if (node == null) {
                return null;
            }
            node.setPre(new Edge(weight.reverse(), this));
            Edge edge = new Edge(weight, node);
            suffixes.add(edge);
            for (Edge suffix : new HashSet<>(node.getEdges().values())) {
                link(weight.multiply(suffix.getWeight()), suffix.getAccess());
            }
            return edge;
        }

        public void link(Fraction fraction, Node node) {
            link(new Edge(fraction, node));
        }

        public void link(Edge edge) {
            edges.put(edge.getAccess().getPkg(), new Edge(edge.getWeight(), edge.getAccess()));
            edge.getAccess().getEdges().put(pkg, new Edge(edge.getWeight().reverse(), this));
        }

        @Override
        public String toString() {
            return "Node{" +
                    "val='" + val + '\'' +
                    ", pkg='" + pkg + '\'' +
                    '}';
        }
    }

    /**
     * 有向图的边
     */
    @Data
    @Accessors(chain = true)
    public static class Edge implements Serializable {

        private Fraction weight;

        private Node access;

        public Edge(Fraction weight, Node access) {
            this.weight = weight;
            this.access = access;
        }

    }

    /**
     * 近义词组
     *
     * @author: CIdea
     */
    public static class Synonyms {

        private static Map<String, Set<String>> ref = new HashMap<>();

        static {
            // 默认的同义词组，不分先后
            List<Set<String>> data = new ArrayList<>();
            data.add(new HashSet<>(Arrays.asList(new String[]{"粒", "片", "丸", "枚", "#"})));
            data.add(new HashSet<>(Arrays.asList(new String[]{"袋", "包"})));
            data.add(new HashSet<>(Arrays.asList(new String[]{"mg", "毫克"})));
            data.add(new HashSet<>(Arrays.asList(new String[]{"g", "克"})));
            data.add(new HashSet<>(Arrays.asList(new String[]{"kg", "千克"})));
            data.add(new HashSet<>(Arrays.asList(new String[]{"t", "吨"})));
            data.add(new HashSet<>(Arrays.asList(new String[]{"l", "升"})));
            data.add(new HashSet<>(Arrays.asList(new String[]{"ml", "毫升"})));

            for (Set<String> terms : data) {
                for (String term : terms) {
                    ref.put(term, terms);
                }
            }
        }

        /**
         * 获取近义词组
         *
         * @param term
         * @return
         */
        public static List<String> get(String term) {
            Set<String> set = ref.get(term);
            if (set == null) {
                return Collections.singletonList(term);
            }
            set = new HashSet<>(set);
            // term放第一个
            return set.stream().sorted((o1, o2) -> o1.equals(term) ? -1 : 1).collect(Collectors.toList());
        }

        /**
         * 添加近义词
         *
         * @param term1
         * @param term2
         */
        public static void add(String term1, String term2) {
            if (StringUtils.isAnyBlank(term1, term2)) {
                return;
            }
            Set<String> s1 = ref.computeIfAbsent(term1, k -> new HashSet<>());
            Set<String> s2 = ref.computeIfAbsent(term2, k -> new HashSet<>());
            s1.add(term1);
            s1.add(term2);
            s1.addAll(s2);
            ref.put(term1, s1);
            ref.put(term2, s1);
        }

        /**
         * 清空近义词
         * 允许清除预设完全自定义
         */
        public static void clear() {
            ref.clear();
        }

    }

}
