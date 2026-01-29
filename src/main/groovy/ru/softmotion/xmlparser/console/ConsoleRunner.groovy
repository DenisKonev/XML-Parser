package ru.softmotion.xmlparser.console

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import ru.softmotion.xmlparser.service.XmlParsingService

import java.util.Scanner

/**
 * Консольный интерфейс взаимодействия с сервисом.
 */
@Component
class ConsoleRunner implements CommandLineRunner {

    private final XmlParsingService xmlService

    @Autowired
    ConsoleRunner(XmlParsingService xmlService) {
        this.xmlService = xmlService
    }

    @Override
    void run(String... args) {
        Scanner scanner = new Scanner(System.in)
        boolean running = true

        println("XML Parser Console Interface Started")
        
        while (running) {
            println("\nSelect an option:")
            println("1 - List tables")
            println("2 - Generate DDL for a table")
            println("3 - Update all tables in DB")
            println("4 - Update specific table in DB")
            println("5 - Get column names for a table")
            println("6 - Check if column is unique")
            println("7 - Get DDL changes for a table")
            println("8 - Exit")
            
            int choice = scanner.nextInt()
            scanner.nextLine() // consume newline
            
            switch (choice) {
                case 1:
                    println("Available tables:")
                    xmlService.getTableNames().each { println("- ${it}") }
                    break
                case 2:
                    print("Enter table name: ")
                    String tableName = scanner.nextLine()
                    println(xmlService.getTableDDL(tableName))
                    break
                case 3:
                    try {
                        xmlService.update()
                        println("All tables updated successfully")
                    } catch (Exception e) {
                        println("Error updating tables: ${e.message}")
                    }
                    break
                case 4:
                    print("Enter table name: ")
                    String tableToUpdate = scanner.nextLine()
                    try {
                        xmlService.update(tableToUpdate)
                        println("Table ${tableToUpdate} updated successfully")
                    } catch (Exception e) {
                        println("Error updating table: ${e.message}")
                    }
                    break
                case 5:
                    print("Enter table name: ")
                    String tableForColumns = scanner.nextLine()
                    println("Columns in ${tableForColumns}:")
                    xmlService.getColumnNames(tableForColumns).each { println("- ${it}") }
                    break
                case 6:
                    print("Enter table name: ")
                    String tableForCheck = scanner.nextLine()
                    print("Enter column name: ")
                    String columnToCheck = scanner.nextLine()
                    boolean isUnique = xmlService.isColumnId(tableForCheck, columnToCheck)
                    println("Column ${columnToCheck} in table ${tableForCheck} is ${isUnique ? 'unique' : 'not unique'}")
                    break
                case 7:
                    print("Enter table name: ")
                    String tableForChanges = scanner.nextLine()
                    String changes = xmlService.getDDLChange(tableForChanges)
                    if (changes) {
                        println("DDL changes for ${tableForChanges}: ${changes}")
                    } else {
                        println("No DDL changes detected for ${tableForChanges}")
                    }
                    break
                case 8:
                    println("Exiting...")
                    running = false
                    break
                default:
                    println("Unknown command")
            }
        }
        
        scanner.close()
        println("Console interface closed")
    }
}