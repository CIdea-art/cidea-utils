package cn.cidea.core.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

/**
 * @author: CIdea
 */
public class JsonOpt {

    public static List<JSONObject> getList(JSONObject json, String key) {
        return getList(json, JSONObject.class, key);
    }

    public static <T> List<T> getList(JSONObject json, Class<T> clz, String... keys) {
        JSONObject obj = getObject(json, ArrayUtils.subarray(keys, 0, keys.length - 1));
        if (obj == null) {
            obj = new JSONObject();
        }
        Object data = obj.get(keys[keys.length - 1]);
        JSONArray array;
        if (data == null) {
            array = new JSONArray();
        } else if (data instanceof JSONArray) {
            array = (JSONArray) data;
        } else {
            array = new JSONArray();
            array.add(data);
        }
        return array.toJavaList(clz);
    }

    public static JSONObject getObject(JSONObject json, String... keys) {
        if (json == null) {
            return null;
        }
        JSONObject pointer = json;
        if (ArrayUtils.isNotEmpty(keys)) {
            for (String key : keys) {
                pointer = pointer.getJSONObject(key);
                if (pointer == null) {
                    return null;
                }
            }
        }
        return pointer;
    }

}
