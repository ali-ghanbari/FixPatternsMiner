package de.benjaminborbe.xmlrpc.service;

import com.google.inject.Injector;
import de.benjaminborbe.tools.guice.GuiceInjectorBuilder;
import de.benjaminborbe.xmlrpc.api.XmlrpcService;
import de.benjaminborbe.xmlrpc.guice.XmlrpcModulesMock;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertNotNull;

public class XmlrpcServiceImplIntegrationTest {

	private final static String CONFLUENCE_URL = "https://www.benjamin-borbe.de/confluence/rpc/xmlrpc";

	private final static String CONFLUENCE_USERNAME = "test";

	private final static String CONFLUENCE_PASSWORD = "z9W7CUwY4brR";

	@Test
	public void testInject() {
		final Injector injector = GuiceInjectorBuilder.getInjector(new XmlrpcModulesMock());
		assertNotNull(injector.getInstance(XmlrpcService.class));
	}

	@Test
	public void testCall() throws Exception {
		final Injector injector = GuiceInjectorBuilder.getInjector(new XmlrpcModulesMock());
		final XmlrpcService xmlrpcService = injector.getInstance(XmlrpcService.class);
		final String token = (String) xmlrpcService.execute(new URL(CONFLUENCE_URL), "confluence1.login", new Object[]{CONFLUENCE_USERNAME, CONFLUENCE_PASSWORD});
		assertNotNull(token);
	}
}
