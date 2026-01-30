package ru.softmotion.xmlparser.service


import groovy.xml.slurpersupport.GPathResult
import ru.softmotion.xmlparser.service.xml.XmlParsingServiceImpl

/**
 * Test helper class to access protected methods in XmlParsingServiceImpl
 */
class TestXmlParsingService extends XmlParsingServiceImpl {
    private GPathResult testRoot
    
    TestXmlParsingService(GPathResult root) {
        super(null)  // Pass null as jdbcTemplate since we're not using it
        this.testRoot = root
    }
    
    @Override
    protected GPathResult getRoot() {
        return testRoot
    }
    
    // Expose protected methods for testing
    List<String> testGetColumnNames(String tableName) {
        return getColumnNames(tableName)
    }
    
    String testGetTableDDL(String tableName) {
        return getTableDDL(tableName)
    }
    
    String testGetDDLChange(String tableName) {
        return getDDLChange(tableName)
    }
    
    List<String> testGetTableNames() {
        return getTableNames()
    }
}