package com.example.qrredirect.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Random base62 codes. Not sequential, so printed codes can't be guessed or enumerated.
 * 62^7 is roughly 3.5 trillion combinations.
 */
@Component
public class ShortCodeGenerator {

    public static final int LENGTH = 7;
    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private final SecureRandom random = new SecureRandom();

    public String next() {
        char[] out = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            out[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(out);
    }
}
