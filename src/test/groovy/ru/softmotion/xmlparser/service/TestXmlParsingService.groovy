package ru.softmotion.xmlparser.service

import groovy.xml.slurpersupport.GPathResult
import ru.softmotion.xmlparser.service.xml.XmlParsingServiceImpl
import ru.softmotion.xmlparser.service.xml.XmlLoader
import ru.softmotion.xmlparser.service.jdbc.DbMetadataService
import ru.softmotion.xmlparser.service.jdbc.DdlBuilder
import ru.softmotion.xmlparser.service.jdbc.DataInserter

/**
 * Test helper class to access protected methods in XmlParsingServiceImpl
 */
class TestXmlParsingService extends XmlParsingServiceImpl {
    private GPathResult testRoot

    TestXmlParsingService(GPathResult root) {
        super(new TestXmlLoader(root), null, new DdlBuilder(), null)
        this.testRoot = root
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

/**
 * Test helper for XmlLoader
 */
class TestXmlLoader extends XmlLoader {
    private GPathResult testRoot

    TestXmlLoader(GPathResult root) {
        super("http://example.com/test.xml")  // Dummy URL
        this.testRoot = root
    }

    @Override
    GPathResult getRoot() {
        return testRoot
    }
}