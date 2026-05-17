package com.react.spring.common.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

public final class SlugUtils {

    private SlugUtils() {}

    public static String toSlug(String name) {
        if (name == null) return "";
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    public static String nextUniqueCode(String baseSlug, Predicate<String> exists) {
        if (!exists.test(baseSlug)) return baseSlug;
        int n = 1;
        while (exists.test(baseSlug + "-" + n)) n++;
        return baseSlug + "-" + n;
    }
}
