package com.boxai.common.market;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 市场灰度与租户可见性：GLOBAL + rollout_percent，或 TENANT + tenant_ids_json。
 */
public final class MarketRolloutSupport {

    private static final Pattern TENANT_ID_TOKEN = Pattern.compile("\\d+");

    private MarketRolloutSupport() {
    }

    public static boolean isVisibleToTenant(Long tenantId,
                                            Long itemId,
                                            String visibility,
                                            String tenantIdsJson,
                                            Integer rolloutPercent) {
        if (tenantId == null || itemId == null) {
            return false;
        }
        String mode = visibility == null || visibility.isBlank()
                ? "GLOBAL"
                : visibility.trim().toUpperCase(Locale.ROOT);
        if ("TENANT".equals(mode)) {
            return tenantAllowed(tenantId, tenantIdsJson);
        }
        return inRolloutBucket(tenantId, itemId, rolloutPercent);
    }

    public static boolean inRolloutBucket(Long tenantId, Long itemId, Integer rolloutPercent) {
        int percent = normalizePercent(rolloutPercent);
        if (percent >= 100) {
            return true;
        }
        if (percent <= 0) {
            return false;
        }
        int bucket = stableBucket(tenantId, itemId);
        return bucket < percent;
    }

    public static int stableBucket(Long tenantId, Long itemId) {
        long hash = tenantId * 31L + itemId;
        return (int) (Math.abs(hash) % 100);
    }

    public static String serializeTenantIds(Set<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        boolean first = true;
        for (Long tenantId : tenantIds) {
            if (tenantId == null) {
                continue;
            }
            if (!first) {
                builder.append(',');
            }
            builder.append(tenantId);
            first = false;
        }
        builder.append(']');
        return builder.toString();
    }

    public static Set<Long> parseTenantIds(String tenantIdsJson) {
        if (tenantIdsJson == null || tenantIdsJson.isBlank()) {
            return Set.of();
        }
        Set<Long> ids = new HashSet<>();
        var matcher = TENANT_ID_TOKEN.matcher(tenantIdsJson);
        while (matcher.find()) {
            ids.add(Long.parseLong(matcher.group()));
        }
        return ids;
    }

    public static Set<Long> parseTenantIdList(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        Set<Long> ids = new HashSet<>();
        for (String token : raw.split("[,;\\s]+")) {
            if (token.isBlank()) {
                continue;
            }
            ids.add(Long.parseLong(token.trim()));
        }
        return ids;
    }

    private static boolean tenantAllowed(Long tenantId, String tenantIdsJson) {
        Set<Long> allowed = parseTenantIds(tenantIdsJson);
        return allowed.contains(tenantId);
    }

    private static int normalizePercent(Integer rolloutPercent) {
        if (rolloutPercent == null) {
            return 100;
        }
        return Math.max(0, Math.min(100, rolloutPercent));
    }
}
