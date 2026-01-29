# Тестовое задание

**Задача:** разработать сервис или класс для обработки XML.

**Исходные данные:**
*   Источник XML (читать по HTTPS): `https://expro.ru/bitrix/catalog_export/export_Sai.xml`
*   Библиотека для парсинга: `groovy.xml.XmlSlurper`
*   База данных: `PostgreSQL`
*   Способ обновления БД: `JDBC`
*   Предположение: столбец `offers.vendorCode` является уникальным.

**Требования к интерфейсу:** минимальный консольный интерфейс (интерактивный, с параметрами запуска или просто `main()`-метод, демонстрирующий работу всех функций).

## Обязательные функции

```java
/**
 * Возвращает названия таблиц из XML (currency, categories, offers)
 * @return ArrayList
 */
String getTableNames();

/**
 * Создает SQL для создания таблиц динамически из XML
 * @param tableName Таблица БД
 * @return String
 */
String getTableDDL(String tableName);

/**
 * Обновляет данные в таблицах БД на основе Id.
 * Если структура XML изменилась — выбрасывает исключение.
 */
void update();

/**
 * Обновляет данные в указанной таблице БД.
 * Если структура XML изменилась — выбрасывает исключение.
 * @param tableName Таблица БД
 */
void update(String tableName);
```

## Функции по желанию

```java
// Возвращает наименования столбцов таблицы (динамически)
ArrayList getColumnNames(String tableName);

// Возвращает true, если столбец не имеет повторяющихся значений
boolean isColumnId(String tableName, String columnName);

// Возвращает SQL для изменения таблицы (допустимо только добавление новых столбцов)
String getDDLChange(String tableName);
```