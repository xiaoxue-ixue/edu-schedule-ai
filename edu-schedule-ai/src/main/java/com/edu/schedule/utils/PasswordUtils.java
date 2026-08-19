package com.edu.schedule.utils;

import cn.hutool.crypto.digest.BCrypt;

/**
 * 密码工具（BCrypt）
 */
public class PasswordUtils {

    public static String encode(String raw) {
        if (raw == null) {
            raw = "";
        }
        return BCrypt.hashpw(raw, BCrypt.gensalt());
    }

    public static boolean matches(String raw, String encoded) {
        if (raw == null || encoded == null) {
            return false;
        }
        return BCrypt.checkpw(raw, encoded);
    }
}
