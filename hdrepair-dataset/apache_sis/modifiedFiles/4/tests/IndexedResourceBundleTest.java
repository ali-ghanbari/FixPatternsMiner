/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.util.resources;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import org.opengis.util.InternationalString;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import org.junit.After;

import static org.junit.Assert.*;


/**
 * Tests the {@link IndexedResourceBundle} subclasses.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.2)
 * @version 0.3
 */
@DependsOn(org.apache.sis.util.ArraysTest.class)
public final strictfp class IndexedResourceBundleTest extends TestCase {
    /**
     * The resource bundle in process of being tested. Shall be reset to {@code null} after every
     * test. If non-null, then {@link #dumpResourcesOnError()} will consider that the test failed
     * and will dump the resource bundle content to the standard error stream.
     */
    private IndexedResourceBundle testing;

    /**
     * Tests the {@link Errors#getResources(Locale)} method on different locales.
     */
    @Test
    public void testGetResources() {
        final Errors english = Errors.getResources(Locale.ENGLISH);
        final Errors french  = Errors.getResources(Locale.FRENCH);
        assertNotSame(english, french);

        assertSame(english, Errors.getResources(Locale.ENGLISH));
        assertSame(english, Errors.getResources(Locale.US));
        assertSame(english, Errors.getResources(Locale.UK));
        assertSame(english, Errors.getResources(Locale.CANADA));
        assertSame(french,  Errors.getResources(Locale.FRENCH));
        assertSame(french,  Errors.getResources(Locale.CANADA_FRENCH));
    }

    /**
     * Tests the {@link IndexedResourceBundle#list(Appendable)} method.
     *
     * @throws IOException Should never happen.
     */
    @Test
    @DependsOnMethod("testGetResources")
    public void testList() throws IOException {
        final StringBuilder buffer = new StringBuilder(4096);
        Errors.getResources(Locale.ENGLISH).list(buffer);
        final String text = buffer.toString();
        final int key     = text.indexOf("NullArgument_1");
        final int value   = text.indexOf("Argument ‘{0}’ shall not be null.");
        assertTrue(key   > 0);
        assertTrue(value > key);
    }

    /**
     * Tests the {@link IndexedResourceBundle#getKeys()} method.
     */
    @Test
    @DependsOnMethod("testGetResources")
    public void testGetKeys() {
        testing = Errors.getResources(Locale.ENGLISH);
        final Enumeration<String> e = testing.getKeys();
        int count = 0;
        boolean foundNullArgument_1 = false;
        while (e.hasMoreElements()) {
            final String key = e.nextElement();
            if (key.equals("NullArgument_1")) {
                foundNullArgument_1 = true;
            }
            count++;
        }
        assertTrue("foundNullArgument_1:", foundNullArgument_1);
        assertTrue("count > 5", count > 5);
        testing = null;
    }

    /**
     * Tests the {@link IndexedResourceBundle#getString(int)} method on different locales.
     */
    @Test
    @DependsOnMethod("testGetResources")
    public void testGetString() {
        final Errors english = Errors.getResources(Locale.ENGLISH);
        final Errors french  = Errors.getResources(Locale.FRENCH);

        assertEquals("Argument ‘{0}’ shall not be null.",      (testing = english).getString(Errors.Keys.NullArgument_1));
        assertEquals("L’argument ‘{0}’ ne doit pas être nul.", (testing = french) .getString(Errors.Keys.NullArgument_1));
        testing = null;
    }

    /**
     * Tests the {@link IndexedResourceBundle#getString(String)} method on different locales.
     */
    @Test
    @DependsOnMethod("testGetResources")
    public void testGetStringByName() {
        final Errors english = Errors.getResources(Locale.ENGLISH);
        final Errors french  = Errors.getResources(Locale.FRENCH);

        assertEquals("Argument ‘{0}’ shall not be null.",      (testing = english).getString("NullArgument_1"));
        assertEquals("L’argument ‘{0}’ ne doit pas être nul.", (testing = french) .getString("NullArgument_1"));
        testing = null;
    }

    /**
     * Tests the {@link IndexedResourceBundle#getString(int, Object)} method on different locales.
     */
    @Test
    @DependsOnMethod("testGetString")
    public void testGetStringWithParameter() {
        testing = Errors.getResources(Locale.ENGLISH);
        assertEquals("Argument ‘CRS’ shall not be null.", testing.getString(Errors.Keys.NullArgument_1, "CRS"));
        testing = Errors.getResources(Locale.FRENCH);
        assertEquals("L’argument ‘CRS’ ne doit pas être nul.", testing.getString(Errors.Keys.NullArgument_1, "CRS"));
        testing = null;
    }

    /**
     * Tests the formatting of an international string.
     */
    @Test
    @DependsOnMethod("testGetResources")
    public void testFormatInternational() {
        InternationalString i18n = Errors.formatInternational(Errors.Keys.NullArgument_1);
        assertEquals("Argument ‘{0}’ shall not be null.",      i18n.toString(Locale.ENGLISH));
        assertEquals("L’argument ‘{0}’ ne doit pas être nul.", i18n.toString(Locale.FRENCH));

        i18n = Errors.formatInternational(Errors.Keys.NullArgument_1, "CRS");
        assertEquals("Argument ‘CRS’ shall not be null.",      i18n.toString(Locale.ENGLISH));
        assertEquals("L’argument ‘CRS’ ne doit pas être nul.", i18n.toString(Locale.FRENCH));
    }

    /**
     * Tests the {@link IndexedResourceBundle#getLogRecord(Level, int, Object)} method.
     */
    @Test
    @DependsOnMethod("testGetResources")
    public void testGetLogRecord() {
        testing = Errors.getResources(Locale.ENGLISH);
        final LogRecord record = testing.getLogRecord(Level.FINE, Errors.Keys.NullArgument_1, "CRS");
        assertEquals("NullArgument_1", record.getMessage());

        final SimpleFormatter formatter = new SimpleFormatter();
        final String message = formatter.format(record);
        assertTrue(message.contains("Argument ‘CRS’ shall not be null."));
        testing = null;
    }

    /**
     * If a test failed, lists the resource bundle content to the
     * {@linkplain System#err standard error stream}.
     *
     * @throws IOException Should never happen.
     */
    @After
    public void dumpResourcesOnError() throws IOException {
        if (testing != null) {
            final PrintStream err = System.err;
            err.print("Error while testing ");
            err.print(testing);
            err.println(". Bundle content is:");
            testing.list(err);
            err.flush();
        }
    }
}
