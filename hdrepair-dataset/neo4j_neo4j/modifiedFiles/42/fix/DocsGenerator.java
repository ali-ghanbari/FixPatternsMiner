/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.GraphDefinition;
import org.neo4j.test.TestData.Producer;
import org.neo4j.visualization.graphviz.AsciiDocStyle;
import org.neo4j.visualization.graphviz.GraphvizWriter;
import org.neo4j.walk.Walker;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientRequest.Builder;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Generate asciidoc-formatted documentation from HTTP requests and responses.
 * The status and media type of all responses is checked as well as the
 * existence of any expected headers.
 */
public class DocsGenerator
{
    private static final String DOCUMENTATION_END = "\n...\n";

    private static final Builder REQUEST_BUILDER = ClientRequest.create();

    private static final List<String> RESPONSE_HEADERS = Arrays.asList( new String[] { "Content-Type", "Location" } );

    private static final List<String> REQUEST_HEADERS = Arrays.asList( new String[] { "Content-Type", "Accept" } );

    public static final Producer<DocsGenerator> PRODUCER = new Producer<DocsGenerator>()
    {
        @Override
        public DocsGenerator create( GraphDefinition graph, String title, String documentation )
        {
            return DocsGenerator.create( title )
                    .description( documentation );
        }

        @Override
        public void destroy( DocsGenerator product, boolean successful )
        {
            // TODO: invoke some complete method here?
        }
    };

    private final String title;
    private String description = null;
    private int expectedResponseStatus = -1;
    private MediaType expectedMediaType = MediaType.APPLICATION_JSON_TYPE;
    private MediaType payloadMediaType = MediaType.APPLICATION_JSON_TYPE;
    private final List<String> expectedHeaderFields = new ArrayList<String>();
    private String payload;
    private GraphDatabaseService graph;

    /**
     * Creates a documented test case. Finish building it by using one of these:
     * {@link #get(String)}, {@link #post(String)}, {@link #put(String)},
     * {@link #delete(String)}, {@link #request(ClientRequest)}. To access the
     * response, use {@link ResponseEntity#entity} to get the entity or
     * {@link ResponseEntity#response} to get the rest of the response
     * (excluding the entity).
     * 
     * @param title title of the test
     */
    public static DocsGenerator create( final String title )
    {
        if ( title == null )
        {
            throw new IllegalArgumentException( "The title can not be null" );
        }
        return new DocsGenerator( title );
    }

    private DocsGenerator( final String title )
    {
        this.title = title;
    }
    
    public DocsGenerator setGraph(GraphDatabaseService graph)
    {
        this.graph = graph;
        return this;
    }

    /**
     * Add a description to the test (in asciidoc format). Adding multiple
     * descriptions will yield one paragraph per description.
     * 
     * @param description the description
     */
    public DocsGenerator description( final String description )
    {
        if ( description == null )
        {
            throw new IllegalArgumentException( "The description can not be null" );
        }
        String content;
        int pos = description.indexOf( DOCUMENTATION_END );
        if ( pos != -1 )
        {
            content = description.substring( 0, pos );
        }
        else
        {
            content = description;
        }
        if ( this.description == null )
        {
            this.description = content;
        }
        else
        {
            this.description += "\n\n" + content;
        }
        return this;
    }

    /**
     * Set the expected status of the response. The test will fail if the
     * response has a different status. Defaults to HTTP 200 OK.
     * 
     * @param expectedResponseStatus the expected response status
     */
    public DocsGenerator expectedStatus( final int expectedResponseStatus )
    {
        this.expectedResponseStatus = expectedResponseStatus;
        return this;
    }

    /**
     * Set the expected media type of the response. The test will fail if the
     * response has a different media type. Defaults to application/json.
     * 
     * @param expectedMediaType the expected media tyupe
     */
    public DocsGenerator expectedType( final MediaType expectedMediaType )
    {
        this.expectedMediaType = expectedMediaType;
        return this;
    }

    /**
     * The media type of the request payload. Defaults to application/json.
     * 
     * @param payloadMediaType the media type to use
     */
    public DocsGenerator payloadType( final MediaType payloadMediaType )
    {
        this.payloadMediaType = payloadMediaType;
        return this;
    }

    /**
     * Set the payload of the request.
     * 
     * @param payload the payload
     */
    public DocsGenerator payload( final String payload )
    {
        this.payload = payload;
        return this;
    }

    /**
     * Add an expected response header. If the heading is missing in the
     * response the test will fail. The header and its value are also included
     * in the documentation.
     * 
     * @param expectedHeaderField the expected header
     */
    public DocsGenerator expectedHeader( final String expectedHeaderField )
    {
        this.expectedHeaderFields.add( expectedHeaderField );
        return this;
    }

    /**
     * Send a request using your own request object.
     * 
     * @param request the request to perform
     */
    public ResponseEntity request( final ClientRequest request )
    {
        return retrieveResponse( title, description, request.getURI()
                .toString(), expectedResponseStatus, expectedMediaType, expectedHeaderFields, request );
    }

    /**
     * Send a GET request.
     * 
     * @param uri the URI to use.
     */
    public ResponseEntity get( final String uri )
    {
        return retrieveResponseFromRequest( title, description, "GET", uri, expectedResponseStatus, expectedMediaType,
                expectedHeaderFields );
    }

    /**
     * Send a POST request.
     * 
     * @param uri the URI to use.
     */
    public ResponseEntity post( final String uri )
    {
        return retrieveResponseFromRequest( title, description, "POST", uri, payload, payloadMediaType,
                expectedResponseStatus, expectedMediaType, expectedHeaderFields );
    }

    /**
     * Send a PUT request.
     * 
     * @param uri the URI to use.
     */
    public ResponseEntity put( final String uri )
    {
        return retrieveResponseFromRequest( title, description, "PUT", uri, payload, payloadMediaType,
                expectedResponseStatus, expectedMediaType, expectedHeaderFields );
    }

    /**
     * Send a DELETE request.
     * 
     * @param uri the URI to use.
     */
    public ResponseEntity delete( final String uri )
    {
        return retrieveResponseFromRequest( title, description, "DELETE", uri, payload, payloadMediaType,
                expectedResponseStatus, expectedMediaType, expectedHeaderFields );
    }

    /**
     * Send a request with no payload.
     */
    private ResponseEntity retrieveResponseFromRequest( final String title, final String description,
            final String method, final String uri, final int responseCode, final MediaType accept,
            final List<String> headerFields )
    {
        ClientRequest request;
        try
        {
            request = REQUEST_BUILDER.accept( accept )
                    .build( new URI( uri ), method );
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException( e );
        }
        return retrieveResponse( title, description, uri, responseCode, accept, headerFields, request );
    }

    /**
     * Send a request with payload.
     */
    private ResponseEntity retrieveResponseFromRequest( final String title, final String description,
            final String method, final String uri, final String payload, final MediaType payloadType,
            final int responseCode, final MediaType accept, final List<String> headerFields )
    {
        ClientRequest request;
        try
        {
            if ( payload != null )
            {
                request = REQUEST_BUILDER.type( payloadType )
                        .accept( accept )
                        .entity( payload )
                        .build( new URI( uri ), method );
            }
            else
            {
                request = REQUEST_BUILDER.accept( accept )
                        .build( new URI( uri ), method );
            }
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException( e );
        }
        return retrieveResponse( title, description, uri, responseCode, accept, headerFields, request );
    }

    /**
     * Send the request and create the documentation.
     */
    private ResponseEntity retrieveResponse( final String title, final String description, final String uri,
            final int responseCode, final MediaType type, final List<String> headerFields, final ClientRequest request )
    {
        DocumentationData data = new DocumentationData();
        getRequestHeaders( data, request.getHeaders() );
        if ( request.getEntity() != null )
        {
            data.setPayload( String.valueOf( request.getEntity() ) );
        }
        Client client = new Client();
        ClientResponse response = client.handle( request );
        assertEquals( responseCode, response.getStatus() );
        if ( response.getType() != null )
        {
            assertEquals( type, response.getType() );
        }
        for ( String headerField : headerFields )
        {
            assertNotNull( response.getHeaders()
                    .get( headerField ) );
        }
        data.setTitle( title );
        data.setDescription( description );
        data.setMethod( request.getMethod() );
        data.setUri( uri );
        data.setStatus( responseCode );
        if ( response.hasEntity() && response.getStatus() != 204 )
        {
            data.setEntity( response.getEntity( String.class ) );
        }
        getResponseHeaders( data, response.getHeaders(), headerFields );
        document( data );
        return new ResponseEntity( response, data.entity );
    }

    private void getResponseHeaders( final DocumentationData data, final MultivaluedMap<String, String> headers,
            final List<String> additionalFilter )
    {
        data.setResponseHeaders( getHeaders( headers, RESPONSE_HEADERS, additionalFilter ) );
    }

    private void getRequestHeaders( final DocumentationData data, final MultivaluedMap<String, Object> headers )
    {
        data.setRequestHeaders( getHeaders( headers, REQUEST_HEADERS, Collections.<String>emptyList() ) );
    }

    private <T> Map<String, String> getHeaders( final MultivaluedMap<String, T> headers, final List<String> filter,
            final List<String> additionalFilter )
    {
        Map<String, String> filteredHeaders = new TreeMap<String, String>();
        for ( Entry<String, List<T>> header : headers.entrySet() )
        {
            if ( filter.contains( header.getKey() ) || additionalFilter.contains( header.getKey() ) )
            {
                String values = "";
                for ( T value : header.getValue() )
                {
                    if ( !values.isEmpty() )
                    {
                        values += ", ";
                    }
                    values += String.valueOf( value );
                }
                filteredHeaders.put( header.getKey(), values );
            }
        }
        return filteredHeaders;
    }

    /**
     * Wraps a response, to give access to the response entity as well.
     */
    public static class ResponseEntity
    {
        private final String entity;
        private final JaxRsResponse response;

        public ResponseEntity( ClientResponse response, String entity )
        {
            this.response = new JaxRsResponse(response,entity);
            this.entity = entity;
        }

        /**
         * The response entity as a String.
         */
        public String entity()
        {
            return entity;
        }

        /**
         * Note that the response object returned does not give access to the
         * response entity.
         */
        public JaxRsResponse response()
        {
            return response;
        }
    }

    private class DocumentationData
    {
        public String payload;
        public String title;
        public String description;
        public String uri;
        public String method;
        public int status;
        public String entity;
        public Map<String, String> requestHeaders;
        public Map<String, String> responseHeaders;

        public void setPayload( final String payload )
        {
            this.payload = payload;
        }

        public void setDescription( final String description )
        {
            this.description = description;
        }

        public void setTitle( final String title )
        {
            this.title = title;
        }

        public void setUri( final String uri )
        {
            this.uri = uri;
        }

        public void setMethod( final String method )
        {
            this.method = method;
        }

        public void setStatus( final int responseCode )
        {
            this.status = responseCode;

        }

        public void setEntity( final String entity )
        {
            this.entity = entity;
        }

        public void setResponseHeaders( final Map<String, String> response )
        {
            responseHeaders = response;
        }

        public void setRequestHeaders( final Map<String, String> request )
        {
            requestHeaders = request;
        }

        @Override
        public String toString()
        {
            return "DocumentationData [payload=" + payload + ", title=" + title + ", description=" + description
                   + ", uri=" + uri + ", method=" + method + ", status=" + status + ", entity=" + entity
                   + ", requestHeaders=" + requestHeaders + ", responseHeaders=" + responseHeaders + "]";
        }
    }

    protected void document( final DocumentationData data )
    {
        FileWriter fw = null;
        try
        {
            File dirs = new File( "target/rest-api" );
            if ( !dirs.exists() )
            {
                dirs.mkdirs();
            }
            String name = data.title.replace( " ", "-" )
                    .toLowerCase();
            File out = new File( dirs, name + ".txt" );
            if ( out.exists() )
            {
                out.delete();
            }
            if ( !out.createNewFile() )
            {
                throw new RuntimeException( "File exists: " + out.getAbsolutePath() );
            }

            fw = new FileWriter( out, false );

            line( fw, "[[rest-api-" + name.replaceAll( "\\(|\\)", "" ) + "]]" );
            line( fw, "=== " + data.title + " ===" );
            line( fw, "" );
            if ( data.description != null && !data.description.isEmpty() )
            {
                line( fw, data.description );
                line( fw, "" );
            }
            if( graph != null) {
                line(fw, "[\"dot\", \""+name+".svg\", \"neoviz\"]");
                fw.append( createGraphViz( graph ));
                line(fw, "" );
            }
            line( fw, "_Example request_" );
            line( fw, "" );
            line( fw, "* *+" + data.method + "+*  +" + data.uri + "+" );
            if ( data.requestHeaders != null )
            {
                for ( Entry<String, String> header : data.requestHeaders.entrySet() )
                {
                    line( fw, "* *+" + header.getKey() + ":+* +" + header.getValue() + "+" );
                }
            }
            writeEntity( fw, data.payload );
            line( fw, "" );
            line( fw, "_Example response_" );
            line( fw, "" );
            line( fw, "* *+" + data.status + ":+* +" + Response.Status.fromStatusCode( data.status )
                    .toString() + "+" );
            if ( data.responseHeaders != null )
            {
                for ( Entry<String, String> header : data.responseHeaders.entrySet() )
                {
                    line( fw, "* *+" + header.getKey() + ":+* +" + header.getValue() + "+" );
                }
            }
            writeEntity( fw, data.entity );
            line( fw, "" );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            fail();
        }
        finally
        {
            if ( fw != null )
            {
                try
                {
                    fw.close();
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                    fail();
                }
            }
        }
    }

    private void writeEntity( final FileWriter fw, final String entity ) throws IOException
    {
        if ( entity != null )
        {
            line( fw, "[source,javascript]" );
            line( fw, "----" );
            line( fw, entity );
            line( fw, "----" );
            line( fw, "" );
        }
    }

    private void line( final FileWriter fw, final String string ) throws IOException
    {
        fw.append( string );
        fw.append( "\n" );
    }
    
    public String createGraphViz( GraphDatabaseService graphDatabaseService )
    {
        OutputStream out = new ByteArrayOutputStream();
        GraphvizWriter writer = new GraphvizWriter(new AsciiDocStyle());
        try
        {
            writer.emit( out, Walker.fullGraph( graphDatabaseService ) );
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "----\n" +
                out.toString() +
                "----\n";
    }
    

}
