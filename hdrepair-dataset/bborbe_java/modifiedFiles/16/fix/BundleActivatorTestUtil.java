package de.benjaminborbe.tools.osgi.test;

import java.util.Dictionary;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import de.benjaminborbe.tools.osgi.mock.ExtHttpServiceMock;
import de.benjaminborbe.tools.url.UrlUtilImpl;

public class BundleActivatorTestUtil {

	public ExtHttpServiceMock startBundle(final BundleActivator bundleActivator) throws Exception {
		final long bundleId = 1337l;

		final Bundle bundle = EasyMock.createMock(Bundle.class);
		EasyMock.expect(bundle.getBundleId()).andReturn(bundleId);
		EasyMock.replay(bundle);

		final Filter filter = EasyMock.createMock(Filter.class);
		EasyMock.replay(filter);

		final ExtHttpServiceMock extBundle = new ExtHttpServiceMock(new UrlUtilImpl());

		final ServiceReference extServiceReference = EasyMock.createMock(ServiceReference.class);
		EasyMock.expect(extServiceReference.getBundle()).andReturn(extBundle);
		EasyMock.replay(extServiceReference);

		final ServiceRegistration serviceRegistration = EasyMock.createMock(ServiceRegistration.class);
		EasyMock.replay(serviceRegistration);

		final ServiceReference[] extServiceReferences = new ServiceReference[] { extServiceReference };
		final ServiceReference[] emptyServiceReferences = new ServiceReference[] {};

		final BundleContext context = EasyMock.createMock(BundleContext.class);
		context.addServiceListener(EasyMock.anyObject(ServiceListener.class), EasyMock.anyObject(String.class));
		EasyMock.expectLastCall().anyTimes();
		EasyMock.expect(context.getBundle()).andReturn(bundle).anyTimes();
		EasyMock.expect(context.createFilter(EasyMock.anyObject(String.class))).andReturn(filter).anyTimes();

		EasyMock.expect(context.getServiceReferences("org.apache.felix.http.api.ExtHttpService", null)).andReturn(extServiceReferences).anyTimes();
		EasyMock.expect(context.getServiceReferences("de.benjaminborbe.cron.api.CronJob", null)).andReturn(emptyServiceReferences).anyTimes();
		EasyMock.expect(context.getServiceReferences("de.benjaminborbe.search.api.SearchServiceComponent", null)).andReturn(emptyServiceReferences).anyTimes();
		EasyMock.expect(context.getServiceReferences("de.benjaminborbe.dashboard.api.DashboardContentWidget", null)).andReturn(emptyServiceReferences).anyTimes();
		EasyMock.expect(context.getServiceReferences("de.benjaminborbe.navigation.api.NavigationEntry", null)).andReturn(emptyServiceReferences).anyTimes();
		EasyMock.expect(context.getServiceReferences("de.benjaminborbe.crawler.api.CrawlerNotifier", null)).andReturn(emptyServiceReferences).anyTimes();
		EasyMock.expect(context.getServiceReferences("de.benjaminborbe.configuration.api.Configuration", null)).andReturn(emptyServiceReferences).anyTimes();
		EasyMock.expect(context.getServiceReferences("de.benjaminborbe.search.api.SearchSpecial", null)).andReturn(emptyServiceReferences).anyTimes();
		EasyMock.expect(context.getServiceReferences("de.benjaminborbe.configuration.api.ConfigurationDescription", null)).andReturn(emptyServiceReferences).anyTimes();
		EasyMock.expect(context.getServiceReferences("de.benjaminborbe.xmpp.api.XmppCommand", null)).andReturn(emptyServiceReferences).anyTimes();
		EasyMock.expect(context.getServiceReferences("de.benjaminborbe.messageservice.api.MessageConsumer", null)).andReturn(emptyServiceReferences).anyTimes();
		EasyMock.expect(context.getService(extServiceReference)).andReturn(extBundle);

		EasyMock.expect(context.registerService(EasyMock.anyObject(String.class), EasyMock.anyObject(Object.class), EasyMock.anyObject(Dictionary.class)))
				.andReturn(serviceRegistration).anyTimes();
		EasyMock.replay(context);

		bundleActivator.start(context);
		return extBundle;
	}
}
