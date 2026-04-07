package com.github.monitor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.util.*;

public class SnapshotSerializer {
    
    private static final ObjectMapper mapper = new ObjectMapper()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
    public static String serialize(Object obj) {
        try {
            Object map = convert(obj, 0, new IdentityHashMap<>());
            return mapper.writeValueAsString(map);
        } catch (Throwable t) {
            return "{\"error\": \"Serialization failed: " + t.getMessage() + "\"}";
        }
    }

    private static Object convert(Object obj, int depth, IdentityHashMap<Object, Boolean> visited) {
        if (obj == null) return null;
        if (depth > AgentConfig.depthLimit) return "MAX_DEPTH_REACHED";
        
        // Circular reference check
        if (visited.containsKey(obj)) return "CIRCULAR_REF";
        visited.put(obj, true);
        
        try {
            Class<?> clz = obj.getClass();
            
            // 1. Primitives & Wrappers & Strings
            if (isPrimitiveOrWrapper(clz) || obj instanceof String) {
                String str = String.valueOf(SensitiveDataSanitizer.sanitizeStructured(obj, new LinkedHashSet<String>()));
                if (str.length() > AgentConfig.lengthLimit) {
                    return str.substring(0, AgentConfig.lengthLimit) + "...(truncated)";
                }
                return obj instanceof String ? str : obj; // Jackson handles primitives
            }
            
            // 2. Arrays
            if (clz.isArray()) {
                int len = Array.getLength(obj);
                int limit = Math.min(len, AgentConfig.collectionLimit);
                List<Object> list = new ArrayList<>(limit);
                for (int i = 0; i < limit; i++) {
                    list.add(convert(Array.get(obj, i), depth + 1, visited));
                }
                if (len > limit) list.add("...(more " + (len - limit) + " elements)");
                return list;
            }
            
            // 3. Collections
            if (obj instanceof Collection) {
                Collection<?> col = (Collection<?>) obj;
                int size = col.size();
                int limit = Math.min(size, AgentConfig.collectionLimit);
                List<Object> list = new ArrayList<>(limit);
                int i = 0;
                for (Object item : col) {
                    if (i++ >= limit) break;
                    list.add(convert(item, depth + 1, visited));
                }
                if (size > limit) list.add("...(more " + (size - limit) + " elements)");
                return list;
            }
            
            // 4. Maps
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                int size = map.size();
                int limit = Math.min(size, AgentConfig.collectionLimit);
                Map<String, Object> res = new HashMap<>(limit);
                int i = 0;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (i++ >= limit) break;
                    String key = String.valueOf(entry.getKey());
                    res.put(key, convert(entry.getValue(), depth + 1, visited));
                }
                if (size > limit) res.put("TRUNCATED", "...(more " + (size - limit) + " entries)");
                return res;
            }
            
            // 5. POJO / Object
            Map<String, Object> map = new LinkedHashMap<>();
            // Only non-static fields
            while (clz != null && clz != Object.class) {
                for (Field field : clz.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    
                    String name = field.getName();
                    if (isBlacklisted(name)) continue;
                    if (!isWhitelisted(name)) {
                         // if whitelist is not empty, and field not in whitelist, skip
                         if (!AgentConfig.fieldWhitelist.isEmpty()) continue;
                    }

                    try {
                        field.setAccessible(true);
                        Object val = field.get(obj);
                        
                        if (isSensitive(name)) {
                            map.put(name, SensitiveDataSanitizer.maskValue(val));
                        } else {
                            map.put(name, convert(SensitiveDataSanitizer.sanitizeStructured(val, new LinkedHashSet<String>()), depth + 1, visited));
                        }
                    } catch (Throwable t) {
                        map.put(name, "ACCESS_ERROR: " + t.getMessage());
                    }
                }
                clz = clz.getSuperclass();
            }
            return map;
            
        } finally {
            visited.remove(obj);
        }
    }

    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() || 
               Number.class.isAssignableFrom(type) || 
               Boolean.class.isAssignableFrom(type) || 
               Character.class.isAssignableFrom(type);
    }
    
    private static boolean isSensitive(String name) {
        for (String s : AgentConfig.sensitiveFields) {
            if (name.toLowerCase().contains(s.toLowerCase())) return true;
        }
        return false;
    }
    
    private static boolean isBlacklisted(String name) {
        return AgentConfig.fieldBlacklist.contains(name);
    }
    
    private static boolean isWhitelisted(String name) {
        return AgentConfig.fieldWhitelist.isEmpty() || AgentConfig.fieldWhitelist.contains(name);
    }
    
}
