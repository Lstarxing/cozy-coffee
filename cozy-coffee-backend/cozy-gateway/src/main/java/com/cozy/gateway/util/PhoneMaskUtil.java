package com.cozy.gateway.util;

/**
 * 手机号脱敏工具
 */
public final class PhoneMaskUtil {

    private PhoneMaskUtil() {
    }

    public static String mask(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "***";
        }
        phone = phone.trim();
        if (phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
