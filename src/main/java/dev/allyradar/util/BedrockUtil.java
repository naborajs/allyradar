package dev.allyradar.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Utility to identify Bedrock Edition players connected via GeyserMC / Floodgate
 * to apply network-friendly optimizations and avoid sending unsupported Java packets.
 */
public final class BedrockUtil {
    private static volatile Boolean floodgateAvailable = null;
    private static Method isFloodgatePlayerMethod = null;
    private static Object floodgateInstance = null;

    private BedrockUtil() {
    }

    public static boolean isBedrockPlayer(final Player player) {
        if (player == null) {
            return false;
        }
        return isBedrockUuid(player.getUniqueId(), player.getName());
    }

    public static boolean isBedrockUuid(final UUID uuid, final String name) {
        // 1. Check Floodgate API via reflection if present
        if (floodgateAvailable == null) {
            synchronized (BedrockUtil.class) {
                if (floodgateAvailable == null) {
                    try {
                        final Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                        final Method getInstance = apiClass.getMethod("getInstance");
                        floodgateInstance = getInstance.invoke(null);
                        isFloodgatePlayerMethod = apiClass.getMethod("isFloodgatePlayer", UUID.class);
                        floodgateAvailable = true;
                    } catch (final Throwable ignored) {
                        floodgateAvailable = false;
                    }
                }
            }
        }

        if (Boolean.TRUE.equals(floodgateAvailable) && isFloodgatePlayerMethod != null && floodgateInstance != null && uuid != null) {
            try {
                final Object result = isFloodgatePlayerMethod.invoke(floodgateInstance, uuid);
                if (result instanceof Boolean b && b) {
                    return true;
                }
            } catch (final Throwable ignored) {
            }
        }

        // 2. Check Floodgate prefix convention: default is '.' (e.g. .Aloneboy18k) or '*' or '_'
        if (name != null && !name.isEmpty()) {
            final char first = name.charAt(0);
            return first == '.' || first == '*' || first == '_';
        }
        return false;
    }
}
