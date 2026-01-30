package ru.softmotion.xmlparser.service

import spock.lang.Specification
import groovy.xml.XmlSlurper

/**
 * Tests for SimpleXmlParser functionality
 */
class SimpleXmlParserSpec extends Specification {

    def "should parse XML correctly in main method"() {
        given:
        def originalOut = System.out
        def byteStream = new ByteArrayOutputStream()
        def customOut = new PrintStream(byteStream)
        System.setOut(customOut)

        when:
        SimpleXmlParser.main([] as String[])

        then:
        def output = byteStream.toString()
        output.contains("Testing Groovy XML parsing...")
        output.contains("test")

        cleanup:
        System.setOut(originalOut)
    }

    def "should parse XML with XmlSlurper correctly"() {
        given:
        def xmlContent = '<root><item>test</item></root>'

        when:
        def xml = new XmlSlurper().parseText(xmlContent)

        then:
        xml.item.text() == "test"
    }
}