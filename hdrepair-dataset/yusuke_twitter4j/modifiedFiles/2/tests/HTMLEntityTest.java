/*
Copyright (c) 2007-2010, Yusuke Yamamoto
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Yusuke Yamamoto nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY Yusuke Yamamoto ``AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Yusuke Yamamoto BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package twitter4j.http;

import junit.framework.TestCase;
import twitter4j.internal.http.HTMLEntity;

public class HTMLEntityTest extends TestCase {
    public HTMLEntityTest(String name){
        super(name);
    }

    protected void setUp(){
    }
    protected void tearDown(){
    }

    public void testEscape(){
        String original = "<=% !>";
        String expected = "&lt;=% !&gt;";
        assertEquals(expected, HTMLEntity.escape(original));
        StringBuffer buf = new StringBuffer(original);
        HTMLEntity.escape(buf);
        assertEquals(expected, buf.toString());
    }
    public void testUnescape(){
        String original = "&lt;&lt;=% !&nbsp;&gt;";
        String expected = "<<=% !\u00A0>";
        assertEquals(expected, HTMLEntity.unescape(original));
        StringBuffer buf = new StringBuffer(original);
        HTMLEntity.unescape(buf);
        assertEquals(expected, buf.toString());

        original = "&asd&gt;";
        expected = "&asd>";
        assertEquals(expected, HTMLEntity.unescape(original));
        buf = new StringBuffer(original);
        HTMLEntity.unescape(buf);
        assertEquals(expected, buf.toString());

        original = "&quot;;&;asd&;gt;";
        expected = "\";&;asd&;gt;";
        assertEquals(expected, HTMLEntity.unescape(original));
        buf = new StringBuffer(original);
        HTMLEntity.unescape(buf);
        assertEquals(expected, buf.toString());

        original = "\\u5e30%u5e30 &lt;%}& foobar &lt;&Cynthia&gt;";
        expected = "\\u5e30%u5e30 <%}& foobar <&Cynthia>";
        assertEquals(expected, HTMLEntity.unescape(original));
        buf = new StringBuffer(original);
        HTMLEntity.unescape(buf);
        assertEquals(expected, buf.toString());


    }
}
