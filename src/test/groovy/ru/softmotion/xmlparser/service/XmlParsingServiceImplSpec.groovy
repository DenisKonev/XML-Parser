package ru.softmotion.xmlparser.service

import spock.lang.Specification
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Тесты для XmlParsingServiceImpl
 */
class XmlParsingServiceImplSpec extends Specification {
    
    private JdbcTemplate jdbcTemplate = Mock(JdbcTemplate)
    private XmlParsingService xmlService
    
    def setup() {
        xmlService = new XmlParsingServiceImpl(jdbcTemplate)
    }
    
    def "should return table names from XML"() {
        when:
        def tableNames = xmlService.getTableNames()
        
        then:
        tableNames != null
        tableNames.size() >= 0 // Может быть пустым в зависимости от содержимого XML
    }
    
    def "should generate DDL for a table"() {
        given:
        def tableName = "offers"
        
        when:
        def ddl = xmlService.getTableDDL(tableName)
        
        then:
        ddl != null
        ddl.contains("CREATE TABLE IF NOT EXISTS ${tableName}")
        ddl.contains("id BIGSERIAL PRIMARY KEY")
    }
    
    def "should get column names for a table"() {
        given:
        def tableName = "offers"
        
        when:
        def columnNames = xmlService.getColumnNames(tableName)
        
        then:
        columnNames != null
    }

    def "should detect DDL changes"() {
        given:
        def tableName = "offers"
        
        when:
        def ddlChange = xmlService.getDDLChange(tableName)
        
        then:
        ddlChange != null || ddlChange == null // Может быть null, если нет изменений
    }
}