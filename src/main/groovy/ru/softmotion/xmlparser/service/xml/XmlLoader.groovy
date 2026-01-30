package ru.softmotion.xmlparser.service.xml

import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.XMLReader

import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

/**
 * Компонент, отвечающий за безопасную загрузку и парсинг удалённого XML.
 * Ленивая и потокобезопасная инициализация root-узла.
 */
@Slf4j
@Component
class XmlLoader {

    private final String sourceUrl
    private volatile GPathResult root

    /**
     * @param sourceUrl URL XML-источника, задаётся через application.properties
     */
    XmlLoader(
            @Value('${xml.loader.source-url}') String sourceUrl
    ) {
        this.sourceUrl = sourceUrl
        log.info("XmlLoader initialized with sourceUrl={}", sourceUrl)
    }

    /**
     * Возвращает корень распарсенного XML. Парсинг выполняется один раз (lazy, double-check locking).
     *
     * @return корневой GPathResult или выбрасывает RuntimeException при ошибке
     */
    GPathResult getRoot() {
        if (root == null) {
            synchronized (this) {
                if (root == null) {
                    try {
                        log.info("Loading XML from {}", sourceUrl)
                        root = loadXmlWithXmlSlurper(sourceUrl)
                        log.info("XML loaded successfully")
                    } catch (Exception e) {
                        log.error("Failed to load XML from {}", sourceUrl, e)
                        throw new RuntimeException("Failed to parse XML from ${sourceUrl}", e)
                    }
                }
            }
        }
        return root
    }

    private static GPathResult loadXmlWithXmlSlurper(String urlString) throws Exception {
        URI uri = URI.create(urlString)
        URLConnection connection = uri.toURL().openConnection()
        connection.setConnectTimeout(10_000)
        connection.setReadTimeout(30_000)
        connection.setRequestProperty("User-Agent", "XML-Parser/1.0")

        try (InputStream is = connection.getInputStream()) {
            SAXParserFactory factory = SAXParserFactory.newInstance()
            factory.setNamespaceAware(true)

            // Безопасные опции
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)

            XMLReader xmlReader = factory.newSAXParser().getXMLReader()

            xmlReader.setEntityResolver(new EntityResolver() {
                @Override
                InputSource resolveEntity(String publicId, String systemId) {
                    log.warn("Blocked external entity resolution. publicId={}, systemId={}", publicId, systemId)
                    return new InputSource(new StringReader(""))
                }
            })

            XmlSlurper slurper = new XmlSlurper(xmlReader)
            return (GPathResult) slurper.parse(is)
        }
    }
}
