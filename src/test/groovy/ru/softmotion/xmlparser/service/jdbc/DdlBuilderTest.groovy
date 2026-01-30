package ru.softmotion.xmlparser.service.jdbc

import spock.lang.Specification

/**
 * Unit tests for DdlBuilder
 */
class DdlBuilderTest extends Specification {

    def "should convert plural to singular correctly"() {
        given:
        def method = DdlBuilder.class.getDeclaredMethod("toSingular", String)
        method.setAccessible(true)

        expect:
        method.invoke(null, "categories") == "category"
        method.invoke(null, "currencies") == "currency"
        method.invoke(null, "offers") == "offer"
        method.invoke(null, "test") == "test"
        method.invoke(null, "cats") == "cat"
        method.invoke(null, "dogs") == "dog"
    }

    def "should build correct DDL for table"() {
        given:
        def ddlBuilder = new DdlBuilder()

        when:
        def ddl = DdlBuilder.buildCreateTable("offers", ["id", "name", "price"])

        then:
        ddl.contains("CREATE TABLE IF NOT EXISTS offers")
        ddl.contains("offer_id BIGSERIAL PRIMARY KEY")
        ddl.contains("id TEXT")
        ddl.contains("name TEXT")
        ddl.contains("price TEXT")
    }

    def "should build DDL with no columns"() {
        given:
        def ddlBuilder = new DdlBuilder()

        when:
        def ddl = DdlBuilder.buildCreateTable("empty_table", [])

        then:
        ddl.contains("CREATE TABLE IF NOT EXISTS empty_table")
        ddl.contains("empty_table_id BIGSERIAL PRIMARY KEY")
        !ddl.contains("TEXT")  // No TEXT columns since no columns provided
    }
}