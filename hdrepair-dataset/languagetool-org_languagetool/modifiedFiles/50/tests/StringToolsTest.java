/* LanguageTool, a natural language style checker 
 * Copyright (C) 2006 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.rules.Category;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.en.AvsAnRule;
import org.languagetool.rules.patterns.Element;
import org.languagetool.rules.patterns.PatternRule;

/**
 * @author Daniel Naber
 */
public class StringToolsTest extends TestCase {

  public void testAssureSet() {
    String s = "";
    try {
      StringTools.assureSet(s, "varName");
      fail();
    } catch (IllegalArgumentException expected) {
      // expected exception
    }
    s = " \t";
    try {
      StringTools.assureSet(s, "varName");
      fail();
    } catch (IllegalArgumentException expected) {
      // expected exception
    }
    s = null;
    try {
      StringTools.assureSet(s, "varName");
      fail();
    } catch (NullPointerException expected) {
      // expected exception
    }
    s = "foo";
    StringTools.assureSet(s, "varName");
  }

  public void testReadFile() throws IOException {
    final String content = StringTools.readFile(new FileInputStream("src/test/testinput.txt"), "utf-8");
    assertEquals("one\ntwo\nöäüß\nșțîâăȘȚÎÂĂ\n", content);
  }
  
  public void testIsAllUppercase() {
    assertTrue(StringTools.isAllUppercase("A"));
    assertTrue(StringTools.isAllUppercase("ABC"));
    assertTrue(StringTools.isAllUppercase("ASV-EDR"));
    assertTrue(StringTools.isAllUppercase("ASV-ÖÄÜ"));
    assertTrue(StringTools.isAllUppercase(""));
    
    assertFalse(StringTools.isAllUppercase("ß"));
    assertFalse(StringTools.isAllUppercase("AAAAAAAAAAAAq"));
    assertFalse(StringTools.isAllUppercase("a"));
    assertFalse(StringTools.isAllUppercase("abc"));
  }
  
  public void testIsMixedCase() {
    assertTrue(StringTools.isMixedCase("AbC"));
    assertTrue(StringTools.isMixedCase("MixedCase"));
    assertTrue(StringTools.isMixedCase("iPod"));
    assertTrue(StringTools.isMixedCase("AbCdE"));
    
    assertFalse(StringTools.isMixedCase(""));
    assertFalse(StringTools.isMixedCase("ABC"));
    assertFalse(StringTools.isMixedCase("abc"));
    assertFalse(StringTools.isMixedCase("!"));
    assertFalse(StringTools.isMixedCase("Word"));
  }
  
  public void testIsCapitalizedWord() {
    assertTrue(StringTools.isCapitalizedWord("Abc"));
    assertTrue(StringTools.isCapitalizedWord("Uppercase"));
    assertTrue(StringTools.isCapitalizedWord("Ipod"));    
    
    assertFalse(StringTools.isCapitalizedWord(""));
    assertFalse(StringTools.isCapitalizedWord("ABC"));
    assertFalse(StringTools.isCapitalizedWord("abc"));
    assertFalse(StringTools.isCapitalizedWord("!"));
    assertFalse(StringTools.isCapitalizedWord("wOrD"));
  }

  public void testStartsWithUppercase() {
    assertTrue(StringTools.startsWithUppercase("A"));
    assertTrue(StringTools.startsWithUppercase("ÄÖ"));
    
    assertFalse(StringTools.startsWithUppercase(""));
    assertFalse(StringTools.startsWithUppercase("ß"));
    assertFalse(StringTools.startsWithUppercase("-"));
  }

  public void testUppercaseFirstChar() {
    assertEquals("", StringTools.uppercaseFirstChar(""));
    assertEquals("A", StringTools.uppercaseFirstChar("A"));
    assertEquals("Öäü", StringTools.uppercaseFirstChar("öäü"));
    assertEquals("ßa", StringTools.uppercaseFirstChar("ßa"));
    assertEquals("'Test'", StringTools.uppercaseFirstChar("'test'"));
    assertEquals("''Test", StringTools.uppercaseFirstChar("''test"));
    assertEquals("''T", StringTools.uppercaseFirstChar("''t"));
    assertEquals("'''", StringTools.uppercaseFirstChar("'''"));
  }

  public void testLowercaseFirstChar() {
    assertEquals("", StringTools.lowercaseFirstChar(""));
    assertEquals("a", StringTools.lowercaseFirstChar("A"));
    assertEquals("öäü", StringTools.lowercaseFirstChar("Öäü"));
    assertEquals("ßa", StringTools.lowercaseFirstChar("ßa"));
    assertEquals("'test'", StringTools.lowercaseFirstChar("'Test'"));
    assertEquals("''test", StringTools.lowercaseFirstChar("''Test"));
    assertEquals("''t", StringTools.lowercaseFirstChar("''T"));
    assertEquals("'''", StringTools.lowercaseFirstChar("'''"));
  }

  public void testReaderToString() throws IOException {
    final String str = StringTools.readerToString(new StringReader("bla\nöäü"));
    assertEquals("bla\nöäü", str);
    final StringBuilder longStr = new StringBuilder();
    for (int i = 0; i < 4000; i++) {
      longStr.append("x");
    }
    longStr.append("1234567");
    assertEquals(4007, longStr.length());
    final String str2 = StringTools.readerToString(new StringReader(longStr.toString()));
    assertEquals(longStr.toString(), str2);
  }

  public void testEscapeXMLandHTML() {
    assertEquals("!ä&quot;&lt;&gt;&amp;&amp;", StringTools.escapeXML("!ä\"<>&&"));
    assertEquals("!ä&quot;&lt;&gt;&amp;&amp;", StringTools.escapeHTML("!ä\"<>&&"));
  }

  public void testRuleMatchesToXML() throws IOException {
    final List<RuleMatch> matches = new ArrayList<RuleMatch>();
    final String text = "This is an test sentence. Here's another sentence with more text.";
    final RuleMatch match = new RuleMatch(new AvsAnRule(null), 8, 10, "myMessage");
    match.setColumn(99);
    match.setEndColumn(100);
    match.setLine(44);
    match.setEndLine(45);
    matches.add(match);
    final String xml = StringTools.ruleMatchesToXML(matches, text, 5, StringTools.XmlPrintMode.NORMAL_XML);
    assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"));
    final Pattern matchesPattern =
            Pattern.compile(".*<matches software=\"LanguageTool\" version=\"" + JLanguageTool.VERSION + "\" buildDate=\".*?\">.*", Pattern.DOTALL);
    final Matcher matcher = matchesPattern.matcher(xml);
    assertTrue(matcher.matches());
    assertTrue(xml.contains(">\n" +
            "<error fromy=\"44\" fromx=\"98\" toy=\"45\" tox=\"99\" ruleId=\"EN_A_VS_AN\" msg=\"myMessage\" replacements=\"\" context=\"...s is an test...\" contextoffset=\"8\" errorlength=\"2\"/>\n" +
            "</matches>\n"));
  }

  public void testRuleMatchesToXMLWithCategory() throws IOException {
    final List<RuleMatch> matches = new ArrayList<RuleMatch>();
    final String text = "This is a test sentence.";
    final List<Element> elements = Collections.emptyList();
    final Rule patternRule = new PatternRule("MY_ID", Language.GERMAN, elements, "my description", "my message", "short message");
    patternRule.setCategory(new Category("MyCategory"));
    final RuleMatch match = new RuleMatch(patternRule, 8, 10, "myMessage");
    match.setColumn(99);
    match.setEndColumn(100);
    match.setLine(44);
    match.setEndLine(45);
    matches.add(match);
    final String xml = StringTools.ruleMatchesToXML(matches, text, 5, StringTools.XmlPrintMode.NORMAL_XML);
    assertTrue(xml.contains(">\n" +
            "<error fromy=\"44\" fromx=\"98\" toy=\"45\" tox=\"99\" ruleId=\"MY_ID\" msg=\"myMessage\" replacements=\"\" context=\"...s is a test ...\" contextoffset=\"8\" errorlength=\"2\" category=\"MyCategory\"/>\n" +
            "</matches>\n"));
  }

  public void testRuleMatchesWithUrlToXML() throws IOException {
    final List<RuleMatch> matches = new ArrayList<RuleMatch>();
    final String text = "This is an test sentence. Here's another sentence with more text.";
    final RuleMatch match = new RuleMatch(new AvsAnRule(null) {
      @Override
      public URL getUrl() {
        try {
          return new URL("http://server.org?id=1&foo=bar");
        } catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }
    }, 8, 10, "myMessage");
    match.setColumn(99);
    match.setEndColumn(100);
    match.setLine(44);
    match.setEndLine(45);
    matches.add(match);
    final String xml = StringTools.ruleMatchesToXML(matches, text, 5, StringTools.XmlPrintMode.NORMAL_XML);
    assertTrue(xml.contains(">\n" +
            "<error fromy=\"44\" fromx=\"98\" toy=\"45\" tox=\"99\" ruleId=\"EN_A_VS_AN\" msg=\"myMessage\" replacements=\"\" context=\"...s is an test...\" contextoffset=\"8\" errorlength=\"2\" url=\"http://server.org?id=1&amp;foo=bar\"/>\n" +
            "</matches>\n"));
  }

  public void testRuleMatchesToXMLEscapeBug() throws IOException {
    final List<RuleMatch> matches = new ArrayList<RuleMatch>();
    final String text = "This is \"an test sentence. Here's another sentence with more text.";
    final RuleMatch match = new RuleMatch(new AvsAnRule(null), 9, 11, "myMessage");
    match.setColumn(99);
    match.setEndColumn(100);
    match.setLine(44);
    match.setEndLine(45);
    matches.add(match);
    final String xml = StringTools.ruleMatchesToXML(matches, text, 5, StringTools.XmlPrintMode.NORMAL_XML);
    assertTrue(xml.contains(">\n" +
            "<error fromy=\"44\" fromx=\"98\" toy=\"45\" tox=\"99\" ruleId=\"EN_A_VS_AN\" msg=\"myMessage\" replacements=\"\" context=\"... is &quot;an test...\" contextoffset=\"8\" errorlength=\"2\"/>\n" +
            "</matches>\n"));
  }

  public void testListToString() {
    final List<String> list = new ArrayList<String>();
    list.add("foo");
    list.add("bar");
    list.add(",");
    assertEquals("foo,bar,,", StringTools.listToString(list, ","));
    assertEquals("foo\tbar\t,", StringTools.listToString(list, "\t"));
  }

  public void testGetContext() {
    final String input = "This is a test sentence. Here's another sentence with more text.";
    final String result = StringTools.getContext(8, 14, input, 5);
    assertEquals("...s is a test sent...\n        ^^^^^^     ", result);
  }

  public void testTrimWhitespace() {
    try {
      assertEquals(null, StringTools.trimWhitespace(null));
      fail();
    } catch (NullPointerException expected) {
      // expected
    }
    assertEquals("", StringTools.trimWhitespace(""));
    assertEquals("", StringTools.trimWhitespace(" "));
    assertEquals("XXY", StringTools.trimWhitespace(" \nXX\t Y"));
    assertEquals("XXY", StringTools.trimWhitespace(" \r\nXX\t Y"));
    assertEquals("word", StringTools.trimWhitespace("word"));
  }

  public void testAddSpace() {
    assertEquals(" ", StringTools.addSpace("word", Language.ENGLISH));
    assertEquals("", StringTools.addSpace(",", Language.ENGLISH));
    assertEquals("", StringTools.addSpace(",", Language.FRENCH));
    assertEquals("", StringTools.addSpace(",", Language.ENGLISH));
    assertEquals(" ", StringTools.addSpace(":", Language.FRENCH));
    assertEquals("", StringTools.addSpace(",", Language.ENGLISH));
    assertEquals(" ", StringTools.addSpace(";", Language.FRENCH));    
  }
  
  public void testGetLabel() {    
    assertEquals("This is a Label", StringTools.getLabel("This is a &Label"));
    assertEquals("Bits & Pieces", StringTools.getLabel("Bits && Pieces"));
  }
  
  public void testGetOOoLabel() {    
    assertEquals("This is a ~Label", StringTools.getOOoLabel("This is a &Label"));
    assertEquals("Bits & Pieces", StringTools.getLabel("Bits && Pieces"));
  }
  
  public void testGetMnemonic() {
    assertEquals('F', StringTools.getMnemonic("&File"));
    assertEquals('O', StringTools.getMnemonic("&OK"));
    assertEquals('\u0000', 
        StringTools.getMnemonic("File && String operations"));
    assertEquals('O', 
      StringTools.getMnemonic("File && String &Operations"));
  }
  
  public void testIsWhitespace() {
    assertEquals(true, StringTools.isWhitespace("  "));
    assertEquals(true, StringTools.isWhitespace("\t"));
    assertEquals(true, StringTools.isWhitespace("\u2002"));    
    //non-breaking space is not a whitespace
    assertEquals(false, StringTools.isWhitespace("\u00a0"));
    assertEquals(false, StringTools.isWhitespace("abc"));
    //non-breaking OOo field
    assertEquals(false, StringTools.isWhitespace("\\u02"));
    assertEquals(false, StringTools.isWhitespace("\u0001"));
  }
  
  public void testIsPositiveNumber() {
    assertEquals(true, StringTools.isPositiveNumber('3'));
    assertEquals(false, StringTools.isPositiveNumber('a'));      
  }
  
  public void testIsEmpty() {
    assertEquals(true, StringTools.isEmpty(""));
    assertEquals(true, StringTools.isEmpty(null));
    assertEquals(false, StringTools.isEmpty("a"));      
  }

  public void testFilterXML() {
    assertEquals("test", StringTools.filterXML("test"));
    assertEquals("<<test>>", StringTools.filterXML("<<test>>"));
    assertEquals("test", StringTools.filterXML("<b>test</b>"));
    assertEquals("A sentence with a test", StringTools.filterXML("A sentence with a <em>test</em>"));
  }

  public void testAsString() {
    assertNull(StringTools.asString(null));
    assertEquals("foo!", "foo!");
  }

}
