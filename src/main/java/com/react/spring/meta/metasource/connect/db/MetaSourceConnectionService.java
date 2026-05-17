package com.react.spring.meta.metasource.connect.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.react.spring.meta.metasource.connect.db.dto.QueryResultDto;
import com.react.spring.meta.metasource.connect.db.dto.SchemaDto;
import com.react.spring.meta.metasource.entity.MetaSource;
import com.react.spring.common.enums.ConnectorType;
import com.react.spring.common.exception.NotFoundException;
import com.vn.security.core.security.data.SecureDataManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Service
@Transactional(readOnly = true)
public class MetaSourceConnectionService {

    private static final int CONNECT_TIMEOUT_SEC = 5;
    private static final int QUERY_TIMEOUT_SEC = 30;
    private static final int MAX_ROWS = 1000;

    private final SecureDataManager secureDataManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public MetaSourceConnectionService(SecureDataManager secureDataManager) {
        this.secureDataManager = secureDataManager;
    }

    public SchemaDto fetchSchema(UUID metaSourceId) {
        PgConfig cfg = loadPostgresConfig(metaSourceId);
        try (Connection conn = openConnection(cfg)) {
            return readPostgresSchema(conn, cfg.schemaName());
        } catch (SQLException e) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
                    "Cannot fetch schema: " + e.getMessage());
        }
    }

    public QueryResultDto executeQuery(UUID metaSourceId, String sql) {
        PgConfig cfg = loadPostgresConfig(metaSourceId);
        long t0 = System.currentTimeMillis();
        try (Connection conn = openConnection(cfg);
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(QUERY_TIMEOUT_SEC);
            stmt.setMaxRows(MAX_ROWS);
            boolean isResultSet = stmt.execute(sql);
            if (!isResultSet) {
                int updated = stmt.getUpdateCount();
                return new QueryResultDto(
                        List.of("affected_rows"),
                        List.of(Map.of("affected_rows", updated)),
                        1,
                        System.currentTimeMillis() - t0
                );
            }
            try (ResultSet rs = stmt.getResultSet()) {
                return readResultSet(rs, t0);
            }
        } catch (SQLException e) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
                    "Query failed: " + e.getMessage());
        }
    }

    private PgConfig loadPostgresConfig(UUID id) {
        MetaSource ms = secureDataManager.loadOne(MetaSource.class, id)
                .orElseThrow(() -> new NotFoundException("MetaSource not found: " + id));
        if (ms.getConnectorType() != ConnectorType.POSTGRES) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Connect endpoint chỉ hỗ trợ POSTGRES, got: " + ms.getConnectorType());
        }
        Map<String, Object> cfg = parseConfig(ms.getConnectorConfig());
        return new PgConfig(
                strOrThrow(cfg, "host"),
                intOrThrow(cfg, "port"),
                strOrThrow(cfg, "database"),
                str(cfg, "username"),
                str(cfg, "password"),
                strOr(cfg, "schema", "public")
        );
    }

    private Connection openConnection(PgConfig cfg) throws SQLException {
        DriverManager.setLoginTimeout(CONNECT_TIMEOUT_SEC);
        String url = "jdbc:postgresql://" + cfg.host() + ":" + cfg.port() + "/" + cfg.database();
        Connection conn = DriverManager.getConnection(url, cfg.username(), cfg.password());
        String schema = cfg.schemaName().replaceAll("[^\\w]", "");
        try (Statement st = conn.createStatement()) {
            st.execute("SET search_path TO \"" + schema + "\", public");
        }
        return conn;
    }

    private SchemaDto readPostgresSchema(Connection conn, String schema) throws SQLException {
        Map<String, List<SchemaDto.FieldDto>> tableFields = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = ? AND table_type = 'BASE TABLE' " +
                        "ORDER BY table_name")) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableFields.put(rs.getString("table_name"), new ArrayList<>());
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT table_name, column_name, data_type, is_nullable " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = ? " +
                        "ORDER BY table_name, ordinal_position")) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    List<SchemaDto.FieldDto> fields = tableFields.get(tableName);
                    if (fields != null) {
                        fields.add(new SchemaDto.FieldDto(
                                rs.getString("column_name"),
                                rs.getString("data_type"),
                                "YES".equalsIgnoreCase(rs.getString("is_nullable")),
                                null,
                                null
                        ));
                    }
                }
            }
        }

        Map<String, java.util.Set<String>> pks = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT tc.table_name, kcu.column_name " +
                        "FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu " +
                        "  ON tc.constraint_name = kcu.constraint_name " +
                        "  AND tc.table_schema = kcu.table_schema " +
                        "WHERE tc.constraint_type = 'PRIMARY KEY' AND tc.table_schema = ?")) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pks.computeIfAbsent(rs.getString("table_name"), k -> new java.util.HashSet<>())
                            .add(rs.getString("column_name"));
                }
            }
        }

        Map<String, Map<String, SchemaDto.Fk>> fks = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT tc.table_name, kcu.column_name, " +
                        "       ccu.table_name AS foreign_table, ccu.column_name AS foreign_column " +
                        "FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu " +
                        "  ON tc.constraint_name = kcu.constraint_name " +
                        "  AND tc.table_schema = kcu.table_schema " +
                        "JOIN information_schema.constraint_column_usage ccu " +
                        "  ON ccu.constraint_name = tc.constraint_name " +
                        "  AND ccu.table_schema = tc.table_schema " +
                        "WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = ?")) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fks.computeIfAbsent(rs.getString("table_name"), k -> new HashMap<>())
                            .put(rs.getString("column_name"),
                                    new SchemaDto.Fk(
                                            rs.getString("foreign_table"),
                                            rs.getString("foreign_column")
                                    ));
                }
            }
        }

        List<SchemaDto.TableDto> tables = new ArrayList<>(tableFields.size());
        for (Map.Entry<String, List<SchemaDto.FieldDto>> e : tableFields.entrySet()) {
            String t = e.getKey();
            java.util.Set<String> tPks = pks.getOrDefault(t, java.util.Set.of());
            Map<String, SchemaDto.Fk> tFks = fks.getOrDefault(t, Map.of());
            List<SchemaDto.FieldDto> enriched = new ArrayList<>(e.getValue().size());
            for (SchemaDto.FieldDto f : e.getValue()) {
                enriched.add(new SchemaDto.FieldDto(
                        f.name(), f.type(), f.nullable(),
                        tPks.contains(f.name()) ? Boolean.TRUE : null,
                        tFks.get(f.name())
                ));
            }
            tables.add(new SchemaDto.TableDto(t, enriched));
        }
        return new SchemaDto(tables);
    }

    private QueryResultDto readResultSet(ResultSet rs, long startedAt) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int n = meta.getColumnCount();
        List<String> columns = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) columns.add(meta.getColumnLabel(i));

        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= n; i++) {
                Object v = rs.getObject(i);
                row.put(columns.get(i - 1), normalize(v));
            }
            rows.add(row);
        }
        return new QueryResultDto(columns, rows, rows.size(), System.currentTimeMillis() - startedAt);
    }

    private Object normalize(Object v) {
        if (v == null) return null;
        if (v instanceof java.sql.Timestamp || v instanceof java.sql.Date || v instanceof java.sql.Time) {
            return v.toString();
        }
        if (v instanceof java.util.UUID) return v.toString();
        if (v instanceof byte[]) return "<binary>";
        return v;
    }

    private Map<String, Object> parseConfig(String json) {
        if (json == null || json.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "connectorConfig trống");
        }
        try {
            return mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "connectorConfig không phải JSON hợp lệ");
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : v.toString();
    }
    private String strOr(Map<String, Object> m, String k, String fallback) {
        String s = str(m, k);
        return s == null || s.isBlank() ? fallback : s;
    }
    private String strOrThrow(Map<String, Object> m, String k) {
        String s = str(m, k);
        if (s == null || s.isBlank())
            throw new ResponseStatusException(BAD_REQUEST, "Thiếu field connectorConfig." + k);
        return s;
    }
    private int intOrThrow(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) throw new ResponseStatusException(BAD_REQUEST, "Thiếu field connectorConfig." + k);
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(BAD_REQUEST, "connectorConfig." + k + " phải là số");
        }
    }

    private record PgConfig(
            String host, int port, String database, String username, String password, String schemaName
    ) {}
}
