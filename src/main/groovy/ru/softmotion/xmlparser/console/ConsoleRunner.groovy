package ru.softmotion.xmlparser.console

import groovy.util.logging.Slf4j
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * Точка входа консольного интерфейса приложения.
 * Отвечает только за запуск и корректное завершение CLI.
 */
@Slf4j
@Component
class ConsoleRunner implements CommandLineRunner {

    private final ConsoleCommandHandler commandHandler

    ConsoleRunner(ConsoleCommandHandler commandHandler) {
        this.commandHandler = commandHandler
    }

    /**
     * Запуск консольного интерфейса.
     *
     * @param args аргументы командной строки
     */
    @Override
    void run(String... args) {
        log.info("XML Parser console interface started")

        try {
            commandHandler.start()
        } catch (Exception e) {
            log.error("Fatal error in console interface", e)
        }

        log.info("XML Parser console interface stopped")
    }
}
