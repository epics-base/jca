/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package gov.aps.jca.configuration;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A SAXConfigurationHandler helps build Configurations out of sax events,
 * including namespace information.
 *
 * @author <a href="mailto:fede@apache.org">Federico Barbieri</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version 1.0
 */
public class NamespacedSAXConfigurationHandler
    extends SAXConfigurationHandler
{
    /**
     * Likely number of nested configuration items. If more is
     * encountered the lists will grow automatically.
     */
    private static final int EXPECTED_DEPTH = 5;
    private final ArrayList m_elements = new ArrayList( EXPECTED_DEPTH );
    private final ArrayList m_prefixes = new ArrayList( EXPECTED_DEPTH );
    private final ArrayList m_values = new ArrayList( EXPECTED_DEPTH );
    /**
     * Contains true at index n if space in the configuration with
     * depth n is to be preserved.
     */
    private final BitSet m_preserveSpace = new BitSet();
    private Configuration m_configuration;
    private Locator m_locator;
    private NamespaceSupport m_namespaceSupport = new NamespaceSupport();

    /**
     * Get the configuration object that was built.
     *
     * @return a <code>Configuration</code> object
     */
    public Configuration getConfiguration()
    {
        return m_configuration;
    }

    /**
     * Clears all data from this configuration handler.
     */
    public void clear()
    {
        m_elements.clear();
        Iterator i = m_prefixes.iterator();
        while( i.hasNext() )
        {
            ( (ArrayList)i.next() ).clear();
        }
        m_prefixes.clear();
        m_values.clear();
        m_locator = null;
    }

    /**
     * Set the document <code>Locator</code> to use.
     *
     * @param locator a <code>Locator</code> value
     */
    public void setDocumentLocator( final Locator locator )
    {
        m_locator = locator;
    }

    /**
     * Handling hook for starting the document parsing.
     *
     * @throws SAXException if an error occurs
     */
    public void startDocument()
        throws SAXException
    {
        m_namespaceSupport.reset();
        super.startDocument();
    }

    /**
     * Handling hook for ending the document parsing.
     *
     * @throws SAXException if an error occurs
     */
    public void endDocument()
        throws SAXException
    {
        super.endDocument();
        m_namespaceSupport.reset();
    }

    /**
     * Handling hook for character data.
     *
     * @param ch a <code>char[]</code> of data
     * @param start offset in the character array from which to start reading
     * @param end length of character data
     * @throws SAXException if an error occurs
     */
    public void characters( final char[] ch, int start, int end )
        throws SAXException
    {
        // it is possible to play micro-optimization here by doing
        // manual trimming and thus preserve some precious bits
        // of memory, but it's really not important enough to justify
        // resulting code complexity
        final int depth = m_values.size() - 1;
        final StringBuffer valueBuffer = (StringBuffer)m_values.get( depth );
        valueBuffer.append( ch, start, end );
    }

    /**
     * Handling hook for finishing parsing of an element.
     *
     * @param namespaceURI a <code>String</code> value
     * @param localName a <code>String</code> value
     * @param rawName a <code>String</code> value
     * @throws SAXException if an error occurs
     */
    public void endElement( final String namespaceURI,
                            final String localName,
                            final String rawName )
        throws SAXException
    {
        final int depth = m_elements.size() - 1;
        final DefaultConfiguration finishedConfiguration =
            (DefaultConfiguration)m_elements.remove( depth );
        final String accumulatedValue =
            ( (StringBuffer)m_values.remove( depth ) ).toString();
        final ArrayList prefixes = (ArrayList)m_prefixes.remove( depth );

        final Iterator i = prefixes.iterator();
        while( i.hasNext() )
        {
            endPrefixMapping( (String)i.next() );
        }
        prefixes.clear();

        if( finishedConfiguration.getChildren().length == 0 )
        {
            // leaf node
            String finishedValue;
            if( m_preserveSpace.get( depth ) )
            {
                finishedValue = accumulatedValue;
            }
            else if( 0 == accumulatedValue.length() )
            {
                finishedValue = null;
            }
            else
            {
                finishedValue = accumulatedValue.trim();
            }
            finishedConfiguration.setValue( finishedValue );
        }
        else
        {
            final String trimmedValue = accumulatedValue.trim();
            if( trimmedValue.length() > 0 )
            {
                throw new SAXException( "Not allowed to define mixed content in the " +
                                        "element " + finishedConfiguration.getName() + " at " +
                                        finishedConfiguration.getLocation() );
            }
        }

        if( 0 == depth )
        {
            m_configuration = finishedConfiguration;
        }

        m_namespaceSupport.popContext();
    }

    /**
     * Create a new <code>DefaultConfiguration</code> with the specified
     * local name, namespace, and location.
     *
     * @param localName a <code>String</code> value
     * @param namespaceURI a <code>String</code> value
     * @param location a <code>String</code> value
     * @return a <code>DefaultConfiguration</code> value
     */
    protected DefaultConfiguration createConfiguration( final String localName,
                                                        final String namespaceURI,
                                                        final String location )
    {
        String prefix = m_namespaceSupport.getPrefix( namespaceURI );
        if( prefix == null )
        {
            prefix = "";
        }
        return new DefaultConfiguration( localName, location, namespaceURI, prefix );
    }

    /**
     * Handling hook for starting parsing of an element.
     *
     * @param namespaceURI a <code>String</code> value
     * @param localName a <code>String</code> value
     * @param rawName a <code>String</code> value
     * @param attributes an <code>Attributes</code> value
     * @throws SAXException if an error occurs
     */
    public void startElement( final String namespaceURI,
                              final String localName,
                              final String rawName,
                              final Attributes attributes )
        throws SAXException
    {
        m_namespaceSupport.pushContext();
        final DefaultConfiguration configuration =
            createConfiguration( localName, namespaceURI, getLocationString() );
        // depth of new configuration (not decrementing here, configuration
        // is to be added)
        final int depth = m_elements.size();
        boolean preserveSpace = false; // top level element trims space by default

        if( depth > 0 )
        {
            final DefaultConfiguration parent =
                (DefaultConfiguration)m_elements.get( depth - 1 );
            parent.addChild( configuration );
            // inherits parent's space preservation policy
            preserveSpace = m_preserveSpace.get( depth - 1 );
        }

        m_elements.add( configuration );
        m_values.add( new StringBuffer() );

        final ArrayList prefixes = new ArrayList();
        AttributesImpl componentAttr = new AttributesImpl();

        for( int i = 0; i < attributes.getLength(); i++ )
        {
            if( attributes.getQName( i ).startsWith( "xmlns" ) )
            {
                prefixes.add( attributes.getLocalName( i ) );
                this.startPrefixMapping( attributes.getLocalName( i ),
                                         attributes.getValue( i ) );
            }
            else if ( attributes.getQName( i ).equals( "xml:space" ) )
            {
                preserveSpace = attributes.getValue( i ).equals( "preserve" );
            }
            else
            {
                componentAttr.addAttribute( attributes.getURI( i ),
                                            attributes.getLocalName( i ),
                                            attributes.getQName( i ),
                                            attributes.getType( i ),
                                            attributes.getValue( i ) );
            }
        }

        if( preserveSpace )
        {
            m_preserveSpace.set( depth );
        }
        else
        {
            m_preserveSpace.clear( depth );
        }

        m_prefixes.add( prefixes );

        final int attributesSize = componentAttr.getLength();

        for( int i = 0; i < attributesSize; i++ )
        {
            final String name = componentAttr.getQName( i );
            final String value = componentAttr.getValue( i );
            configuration.setAttribute( name, value );
        }
    }

    /**
     * This just throws an exception on a parse error.
     * @param exception the parse error
     * @throws SAXException if an error occurs
     */
    public void error( final SAXParseException exception )
        throws SAXException
    {
        throw exception;
    }

    /**
     * This just throws an exception on a parse error.
     * @param exception the parse error
     * @throws SAXException if an error occurs
     */
    public void warning( final SAXParseException exception )
        throws SAXException
    {
        throw exception;
    }

    /**
     * This just throws an exception on a parse error.
     * @param exception the parse error
     * @throws SAXException if an error occurs
     */
    public void fatalError( final SAXParseException exception )
        throws SAXException
    {
        throw exception;
    }

    /**
     * Returns a string showing the current system ID, line number and column number.
     *
     * @return a <code>String</code> value
     */
    protected String getLocationString()
    {
        if( null == m_locator )
        {
            return "Unknown";
        }
        else
        {
            return
                m_locator.getSystemId() + ":" +
                m_locator.getLineNumber() + ":" +
                m_locator.getColumnNumber();
        }
    }

    /**
     * Handling hook for starting prefix mapping.
     *
     * @param prefix a <code>String</code> value
     * @param uri a <code>String</code> value
     * @throws SAXException if an error occurs
     */
    public void startPrefixMapping( String prefix, String uri )
        throws SAXException
    {
        m_namespaceSupport.declarePrefix( prefix, uri );
        super.startPrefixMapping( prefix, uri );
    }
}
