package ru.softmotion.xmlparser.service.jdbc

import groovy.util.logging.Slf4j
import groovy.xml.slurpersupport.GPathResult
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import ru.softmotion.xmlparser.service.xml.XmlLoader

/**
 * Компонент, который отвечает за вставку данных в таблицы и очистку таблиц.
 * Используется JdbcTemplate для выполнения DML.
 */
@Slf4j
@Component
class DataInserter {

    private final JdbcTemplate jdbcTemplate
    private final XmlLoader xmlLoader

    DataInserter(JdbcTemplate jdbcTemplate, XmlLoader xmlLoader) {
        this.jdbcTemplate = jdbcTemplate
        this.xmlLoader = xmlLoader
    }

    /**
     * Удаляет все данные в таблице и сбрасывает идентичности.
     *
     * @param tableName имя таблицы
     */
    void clearTable(String tableName) {
        log.info("Clearing table {}", tableName)
        jdbcTemplate.execute("TRUNCATE TABLE ${tableName} RESTART IDENTITY CASCADE")
    }

    /**
     * Вставляет все данные из XML в указанную таблицу.
     *
     * @param tableName имя таблицы
     */
    void insertData(String tableName) {
        log.info("Inserting data into {}", tableName)
        switch (tableName) {
            case "currencies":
                insertCurrencyData()
                break
            case "categories":
                insertCategoryData()
                break
            case "offers":
                insertOfferData()
                break
            default:
                log.warn("No inserter defined for table {}", tableName)
        }
    }

    private void insertCurrencyData() {
        GPathResult root = xmlLoader.getRoot()
        root?.shop?.currencies?.currency?.each { GPathResult currency ->
            Map<String, Object> params = [:]
            currency.attributes().each { String attrName, Object attrValue ->
                params[attrName] = attrValue
            }
            insertRow("currencies", params)
        }
    }

    private void insertCategoryData() {
        GPathResult root = xmlLoader.getRoot()
        root?.shop?.categories?.category?.each { GPathResult category ->
            Map<String, Object> params = [:]
            category.attributes().each { String attrName, Object attrValue ->
                params[attrName] = attrValue
            }
            String text = category.text()
            if (text) params["text_content"] = text
            insertRow("categories", params)
        }
    }

    private void insertOfferData() {
        GPathResult root = xmlLoader.getRoot()
        root?.shop?.offers?.offer?.each { GPathResult offer ->
            Map<String, Object> params = [:]
            offer.attributes().each { String attrName, Object attrValue ->
                params[attrName] = attrValue
            }
            offer.children().each { Object child ->
                String name = child.name()
                if (name) params[name] = child.text()
            }
            insertRow("offers", params)
        }
    }

    /**
     * Универсальный инсертер строки в таблицу. Если params пустые — ничего не делает.
     */
    private void insertRow(String tableName, Map<String, Object> params) {
        if (!params || params.isEmpty()) {
            log.debug("No params to insert for table {}", tableName)
            return
        }
        String sql = "INSERT INTO ${tableName} (${params.keySet().join(', ')}) VALUES (${params.keySet().collect{'?'}.join(', ')})"
        jdbcTemplate.update(sql, params.values().toArray())
    }
}
