package ru.softmotion.xmlparser.console

import spock.lang.Specification

/**
 * Unit tests for ConsoleCommand
 */
class ConsoleCommandTest extends Specification {

    def "should map codes to commands correctly"() {
        expect:
        ConsoleCommand.fromCode(1) == ConsoleCommand.LIST_TABLES
        ConsoleCommand.fromCode(2) == ConsoleCommand.GENERATE_DDL
        ConsoleCommand.fromCode(3) == ConsoleCommand.UPDATE_ALL
        ConsoleCommand.fromCode(4) == ConsoleCommand.UPDATE_TABLE
        ConsoleCommand.fromCode(5) == ConsoleCommand.LIST_COLUMNS
        ConsoleCommand.fromCode(6) == ConsoleCommand.CHECK_UNIQUE
        ConsoleCommand.fromCode(7) == ConsoleCommand.DDL_CHANGES
        ConsoleCommand.fromCode(8) == ConsoleCommand.EXIT
    }

    def "should return null for invalid code"() {
        when:
        def command = ConsoleCommand.fromCode(99)

        then:
        command == null
    }

    def "should have correct codes"() {
        expect:
        ConsoleCommand.LIST_TABLES.code == 1
        ConsoleCommand.GENERATE_DDL.code == 2
        ConsoleCommand.UPDATE_ALL.code == 3
        ConsoleCommand.UPDATE_TABLE.code == 4
        ConsoleCommand.LIST_COLUMNS.code == 5
        ConsoleCommand.CHECK_UNIQUE.code == 6
        ConsoleCommand.DDL_CHANGES.code == 7
        ConsoleCommand.EXIT.code == 8
    }
}