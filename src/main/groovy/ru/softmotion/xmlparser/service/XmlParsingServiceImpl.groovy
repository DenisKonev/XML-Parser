package ru.softmotion.xmlparser.service

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

import java.sql.DatabaseMetaData
import java.sql.ResultSet

/**
 * Реализация сервиса для обработки XML и управления данными в БД
 */
@Service
class XmlParsingServiceImpl implements XmlParsingService {

    private static final String XML_URL = "https://expro.ru/bitrix/catalog_export/export_Sai.xml"
    private GPathResult root
    private final JdbcTemplate jdbcTemplate

    @Autowired
    XmlParsingServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate
        // Загрузка XML будет происходить при первом обращении к методам
    }

    private GPathResult getRoot() {
        if (root == null) {
            synchronized(this) {
                if (root == null) {
                    try {
                        this.root = new XmlSlurper().parse(new URL(XML_URL))
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse XML from ${XML_URL}", e)
                    }
                }
            }
        }
        return root
    }

    @Override
    List<String> getTableNames() {
        List<String> tables = new ArrayList<>()
        GPathResult root = getRoot()
        if (root?.shop?.currencies) {
            tables.add("currencies")
        }
        if (root?.shop?.categories) {
            tables.add("categories")
        }
        if (root?.shop?.offers) {
            tables.add("offers")
        }
        return tables
    }

    @Override
    String getTableDDL(String tableName) {
        List<String> columns = getColumnNames(tableName)
        StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS ${tableName} (\n")
        ddl.append("    id BIGSERIAL PRIMARY KEY,\n")

        for (String column : columns) {
            ddl.append("    ${column} TEXT,\n")
        }

        if ("offers".equals(tableName)) {
            ddl.append("    vendor_code TEXT UNIQUE\n")
        } else {
            // Удаляем последнюю запятую
            ddl.deleteCharAt(ddl.length() - 2)
            ddl.append("\n")
        }

        ddl.append(");")
        return ddl.toString()
    }

    @Override
    void update() {
        List<String> tableNames = getTableNames()
        for (String tableName : tableNames) {
            update(tableName)
        }
    }

    @Override
    void update(String tableName) {
        // Проверяем, существует ли таблица
        if (!tableExists(tableName)) {
            throw new RuntimeException("Table ${tableName} does not exist in database")
        }

        // Проверяем, изменилась ли структура
        String ddlChange = getDDLChange(tableName)
        if (ddlChange) {
            throw new RuntimeException("Structure of table ${tableName} has changed: ${ddlChange}")
        }

        // Очищаем таблицу и вставляем новые данные
        clearTable(tableName)
        insertData(tableName)
    }

    @Override
    List<String> getColumnNames(String tableName) {
        List<String> columns = new ArrayList<>()
        GPathResult root = getRoot()

        switch (tableName) {
            case "currencies":
                if (root?.shop?.currencies?.currency) {
                    def firstCurrency = root.shop.currencies.currency[0]
                    if (firstCurrency) {
                        columns.addAll(firstCurrency.attributes().keySet())
                    }
                }
                break
            case "categories":
                if (root?.shop?.categories?.category) {
                    def firstCategory = root.shop.categories.category[0]
                    if (firstCategory) {
                        columns.addAll(firstCategory.attributes().keySet())
                        // Добавляем текстовое содержимое, если оно есть
                        if (firstCategory.text()) {
                            columns.add("text_content")
                        }
                    }
                }
                break
            case "offers":
                if (root?.shop?.offers?.offer) {
                    def firstOffer = root.shop.offers.offer[0]
                    if (firstOffer) {
                        columns.addAll(firstOffer.attributes().keySet())
                        // Добавляем дочерние элементы
                        firstOffer.children().each { child ->
                            if (child.name() && !columns.contains(child.name())) {
                                columns.add(child.name())
                            }
                        }
                    }
                }
                break
        }

        return columns
    }

    @Override
    boolean isColumnId(String tableName, String columnName) {
        // Проверяем, является ли столбец уникальным
        if ("offers".equals(tableName) && "vendor_code".equals(columnName)) {
            return true
        }

        // Проверяем уникальность в базе данных
        String sql = """
            SELECT COUNT(*), COUNT(DISTINCT ${columnName}) 
            FROM ${tableName}
        """
        def result = jdbcTemplate.query(sql) { rs, rowNum ->
            [rs.getLong(1), rs.getLong(2)]
        }
        if (result && result.size() > 0) {
            def row = result[0]
            return row[0] == row[1] // Если общее количество равно количеству уникальных, то столбец уникален
        }
        return false
    }

    @Override
    String getDDLChange(String tableName) {
        List<String> expectedColumns = getColumnNames(tableName)
        List<String> actualColumns = getActualColumnsFromDB(tableName)

        // Проверяем, есть ли новые столбцы
        List<String> newColumns = expectedColumns.findAll { !actualColumns.contains(it) }

        if (newColumns.size() > 0) {
            return "New columns detected: ${newColumns.join(', ')}"
        }

        // Проверяем, были ли удалены столбцы
        List<String> removedColumns = actualColumns.findAll { !expectedColumns.contains(it) }

        if (removedColumns.size() > 0) {
            return "Removed columns detected: ${removedColumns.join(', ')}"
        }

        return null // Нет изменений
    }

    private boolean tableExists(String tableName) {
        DatabaseMetaData metaData = jdbcTemplate.getDataSource().getConnection().getMetaData()
        ResultSet resultSet = metaData.getTables(null, null, tableName, ['TABLE'] as String[])
        return resultSet.next()
    }

    private void clearTable(String tableName) {
        jdbcTemplate.execute("TRUNCATE TABLE ${tableName} RESTART IDENTITY CASCADE")
    }

    private void insertData(String tableName) {
        switch (tableName) {
            case "currencies":
                insertCurrencyData()
                break
            case "categories":
                insertCategoryData()
                break
            case "offers":
                insertOfferData()
                break
        }
    }

    private void insertCurrencyData() {
        GPathResult root = getRoot()
        root?.shop?.currencies?.currency?.each { currency ->
            Map<String, Object> params = [:]
            currency.attributes().each { attrName, attrValue ->
                params[attrName] = attrValue
            }
            insertRow("currencies", params)
        }
    }

    private void insertCategoryData() {
        GPathResult root = getRoot()
        root?.shop?.categories?.category?.each { category ->
            Map<String, Object> params = [:]
            category.attributes().each { attrName, attrValue ->
                params[attrName] = attrValue
            }
            // Добавляем текстовое содержимое, если оно есть
            if (category.text()) {
                params["text_content"] = category.text()
            }
            insertRow("categories", params)
        }
    }

    private void insertOfferData() {
        GPathResult root = getRoot()
        root?.shop?.offers?.offer?.each { offer ->
            Map<String, Object> params = [:]
            offer.attributes().each { attrName, attrValue ->
                params[attrName] = attrValue
            }
            // Добавляем дочерние элементы
            offer.children().each { child ->
                params[child.name()] = child.text()
            }
            insertRow("offers", params)
        }
    }

    private void insertRow(String tableName, Map<String, Object> params) {
        if (params.isEmpty()) return

        StringBuilder sql = new StringBuilder("INSERT INTO ${tableName} (")
        sql.append(params.keySet().join(", "))
        sql.append(") VALUES (")

        // Подготовка плейсхолдеров
        String placeholders = params.keySet().collect { "?" }.join(", ")
        sql.append(placeholders)
        sql.append(")")

        List<Object> values = params.values().toList()
        jdbcTemplate.update(sql.toString(), values.toArray())
    }

    private List<String> getActualColumnsFromDB(String tableName) {
        List<String> columns = []
        DatabaseMetaData metaData = jdbcTemplate.getDataSource().getConnection().getMetaData()
        ResultSet columnsResultSet = metaData.getColumns(null, null, tableName, null)

        while (columnsResultSet.next()) {
            String columnName = columnsResultSet.getString("COLUMN_NAME")
            if (!"id".equals(columnName)) { // Исключаем столбец id
                columns.add(columnName)
            }
        }

        return columns
    }
}