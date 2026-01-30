package ru.softmotion.xmlparser.service.jdbc

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

/**
 * Компонент для построения DDL (CREATE TABLE) по списку колонок.
 * Содержит вспомогательную логику преобразования имени таблицы в singular для PK.
 */
@Slf4j
@Component
class DdlBuilder {

    /**
     * Собрать CREATE TABLE SQL на основе имён колонок.
     *
     * @param tableName имя таблицы
     * @param columns   список имён колонок (без PK)
     * @return SQL строка CREATE TABLE
     */
    static String buildCreateTable(String tableName, List<String> columns) {
        String pkName = "${toSingular(tableName)}_id"
        StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS ${tableName} (\n")
        ddl.append("    ${pkName} BIGSERIAL PRIMARY KEY")

        if (columns && !columns.isEmpty()) {
            ddl.append(",\n")
            ddl.append(columns.collect { "    ${it} TEXT" }.join(",\n"))
            ddl.append("\n")
        } else {
            ddl.append("\n")
        }

        ddl.append(");")
        log.debug("DDL for {} built ({} columns)", tableName, columns?.size() ?: 0)
        return ddl.toString()
    }

    /**
     * Упрощённое преобразование имени таблицы в единственное число.
     *
     * @param name имя таблицы
     * @return имя в singular (best-effort)
     */
    private static String toSingular(String name) {
        if (!name) return "id"
        String n = name.trim()
        if (n.toLowerCase().endsWith("ies")) {
            return n.substring(0, n.length() - 3) + "y"
        } else if (n.toLowerCase().endsWith("s")) {
            return n.substring(0, n.length() - 1)
        } else {
            return n
        }
    }
}
