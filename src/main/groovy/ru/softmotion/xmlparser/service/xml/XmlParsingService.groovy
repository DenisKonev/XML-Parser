package ru.softmotion.xmlparser.service.xml

/**
 * Интерфейс сервиса для обработки XML и управления данными в БД.
 * <p>
 * Методы описаны лаконично: получение списка таблиц, генерация DDL,
 * обновление данных и диагностические методы.
 */
interface XmlParsingService {

    /**
     * Возвращает список таблиц, которые присутствуют в XML-данных.
     *
     * @return список имён таблиц (например: currencies, categories, offers)
     */
    List<String> getTableNames()

    /**
     * Сформировать SQL DDL для указанной таблицы на основе структуры XML.
     *
     * @param tableName имя таблицы
     * @return SQL CREATE TABLE statement
     */
    String getTableDDL(String tableName)

    /**
     * Обновить все таблицы в базе по данным из XML.
     * Осуществляется проверка несовместимых изменений структуры (в этом случае бросается исключение).
     */
    void update()

    /**
     * Обновить данные для конкретной таблицы.
     * @param tableName имя таблицы
     */
    void update(String tableName)

    /**
     * Получить список динамических колонок для таблицы на основании XML.
     *
     * @param tableName имя таблицы
     * @return список имён колонок
     */
    List<String> getColumnNames(String tableName)

    /**
     * Проверяет, является ли колонка уникальной (no duplicates) в текущей базе.
     *
     * @param tableName  имя таблицы
     * @param columnName имя колонки
     * @return true, если все значения в колонке уникальны или таблица пуста
     */
    boolean isColumnId(String tableName, String columnName)

    /**
     * Проверяет, есть ли несовместимые DDL-изменения между ожидаемой структурой (XML) и фактической (DB).
     * Разрешено только добавление новых колонок — удаление/переименование считается несовместимым.
     *
     * @param tableName имя таблицы
     * @return текст описания изменений или null, если изменений нет
     */
    String getDDLChange(String tableName)
}
