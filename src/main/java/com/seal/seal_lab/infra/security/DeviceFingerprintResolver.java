package com.seal.seal_lab.infra.security;

import java.util.Locale;

public final class DeviceFingerprintResolver {

    private DeviceFingerprintResolver() {
    }

    public static String toDeviceFingerprint(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "fp:unknown|unknown|unknown";
        }

        if (userAgent.startsWith("fp:")) {
            return userAgent;
        }

        String normalized = userAgent.toLowerCase(Locale.ROOT);
        String browser = resolveBrowserFamily(normalized);
        String operatingSystem = resolveOperatingSystem(normalized);
        String deviceType = resolveDeviceType(normalized, operatingSystem);

        return "fp:" + browser + "|" + operatingSystem + "|" + deviceType;
    }

    private static String resolveBrowserFamily(String normalizedUserAgent) {
        if (normalizedUserAgent.contains("edg/")) {
            return "edge";
        }
        if (normalizedUserAgent.contains("chrome/") || normalizedUserAgent.contains("chromium/")) {
            return "chrome";
        }
        if (normalizedUserAgent.contains("safari/") && normalizedUserAgent.contains("version/")) {
            return "safari";
        }
        if (normalizedUserAgent.contains("firefox/")) {
            return "firefox";
        }
        if (normalizedUserAgent.contains("curl/")) {
            return "curl";
        }
        if (normalizedUserAgent.contains("postmanruntime/")) {
            return "postman";
        }
        if (normalizedUserAgent.contains("okhttp/")) {
            return "okhttp";
        }
        if (normalizedUserAgent.contains("java/")) {
            return "java";
        }

        String[] productTokens = normalizedUserAgent.split("[\\s/;()]+");
        for (String token : productTokens) {
            if (!token.isBlank()) {
                return token;
            }
        }

        return "unknown";
    }

    private static String resolveOperatingSystem(String normalizedUserAgent) {
        if (normalizedUserAgent.contains("iphone") || normalizedUserAgent.contains("ipad") || normalizedUserAgent.contains("ios")) {
            return "ios";
        }
        if (normalizedUserAgent.contains("android")) {
            return "android";
        }
        if (normalizedUserAgent.contains("windows")) {
            return "windows";
        }
        if (normalizedUserAgent.contains("mac os x") || normalizedUserAgent.contains("macintosh")) {
            return "macos";
        }
        if (normalizedUserAgent.contains("linux")) {
            return "linux";
        }
        if (normalizedUserAgent.contains("x11") || normalizedUserAgent.contains("unix")) {
            return "unix";
        }

        return "unknown";
    }

    private static String resolveDeviceType(String normalizedUserAgent, String operatingSystem) {
        if (normalizedUserAgent.contains("mobile") || normalizedUserAgent.contains("iphone")
                || normalizedUserAgent.contains("android")) {
            return "mobile";
        }
        if ("windows".equals(operatingSystem) || "macos".equals(operatingSystem)
                || "linux".equals(operatingSystem) || "unix".equals(operatingSystem)) {
            return "desktop";
        }

        return "unknown";
    }
}
