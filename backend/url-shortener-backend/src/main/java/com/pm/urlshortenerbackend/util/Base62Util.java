package com.pm.urlshortenerbackend.util;

/**
 * Author: sathwikpillalamarri
 * Date: 9/14/25
 * Project: url-shortener-backend
 */
public final class Base62Util {
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final char[] CHAR_MAP = CHARACTERS.toCharArray();
    private static final int BASE = 62;

    private Base62Util() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String encode(long value) {
        if(value == 0) {
            return String.valueOf(CHAR_MAP[0]);
        }
        StringBuilder sb = new StringBuilder();
        while(value > 0) {
            int remainder = (int) (value % BASE);
            sb.append(CHAR_MAP[remainder]);
            value = value / BASE;
        }
        return sb.reverse().toString();
    }

    public static long decode(String base62) {
        long value = 0;
        for(int i = 0; i < base62.length(); i++) {
            value = value * BASE + CHARACTERS.indexOf(base62.charAt(i));
        }
        return value;
    }
}
