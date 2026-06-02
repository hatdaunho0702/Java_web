package com.dienmay.entity.nhom5.config;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrate() {
        if (!uidColumnExists()) {
            log.info("Bắt đầu migrate SQLite UID cho bảng users");
            jdbcTemplate.execute("PRAGMA foreign_keys = OFF");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN uid VARCHAR(128)");
            jdbcTemplate.execute("DELETE FROM cart_items");
            jdbcTemplate.execute("DELETE FROM orders");
            jdbcTemplate.execute("DELETE FROM order_items");
            jdbcTemplate.execute("DELETE FROM reviews");
            jdbcTemplate.execute("DELETE FROM notifications");
            jdbcTemplate.execute("DELETE FROM user_addresses");
            jdbcTemplate.execute("DELETE FROM users");
            jdbcTemplate.execute("PRAGMA foreign_keys = ON");
            log.info("Đã migrate xong cột uid cho bảng users");
        }

        if (!isPrimaryColumnExists()) {
            log.info("Bắt đầu migrate SQLite is_primary cho bảng product_images");
            jdbcTemplate.execute("ALTER TABLE product_images ADD COLUMN is_primary BOOLEAN DEFAULT 0");
            log.info("Đã migrate xong cột is_primary cho bảng product_images");
        }
    }

    private boolean uidColumnExists() {
        List<String> columnNames = jdbcTemplate.queryForList("PRAGMA table_info(users)")
                .stream()
                .map(row -> String.valueOf(row.get("name")))
                .toList();
        return columnNames.stream().anyMatch(name -> "uid".equalsIgnoreCase(name));
    }

    private boolean isPrimaryColumnExists() {
        List<String> columnNames = jdbcTemplate.queryForList("PRAGMA table_info(product_images)")
                .stream()
                .map(row -> String.valueOf(row.get("name")))
                .toList();
        return columnNames.stream().anyMatch(name -> "is_primary".equalsIgnoreCase(name));
    }
}