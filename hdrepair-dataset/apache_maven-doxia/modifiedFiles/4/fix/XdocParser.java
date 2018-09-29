package org.apache.maven.doxia.module.xdoc;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;

import org.apache.maven.doxia.macro.MacroExecutionException;
import org.apache.maven.doxia.macro.manager.MacroNotFoundException;
import org.apache.maven.doxia.macro.MacroRequest;
import org.apache.maven.doxia.parser.ParseException;
import org.apache.maven.doxia.parser.XhtmlBaseParser;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributeSet;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Parse an xdoc model and emit events into the specified doxia Sink.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @since 1.0
 * @plexus.component role="org.apache.maven.doxia.parser.Parser" role-hint="xdoc"
 */
public class XdocParser
    extends XhtmlBaseParser
    implements XdocMarkup
{
    /** The source content of the input reader. Used to pass into macros. */
    private String sourceContent;

    /** True if a &lt;script&gt;&lt;/script&gt; block is read. CDATA sections within are handled as rawText. */
    private boolean scriptBlock;

    /** Empty elements don't write a closing tag. */
    private boolean isEmptyElement;

    /** A macro name. */
    private String macroName;

    /** The macro parameters. */
    private Map macroParameters = new HashMap();


    /** {@inheritDoc} */
    public void parse( Reader source, Sink sink )
        throws ParseException
    {
        try
        {
            StringWriter contentWriter = new StringWriter();
            IOUtil.copy( source, contentWriter );
            sourceContent = contentWriter.toString();
        }
        catch ( IOException ex )
        {
            throw new ParseException( "Error reading the input source: " + ex.getMessage(), ex );
        }
        finally
        {
            IOUtil.close( source );
        }

        Reader tmp = new StringReader( sourceContent );

        // leave this at default (false) until everything is properly implemented, see DOXIA-226
        //setIgnorableWhitespace( true );

        super.parse( tmp, sink );
    }

    /** {@inheritDoc} */
    protected void handleStartTag( XmlPullParser parser, Sink sink )
        throws XmlPullParserException, MacroExecutionException
    {
        isEmptyElement = parser.isEmptyElementTag();

        SinkEventAttributeSet attribs = getAttributesFromParser( parser );

        if ( parser.getName().equals( DOCUMENT_TAG.toString() ) )
        {
            //Do nothing
            return;
        }
        else if ( parser.getName().equals( Tag.HEAD.toString() ) )
        {
            sink.head( attribs );
        }
        else if ( parser.getName().equals( Tag.TITLE.toString() ) )
        {
            sink.title( attribs );
        }
        else if ( parser.getName().equals( AUTHOR_TAG.toString() ) )
        {
            sink.author( attribs );
        }
        else if ( parser.getName().equals( DATE_TAG.toString() ) )
        {
            sink.date( attribs );
        }
        else if ( parser.getName().equals( Tag.BODY.toString() ) )
        {
            sink.body( attribs );
        }
        else if ( parser.getName().equals( SECTION_TAG.toString() ) )
        {
            consecutiveSections( Sink.SECTION_LEVEL_1, sink );

            Object id = attribs.getAttribute( Attribute.ID.toString() );
            if ( id != null )
            {
                sink.anchor( id.toString() );
                sink.anchor_();
            }

            sink.section( Sink.SECTION_LEVEL_1, attribs );

            sink.sectionTitle( Sink.SECTION_LEVEL_1, attribs );

            sink.text( parser.getAttributeValue( null, Attribute.NAME.toString() ) );

            sink.sectionTitle1_();
        }
        else if ( parser.getName().equals( SUBSECTION_TAG.toString() ) )
        {
            consecutiveSections( Sink.SECTION_LEVEL_2, sink );

            Object id = attribs.getAttribute( Attribute.ID.toString() );
            if ( id != null )
            {
                sink.anchor( id.toString() );
                sink.anchor_();
            }

            sink.section( Sink.SECTION_LEVEL_2, attribs );

            sink.sectionTitle( Sink.SECTION_LEVEL_2, attribs );

            sink.text( parser.getAttributeValue( null, Attribute.NAME.toString() ) );

            sink.sectionTitle2_();
        }
        else if ( parser.getName().equals( SOURCE_TAG.toString() ) )
        {
            verbatim();

            attribs.addAttributes( SinkEventAttributeSet.BOXED );

            sink.verbatim( attribs );
        }
        else if ( parser.getName().equals( PROPERTIES_TAG.toString() ) )
        {
            sink.head();
        }

        // ----------------------------------------------------------------------
        // Macro
        // ----------------------------------------------------------------------

        else if ( parser.getName().equals( MACRO_TAG.toString() ) )
        {
            if ( !isSecondParsing() )
            {
                macroName = parser.getAttributeValue( null, Attribute.NAME.toString() );

                if ( macroParameters == null )
                {
                    macroParameters = new HashMap();
                }

                if ( StringUtils.isEmpty( macroName ) )
                {
                    throw new MacroExecutionException( "The '" + Attribute.NAME.toString() + "' attribute for the '"
                        + MACRO_TAG.toString() + "' tag is required." );
                }
            }
        }
        else if ( parser.getName().equals( Tag.PARAM.toString() ) )
        {
            if ( !isSecondParsing() )
            {
                if ( StringUtils.isNotEmpty( macroName ) )
                {
                    String paramName = parser.getAttributeValue( null, Attribute.NAME.toString() );
                    String paramValue = parser.getAttributeValue( null, Attribute.VALUE.toString() );

                    if ( StringUtils.isEmpty( paramName ) || StringUtils.isEmpty( paramValue ) )
                    {
                        throw new MacroExecutionException( "'" + Attribute.NAME.toString() + "' and '"
                            + Attribute.VALUE.toString() + "' attributes for the '" + Tag.PARAM.toString()
                            + "' tag are required inside the '" + MACRO_TAG.toString() + "' tag." );
                    }

                    macroParameters.put( paramName, paramValue );
                }
                else
                {
                    // param tag from non-macro object, see MSITE-288
                    handleUnknown( parser, sink, TAG_TYPE_START );
                }
            }
        }
        else if ( parser.getName().equals( Tag.SCRIPT.toString() ) )
        {
            handleUnknown( parser, sink, TAG_TYPE_START );
            scriptBlock = true;
        }
        else if ( !baseStartTag( parser, sink ) )
        {
            if ( isEmptyElement )
            {
                handleUnknown( parser, sink, TAG_TYPE_SIMPLE );
            }
            else
            {
                handleUnknown( parser, sink, TAG_TYPE_START );
            }

            if ( getLog().isDebugEnabled() )
            {
                String position = "[" + parser.getLineNumber() + ":"
                    + parser.getColumnNumber() + "]";
                String tag = "<" + parser.getName() + ">";

                getLog().debug( "Unrecognized xdoc tag: " + tag + " at " + position );
            }
        }
    }

    /** {@inheritDoc} */
    protected void handleEndTag( XmlPullParser parser, Sink sink )
        throws XmlPullParserException, MacroExecutionException
    {
        if ( parser.getName().equals( DOCUMENT_TAG.toString() ) )
        {
            //Do nothing
            return;
        }
        else if ( parser.getName().equals( Tag.HEAD.toString() ) )
        {
            sink.head_();
        }
        else if ( parser.getName().equals( Tag.BODY.toString() ) )
        {
            consecutiveSections( 0, sink );

            sink.body_();
        }
        else if ( parser.getName().equals( Tag.TITLE.toString() ) )
        {
            sink.title_();
        }
        else if ( parser.getName().equals( AUTHOR_TAG.toString() ) )
        {
            sink.author_();
        }
        else if ( parser.getName().equals( DATE_TAG.toString() ) )
        {
            sink.date_();
        }
        else if ( parser.getName().equals( SOURCE_TAG.toString() ) )
        {
            verbatim_();

            sink.verbatim_();
        }
        else if ( parser.getName().equals( PROPERTIES_TAG.toString() ) )
        {
            sink.head_();
        }

        // ----------------------------------------------------------------------
        // Macro
        // ----------------------------------------------------------------------

        else if ( parser.getName().equals( MACRO_TAG.toString() ) )
        {
            if ( !isSecondParsing() )
            {
                if ( StringUtils.isNotEmpty( macroName ) )
                {
                    // TODO handles specific macro attributes
                    macroParameters.put( "sourceContent", sourceContent );

                    XdocParser xdocParser = new XdocParser();
                    xdocParser.setSecondParsing( true );
                    macroParameters.put( "parser", xdocParser );

                    MacroRequest request = new MacroRequest( macroParameters, getBasedir() );

                    try
                    {
                        executeMacro( macroName, request, sink );
                    }
                    catch ( MacroNotFoundException me )
                    {
                        throw new MacroExecutionException( "Macro not found: " + macroName, me );
                    }
                }
            }

            // Reinit macro
            macroName = null;
            macroParameters = null;
        }
        else if ( parser.getName().equals( Tag.PARAM.toString() ) )
        {
            if ( !StringUtils.isNotEmpty( macroName ) )
            {
                handleUnknown( parser, sink, TAG_TYPE_END );
            }
        }
        else if ( parser.getName().equals( SECTION_TAG.toString() ) )
        {
            consecutiveSections( 0, sink );

            sink.section1_();
        }
        else if ( parser.getName().equals( SUBSECTION_TAG.toString() ) )
        {
            consecutiveSections( Sink.SECTION_LEVEL_1, sink );
        }
        else if ( parser.getName().equals( Tag.SCRIPT.toString() ) )
        {
            handleUnknown( parser, sink, TAG_TYPE_END );

            scriptBlock = false;
        }
        else if ( !baseEndTag( parser, sink ) )
        {
            if ( !isEmptyElement )
            {
                handleUnknown( parser, sink, TAG_TYPE_END );
            }
        }

        isEmptyElement = false;
    }

    /** {@inheritDoc} */
    protected void handleCdsect( XmlPullParser parser, Sink sink )
        throws XmlPullParserException
    {
        String text = getText( parser );

        if ( scriptBlock )
        {
            sink.rawText( text );
        }
        else
        {
            sink.text( text );
        }
    }

    /** {@inheritDoc} */
    protected void consecutiveSections( int newLevel, Sink sink )
    {
        closeOpenSections( newLevel, sink );
        openMissingSections( newLevel, sink );

        setSectionLevel( newLevel );
    }

    /**
     * Close open h4, h5, h6 sections.
     */
    private void closeOpenSections( int newLevel, Sink sink )
    {
        while ( getSectionLevel() >= newLevel )
        {
            if ( getSectionLevel() == Sink.SECTION_LEVEL_5 )
            {
                sink.section5_();
            }
            else if ( getSectionLevel() == Sink.SECTION_LEVEL_4 )
            {
                sink.section4_();
            }
            else if ( getSectionLevel() == Sink.SECTION_LEVEL_3 )
            {
                sink.section3_();
            }
            else if ( getSectionLevel() == Sink.SECTION_LEVEL_2 )
            {
                sink.section2_();
            }

            setSectionLevel( getSectionLevel() - 1 );
        }
    }

    /**
     * Open missing h4, h5, h6 sections.
     */
    private void openMissingSections( int newLevel, Sink sink )
    {
        while ( getSectionLevel() < newLevel - 1 )
        {
            setSectionLevel( getSectionLevel() + 1 );

            if ( getSectionLevel() == Sink.SECTION_LEVEL_5 )
            {
                sink.section5();
            }
            else if ( getSectionLevel() == Sink.SECTION_LEVEL_4 )
            {
                sink.section4();
            }
            else if ( getSectionLevel() == Sink.SECTION_LEVEL_3 )
            {
                sink.section3();
            }
            else if ( getSectionLevel() == Sink.SECTION_LEVEL_2 )
            {
                sink.section2();
            }
        }
    }
}
