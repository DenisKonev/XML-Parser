package ru.softmotion.xmlparser.console

/**
 * Перечень поддерживаемых консольных команд.
 * Исключает использование magic numbers.
 */
enum ConsoleCommand {

    LIST_TABLES(1),
    GENERATE_DDL(2),
    UPDATE_ALL(3),
    UPDATE_TABLE(4),
    LIST_COLUMNS(5),
    CHECK_UNIQUE(6),
    DDL_CHANGES(7),
    EXIT(8)

    final int code

    ConsoleCommand(int code) {
        this.code = code
    }

    /**
     * Получение команды по числовому коду.
     *
     * @param code числовой код команды
     * @return соответствующая команда или null
     */
    static ConsoleCommand fromCode(int code) {
        values().find { it.code == code }
    }
}
