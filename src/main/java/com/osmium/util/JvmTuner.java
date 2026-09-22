package com.osmium.util;

import com.osmium.OsmiumConstants;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public final class JvmTuner {
    private static final String[] RECOMMENDED_FLAGS = {
        "-XX:+UseG1GC",
        "-XX:G1NewSizePercent=40",
        "-XX:G1MaxNewSizePercent=60",
        "-XX:MaxGCPauseMillis=30",
        "-XX:+AlwaysPreTouch",
        "-XX:+DisableExplicitGC",
        "-XX:+UseStringDeduplication"
    };

    private JvmTuner() {}

    public static void logRecommendations() {
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        List<String> missing = new ArrayList<>();

        for (String flag : RECOMMENDED_FLAGS) {
            boolean present = false;
            String flagKey = flag.contains("=") ? flag.substring(0, flag.indexOf('=')) : flag;
            for (String arg : jvmArgs) {
                if (arg.startsWith(flagKey)) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                missing.add(flag);
            }
        }

        if (!missing.isEmpty()) {
            OsmiumConstants.LOGGER.info("[Osmium] Recommended JVM arguments for low-RAM tuning: {}", String.join(" ", missing));
        } else {
            OsmiumConstants.LOGGER.info("[Osmium] Optimal JVM arguments detected.");
        }
    }
}
