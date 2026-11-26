package com.veincore.auth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 自定义权限组：owner（房主/服主/控制台）与 allowed（被授权玩家）。
 * owner 只能由服主/控制台增删，玩家不能自授权、不能 self-deny owner。
 * 平台层负责把 UUID/名称解析后传入字符串 id。
 */
public final class AccessManager {

    private final Set<String> owners = new LinkedHashSet<>();
    private final Set<String> allowed = new LinkedHashSet<>();

    public boolean isOwner(String id) {
        return id != null && owners.contains(id);
    }

    public boolean isAllowed(String id) {
        return id != null && (owners.contains(id) || allowed.contains(id));
    }

    /** 仅 owner/控制台可调用；owner 不可被 allow 重复操作。 */
    public boolean allow(String id) {
        return id != null && !owners.contains(id) && allowed.add(id);
    }

    /** 仅 owner/控制台可调用；owner 不可被移除。 */
    public boolean deny(String id) {
        return id != null && !owners.contains(id) && allowed.remove(id);
    }

    public boolean addOwner(String id) {
        return id != null && owners.add(id);
    }

    public boolean removeOwner(String id) {
        return id != null && owners.remove(id);
    }

    public Set<String> owners() {
        return Set.copyOf(owners);
    }

    public Set<String> allowed() {
        return Set.copyOf(allowed);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("owners", new ArrayList<>(owners));
        m.put("allowed", new ArrayList<>(allowed));
        return m;
    }

    public static AccessManager fromMap(Map<String, Object> map) {
        AccessManager a = new AccessManager();
        if (map == null) {
            return a;
        }
        List<?> os = map.get("owners") instanceof List<?> l ? l : List.of();
        List<?> as = map.get("allowed") instanceof List<?> l ? l : List.of();
        for (Object o : os) {
            if (o instanceof String s) {
                a.owners.add(s);
            }
        }
        for (Object o : as) {
            if (o instanceof String s) {
                a.allowed.add(s);
            }
        }
        return a;
    }
}
