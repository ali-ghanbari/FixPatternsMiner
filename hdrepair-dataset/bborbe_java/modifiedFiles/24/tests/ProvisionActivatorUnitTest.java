package de.benjaminborbe.bridge;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.easymock.EasyMock;
import org.junit.Test;

public class ProvisionActivatorUnitTest {

	@Test
	public void testGetProperties() {
		final ServletContext servletContext = EasyMock.createMock(ServletContext.class);
		final InputStream inputStream = new ByteArrayInputStream("bb.bundle.1 = bla".getBytes());
		EasyMock.expect(servletContext.getResourceAsStream("/WEB-INF/activator.properties")).andReturn(inputStream);
		EasyMock.replay(servletContext);
		final ProvisionActivator pa = new ProvisionActivator(servletContext);
		final Properties props = pa.getProperties();
		assertTrue(props.containsKey("bb.bundle.1"));
	}

}
