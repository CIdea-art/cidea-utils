package cn.cidea.core.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理StringBuilder对象内字符
 * 暂时处理不了StringBuffer，父类AbstractStringBuilder不是public
 *
 * @author: CIdea
 */
public class StringSeqUtils {

    private static final char[] prefixes = new char[]{'(', '（', '[', '{', '【'};
    private static final char[] suffixes = new char[prefixes.length];

    static {
        for (int i = 0; i < prefixes.length; i++) {
            suffixes[i] = (char) (prefixes[i] + 1);
        }
    }

    public static boolean endWithBracket(StringBuilder str) {
        return endWithAny(str, suffixes);
    }

    public static String bracket(StringBuilder str) {
        return bracket(str, true, true);
    }

    /**
     * 提取最后一个括号里的内容
     *
     * @param str
     * @param delete 是否从StringBuilder中移除
     * @param single 是否单括号，单括号不严格对应开符和闭符，一个开符一个闭符就算，主要是兼容乱七八糟的情况（一个中文开符一个英文闭符）
     * @return
     */
    public static String bracket(StringBuilder str, boolean delete, boolean single) {
        Stack<Character> bracketStack = new Stack<>();
        Stack<Character> charStack = new Stack<>();

        int start = 0;
        int end = 0;
        char[] chars = str.toString().toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            charStack.push(c);
            if (ArrayUtils.contains(prefixes, c)) {
                // 检索到开符
                if (bracketStack.isEmpty()) {
                    // 如果是首个开符，清空前面的字符栈
                    charStack.clear();
                    start = i;
                }
                bracketStack.push(c);
            } else if (ArrayUtils.contains(suffixes, c)) {
                // 检索到闭符
                // 闭符对应的上一个开符不能为空
                Assert.isTrue(!bracketStack.isEmpty(), "bracket of string '" + str + "' is not legal");
                Character peek = bracketStack.peek();
                if (single || c == peek + 1) {
                    // 如果检索到的闭符与上一个开闭符匹配，则视为一组
                    bracketStack.pop();
                }
                if (bracketStack.isEmpty()) {
                    // 开闭符匹配完，截取完成，字符栈出栈最后的闭符
                    charStack.pop();
                    end = i;
                    break;
                }
            }
        }
        Assert.isTrue(bracketStack.isEmpty(), "bracket of string '" + str + "' is not legal");
        if (delete) {
            str.delete(start, end + 1);
        }
        StringBuilder sb = new StringBuilder();
        while (!charStack.isEmpty()) {
            sb.insert(0, charStack.pop());
        }
        return sb.toString();
    }

    public static void lowerCase(StringBuilder str) {
        String lowerCase = StringUtils.lowerCase(str.toString());
        reset(str, lowerCase);
    }

    public static void clear(StringBuilder str) {
        str.delete(0, str.length());
    }

    public static void reset(StringBuilder str, String next) {
        clear(str);
        str.append(next);
    }

    public static StringBuilder replaceAll(StringBuilder str, String old, String next) {
        int i;
        while ((i = str.lastIndexOf(old)) >= 0) {
            str.replace(i, i + old.length(), next);
        }
        return str;
    }

    public static StringBuilder removeAll(StringBuilder str, String old) {
        return replaceAll(str, old, "");
    }

    public static boolean deleteTails(StringBuilder str, boolean loop, char... chars) {
        return endWithAny(str, true, loop, chars);
    }

    public static boolean endWithAny(StringBuilder str, char... chars) {
        return endWithAny(str, false, false, chars);
    }

    /**
     * 是否以入参字符结尾
     *
     * @param str    字符串
     * @param delete 是否删除
     * @param loop   是否循环删除
     * @param chars  尾字符
     * @return
     */
    private static boolean endWithAny(StringBuilder str, boolean delete, boolean loop, char... chars) {
        if (str == null) {
            return false;
        }
        boolean end = false;
        while (str.length() > 0) {
            char c = str.charAt(str.length() - 1);
            boolean contains = ArrayUtils.contains(chars, c);
            if (!contains) {
                break;
            }
            end = true;
            if (!delete) {
                break;
            }
            str.deleteCharAt(str.length() - 1);
            if (!loop) {
                break;
            }
        }
        return end;
    }

    /**
     * 正则匹配
     *
     * @param str
     * @param regex
     * @param delete 是否删除
     * @return
     */
    public static String pattern(StringBuilder str, String regex, boolean delete) {
        Matcher matcher = Pattern.compile(regex).matcher(str.toString());
        if (!matcher.find()) {
            return null;
        }
        String get = matcher.group();
        if (delete) {
            reset(str, matcher.replaceAll(StringUtils.EMPTY));
        }
        return get;
    }

}
