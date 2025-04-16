package cn.cidea.core.utils;

import com.google.common.base.CaseFormat;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串格式转换
 * @author CIdea
 */
public class StringFormatUtils {

    /**
     * 驼峰转下划线（guava）
     * @param str
     * @return
     */
    public static String humpToUnderline(String str) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str);
    }

    /**
     * 下划线转驼峰（guava）
     * @param str
     * @return
     */
    public static String underlineToHump(String str) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, str);
    }

    public static String encodeZh(String str) throws UnsupportedEncodingException {
        Matcher matcher = Pattern.compile(RegexStr.ZH_CHAR).matcher(str);
        while (matcher.find()) {
            String tmp = matcher.group();
            str = str.replaceAll(tmp, URLEncoder.encode(tmp, "UTF-8"));
        }
        return str.replace(" ", "%20");
    }

}
