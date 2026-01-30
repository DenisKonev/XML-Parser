package ru.softmotion.xmlparser.service.jdbc

import groovy.util.logging.Slf4j
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet

/**
 * Сервис для работы с метаданными БД:
 *  - проверка существования таблицы
 *  - получение списка фактических колонок таблицы
 *
 * Все соединения и ResultSet корректно закрываются.
 */
@Slf4j
@Component
class DbMetadataService {

    private final JdbcTemplate jdbcTemplate

    DbMetadataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate
    }

    /**
     * Проверяет, существует ли таблица в текущей БД.
     *
     * @param tableName имя таблицы
     * @return true если таблица существует
     */
    boolean tableExists(String tableName) {
        Connection conn = null
        ResultSet rs = null
        try {
            conn = jdbcTemplate.dataSource.connection
            DatabaseMetaData meta = conn.metaData
            rs = meta.getTables(null, null, tableName, ['TABLE'] as String[])
            boolean exists = rs.next()
            log.debug("tableExists('{}') -> {}", tableName, exists)
            return exists
        } finally {
            try { rs?.close() } catch (Exception e) { log.debug("Ignoring RS close error", e) }
            try { conn?.close() } catch (Exception e) { log.debug("Ignoring connection close error", e) }
        }
    }

    /**
     * Возвращает имена колонок таблицы (исключая стандартные *_id).
     *
     * @param tableName имя таблицы
     * @return список имён колонок (оригинальные регистры)
     */
    List<String> getActualColumnsFromDB(String tableName) {
        List<String> columns = []
        Connection conn = null
        ResultSet rs = null
        try {
            conn = jdbcTemplate.dataSource.connection
            DatabaseMetaData meta = conn.metaData
            rs = meta.getColumns(null, null, tableName, null)
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME")
                if (columnName && !columnName.toLowerCase().endsWith('_id')) {
                    columns.add(columnName)
                }
            }
            log.debug("getActualColumnsFromDB('{}') -> {}", tableName, columns)
            return columns
        } finally {
            try { rs?.close() } catch (Exception e) { log.debug("Ignoring RS close error", e) }
            try { conn?.close() } catch (Exception e) { log.debug("Ignoring connection close error", e) }
        }
    }
}
