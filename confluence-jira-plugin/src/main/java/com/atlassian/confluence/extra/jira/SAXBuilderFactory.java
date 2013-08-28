package com.atlassian.confluence.extra.jira;

import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class SAXBuilderFactory
{
    private static InputSource EMPTY_INPUT_SOURCE = new InputSource(new ByteArrayInputStream(new byte[0]));

    private static EntityResolver emptyEntityResolver = new EntityResolver()
    {
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
        {
            return EMPTY_INPUT_SOURCE;
        }
    };

    static XMLReader createNamespaceAwareXmlReader()
    {
        try{
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            XMLReader xr = spf.newSAXParser().getXMLReader();
            xr.setEntityResolver(emptyEntityResolver);
            return xr;
        }
        catch (ParserConfigurationException e)
        {
            throw new RuntimeException(e);
        }
        catch (SAXException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static SAXBuilder createSAXBuilder()
    {
        return new SAXBuilder() {
            @Override
            protected XMLReader createParser() throws JDOMException
            {
                return createNamespaceAwareXmlReader();
            }
        };        
    }
}
