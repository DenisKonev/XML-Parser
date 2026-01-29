package ru.softmotion.xmlparser.service

import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.XMLReader

import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import java.sql.DatabaseMetaData
import java.sql.ResultSet

/**
 * Реализация сервиса для обработки XML и управления данными в БД
 */
@Service
@Slf4j
class XmlParsingServiceImpl implements XmlParsingService {

    private static final String XML_URL = "https://expro.ru/bitrix/catalog_export/export_Sai.xml"
    private volatile GPathResult root
    private final JdbcTemplate jdbcTemplate

    @Autowired
    XmlParsingServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate
        // Загрузка XML будет происходить при первом обращении к методам
    }

    private GPathResult getRoot() {
        if (root == null) {
            synchronized (this) {
                if (root == null) {
                    try {
                        this.root = loadXmlWithXmlSlurper(XML_URL)
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse XML from " + XML_URL, e)
                    }
                }
            }
        }
        return root
    }

    private static GPathResult loadXmlWithXmlSlurper(String urlString) throws Exception {
        URI uri = URI.create(urlString)
        URLConnection connection = uri.toURL().openConnection()
        connection.setConnectTimeout(10_000)
        connection.setReadTimeout(30_000)
        connection.setRequestProperty("User-Agent", "XML-Parser/1.0")

        try (InputStream is = connection.getInputStream()) {
            SAXParserFactory factory = SAXParserFactory.newInstance()
            factory.setNamespaceAware(true)

            // Безопасные опции: запрещаем загрузку внешних сущностей
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)

            XMLReader xmlReader = factory.newSAXParser().getXMLReader()

            // Блокируем реальные запросы внешних сущностей — возвращаем пустую сущность
            xmlReader.setEntityResolver(new EntityResolver() {
                @Override
                InputSource resolveEntity(String publicId, String systemId) {
                    log.warn("Blocked external entity resolution. publicId={}, systemId={}", publicId, systemId)
                    // Возвращаем пустой InputSource — парсер не будет пытаться читать DTD
                    return new InputSource(new StringReader(""))
                }
            })

            XmlSlurper slurper = new XmlSlurper(xmlReader)
            return (GPathResult) slurper.parse(is)
        }
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
        String pkName = "${toSingular(tableName)}_id"

        StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS ${tableName} (\n")
        ddl.append("    ${pkName} BIGSERIAL PRIMARY KEY")

        List<String> otherDefs = []

        for (String column : columns) {
            otherDefs.add("    ${column} TEXT")
        }

        if (!otherDefs.isEmpty()) {
            ddl.append(",\n")
            ddl.append(otherDefs.join(",\n"))
            ddl.append("\n")
        } else {
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
        if (root == null) return columns

        switch (tableName) {
            case "currencies":
                // Пройти по всем <currency> и собрать все атрибуты (union)
                root.shop?.currencies?.currency?.each { currency ->
                    // attributes() возвращает Map-like; используем keySet()
                    def attrs = currency.attributes()
                    if (attrs) {
                        attrs.keySet().each { k ->
                            if (k && !columns.contains(k)) columns.add(k.toString())
                        }
                    }
                }
                break

            case "categories":
                // Пройти по всем <category> и собрать union атрибутов + флаг текстового содержимого
                boolean anyText = false
                root.shop?.categories?.category?.each { category ->
                    def attrs = category.attributes()
                    if (attrs) {
                        attrs.keySet().each { k ->
                            if (k && !columns.contains(k)) columns.add(k.toString())
                        }
                    }
                    // Если у этого category есть текст (не пустой), пометим флаг
                    String txt = category.text()?.toString()?.trim()
                    if (txt) {
                        anyText = true
                    }
                }
                if (anyText && !columns.contains("text_content")) {
                    columns.add("text_content")
                }
                break

            case "offers":
                // Пройти по всем <offer>, собрать union атрибутов и union имён дочерних элементов
                root.shop?.offers?.offer?.each { offer ->
                    def attrs = offer.attributes()
                    if (attrs) {
                        attrs.keySet().each { k ->
                            if (k && !columns.contains(k)) columns.add(k.toString())
                        }
                    }
                    // children() возвращает узлы-элементы; соберём имена
                    offer.children().each { child ->
                        if (child?.name()) {
                            String nm = child.name().toString()
                            if (nm && !columns.contains(nm)) columns.add(nm)
                        }
                    }
                }
                break
        }

        return columns
    }

    @Override
    boolean isColumnId(String tableName, String columnName) {
        // Проверяем уникальность в базе данных
        String sql = """
            SELECT COUNT(*), COUNT(DISTINCT ${columnName}) 
            FROM ${tableName}
        """
        def result = jdbcTemplate.query(sql, { rs, rowNum ->
            [rs.getLong(1), rs.getLong(2)]
        } as RowMapper)

        if (result && !result.isEmpty()) {
            List<Long> row = (List<Long>) result.get(0)
            if (row != null && row.size() >= 2) {
                return row.get(0).longValue() == row.get(1).longValue()
            }
        }
        return false
    }

    @Override
    String getDDLChange(String tableName) {
        List<String> expectedColumns = getColumnNames(tableName).collect { it?.toString() ?: '' }
        List<String> actualColumns = getActualColumnsFromDB(tableName).collect { it?.toString() ?: '' }

        // сопоставление lower-case -> оригинальное имя для корректного отображения
        Map<String, String> expectedLowerToOrig = expectedColumns.collectEntries { [ (it.toLowerCase()) , it ] }
        Map<String, String> actualLowerToOrig   = actualColumns.collectEntries   { [ (it.toLowerCase()) , it ] }

        Set<String> expectedLower = expectedLowerToOrig.keySet() as Set
        Set<String> actualLower   = actualLowerToOrig.keySet() as Set

        // optional columns per table (в нижнем регистре)
        Map<String, Set<String>> optionalColumnsByTable = [
                categories: ['parentid'] as Set   // parentId — это опциональная колонка для categories
        ]

        Set<String> optionalForThis = optionalColumnsByTable.get(tableName?.toLowerCase()) ?: [] as Set

        // Новые колонки: те, что в expected, но нет в actual, и не помечены как optional
        List<String> newLower = expectedLower
                .findAll { !(actualLower.contains(it)) && !optionalForThis.contains(it) }
                .toList()
        if (!newLower.isEmpty()) {
            List<String> newColumns = newLower.collect { expectedLowerToOrig[it] } // вернём оригинальные имена
            return "New columns detected: ${newColumns.join(', ')}"
        }

        // Удалённые колонки: те, что в actual, но нет в expected.
        // (optional — тут не нужно фильтровать, т.к. optional отсутствуют в expected и будут считаться удалёнными;
        //  если хотите игнорировать их в обе стороны — добавьте дополнительную логику)
        List<String> removedLower = actualLower
                .findAll { !(expectedLower.contains(it)) }
                .toList()
        if (!removedLower.isEmpty()) {
            List<String> removedColumns = removedLower.collect { actualLowerToOrig[it] }
            return "Removed columns detected: ${removedColumns.join(', ')}"
        }

        return null // Нет изменений
    }

    // Простая функция для преобразования имени таблицы в единственное число.
    // Правила:
    //  - ...ies -> ...y  (categories -> category, currencies -> currency)
    //  - ...s   -> ...   (offers -> offer)
    //  - иначе — оставляем как есть (если уже в единственном числе)
    private static String toSingular(String name) {
        if (name == null) return "id"
        name = name.trim()
        if (name.toLowerCase().endsWith("ies")) {
            return name.substring(0, name.length() - 3) + "y"
        } else if (name.toLowerCase().endsWith("s")) {
            return name.substring(0, name.length() - 1)
        } else {
            return name
        }
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
        root?.shop?.currencies?.currency?.each { GPathResult currency ->
            Map<String, Object> params = [:]

            currency.attributes().each { String attrName, Object attrValue ->
                params[attrName] = attrValue
            }

            insertRow("currencies", params)
        }
    }


    private void insertCategoryData() {
        GPathResult root = getRoot()
        root?.shop?.categories?.category?.each { GPathResult category ->
            Map<String, Object> params = [:]

            category.attributes().each { String attrName, Object attrValue ->
                params[attrName] = attrValue
            }

            // Добавляем текстовое содержимое, если оно есть
            String text = category.text()
            if (text) {
                params["text_content"] = text
            }

            insertRow("categories", params)
        }
    }

    private void insertOfferData() {
        GPathResult root = getRoot()
        root?.shop?.offers?.offer?.each { GPathResult offer ->
            Map<String, Object> params = [:]

            offer.attributes().each { String attrName, Object attrValue ->
                params[attrName] = attrValue
            }

            // Добавляем дочерние элементы
            offer.children().each { Object child ->
                String name = child.name()
                if (name) {
                    params[name] = child.text()
                }
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
            String cn = columnName?.toLowerCase()
            // Пропускаем стандартный id и любые столбцы, оканчивающиеся на "_id"
            if (!cn.endsWith('_id')) {
                columns.add(columnName)
            }
        }

        return columns
    }
}