package ru.softmotion.xmlparser.service.xml

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult

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