package cn.cidea.core.utils;

/**
 * 正则式
 * @author: CIdea
 */
public class RegexStr {

    /**
     * 标准日期
     * yyyy-MM-dd
     */
    public static final String NORM_DATE = "^(?:(?:(?:1[6-9]|[2-9]\\d)\\d{2})-(?:(?:(?:0[13578]|1[02])-31)|(?:(?:0[13-9]|1[0-2])-(?:29|30)))|(?:(?:(?:(?:1[6-9]|[2-9]\\d)(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00)))-02-29)|(?:1[6-9]|[2-9]\\d)\\d{2}-(?:(?:0[1-9])|(?:1[0-2]))-(?:0[1-9]|1\\d|2[0-8]))$";
    /**
     * 标准日期时间
     * yyyy-MM-dd HH:mm:ss
     */
    public static final String NORM_DATE_TIME = NORM_DATE.replace("$", "") + " (?:[0-1]\\d|2[0-3]):(?:[0-5]\\d):(?:[0-5]\\d)$";

    /**
     * 纯日期
     * yyyyMMdd
     */
    public static final String PURE_DATE = "^(?:(?:(?:1[6-9]|[2-9]\\d)\\d{2})(?:(?:(?:0[13578]|1[02])31)|(?:(?:0[13-9]|1[0-2])(?:29|30)))|(?:(?:(?:(?:1[6-9]|[2-9]\\d)(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00)))0229)|(?:1[6-9]|[2-9]\\d)\\d{2}(?:(?:0[1-9])|(?:1[0-2]))(?:0[1-9]|1\\d|2[0-8]))$";
    /**
     * 纯日期时间
     * yyyyMMddHHmmss
     */
    public static final String PURE_DATE_TIME = PURE_DATE.replace("$", "") + "(?:[0-1]\\d|2[0-3])(?:[0-5]\\d)(?:[0-5]\\d)$";

    /**
     * 数字，含小数，不含科学计数
     */
    public static final String NUMBER = "(0|[1-9][0-9]*)(\\.[0-9]*)?";
    // public static final String NUMBER = "(0|[1-9][0-9]*)(\\.[0-9]*)?((e|E))";

    /**
     * 中文字符
     */
    public static final String ZH_CHAR = "[\\u4E00-\\u9FA5]";
}
