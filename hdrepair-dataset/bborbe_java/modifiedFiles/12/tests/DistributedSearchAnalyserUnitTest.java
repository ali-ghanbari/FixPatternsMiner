package de.benjaminborbe.distributed.search.util;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DistributedSearchAnalyserUnitTest {

	@Test
	public void testParse() throws Exception {
		final DistributedSearchAnalyser analyser = new DistributedSearchAnalyser();
		assertThat(analyser.parseSearchTerm("foo").size(), is(1));
		assertThat(analyser.parseSearchTerm("foo"), is(hasItem("foo")));
	}
}
