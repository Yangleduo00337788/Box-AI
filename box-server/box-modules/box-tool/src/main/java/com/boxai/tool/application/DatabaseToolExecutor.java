package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.crypto.SecretCipher;
import com.boxai.domain.tool.ToolDatabaseConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseToolExecutor {

    private final SecretCipher secretCipher;
    private final ObjectMapper objectMapper;

    public DatabaseToolExecutor(SecretCipher secretCipher, ObjectMapper objectMapper) {
        this.secretCipher = secretCipher;
        this.objectMapper = objectMapper;
    }

    public String execute(ToolDatabaseConfig config, String sql) {
        long started = System.currentTimeMillis();
        String safeSql = SqlSafetyValidator.validateSelectOnly(sql);
        int maxRows = config.getMaxRows() == null ? 100 : config.getMaxRows();
        int timeoutMs = config.getTimeoutMs() == null ? 10000 : config.getTimeoutMs();
        String jdbcUrl = buildJdbcUrl(config);
        String password = secretCipher.decrypt(config.getPasswordCiphertext());
        try (Connection connection = DriverManager.getConnection(jdbcUrl, config.getUsername(), password)) {
            connection.setReadOnly(true);
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(Math.max(1, timeoutMs / 1000));
                statement.setMaxRows(maxRows);
                try (ResultSet resultSet = statement.executeQuery(safeSql)) {
                    List<Map<String, Object>> rows = readRows(resultSet, maxRows);
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("rowCount", rows.size());
                    payload.put("rows", rows);
                    payload.put("durationMs", System.currentTimeMillis() - started);
                    return objectMapper.writeValueAsString(payload);
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "数据库工具执行失败: " + ex.getMessage());
        }
    }

    private List<Map<String, Object>> readRows(ResultSet resultSet, int maxRows) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData meta = resultSet.getMetaData();
        int columnCount = meta.getColumnCount();
        while (resultSet.next() && rows.size() < maxRows) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String label = meta.getColumnLabel(i);
                row.put(label, resultSet.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }

    private String buildJdbcUrl(ToolDatabaseConfig config) {
        String type = config.getDatabaseType() == null ? "MYSQL" : config.getDatabaseType().trim().toUpperCase();
        if (!"MYSQL".equals(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前仅支持 MYSQL 数据库工具");
        }
        int port = config.getPort() == null ? 3306 : config.getPort();
        return "jdbc:mysql://" + config.getHost().trim() + ":" + port + "/" + config.getDatabaseName().trim()
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false";
    }
}
