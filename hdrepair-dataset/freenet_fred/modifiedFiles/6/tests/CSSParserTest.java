package freenet.clients.http.filter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import freenet.clients.http.filter.CSSTokenizerFilter.CSSPropertyVerifier;
import freenet.l10n.NodeL10n;
import freenet.support.Logger;
import freenet.support.LoggerHook.InvalidThresholdException;
import junit.framework.TestCase;

public class CSSParserTest extends TestCase {




	/** CSS1 Selectors */

	private final static HashMap<String,String> CSS1_SELECTOR= new HashMap<String,String>();
	static
	{
		CSS1_SELECTOR.put("h1 { }","h1");
		CSS1_SELECTOR.put("h1:link { }","h1:link");
		CSS1_SELECTOR.put("h1:visited { }","h1:visited");
		CSS1_SELECTOR.put("h1.warning { }","h1.warning");
		CSS1_SELECTOR.put("h1#myid { }","h1#myid");
		CSS1_SELECTOR.put("h1 h2 { }","h1 h2");
		CSS1_SELECTOR.put("h1:active { }","h1:active");
		CSS1_SELECTOR.put("h1:hover { }","h1:hover");
		CSS1_SELECTOR.put("h1:focus { }" ,"h1:focus");
		CSS1_SELECTOR.put("h1:first-line { }" ,"h1:first-line");
		CSS1_SELECTOR.put("h1:first-letter { }" ,"h1:first-letter");




	}

	/** CSS2 Selectors */
	private final static HashMap<String,String> CSS2_SELECTOR= new HashMap<String,String>();
	static
	{
		CSS2_SELECTOR.put("* { }","*");
		CSS2_SELECTOR.put("h1[foo] { }","h1[foo]");
		CSS2_SELECTOR.put("h1[foo=\"bar\"] { }", "h1[foo=\"bar\"]"); 
		CSS2_SELECTOR.put("h1[foo~=\"bar\"] { }", "h1[foo~=\"bar\"]");
		CSS2_SELECTOR.put("h1[foo~=\"bar\"] { }","h1[foo~=\"bar\"]");
		CSS2_SELECTOR.put("h1[foo|=\"en\"] { }","h1[foo|=\"en\"]");
		CSS2_SELECTOR.put("h1:first-child { }","h1:first-child");
		CSS2_SELECTOR.put("h1:lang(fr) { }","h1:lang(fr)");
		CSS2_SELECTOR.put("h1>h2 { }","h1>h2");
		CSS2_SELECTOR.put("h1+h2 { }", "h1+h2");
		CSS2_SELECTOR.put("div.foo { }", "div.foo");
		
		// Spaces in a selector string
		CSS2_SELECTOR.put("h1[foo=\"bar bar\"] { }", "h1[foo=\"bar bar\"]");
		CSS2_SELECTOR.put("h1[foo=\"bar+bar\"] { }", "h1[foo=\"bar+bar\"]");
		CSS2_SELECTOR.put("h1[foo=\"bar\\\" bar\"] { }", "h1[foo=\"bar\\\" bar\"]");
		// Wierd one from the CSS spec
		CSS2_SELECTOR.put("p[example=\"public class foo\\\n{\\\n    private int x;\\\n\\\n    foo(int x) {\\\n        this.x = x;\\\n    }\\\n\\\n}\"] { color: red }", 
				"p[example=\"public class foo{    private int x;    foo(int x) {        this.x = x;    }}\"] { color:red;}\n");
	}

	private static final String CSS_STRING_NEWLINES = "* { content: \"this string does not terminate\n}\nbody {\nbackground: url(http://www.google.co.uk/intl/en_uk/images/logo.gif); }\n\" }";
	private static final String CSS_STRING_NEWLINESC = " * {}\n body {}\n";

	private static final String CSS_BACKGROUND_URL = "* { background: url(/SSK@qd-hk0vHYg7YvK2BQsJMcUD5QSF0tDkgnnF6lnWUH0g,xTFOV9ddCQQk6vQ6G~jfL6IzRUgmfMcZJ6nuySu~NUc,AQACAAE/activelink-index-text-76/activelink.png); }";
	private static final String CSS_BACKGROUND_URLC = " * { background:url(/SSK@qd-hk0vHYg7YvK2BQsJMcUD5QSF0tDkgnnF6lnWUH0g,xTFOV9ddCQQk6vQ6G~jfL6IzRUgmfMcZJ6nuySu~NUc,AQACAAE/activelink-index-text-76/activelink.png);}\n";
	
	private static final String CSS_LCASE_BACKGROUND_URL = "* { background: url(/ssk@qd-hk0vHYg7YvK2BQsJMcUD5QSF0tDkgnnF6lnWUH0g,xTFOV9ddCQQk6vQ6G~jfL6IzRUgmfMcZJ6nuySu~NUc,AQACAAE/activelink-index-text-76/activelink.png); }";
	private static final String CSS_LCASE_BACKGROUND_URLC = " * { background:url(/SSK@qd-hk0vHYg7YvK2BQsJMcUD5QSF0tDkgnnF6lnWUH0g,xTFOV9ddCQQk6vQ6G~jfL6IzRUgmfMcZJ6nuySu~NUc,AQACAAE/activelink-index-text-76/activelink.png);}\n";
	
	// not adding ?type=text/css is exploitable, so check for it.
	private static final String CSS_IMPORT = "@import url(\"/KSK@test\");";
	private static final String CSS_IMPORTC = "@import url(\"/KSK@test?type=text/css\");";

	private static final String CSS_IMPORT2 = "@import url(\"/chk@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/1-1.html\") screen;";
	private static final String CSS_IMPORT2C = "@import url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/1-1.html?type=text/css\") screen;";

	private static final String CSS_IMPORT_SPACE_IN_STRING = "@import url(\"/chk@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test page\") screen;";
	private static final String CSS_IMPORT_SPACE_IN_STRINGC = "@import url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page?type=text/css\") screen;";
	
	private static final String CSS_IMPORT_NOURL_TWOMEDIAS = "@import \"/chk@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/1-1.html\" screen tty;";
	private static final String CSS_IMPORT_NOURL_TWOMEDIASC = "@import url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/1-1.html?type=text/css\") screen, tty;";
	
	private static final String CSS_ESCAPED_LINK = "* { background: url(\\00002f\\00002fwww.google.co.uk/intl/en_uk/images/logo.gif); }";
	private static final String CSS_ESCAPED_LINKC = " * {}\n";
	
	private static final String CSS_ESCAPED_LINK2 = "* { background: url(\\/\\/www.google.co.uk/intl/en_uk/images/logo.gif); }";
	private static final String CSS_ESCAPED_LINK2C = " * {}\n";
	
	// CSS2.1 spec, 4.1.7
	private static final String CSS_DELETE_INVALID_SELECTOR = "h1, h2 {color: green }\nh3, h4 & h5 {color: red }\nh6 {color: black }\n";
	private static final String CSS_DELETE_INVALID_SELECTORC = "h1, h2 { color:green;}\n h6 { color:black;}\n";
	
	private final static LinkedHashMap<String, String> propertyTests = new LinkedHashMap<String, String>();
	static {
		// Check that the last part of a double bar works
		propertyTests.put("@media speech { h1 { azimuth: behind }; }", "@media speech {\n h1 { azimuth:behind;}\n}\n");
		propertyTests.put("td { background-position:bottom;}\n", " td { background-position:bottom;}\n");
		propertyTests.put("td { background:repeat-x;}\n", " td { background:repeat-x;}\n");
		
		// Double bar: recurse after recognising last element
		propertyTests.put("td { background:repeat-x no;}\n", " td { background:repeat-x no;}\n");
		propertyTests.put("td { background:repeat-x no transparent;}\n", " td { background:repeat-x no transparent;}\n");
		propertyTests.put("td { background:repeat-x no transparent scroll;}\n", " td { background:repeat-x no transparent scroll;}\n");
		
		propertyTests.put("@media speech { h1 { azimuth: 30deg }; }", "@media speech {\n h1 { azimuth:30deg;}\n}\n");
		propertyTests.put("@media speech { h1 { azimuth: 0.877171rad }; }", "@media speech {\n h1 { azimuth:0.877171rad;}\n}\n");
		propertyTests.put("@media speech { h1 { azimuth: left-side behind }; }", "@media speech {\n h1 { azimuth:left-side behind;}\n}\n");
		// Invalid combination
		propertyTests.put("@media speech { h1 { azimuth: left-side behind 30deg }; }", "@media speech {\n h1 {}\n}\n");
		propertyTests.put("@media speech { h1 { azimuth: inherit }; }", "@media speech {\n h1 { azimuth:inherit;}\n}\n");
		// Wrong media type
		propertyTests.put("h1 { azimuth: inherit }", " h1 {}\n");
		
		propertyTests.put("td { background-attachment: scroll}", " td { background-attachment:scroll;}\n");
		propertyTests.put("td { background-color: rgb(255, 255, 255)}", " td { background-color:rgb(255, 255, 255);}\n");
		// Invalid element
		propertyTests.put("silly { background-attachment: scroll}", "");
		propertyTests.put("h3 { background-position: 30% top}", " h3 { background-position:30% top;}\n");
		// Fractional lengths
		propertyTests.put("h3 { background-position: 3.3cm 20%}", " h3 { background-position:3.3cm 20%;}\n");
		// Negative fractional lengths
		propertyTests.put("h3 { background-position: -0.87em 20%}", " h3 { background-position:-0.87em 20%;}\n");
		
		// Url with an encoded space
		propertyTests.put("h3 { background-image: url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\") }", " h3 { background-image:url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\");}\n");
		// Url with a space
		// FIXME rewrite such properties. For now we will just delete them.
		propertyTests.put("h3 { background-image: url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test page\") }", " h3 {}\n");
		// Url with lower case chk@
		// FIXME rewrite such properties. For now we will just delete them.
		propertyTests.put("h3 { background-image: url(\"/chk@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\") }", " h3 {}\n");
		
		// FIXME url without ""
		// FIXME import without url()
		
		// Mixed background
		propertyTests.put("h3 { background: url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\") }", " h3 { background:url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\");}\n");
		propertyTests.put("h3 { background: scroll url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\") }", " h3 { background:scroll url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\");}\n");
		propertyTests.put("h3 { background: scroll #f00 url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\") }", " h3 { background:scroll #f00 url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\");}\n");
		propertyTests.put("h3 { background: scroll #f00 url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\") }", " h3 { background:scroll #f00 url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\");}\n");
		propertyTests.put("h3 { background: scroll rgb(100%, 2%, 1%) url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\") }", " h3 { background:scroll rgb(100%, 2%, 1%) url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\");}\n");
		propertyTests.put("h3 { background: 3.3cm 20%;}", " h3 { background:3.3cm 20%;}\n");
		propertyTests.put("h3 { background: scroll 3.3cm 20%;}", " h3 { background:scroll 3.3cm 20%;}\n");
		propertyTests.put("h3 { background: scroll rgb(100%, 2%, 1%) 3.3cm 20%;}", " h3 { background:scroll rgb(100%, 2%, 1%) 3.3cm 20%;}\n");
		propertyTests.put("h3 { background: 3.3cm 20% url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\");}", " h3 { background:3.3cm 20% url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\");}\n");
		propertyTests.put("h3 { background: scroll rgb(100%, 2%, 1%) 3.3cm 20% url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\") }", " h3 { background:scroll rgb(100%, 2%, 1%) 3.3cm 20% url(\"/CHK@~~vxVQDfC9m8sR~M9zWJQKzCxLeZRWy6T1pWLM2XX74,2LY7xwOdUGv0AeJ2WKRXZG6NmiUL~oqVLKnh3XdviZU,AAIC--8/test%20page\");}\n");
		
		// Counters
		propertyTests.put("table { counter-increment: counter1 1}", " table { counter-increment:counter1 1;}\n");
		// Counters with whacky identifiers
		propertyTests.put("table { counter-increment: c\\ounter1 1}", " table { counter-increment:c\\ounter1 1;}\n");
		propertyTests.put("table { counter-increment: c\\ ounter1 1}", " table { counter-increment:c\\ ounter1 1;}\n");
		propertyTests.put("table { counter-increment: c\\ \\}ounter1 1}", " table { counter-increment:c\\ \\}ounter1 1;}\n");
		propertyTests.put("table { counter-increment: c\\ \\}oun\\:ter1 1}", " table { counter-increment:c\\ \\}oun\\:ter1 1;}\n");
		propertyTests.put("table { counter-increment: c\\ \\}oun\\:ter1\\; 1}", " table { counter-increment:c\\ \\}oun\\:ter1\\; 1;}\n");
		propertyTests.put("table { counter-increment: \\2 \\32 \\ c\\ \\}oun\\:ter1\\; 1}", " table { counter-increment:\\2 \\32 \\ c\\ \\}oun\\:ter1\\; 1;}\n");
		propertyTests.put("table { counter-increment: \\000032\\2 \\32 \\ c\\ \\}oun\\:ter1\\; 1}", " table { counter-increment:\\000032\\2 \\32 \\ c\\ \\}oun\\:ter1\\; 1;}\n");
		
		// Content tests
		propertyTests.put("h1 { content: \"string with spaces\" }", " h1 { content:\"string with spaces\";}\n");
		propertyTests.put("h1 { content: attr(\\ \\ attr\\ with\\ spaces) }", " h1 { content:attr(\\ \\ attr\\ with\\ spaces);}\n");
		propertyTests.put("h1 { content: \"string with spaces\" attr(\\ \\ attr\\ with\\ spaces) }", " h1 { content:\"string with spaces\" attr(\\ \\ attr\\ with\\ spaces);}\n");
		
		// Strip nulls
		propertyTests.put("h2 { color: red }", " h2 { color:red;}\n");
		propertyTests.put("h2 { color: red\0 }", " h2 { color:red;}\n");
		
		// Lengths must have a unit
		propertyTests.put("h2 { border-width: 1.5em;}\n"," h2 { border-width:1.5em;}\n");
		propertyTests.put("h2 { border-width: 12px;}\n"," h2 { border-width:12px;}\n");
		propertyTests.put("h2 { border-width: 1.5;}\n"," h2 {}\n");
		propertyTests.put("h2 { border-width: 0;}\n"," h2 { border-width:0;}\n");
		propertyTests.put("h2 { border-width: 10;}\n"," h2 {}\n");
		
		// Fonts
		propertyTests.put("h2 { font-family: times new roman;}\n", " h2 { font-family:times new roman;}\n");
		propertyTests.put("h2 { font-family: Times New Roman;}\n", " h2 { font-family:Times New Roman;}\n");
		propertyTests.put("h2 { font-family: \"Times New Roman\";}\n", " h2 { font-family:\"Times New Roman\";}\n");
		propertyTests.put("h2 { font-family: inherit;}\n", " h2 { font-family:inherit;}\n");
		propertyTests.put("h2 { font-family: Times New Reman;}\n", " h2 {}\n");
		propertyTests.put("h2 { font-family: \"Times New Reman\";}\n", " h2 {}\n");
		propertyTests.put("h2 { font-family: \"Times New Roman\" , \"Arial\";}\n"," h2 { font-family:\"Times New Roman\" , \"Arial\";}\n"); 
		propertyTests.put("h2 { font-family: \"Times New Roman\", \"Arial\";}\n"," h2 { font-family:\"Times New Roman\", \"Arial\";}\n"); 
		propertyTests.put("h2 { font-family: \"Times New Roman\", \"Arial\", \"Helvetica\";}\n"," h2 { font-family:\"Times New Roman\", \"Arial\", \"Helvetica\";}\n");
		propertyTests.put("h2 { font-family: \"Times New Roman\", Arial;}\n"," h2 { font-family:\"Times New Roman\", Arial;}\n");
		propertyTests.put("h2 { font-family: Times New Roman, Arial;}\n"," h2 { font-family:Times New Roman, Arial;}\n");
		propertyTests.put("h2 { font-family: serif, Times New Roman, Arial;}\n"," h2 { font-family:serif, Times New Roman, Arial;}\n");
		propertyTests.put("h2 { font: Times New Roman;}\n", " h2 { font:Times New Roman;}\n");
		propertyTests.put("h2 { font: \"Times New Roman\";}\n", " h2 { font:\"Times New Roman\";}\n");
		propertyTests.put("h2 { font: medium \"Times New Roman\";}\n", " h2 { font:medium \"Times New Roman\";}\n");
		propertyTests.put("h2 { font: Times New Reman;}\n", " h2 {}\n");
		propertyTests.put("h2 { font: medium Times New Roman, Arial Black;}\n", " h2 { font:medium Times New Roman, Arial Black;}\n");
		propertyTests.put("h2 { font: italic small-caps 500 1.5em Times New Roman, Arial Black;}\n", " h2 { font:italic small-caps 500 1.5em Times New Roman, Arial Black;}\n");
		propertyTests.put("h2 { font: 1.5em/12pt Times New Roman, Arial Black;}\n", " h2 { font:1.5em/12pt Times New Roman, Arial Black;}\n");
		propertyTests.put("h2 { font: small-caps 1.5em/12pt Times New Roman, Arial Black;}\n", " h2 { font:small-caps 1.5em/12pt Times New Roman, Arial Black;}\n");
		propertyTests.put("h2 { font: 500 1.5em/12pt Times New Roman, Arial Black;}\n", " h2 { font:500 1.5em/12pt Times New Roman, Arial Black;}\n");
		propertyTests.put("h2 { font: small-caps 500 1.5em/12pt Times New Roman, Arial Black;}\n", " h2 { font:small-caps 500 1.5em/12pt Times New Roman, Arial Black;}\n");
		propertyTests.put("h2 { font: 500 \"Times New Roman\";}\n", " h2 { font:500 \"Times New Roman\";}\n");
		propertyTests.put("h2 { font-weight: 500;}\n", " h2 { font-weight:500;}\n");
		propertyTests.put("h2 { font: normal \"Times New Roman\";}\n", " h2 { font:normal \"Times New Roman\";}\n");
		propertyTests.put("h2 { font: 500 normal \"Times New Roman\";}\n", " h2 { font:500 normal \"Times New Roman\";}\n");
		propertyTests.put("h2 { font: 500 normal Times New Roman;}\n", " h2 { font:500 normal Times New Roman;}\n");
		propertyTests.put("h2 { font: 500 normal Times New Roman, Arial Black;}\n", " h2 { font:500 normal Times New Roman, Arial Black;}\n");
		propertyTests.put("h2 { font: 500 normal 1.5em/12pt Times New Roman, Arial Black;}\n", " h2 { font:500 normal 1.5em/12pt Times New Roman, Arial Black;}\n");
		propertyTests.put("h2 { font: small-caps 500 normal 1.5em/12pt Times New Roman, Arial Black;}\n", " h2 { font:small-caps 500 normal 1.5em/12pt Times New Roman, Arial Black;}\n");
		
		// Space is not required either after or before comma!
		propertyTests.put("h2 { font-family: Verdana,sans-serif }", " h2 { font-family:Verdana,sans-serif;}\n");
		// Case in generic keywords
		propertyTests.put("h2 { font-family: Verdana,Sans-Serif }", " h2 { font-family:Verdana,Sans-Serif;}\n");
		// This one is from the text Activelink Index
		propertyTests.put("h2 { font: normal 12px/15px Verdana,sans-serif }", " h2 { font:normal 12px/15px Verdana,sans-serif;}\n");
		propertyTests.put("h2 { font-family: times new roman,arial,verdana }", " h2 { font-family:times new roman,arial,verdana;}\n");
	}
	
	public void setUp() throws InvalidThresholdException {
		new NodeL10n();
    	Logger.setupStdoutLogging(Logger.MINOR, "freenet.clients.http.filter:DEBUG");
	}
	
	public void testCSS1Selector() throws IOException, URISyntaxException {


		Collection c = CSS1_SELECTOR.keySet();
		Iterator itr = c.iterator();
		while(itr.hasNext())
		{

			String key=itr.next().toString();
			String value=CSS1_SELECTOR.get(key);
			assertTrue("key=\""+key+"\" value=\""+filter(key)+"\" should be \""+value+"\"", filter(key).contains(value));
		}

		assertTrue("key=\""+CSS_DELETE_INVALID_SELECTOR+"\" value=\""+filter(CSS_DELETE_INVALID_SELECTOR)+"\" should be \""+CSS_DELETE_INVALID_SELECTORC+"\"", CSS_DELETE_INVALID_SELECTORC.equals(filter(CSS_DELETE_INVALID_SELECTOR)));
	}

	public void testCSS2Selector() throws IOException, URISyntaxException {
		Collection c = CSS2_SELECTOR.keySet();
		Iterator itr = c.iterator();
		int i=0; 
		while(itr.hasNext())
		{
			String key=itr.next().toString();
			String value=CSS2_SELECTOR.get(key);
			System.err.println("Test "+(i++)+" : "+key+" -> "+value);
			assertTrue("key="+key+" value="+filter(key)+"\" should be \""+value+"\"", filter(key).contains(value));
		}

	}

	public void testNewlines() throws IOException, URISyntaxException {
		assertTrue("key=\""+CSS_STRING_NEWLINES+"\" value=\""+filter(CSS_STRING_NEWLINES)+"\" should be: \""+CSS_STRING_NEWLINESC+"\"", CSS_STRING_NEWLINESC.equals(filter(CSS_STRING_NEWLINES)));
	}
	
	public void testBackgroundURL() throws IOException, URISyntaxException {
		assertTrue("key="+CSS_BACKGROUND_URL+" value=\""+filter(CSS_BACKGROUND_URL)+"\"", CSS_BACKGROUND_URLC.equals(filter(CSS_BACKGROUND_URL)));
		
		// FIXME support lower case ssk@ in links from CSS
		//assertTrue("key="+CSS_LCASE_BACKGROUND_URL+" value=\""+filter(CSS_LCASE_BACKGROUND_URL)+"\"", CSS_LCASE_BACKGROUND_URLC.equals(filter(CSS_LCASE_BACKGROUND_URL)));
	}
	
	public void testImports() throws IOException, URISyntaxException {
		assertTrue("key="+CSS_IMPORT+" value=\""+filter(CSS_IMPORT)+"\"", CSS_IMPORTC.equals(filter(CSS_IMPORT)));
		assertTrue("key="+CSS_IMPORT2+" value=\""+filter(CSS_IMPORT2)+"\"", CSS_IMPORT2C.equals(filter(CSS_IMPORT2)));
		assertTrue("key="+CSS_IMPORT_SPACE_IN_STRING+" value=\""+filter(CSS_IMPORT_SPACE_IN_STRING)+"\"", CSS_IMPORT_SPACE_IN_STRINGC.equals(filter(CSS_IMPORT_SPACE_IN_STRING)));
		assertTrue("key="+CSS_IMPORT_NOURL_TWOMEDIAS+" value=\""+filter(CSS_IMPORT_NOURL_TWOMEDIAS)+"\"", CSS_IMPORT_NOURL_TWOMEDIASC.equals(filter(CSS_IMPORT_NOURL_TWOMEDIAS)));
	}
	
	public void testEscape() throws IOException, URISyntaxException {
		assertTrue("key="+CSS_ESCAPED_LINK+" value=\""+filter(CSS_ESCAPED_LINK)+"\"", CSS_ESCAPED_LINKC.equals(filter(CSS_ESCAPED_LINK)));
		assertTrue("key="+CSS_ESCAPED_LINK2+" value=\""+filter(CSS_ESCAPED_LINK2)+"\"", CSS_ESCAPED_LINK2C.equals(filter(CSS_ESCAPED_LINK2)));
	}
	
	public void testProperties() throws IOException, URISyntaxException {
		for(Entry<String, String> entry : propertyTests.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			assertTrue("key=\""+key+"\" encoded=\""+filter(key)+"\" should be \""+value+"\"", value.equals(filter(key)));
		}
	}
	
	private String filter(String css) throws IOException, URISyntaxException {
		StringWriter w = new StringWriter();
		GenericReadFilterCallback cb = new GenericReadFilterCallback(new URI("/CHK@OR904t6ylZOwoobMJRmSn7HsPGefHSP7zAjoLyenSPw,x2EzszO4Kqot8akqmKYXJbkD-fSj6noOVGB-K2YisZ4,AAIC--8/1-works.html"), null);
		CSSParser p = new CSSParser(new StringReader(css), w, false, cb);
		p.parse();
		return w.toString();
	}
}
