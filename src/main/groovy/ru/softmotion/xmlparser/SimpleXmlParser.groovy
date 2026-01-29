package ru.softmotion.xmlparser.service

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult

/**
 * Простой класс для проверки работы Groovy
 */
class SimpleXmlParser {
    static void main(String[] args) {
        println("Testing Groovy XML parsing...")
        def xml = new XmlSlurper().parseText('<root><item>test</item></root>')
        println(xml.item.text())
    }
}