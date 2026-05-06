package com.lockerroom.dispatchservice.common;

public final class PiiMasker {

    private PiiMasker() {
    }

    public static String phone(String phone) {
        if (phone == null) return null;
        int len = phone.length();
        if (len < 8) return phone;
        return phone.substring(0, len - 8) + "****" + phone.substring(len - 4);
    }

    public static String email(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }
}
