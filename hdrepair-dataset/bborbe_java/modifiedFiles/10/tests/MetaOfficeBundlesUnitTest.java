package de.benjaminborbe.meta.office;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;

import de.benjaminborbe.meta.util.BundleResolver;
import de.benjaminborbe.meta.util.BundleResolverImpl;

public class MetaOfficeBundlesUnitTest {

	@Test
	public void testBundles() throws Exception {
		final Logger logger = EasyMock.createNiceMock(Logger.class);
		EasyMock.replay(logger);
		final BundleResolver bundleResolver = new BundleResolverImpl(logger);
		final List<String> names = bundleResolver.getBundleSymbolicNames();
		assertNotNull(names);
		assertTrue(names.size() == 0);
		final Set<String> namesUnique = new HashSet<String>(names);
		assertEquals("dupplicate bundle!", names.size(), namesUnique.size());
	}
}
