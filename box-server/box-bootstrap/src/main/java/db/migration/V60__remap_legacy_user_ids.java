package db.migration;

import com.boxai.common.id.SnowflakeIdGenerator;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rewrites leftover AUTO_INCREMENT user ids into snowflake ids and updates references.
 */
public class V60__remap_legacy_user_ids extends BaseJavaMigration {

    private static final long LEGACY_MAX = 1_000_000_000_000L;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        List<long[]> mappings = loadLegacyMappings(connection);
        if (mappings.isEmpty()) {
            return;
        }
        List<String[]> columns = loadUserRefColumns(connection);
        try (Statement toggle = connection.createStatement()) {
            toggle.execute("SET FOREIGN_KEY_CHECKS = 0");
        }
        try {
            for (long[] pair : mappings) {
                long oldId = pair[0];
                long newId = pair[1];
                for (String[] column : columns) {
                    updateColumn(connection, column[0], column[1], oldId, newId);
                }
            }
        } finally {
            try (Statement toggle = connection.createStatement()) {
                toggle.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }

    private static List<long[]> loadLegacyMappings(Connection connection) throws Exception {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        List<long[]> mappings = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT id FROM sys_user WHERE id < " + LEGACY_MAX + " ORDER BY id DESC")) {
            while (rs.next()) {
                mappings.add(new long[]{rs.getLong(1), generator.nextId()});
            }
        }
        return mappings;
    }

    private static List<String[]> loadUserRefColumns(Connection connection) throws Exception {
        Map<String, String[]> unique = new LinkedHashMap<>();
        unique.put("sys_user.id", new String[]{"sys_user", "id"});
        String sql = """
                SELECT TABLE_NAME, COLUMN_NAME
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND DATA_TYPE = 'bigint'
                  AND COLUMN_NAME IN (
                    'user_id', 'owner_id', 'created_by', 'updated_by',
                    'admin_user_id', 'accepted_user_id'
                  )
                """;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                String table = rs.getString(1);
                String column = rs.getString(2);
                unique.put(table + "." + column, new String[]{table, column});
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static void updateColumn(Connection connection, String table, String column, long oldId, long newId)
            throws Exception {
        String sql = "UPDATE `" + table + "` SET `" + column + "` = ? WHERE `" + column + "` = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, newId);
            statement.setLong(2, oldId);
            statement.executeUpdate();
        }
    }
}
