package ru.softmotion.xmlparser.console

import spock.lang.Specification

/**
 * Tests for ConsoleRunner functionality
 */
class ConsoleRunnerSpec extends Specification {

    private ConsoleCommandHandler commandHandler = Mock(ConsoleCommandHandler)
    private ConsoleRunner consoleRunner

    def setup() {
        consoleRunner = new ConsoleRunner(commandHandler)
    }

    def "should initialize with command handler"() {
        expect:
        consoleRunner != null
    }

    def "should have correct method signatures"() {
        when:
        def runMethod = ConsoleRunner.getDeclaredMethod("run", String[].class)

        then:
        runMethod != null
        runMethod.getReturnType() == void.class
    }
}