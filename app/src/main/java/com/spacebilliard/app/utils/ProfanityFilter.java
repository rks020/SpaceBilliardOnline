package com.spacebilliard.app.utils;

import java.util.Arrays;
import java.util.List;

public class ProfanityFilter {

    private static final List<String> BANNED_WORDS = Arrays.asList(
            // English
            "sex", "porn", "xxx", "dick", "cock", "pussy", "fuck", "bitch", "shit", "perv", "anal", "ass",
            // Turkish
            "sik", "amk", "yarrak", "aq", "oc", "kaşar", "orospo", "pic", "yarak", "serefsiz", "meme", "göt", "porno",
            // Reserved
            "admin", "system", "root", "support");

    public static boolean containsProfanity(String input) {
        if (input == null)
            return false;
        String lowerInput = input.toLowerCase();

        for (String word : BANNED_WORDS) {
            // Check for exact matches or containing words
            // Simple containment can be too aggressive (e.g. "asset" contains "ass")
            // So we check stricter rules or just basic contains for critical words.
            // For this game, simple contains might false positive on "analyst",
            // but short usernames (7 chars) make "asset" or "analyst" rare.
            // However, "ass" is short. let's check exact or bounded.

            // For simplicity and safety requested:
            if (lowerInput.contains(word)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty())
            return false;
        if (username.length() > 7)
            return false; // Max 7 chars
        return !containsProfanity(username);
    }
}
