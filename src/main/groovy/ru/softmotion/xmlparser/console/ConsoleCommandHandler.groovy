package ru.softmotion.xmlparser.console

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component
import ru.softmotion.xmlparser.service.xml.XmlParsingService

/**
 * Обработчик консольных команд.
 * Содержит всю бизнес-логику взаимодействия пользователя с сервисом.
 */
@Slf4j
@Component
class ConsoleCommandHandler {

    private final XmlParsingService xmlService
    private final Scanner scanner = new Scanner(System.in)

    ConsoleCommandHandler(XmlParsingService xmlService) {
        this.xmlService = xmlService
    }

    /**
     * Основной цикл обработки пользовательских команд.
     */
    void start() {
        boolean running = true
        println("XML Parser Console Interface Started")

        while (running) {
            printMenu()
            ConsoleCommand command = readCommand()

            if (command == null) {
                println("Unknown command")
                continue
            }

            running = execute(command)
        }

        scanner.close()
        println("Console interface closed")
    }

    private static void printMenu() {
        println("""
Select an option:
1 - List tables
2 - Generate DDL for a table
3 - Update all tables in DB
4 - Update specific table in DB
5 - Get column names for a table
6 - Check if column is unique
7 - Get DDL changes for a table
8 - Exit
""")
    }

    private ConsoleCommand readCommand() {
        try {
            int code = scanner.nextInt()
            scanner.nextLine()
            return ConsoleCommand.fromCode(code)
        } catch (Exception ignored) {
            scanner.nextLine()
            log.warn("Invalid command input")
            return null
        }
    }

    private boolean execute(ConsoleCommand command) {
        log.info("Executing command: {}", command)

        boolean continueRunning = true

        try {
            switch (command) {
                case ConsoleCommand.LIST_TABLES -> listTables()
                case ConsoleCommand.GENERATE_DDL -> generateDDL()
                case ConsoleCommand.UPDATE_ALL -> updateAll()
                case ConsoleCommand.UPDATE_TABLE -> updateTable()
                case ConsoleCommand.LIST_COLUMNS -> listColumns()
                case ConsoleCommand.CHECK_UNIQUE -> checkUnique()
                case ConsoleCommand.DDL_CHANGES -> ddlChanges()
                case ConsoleCommand.EXIT -> {
                    println("Exiting...")
                    continueRunning = false
                }
            }
        } catch (Exception e) {
            log.error("Error while executing command {}", command, e)
            println("Operation failed: ${e.message}")
        }

        return continueRunning
    }

    private void listTables() {
        println("Available tables:")
        xmlService.getTableNames().each { println("- $it") }
    }

    private void generateDDL() {
        String table = read("Enter table name: ")
        println(xmlService.getTableDDL(table))
    }

    private void updateAll() {
        xmlService.update()
        println("All tables updated successfully")
    }

    private void updateTable() {
        String table = read("Enter table name: ")
        xmlService.update(table)
        println("Table $table updated successfully")
    }

    private void listColumns() {
        String table = read("Enter table name: ")
        println("Columns in $table:")
        xmlService.getColumnNames(table).each { println("- $it") }
    }

    private void checkUnique() {
        String table = read("Enter table name: ")
        String column = read("Enter column name: ")
        boolean unique = xmlService.isColumnId(table, column)
        println("Column $column in table $table is ${unique ? 'unique' : 'not unique'}")
    }

    private void ddlChanges() {
        String table = read("Enter table name: ")
        String changes = xmlService.getDDLChange(table)

        if (changes) {
            println("DDL changes for $table:\n$changes")
        } else {
            println("No DDL changes detected for $table")
        }
    }

    private String read(String prompt) {
        print(prompt)
        scanner.nextLine()
    }
}
