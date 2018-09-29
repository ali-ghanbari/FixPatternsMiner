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

import com.rexsl.test.XhtmlConverter;
import java.io.StringWriter;
import javax.ws.rs.ext.ContextResolver;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.xmlmatchers.XmlMatchers;

/**
 * Test case for {@link XslResolver}.
 * @author Yegor Bugayenko (yegor@rexsl.com)
 * @author Krzysztof Krason (Krzysztof.Krason@gmail.com)
 * @version $Id$
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ XslResolver.class, JAXBContext.class })
public final class XslResolverTest {

    /**
     * XslResolver can instantiate a marshaller.
     * @throws Exception If something goes wrong
     */
    @Test
    public void instantiatesMarshaller() throws Exception {
        final ContextResolver<Marshaller> resolver = new XslResolver();
        final Marshaller mrsh = resolver.getContext(XslResolverTest.Page.class);
        MatcherAssert.assertThat(mrsh, Matchers.notNullValue());
    }

    /**
     * XslResolver throws exception when Marshaller throws.
     * @throws Exception If something goes wrong
     */
    @Test(expected = IllegalStateException.class)
    public void throwsExceptionWhenMarshallerReports() throws Exception {
        PowerMockito.mockStatic(JAXBContext.class);
        Mockito.when(JAXBContext.newInstance(Mockito.any(Class.class)))
            .thenThrow(new JAXBException(""));
        new XslResolver().getContext(Object.class);
    }

    /**
     * XslResolver throws exception when Marshaller throws.
     * @throws Exception If something goes wrong
     */
    @Test(expected = IllegalStateException.class)
    public void throwsWhenCreateMarshallerException() throws Exception {
        PowerMockito.mockStatic(JAXBContext.class);
        final JAXBContext context = Mockito.mock(JAXBContext.class);
        Mockito.when(context.createMarshaller())
            .thenThrow(new JAXBException(""));
        Mockito.when(JAXBContext.newInstance(Mockito.any(Class.class)))
            .thenReturn(context);
        new XslResolver().getContext(Object.class);
    }

    /**
     * XslResolvers can avoid duplicated creation of marshaller.
     * @throws Exception If something goes wrong
     * @todo #3 This test is not working at the moment, but it should. We have
     *  to confirm that Marshaller is created only once per class.
     */
    @Ignore
    @Test
    public void avoidsDuplicatedMarshallerCreation() throws Exception {
        // PowerMockito.mockStatic(JAXBContext.class);
        // final JAXBContext context = mock(JAXBContext.class);
        // final Marshaller mrsh = mock(Marshaller.class);
        // doReturn(mrsh).when(context).createMarshaller();
        // when(JAXBContext.newInstance(anyString())).thenReturn(context);
        // final XslResolver resolver = new XslResolver();
        // final XslResolver spy = spy(resolver);
        // spy.getContext(Object.class);
        // verify(spy, times(1)).createContext();
        // reset(spy);
        // spy.getContext(Object.class);
        // verify(spy, times(0)).createContext();
    }

    /**
     * XslResolver can handle dynamically extendable objects.
     * @throws Exception If something goes wrong
     */
    @Test
    public void handlesDynamicallyExtendableObject() throws Exception {
        final XslResolver resolver = new XslResolver();
        resolver.add(XslResolverTest.Injectable.class);
        final Marshaller mrsh = resolver.getContext(Page.class);
        final Page page = new XslResolverTest.Page();
        page.inject(new XslResolverTest.Injectable());
        final StringWriter writer = new StringWriter();
        mrsh.marshal(page, writer);
        MatcherAssert.assertThat(
            XhtmlConverter.the(writer.toString()),
            XmlMatchers.hasXPath("/page/injectable/name")
        );
    }

    /**
     * XslResolver injects xml-stylesheet processing instruction.
     * @throws Exception If something goes wrong
     */
    @Test
    public void injectsDefaultStylesheetPi() throws Exception {
        final XslResolver resolver = new XslResolver();
        final Marshaller mrsh = resolver.getContext(XslResolverTest.Page.class);
        final Page page = new XslResolverTest.Page();
        final StringWriter writer = new StringWriter();
        mrsh.marshal(page, writer);
        MatcherAssert.assertThat(
            XhtmlConverter.the(writer.toString()),
            XmlMatchers.hasXPath(
                // @checkstyle LineLength (1 line)
                "/processing-instruction('xml-stylesheet')[contains(.,\"href='/xsl/Page.xsl'\")]"
            )
        );
        MatcherAssert.assertThat(
            XhtmlConverter.the(writer.toString()),
            XmlMatchers.hasXPath(
                // @checkstyle LineLength (1 line)
                "/processing-instruction('xml-stylesheet')[contains(.,\"type='text/xsl'\")]"
            )
        );
    }

    /**
     * XslResolver understands stylesheet annotation.
     * @throws Exception If something goes wrong
     */
    @Test
    public void testStylesheetAnnotation() throws Exception {
        final XslResolver resolver = new XslResolver();
        final Marshaller mrsh = resolver.getContext(XslResolverTest.Bar.class);
        final Bar bar = new XslResolverTest.Bar();
        final StringWriter writer = new StringWriter();
        mrsh.marshal(bar, writer);
        MatcherAssert.assertThat(
            XhtmlConverter.the(writer.toString()),
            XmlMatchers.hasXPath(
                // @checkstyle LineLength (1 line)
                "/processing-instruction('xml-stylesheet')[contains(.,\"href='test'\")]"
            )
        );
    }

    @XmlRootElement(name = "page")
    @XmlAccessorType(XmlAccessType.NONE)
    public static final class Page {
        /**
         * Injected object.
         */
        private transient Object injected;
        /**
         * Inject an object.
         * @param obj The object to inject
         */
        public void inject(final Object obj) {
            this.injected = obj;
        }
        /**
         * Simple name.
         * @return The name
         */
        @XmlElement
        public String getName() {
            return "some name";
        }
        /**
         * Injected object.
         * @return The object
         */
        @XmlAnyElement(lax = true)
        public Object getInjected() {
            return this.injected;
        }
    }

    @XmlRootElement(name = "injectable")
    @XmlAccessorType(XmlAccessType.NONE)
    public static final class Injectable {
        /**
         * Simple name.
         * @return The name
         */
        @XmlElement
        public String getName() {
            return "some foo name";
        }
    }

    @XmlRootElement(name = "page")
    @XmlAccessorType(XmlAccessType.NONE)
    @Stylesheet("test")
    public static final class Bar {
        /**
         * Simple name.
         * @return The name
         */
        @XmlElement
        public String getName() {
            return "another name";
        }
    }

}
