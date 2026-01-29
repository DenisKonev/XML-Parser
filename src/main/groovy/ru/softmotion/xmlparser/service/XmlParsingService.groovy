package ru.softmotion.xmlparser.service

/**
 * Интерфейс сервиса для обработки XML и управления данными в БД
 */
interface XmlParsingService {
    /**
     * Возвращает названия таблиц из XML (currency, categories, offers)
     * @return ArrayList
     */
    List<String> getTableNames()

    /**
     * Создает sql для создания таблиц динамически из XML
     * @param String tableName
     * @return String
     */
    String getTableDDL(String tableName)

    /**
     * обновляет данные в таблицах бд
     * на основе Id
     * если поменялась структура выдает exception
     **/
    void update()

    /**
     * обновляет данные в таблицах бд
     * если поменялась структура выдает exception
     * @param String tableName
     **/
    void update(String tableName)

    //наименование столбцов таблицы (динамически)
    List<String> getColumnNames(String tableName)

    //true если столбец не имеет повторяющихся значений
    boolean isColumnId(String tableName, String columnName)

    //изменения таблицы, допустимо только добавление новых столбцов
    String getDDLChange(String tableName)
}