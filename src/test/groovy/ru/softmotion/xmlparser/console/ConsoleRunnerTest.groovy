package ru.softmotion.xmlparser.console

import spock.lang.Specification

/**
 * Unit tests for ConsoleRunner
 */
class ConsoleRunnerTest extends Specification {

    def "should initialize with command handler"() {
        given:
        def commandHandler = Mock(ConsoleCommandHandler)
        def consoleRunner = new ConsoleRunner(commandHandler)

        expect:
        consoleRunner != null
    }
}