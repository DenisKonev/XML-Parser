package ru.softmotion.xmlparser.console

import spock.lang.Specification
import ru.softmotion.xmlparser.service.xml.XmlParsingService

/**
 * Tests for ConsoleRunner functionality
 */
class ConsoleRunnerSpec extends Specification {

    private XmlParsingService xmlService = Mock(XmlParsingService)
    private ConsoleRunner consoleRunner

    def setup() {
        consoleRunner = new ConsoleRunner(xmlService)
    }

    def "should initialize with xml service"() {
        expect:
        consoleRunner != null
        consoleRunner.xmlService == xmlService
    }

    def "should have correct method signatures"() {
        when:
        def runMethod = ConsoleRunner.getDeclaredMethod("run", String[].class)
        
        then:
        runMethod != null
        runMethod.getReturnType() == void.class
    }
}