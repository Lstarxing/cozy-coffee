package com.cozy.common.util;

/**
 * 手机号处理工具类
 */
public class PhoneUtils {

    /**
     * 手机号脱敏（前三后四，中间四个*）
     * 
     * @param phone 原始手机号
     * @return 脱敏后的手机号，如 138****1234
     * 
     *         规则：
     *         - 正常手机号（11位）：前3后4，中间4个*
     *         - 短于7位或为空：返回 "***"
     */
    public static String mask(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "***";
        }
        phone = phone.trim();
        if (phone.length() < 7) {
            return "***";
        }
        int len = phone.length();
        return phone.substring(0, 3) + "****" + phone.substring(len - 4);
    }

    /**
     * 验证脱敏后的手机号格式
     * 
     * @param maskedPhone 脱敏后的手机号
     * @return 是否符合格式 ^\d{3}\*{4}\d{4}$
     */
    public static boolean isValidMaskedFormat(String maskedPhone) {
        if (maskedPhone == null) {
            return false;
        }
        return maskedPhone.matches("^\\d{3}\\*{4}\\d{4}$");
    }
}
