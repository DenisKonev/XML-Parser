package ru.softmotion.xmlparser.service

import ru.softmotion.xmlparser.service.xml.XmlParsingServiceImpl
import spock.lang.Specification
import groovy.xml.XmlSlurper

/**
 * Final comprehensive unit tests for XmlParsingServiceImpl
 */
class XmlParsingServiceFinalTest extends Specification {

    def "should convert plural to singular correctly"() {
        given:
        def method = XmlParsingServiceImpl.class.getDeclaredMethod("toSingular", String)
        method.setAccessible(true)

        expect:
        method.invoke(null, "categories") == "category"
        method.invoke(null, "currencies") == "currency"
        method.invoke(null, "offers") == "offer"
        method.invoke(null, "test") == "test"
        method.invoke(null, "cats") == "cat"
        method.invoke(null, "dogs") == "dog"
    }

    def "should extract basic column names from XML"() {
        given:
        def xmlContent = '''<shop>
<currencies>
<currency id="RUR" rate="1"/>
<currency id="USD" rate="0.013"/>
</currencies>
<categories>
<category id="1">Electronics</category>
<category id="2" parentId="1">Phones</category>
</categories>
<offers>
<offer id="1" available="true">
<name>Phone X</name>
<price>500</price>
</offer>
</offers>
</shop>'''
        def root = new XmlSlurper().parseText(xmlContent)
        def service = new TestXmlParsingService(root)

        when:
        def currencyColumns = service.testGetColumnNames("currencies")
        def categoryColumns = service.testGetColumnNames("categories")
        def offerColumns = service.testGetColumnNames("offers")

        then:
        currencyColumns.contains("id")
        currencyColumns.contains("rate")
        currencyColumns.size() >= 2

        categoryColumns.contains("id")
        categoryColumns.contains("parentId") || categoryColumns.contains("text_content")
        categoryColumns.size() >= 2

        offerColumns.contains("id")
        offerColumns.contains("available")
        offerColumns.contains("name") || offerColumns.contains("price")
        offerColumns.size() >= 3
    }

    def "should return table names from XML"() {
        given:
        def xmlContent = '''<shop>
<currencies>
<currency id="RUR" rate="1"/>
</currencies>
<categories>
<category id="1">Electronics</category>
</categories>
<offers>
<offer id="1" available="true">
<name>Phone X</name>
</offer>
</offers>
</shop>'''
        def root = new XmlSlurper().parseText(xmlContent)
        def service = new TestXmlParsingService(root)

        when:
        def tableNames = service.testGetTableNames()

        then:
        tableNames.size() == 3
        tableNames.contains("currencies")
        tableNames.contains("categories")
        tableNames.contains("offers")
    }

    def "should generate basic DDL structure"() {
        given:
        def xmlContent = '''<shop>
<offers>
<offer id="1" available="true">
<name>Phone X</name>
<price>500</price>
</offer>
</offers>
</shop>'''
        def root = new XmlSlurper().parseText(xmlContent)
        def service = new TestXmlParsingService(root)

        when:
        def ddl = service.testGetTableDDL("offers")

        then:
        ddl.contains("CREATE TABLE IF NOT EXISTS offers")
        ddl.contains("BIGSERIAL PRIMARY KEY")
        ddl.contains("TEXT")
    }
}