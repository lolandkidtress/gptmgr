package com.openapi.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author wendong
 * @email wendong@juxinli.com
 * @date 2017/5/23.
 */
public final class ThreadLocalUtil {
    private static final ThreadLocal threadLocal = new ThreadLocal<HashMap>() {
        @Override
        protected HashMap initialValue() {
            return new HashMap(4);
        }
    };

    public static Object getThreadLocal() {
        return threadLocal.get();
    }

    public static void setThreadLocal(Object local) {
        threadLocal.set(local);
    }

    public static <T> T get(String key) {
        Map map = (Map) threadLocal.get();
        return (T) map.get(key);
    }

    public static <T> T getIfAbsent(String key) {
        Map map = (Map) threadLocal.get();
        if(map!=null){
            return (T) map.getOrDefault(key,"");
        }else {
            return (T) "";
        }
    }

    public static <T> T getOrDefault(String key,String defaluValue) {
        Map map = (Map) threadLocal.get();
        if(map!=null){
            return (T) map.getOrDefault(key,defaluValue);
        }else {
            return (T) "";
        }
    }

    public static boolean isExist(String key) {
        Map map = (Map) threadLocal.get();
        if(map!=null){
            return map.containsKey(key);
        }else {
            return false;
        }
    }

    public static void set(String key, Object value) {
        Map<String,Object> map = (Map) threadLocal.get();
        map.put(key, value);
    }

    public static void set(Map<String, Object> keyValueMap) {
        Map map = (Map) threadLocal.get();
        map.putAll(keyValueMap);
    }

    public static void remove() {
        threadLocal.remove();
    }

    public static <T> Map<String, T> fetchVarsByPrefix(String prefix) {
        Map<String, T> vars = new HashMap<>();
        if (prefix == null) {
            return vars;
        }
        Map map = (Map) threadLocal.get();
        Set<Map.Entry> set = map.entrySet();

        for (Map.Entry entry : set) {
            Object key = entry.getKey();
            if (key instanceof String) {
                if (((String) key).startsWith(prefix)) {
                    vars.put((String) key, (T) entry.getValue());
                }
            }
        }
        return vars;
    }

    public static <T> T remove(String key) {
        Map map = (Map) threadLocal.get();
        return (T) map.remove(key);
    }

    public static void clear(String prefix) {
        if (prefix == null) {
            return;
        }
        Map map = (Map) threadLocal.get();
        Set<Map.Entry> set = map.entrySet();
        List<String> removeKeys = new ArrayList<>();

        for (Map.Entry entry : set) {
            Object key = entry.getKey();
            if (key instanceof String) {
                if (((String) key).startsWith(prefix)) {
                    removeKeys.add((String) key);
                }
            }
        }
        for (String key : removeKeys) {
            map.remove(key);
        }
    }
}
