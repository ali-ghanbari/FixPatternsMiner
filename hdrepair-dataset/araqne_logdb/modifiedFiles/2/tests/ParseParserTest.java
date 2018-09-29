/*
 * Copyright 2013 Future Systems
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.araqne.logdb.query.parser;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.araqne.log.api.LogParser;
import org.araqne.log.api.LogParserProfile;
import org.araqne.log.api.LogParserRegistry;
import org.araqne.logdb.FunctionRegistry;
import org.araqne.logdb.QueryCommand;
import org.araqne.logdb.QueryContext;
import org.araqne.logdb.QueryParseException;
import org.araqne.logdb.QueryParserService;
import org.junit.Test;

public class ParseParserTest {
	@Test
	public void parseSuccessTest() {
		assertEquals("parse test", getParseResult("parse test"));
		
		assertEquals("parse ass", getParseResult("parse ass"));

		assertEquals("parse \"[Test: *]\" as test, \"[Time: *]\" as time", 
				getParseResult("parse \"[Test: *]\" as test, \"[Time: *]\" as time"));
	}

	@Test
	public void parseFailureTest() {
		try {
			assertEquals("parse \"t\" for as", getParseResult("parse \"t\" for as"));
			fail();
		} catch (QueryParseException e) {
			assertEquals("21003", e.getType());
		}

		try {
			assertEquals("parse t as as", getParseResult("parse t as as"));
			fail();
		} catch (QueryParseException e) {
			assertEquals("21004", e.getType());
		}

		try {
			assertEquals("parse \"t\" as \"2\"", getParseResult("parse \"t\" for \"2\""));
			fail();
		} catch (QueryParseException e) {
			assertEquals("21003", e.getType());
		}


	}

	private String getParseResult(String testString) {
		ParseParser pp = new ParseParser(mockLogParserRegistry());

		pp.setQueryParserService(mockQueryParserService());

		QueryCommand p1 = pp.parse(mockQueryContext(), testString);

		return p1.toString();
	}

	private QueryParserService mockQueryParserService() {
		QueryParserService qps = mock(QueryParserService.class);
		when(qps.getFunctionRegistry()).thenReturn(mockFunctionRegistry());

		return qps;
	}

	private FunctionRegistry mockFunctionRegistry() {
		FunctionRegistry fr = mock(FunctionRegistry.class);

		return fr;
	}

	private LogParserRegistry mockLogParserRegistry() {
		LogParserRegistry lpr = mock(LogParserRegistry.class);

		when(lpr.getProfile("test")).thenReturn(mock(LogParserProfile.class));
		when(lpr.getProfile("ass")).thenReturn(mock(LogParserProfile.class));
		when(lpr.newParser("test")).thenReturn(mock(LogParser.class));
		when(lpr.newParser("ass")).thenReturn(mock(LogParser.class));
		return lpr;
	}

	private QueryContext mockQueryContext() {
		QueryContext ret = mock(QueryContext.class);

		return ret;

	}
}
