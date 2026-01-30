package ru.softmotion.xmlparser.console

import spock.lang.Specification
import ru.softmotion.xmlparser.service.xml.XmlParsingService

/**
 * Unit tests for ConsoleRunner
 */
class ConsoleRunnerTest extends Specification {

    def "should initialize with xml service"() {
        given:
        def xmlService = Mock(XmlParsingService)
        def consoleRunner = new ConsoleRunner(xmlService)

        expect:
        consoleRunner != null
        consoleRunner.xmlService == xmlService
    }
}