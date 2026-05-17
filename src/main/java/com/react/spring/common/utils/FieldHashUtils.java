package com.react.spring.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.react.spring.meta.metasync.dto.FieldItem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

public final class FieldHashUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FieldHashUtils() {}

    /**
     * Hash chỉ tính từ các field cấu trúc (bỏ description, comment).
     * Thay đổi comment/description sẽ KHÔNG tăng version.
     */
    public static String structural(List<FieldItem> fields) {
        String canonical = fields.stream()
                .sorted(Comparator.comparing(FieldItem::path))
                .map(f -> f.path()
                        + "|" + f.code()
                        + "|" + f.name()
                        + "|" + f.dataType()
                        + "|" + (f.pathParent() != null ? f.pathParent() : "")
                        + "|" + f.isNull()
                        + "|" + f.isPrimaryKey())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
        return sha256(canonical);
    }

    public static String rawJson(String json) {
        if (json == null || json.isBlank()) {
            return sha256("");
        }
        try {
            return sha256(MAPPER.readTree(json).toString());
        } catch (Exception ignored) {
            return sha256(json);
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
