package com.ctrip.framework.apollo.common.constants;

/**
 * Utility class for retrieving version information.
 */
public class VersionUtil {
    private VersionUtil() {
        // Prevent instantiation
    }

    public static String getVersion(Class<?> clazz) {
        String implementationVersion = clazz.getPackage().getImplementationVersion();
        return "java-" + (implementationVersion != null ? implementationVersion : "unknown");
    }
}
