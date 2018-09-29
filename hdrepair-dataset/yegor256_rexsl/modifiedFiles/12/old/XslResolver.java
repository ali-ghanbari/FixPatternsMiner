/**
 * Copyright (c) 2011, ReXSL.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the ReXSL.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rexsl.core;

import com.ymock.util.Logger;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.validation.SchemaFactory;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * Provider of JAXB {@link Marshaller} for JAX-RS framework.
 *
 * <p>You don't need to use this class directly. It is made public only becuase
 * JAX-RS implementation should be able to discover it in classpath.
 *
 * @author Yegor Bugayenko (yegor@rexsl.com)
 * @author Krzysztof Krason (Krzysztof.Krason@gmail.com)
 * @version $Id$
 * @since 0.2
 */
@Provider
@Produces({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
public final class XslResolver implements ContextResolver<Marshaller> {

    /**
     * Folder with XSD files.
     * @see #setServletContext(ServletContext)
     */
    private transient File xsdFolder;

    /**
     * Classes to process.
     */
    private final transient List<Class> classes = new ArrayList<Class>();

    /**
     * JAXB context.
     */
    private transient JAXBContext context;

    /**
     * Set servlet context from container, to be called by JAX-RS framework
     * because of {@link Context} annotation.
     * @param ctx The context
     */
    @Context
    public void setServletContext(final ServletContext ctx) {
        assert ctx != null : "ServletContext can't be NULL";
        final String name = ctx.getInitParameter("com.rexsl.core.XSD_FOLDER");
        if (name != null) {
            this.xsdFolder = new File(name);
            Logger.debug(
                this,
                "#setServletContext(%s): XSD folder set to '%s'",
                ctx.getClass().getName(),
                this.xsdFolder
            );
        }
        Logger.debug(
            this,
            "#setServletContext(%s): context injected by JAX-RS",
            ctx.getClass().getName()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Marshaller getContext(final Class<?> type) {
        Marshaller mrsh;
        try {
            mrsh = this.buildContext(type).createMarshaller();
            mrsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            final String header = String.format(
                "\n<?xml-stylesheet type='text/xml' href='%s'?>",
                StringEscapeUtils.escapeXml(this.stylesheet(type))
            );
            mrsh.setProperty("com.sun.xml.bind.xmlHeaders", header);
        } catch (javax.xml.bind.JAXBException ex) {
            throw new IllegalStateException(ex);
        }
        if (this.xsdFolder == null) {
            Logger.debug(
                this,
                "#getContext(%s): marshaller created (no XSD validator)",
                type.getName()
            );
        } else {
            mrsh = this.addXsdValidator(mrsh, type);
        }
        return mrsh;
    }

    /**
     * Add new class to context.
     * @param cls The class we should add
     */
    public void add(final Class cls) {
        synchronized (this) {
            if (!this.classes.contains(cls)) {
                try {
                    this.classes.add(cls);
                    this.context = JAXBContext.newInstance(
                        this.classes.toArray(new Class[] {})
                    );
                    Logger.info(
                        this,
                        "#add(%s): added to JAXBContext (%d total)",
                        cls.getName(),
                        this.classes.size()
                    );
                } catch (javax.xml.bind.JAXBException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
    }

    /**
     * Create and return a context.
     * @param cls The class we should process
     * @return The context
     */
    private JAXBContext buildContext(final Class cls) {
        this.add(cls);
        return this.context;
    }

    /**
     * Returns the name of XSL stylesheet for this type.
     * @param type The class
     * @return The name of stylesheet
     * @see #getContext(Class)
     */
    private String stylesheet(final Class<?> type) {
        final Annotation antn = type.getAnnotation(Stylesheet.class);
        String stylesheet;
        if (antn == null) {
            stylesheet = String.format(
                "/xsl/%s.xsl",
                type.getSimpleName()
            );
        } else {
            stylesheet = ((Stylesheet) antn).value();
        }
        Logger.debug(
            this,
            "#stylesheet(%s): '%s' stylesheet discovered",
            type.getName(),
            stylesheet
        );
        return stylesheet;
    }

    /**
     * Configure marhaller and return a new one (or the same).
     * @param mrsh The marshaller, already created and ready to marshal
     * @param type The class to be marshalled
     * @return New marshalled to be used instead
     * @see #getContext(Class)
     */
    private Marshaller addXsdValidator(final Marshaller mrsh,
        final Class<?> type) {
        final String name = this.schema(type);
        if (name.isEmpty()) {
            Logger.debug(
                this,
                "Schema validation turned off for class '%s'",
                type.getName()
            );
        } else {
            final File xsd = new File(this.xsdFolder, name);
            if (xsd.exists()) {
                final SchemaFactory factory = SchemaFactory.newInstance(
                    XMLConstants.W3C_XML_SCHEMA_NS_URI
                );
                try {
                    mrsh.setSchema(factory.newSchema(xsd));
                } catch (org.xml.sax.SAXException ex) {
                    throw new IllegalStateException(
                        String.format(
                            "Failed to use XSD schema from '%s' for class '%s'",
                            xsd,
                            type.getName()
                        ),
                        ex
                    );
                }
                try {
                    mrsh.setEventHandler(new XsdEventHandler());
                } catch (javax.xml.bind.JAXBException ex) {
                    throw new IllegalStateException(ex);
                }
                Logger.debug(
                    this,
                    "Class '%s' will be validated with '%s' schema",
                    type.getName(),
                    xsd
                );
            } else {
                Logger.warn(
                    this,
                    "No XSD schema for class '%s' in '%s' file",
                    type.getName(),
                    xsd
                );
            }
        }
        return mrsh;
    }

    /**
     * Returns the name of XSD schema for this type.
     * @param type The class
     * @return The name of XSD file
     */
    private String schema(final Class<?> type) {
        final Annotation antn = type.getAnnotation(Schema.class);
        String schema;
        if (antn == null) {
            schema = String.format("%s.xsd", type.getName());
        } else {
            if (((Schema) antn).ignore()) {
                schema = "";
            } else {
                schema = ((Schema) antn).value();
            }
        }
        Logger.debug(
            this,
            "#schema(%s): '%s' schema discovered",
            type.getName(),
            schema
        );
        return schema;
    }

}
