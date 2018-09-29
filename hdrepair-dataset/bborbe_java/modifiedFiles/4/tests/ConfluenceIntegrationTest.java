package de.benjaminborbe.confluence.test;

import de.benjaminborbe.authentication.api.AuthenticationService;
import de.benjaminborbe.authentication.api.SessionIdentifier;
import de.benjaminborbe.authentication.api.UserIdentifier;
import de.benjaminborbe.confluence.api.ConfluenceInstance;
import de.benjaminborbe.confluence.api.ConfluenceInstanceIdentifier;
import de.benjaminborbe.confluence.api.ConfluenceService;
import de.benjaminborbe.storage.api.StorageService;
import de.benjaminborbe.test.osgi.TestCaseOsgi;
import de.benjaminborbe.test.osgi.TestUtil;
import de.benjaminborbe.tools.osgi.mock.ExtHttpServiceMock;
import de.benjaminborbe.tools.url.UrlUtilImpl;
import org.apache.felix.http.api.ExtHttpService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Collection;

public class ConfluenceIntegrationTest extends TestCaseOsgi {

	private final static String CONFLUENCE_URL = "https://www.benjamin-borbe.de/confluence";

	private final static String CONFLUENCE_USERNAME = "test";

	private final static String CONFLUENCE_PASSWORD = "z9W7CUwY4brR";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetExtHttpService() {
		final BundleContext bundleContext = getContext();
		assertNotNull(bundleContext);
		final ExtHttpServiceMock extHttpService = new ExtHttpServiceMock(new UrlUtilImpl());
		assertNotNull(extHttpService);
		// zum start: keine Dienste registriert
		assertEquals(0, extHttpService.getRegisterFilterCallCounter());
		assertEquals(0, extHttpService.getRegisterServletCallCounter());
		assertEquals(0, extHttpService.getUnregisterFilterCallCounter());
		assertEquals(0, extHttpService.getUnregisterServletCallCounter());
		final ServiceRegistration serviceRegistration = bundleContext.registerService(ExtHttpService.class.getName(), extHttpService, null);
		assertNotNull(serviceRegistration);
		// nach start: Dienste vorhanden?
		assertTrue("no filters registered", extHttpService.getRegisterFilterCallCounter() > 0);
		assertTrue("no servlets registered.", extHttpService.getRegisterServletCallCounter() > 0);
		assertEquals(0, extHttpService.getUnregisterFilterCallCounter());
		assertEquals(0, extHttpService.getUnregisterServletCallCounter());

		// do unregister
		serviceRegistration.unregister();

		assertTrue("no servlets unregistered", extHttpService.getUnregisterServletCallCounter() > 0);
		assertEquals(extHttpService.getRegisterServletCallCounter(), extHttpService.getRegisterServletCallCounter());
		assertEquals(extHttpService.getRegisterFilterCallCounter(), extHttpService.getUnregisterFilterCallCounter());
	}

	// public void testServices() throws Exception {
	// final BundleContext bundleContext = getContext();
	// assertNotNull(bundleContext);
	// for (final ServiceReference a : bundleContext.getAllServiceReferences(null, null)) {
	// // final Bundle bundle = a.getBundle();
	// final Object service = bundleContext.getService(a);
	// System.err.println(service);
	// }
	// }

	public void testConfluenceService() {
		final ConfluenceService confluenceService = getService(ConfluenceService.class);
		assertNotNull(confluenceService);
		assertEquals("de.benjaminborbe.confluence.service.ConfluenceServiceImpl", confluenceService.getClass().getName());
	}

	public void testList() throws Exception {
		final AuthenticationService authenticationService = getService(AuthenticationService.class);
		final StorageService storageService = getService(StorageService.class);
		final TestUtil testUtil = new TestUtil(authenticationService, storageService);
		final SessionIdentifier sessionIdentifier = testUtil.createSessionIdentifier();
		testUtil.createSuperAdmin(sessionIdentifier);

		final ConfluenceService confluenceService = getService(ConfluenceService.class);
		final Collection<ConfluenceInstance> list = confluenceService.getConfluenceInstances(sessionIdentifier);
		assertNotNull(list);
	}

	public void testCreate() throws Exception {
		final AuthenticationService authenticationService = getService(AuthenticationService.class);
		final StorageService storageService = getService(StorageService.class);
		final TestUtil testUtil = new TestUtil(authenticationService, storageService);
		final SessionIdentifier sessionIdentifier = testUtil.createSessionIdentifier();
		final UserIdentifier userIdentifier = testUtil.createSuperAdmin(sessionIdentifier);

		final ConfluenceService confluenceService = getService(ConfluenceService.class);
		final ConfluenceInstanceIdentifier confluenceInstanceIdentifier = confluenceService.createConfluenceIntance(sessionIdentifier, CONFLUENCE_URL, CONFLUENCE_USERNAME,
			CONFLUENCE_PASSWORD, 28, true, 5000l, true, userIdentifier.getId());
		assertNotNull(confluenceInstanceIdentifier);
	}
}
