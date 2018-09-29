package de.benjaminborbe.xmpp.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.List;

import org.junit.Test;

import com.google.inject.Injector;

import de.benjaminborbe.configuration.api.ConfigurationIdentifier;
import de.benjaminborbe.configuration.api.ConfigurationService;
import de.benjaminborbe.tools.guice.GuiceInjectorBuilder;
import de.benjaminborbe.xmpp.XmppConstants;
import de.benjaminborbe.xmpp.config.XmppConfig;
import de.benjaminborbe.xmpp.guice.XmppModulesMock;

public class XmppConnectorIntegrationTest {

	@Test
	public void testInject() {
		final Injector injector = GuiceInjectorBuilder.getInjector(new XmppModulesMock());
		final XmppConnector xmppConnector = injector.getInstance(XmppConnector.class);
		assertNotNull(xmppConnector);
	}

	@Test
	public void testRun() throws Exception {
		final Injector injector = GuiceInjectorBuilder.getInjector(new XmppModulesMock());
		final XmppConnector xmppConnector = injector.getInstance(XmppConnector.class);

		final ConfigurationService configurationService = injector.getInstance(ConfigurationService.class);
		configurationService.setConfigurationValue(new ConfigurationIdentifier(XmppConstants.CONFIG_USERNAME), "bb");
		configurationService.setConfigurationValue(new ConfigurationIdentifier(XmppConstants.CONFIG_PASSWORD), "5VCrQO5jMHOE");
		configurationService.setConfigurationValue(new ConfigurationIdentifier(XmppConstants.CONFIG_SERVERHOST), "127.0.0.1");
		configurationService.setConfigurationValue(new ConfigurationIdentifier(XmppConstants.CONFIG_SERVERPORT), "5222");

		final XmppConfig xmppConfig = injector.getInstance(XmppConfig.class);
		assertEquals("127.0.0.1", xmppConfig.getServerHost());
		assertEquals(5222, xmppConfig.getServerPort());
		assertEquals("bb", xmppConfig.getUsername());
		assertEquals("5VCrQO5jMHOE", xmppConfig.getPassword());
		try {
			xmppConnector.connect();
			{
				final List<XmppUser> users = xmppConnector.getUsers();
				assertNotNull(users);
			}
			{
				final XmppUser user = xmppConnector.getMe();
				assertNotNull(user);
				xmppConnector.sendMessage(user, "hello");
			}
			{
				final XmppUser user = new XmppUser("bborbe@mobile-bb/mobile-bb");
				xmppConnector.sendMessage(user, "hello " + user.getUid());
			}
			{
				final XmppUser user = new XmppUser("bb@mobile-bb/Smack");
				xmppConnector.sendMessage(user, "hello " + user.getUid());
			}

		}
		finally {
			xmppConnector.disconnect();
		}
	}
}
