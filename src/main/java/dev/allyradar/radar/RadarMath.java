package dev.allyradar.radar;

public final class RadarMath {
    private RadarMath() {
    }

    public static double relativeBearing(final double viewerX, final double viewerZ, final float viewerYaw,
                                         final double targetX, final double targetZ) {
        final double absolute = Math.toDegrees(Math.atan2(-(targetX - viewerX), targetZ - viewerZ));
        return ((((absolute - viewerYaw) % 360.0) + 540.0) % 360.0) - 180.0;
    }

    public static String direction(final double bearing) {
        if (bearing >= -22.5 && bearing < 22.5) return "↑";
        if (bearing >= 22.5 && bearing < 67.5) return "↗";
        if (bearing >= 67.5 && bearing < 112.5) return "→";
        if (bearing >= 112.5 && bearing < 157.5) return "↘";
        if (bearing >= 157.5 || bearing < -157.5) return "↓";
        if (bearing >= -157.5 && bearing < -112.5) return "↙";
        if (bearing >= -112.5 && bearing < -67.5) return "←";
        return "↖";
    }
}
