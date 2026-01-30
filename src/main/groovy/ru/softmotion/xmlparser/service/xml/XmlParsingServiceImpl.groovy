package ru.softmotion.xmlparser.service.xml

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service
import ru.softmotion.xmlparser.service.jdbc.DataInserter
import ru.softmotion.xmlparser.service.jdbc.DbMetadataService
import ru.softmotion.xmlparser.service.jdbc.DdlBuilder

/**
 * Реализация XmlParsingService: координирует XmlLoader, DbMetadataService, DdlBuilder и DataInserter.
 * Отвечает за валидацию структуры, сравнение ожидаемых и фактических колонок и за операции обновления.
 */
@Slf4j
@Service
class XmlParsingServiceImpl implements XmlParsingService {

    private final XmlLoader xmlLoader
    private final DbMetadataService dbMetadataService
    private final DdlBuilder ddlBuilder
    private final DataInserter dataInserter

    XmlParsingServiceImpl(XmlLoader xmlLoader,
                          DbMetadataService dbMetadataService,
                          DdlBuilder ddlBuilder,
                          DataInserter dataInserter) {
        this.xmlLoader = xmlLoader
        this.dbMetadataService = dbMetadataService
        this.ddlBuilder = ddlBuilder
        this.dataInserter = dataInserter
    }

    @Override
    List<String> getTableNames() {
        def root = xmlLoader.getRoot()
        List<String> tables = []
        if (root?.shop?.currencies) tables.add("currencies")
        if (root?.shop?.categories) tables.add("categories")
        if (root?.shop?.offers) tables.add("offers")
        log.debug("Detected tables: {}", tables)
        return tables
    }

    @Override
    String getTableDDL(String tableName) {
        List<String> columns = getColumnNames(tableName)
        return ddlBuilder.buildCreateTable(tableName, columns)
    }

    @Override
    void update() {
        log.info("Updating all tables")
        getTableNames().each { update(it) }
    }

    @Override
    void update(String tableName) {
        log.info("Updating table {}", tableName)
        if (!dbMetadataService.tableExists(tableName)) {
            log.error("Table {} does not exist", tableName)
            throw new RuntimeException("Table ${tableName} does not exist in database")
        }

        String ddlChange = getDDLChange(tableName)
        if (ddlChange) {
            log.error("DDL change detected for {}: {}", tableName, ddlChange)
            throw new RuntimeException("Structure of table ${tableName} has changed: ${ddlChange}")
        }

        dataInserter.clearTable(tableName)
        dataInserter.insertData(tableName)
        log.info("Table {} updated successfully", tableName)
    }

    @Override
    List<String> getColumnNames(String tableName) {
        List<String> columns = []
        def root = xmlLoader.getRoot()
        if (!root) return columns

        switch (tableName) {
            case "currencies":
                root.shop?.currencies?.currency?.each { c ->
                    def attrs = c.attributes()
                    if (attrs) attrs.keySet().each { k -> if (k && !columns.contains(k.toString())) columns.add(k.toString()) }
                }
                break
            case "categories":
                boolean anyText = false
                root.shop?.categories?.category?.each { c ->
                    def attrs = c.attributes()
                    if (attrs) attrs.keySet().each { k -> if (k && !columns.contains(k.toString())) columns.add(k.toString()) }
                    String txt = c.text()?.toString()?.trim()
                    if (txt) anyText = true
                }
                if (anyText && !columns.contains("text_content")) columns.add("text_content")
                break
            case "offers":
                root.shop?.offers?.offer?.each { o ->
                    def attrs = o.attributes()
                    if (attrs) attrs.keySet().each { k -> if (k && !columns.contains(k.toString())) columns.add(k.toString()) }
                    o.children().each { child ->
                        if (child?.name()) {
                            String nm = child.name().toString()
                            if (nm && !columns.contains(nm)) columns.add(nm)
                        }
                    }
                }
                break
            default:
                log.warn("getColumnNames called with unknown tableName: {}", tableName)
        }
        log.debug("getColumnNames('{}') -> {}", tableName, columns)
        return columns
    }

    @Override
    boolean isColumnId(String tableName, String columnName) {
        log.debug("Checking uniqueness of {}.{}", tableName, columnName)
        String sql = "SELECT COUNT(*) as total, COUNT(DISTINCT ${columnName}) as uniq FROM ${tableName}"
        List<Map<String, Object>> rows = dataInserter.jdbcTemplate.queryForList(sql)
        if (!rows || rows.isEmpty()) return false
        Map<String, Object> row = rows[0]
        long total = ((Number) row['total']).longValue()
        long uniq = ((Number) row['uniq']).longValue()
        boolean result = total == uniq
        log.debug("isColumnId -> total={}, uniq={}, result={}", total, uniq, result)
        return result
    }

    @Override
    String getDDLChange(String tableName) {
        List<String> expectedColumns = getColumnNames(tableName).collect { it?.toString() ?: '' }
        List<String> actualColumns = dbMetadataService.getActualColumnsFromDB(tableName).collect { it?.toString() ?: '' }

        Map<String, String> expectedLowerToOrig = expectedColumns.collectEntries { [ (it.toLowerCase()) , it ] }
        Map<String, String> actualLowerToOrig   = actualColumns.collectEntries   { [ (it.toLowerCase()) , it ] }

        Set<String> expectedLower = expectedLowerToOrig.keySet() as Set
        Set<String> actualLower   = actualLowerToOrig.keySet() as Set

        Map<String, Set<String>> optionalColumnsByTable = [
                categories: ['parentid'] as Set
        ]
        Set<String> optionalForThis = optionalColumnsByTable.get(tableName?.toLowerCase()) ?: [] as Set

        List<String> newLower = expectedLower.findAll { !(actualLower.contains(it)) && !optionalForThis.contains(it) }.toList()
        if (!newLower.isEmpty()) {
            List<String> newCols = newLower.collect { expectedLowerToOrig[it] }
            return "New columns detected: ${newCols.join(', ')}"
        }

        List<String> removedLower = actualLower.findAll { !(expectedLower.contains(it)) }.toList()
        if (!removedLower.isEmpty()) {
            List<String> removed = removedLower.collect { actualLowerToOrig[it] }
            return "Removed columns detected: ${removed.join(', ')}"
        }

        return null
    }
}
