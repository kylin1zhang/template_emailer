package com.citi.custody.util;

public class SystemUserUtil {
    public static String getCurrentUsername() {
        return System.getProperty("user.name"); // 获取系统用户名，即soeid
    }   
}
