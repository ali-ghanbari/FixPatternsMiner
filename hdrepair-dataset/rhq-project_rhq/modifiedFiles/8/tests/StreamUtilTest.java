 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.util.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Random;

import org.testng.annotations.Test;

/**
 * Tests StreamUtil.
 *
 * @author John Mazzitelli
 */
@Test
public class StreamUtilTest {
    public void testEscapeHtml() {
        String dataString = "string with chars to be HTML-escaped: <>&|,'\"\\";
        String escapedString = "string with chars to be HTML-escaped: &lt;&gt;&amp;&#124;&#44;&#039;&quot;&#092;";

        // baseline check - see that not escaping works as usual
        ByteArrayInputStream in = new ByteArrayInputStream(dataString.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, true, false) == dataString.length();
        assert out.toString().equals(dataString);

        // now see that escaping works making sure copy() returns the proper count, too
        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        long bytesRead = StreamUtil.copy(in, out, true, true);
        assert bytesRead == escapedString.length() : "bad count: " + bytesRead + "<->" + escapedString.length();
        assert out.toString().equals(escapedString);
    }

    public void testCopyStreamRange() {
        String dataString = "a test string that will be copied";
        ByteArrayInputStream in;
        ByteArrayOutputStream out;

        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, 5, 5) == 5;
        assert out.toString().equals(dataString.substring(5, 10));

        // make sure StreamUtil didn't read ahead too many bytes
        assert in.read() == dataString.charAt(10);
        assert in.read() == dataString.charAt(11);
        assert in.read() == dataString.charAt(12);

        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, 4, 20) == 20;
        assert out.toString().equals(dataString.substring(4, 24));
        assert in.read() == dataString.charAt(24);
        assert in.read() == dataString.charAt(25);
        assert in.read() == dataString.charAt(26);

        // ask for more than what the string has - allow it
        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, 7, 100) == (dataString.length() - 7);
        assert out.toString().equals(dataString.substring(7));
        assert in.read() == -1;

        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, 5, -1) == (dataString.length() - 5);
        assert out.toString().equals(dataString.substring(5));
        assert in.read() == -1;

        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, 0, 1) == 1;
        assert out.toString().equals(dataString.substring(0, 1));
        assert in.read() == dataString.charAt(1);
        assert in.read() == dataString.charAt(2);
        assert in.read() == dataString.charAt(3);

        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, 0, -1) == dataString.length();
        assert out.toString().equals(dataString);
        assert in.read() == -1;

        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, dataString.length() - 1, 1) == 1;
        assert out.toString().equals(dataString.substring(dataString.length() - 1));
        assert in.read() == -1;

        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        try {
            StreamUtil.copy(in, out, -1, 1);
            assert false : "should not allow negative start byte";
        } catch (RuntimeException ok) {
        }
    }

    /**
     * Tests copying stream whose contents is larger than the internal buffer used by StreamUtil.copy.
     */
    public void testCopyStreamRangeLarge() {
        final String letters = "abcdefghijklmnopqrstuvwxyz";
        final int lettersLength = letters.length();
        final Random rand = new Random();
        byte[] dataStringBytes = new byte[100000];
        for (int i = 0; i < dataStringBytes.length; i++) {
            dataStringBytes[i] = (byte) letters.charAt(rand.nextInt(lettersLength));
        }

        String dataString = new String(dataStringBytes);
        ByteArrayInputStream in;
        ByteArrayOutputStream out;

        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, 5, 5) == 5;
        assert out.toString().equals(dataString.substring(5, 10));

        // make sure StreamUtil didn't read ahead too many bytes
        assert in.read() == dataString.charAt(10);
        assert in.read() == dataString.charAt(11);
        assert in.read() == dataString.charAt(12);

        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, 4, 20) == 20;
        assert out.toString().equals(dataString.substring(4, 24));
        assert in.read() == dataString.charAt(24);
        assert in.read() == dataString.charAt(25);
        assert in.read() == dataString.charAt(26);

        // skip past the first full buffer (which is 32K, see StreamUtil)
        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, 50000, 40000) == 40000;
        assert out.toString().equals(dataString.substring(50000, 90000));
        assert in.read() == dataString.charAt(90000);
        assert in.read() == dataString.charAt(90001);
        assert in.read() == dataString.charAt(90002);

        // ask for more than what the string has - allow it
        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, 7, dataStringBytes.length + 100) == (dataString.length() - 7);
        assert out.toString().equals(dataString.substring(7));
        assert in.read() == -1;

        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, 5, -1) == (dataString.length() - 5);
        assert out.toString().equals(dataString.substring(5));
        assert in.read() == -1;

        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, 0, 1) == 1;
        assert out.toString().equals(dataString.substring(0, 1));
        assert in.read() == dataString.charAt(1);
        assert in.read() == dataString.charAt(2);
        assert in.read() == dataString.charAt(3);

        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, 0, -1) == dataString.length();
        assert out.toString().equals(dataString);
        assert in.read() == -1;

        in = new ByteArrayInputStream(dataString.getBytes());
        out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out, dataString.length() - 1, 1) == 1;
        assert out.toString().equals(dataString.substring(dataString.length() - 1));
        assert in.read() == -1;
    }

    public void testCopyStream() {
        String dataString = "a test string that will be copied";
        ByteArrayInputStream in = new ByteArrayInputStream(dataString.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assert StreamUtil.copy(in, out) == dataString.length();
        assert out.toString().equals(dataString);
    }

    /**
     * Test serialization methods.
     */
    public void testSerialize() {
        assert serializeDeserialize(new String("hello there")).equals("hello there");
    }

    /**
     * Serializes and then deserializes object and returns the reconstituted object. After the deserialization, this
     * method does not check for object equality but it does check that the reconstituted object is not the same as
     * <code>o</code> (that is, the returned object != <code>o</code>).
     *
     * @param  o object to test
     *
     * @return the object after its been serialized and then deserialized
     */
    private Object serializeDeserialize(Serializable o) {
        byte[] b = StreamUtil.serialize(o);
        assert b != null;
        Object o2 = StreamUtil.deserialize(b);
        assert o2 != null;
        assert o != o2;

        return o2;
    }
}