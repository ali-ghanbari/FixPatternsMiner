package de.benjaminborbe.xmpp.connector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Message.Type;
import org.slf4j.Logger;

import com.google.inject.Inject;

import de.benjaminborbe.xmpp.config.XmppConfig;

public class XmppConnector {

	private final XmppConfig xmppConfig;

	private final Logger logger;

	private XMPPConnection connection;

	@Inject
	public XmppConnector(final Logger logger, final XmppConfig xmppConfig) {
		this.logger = logger;
		this.xmppConfig = xmppConfig;

	}

	public synchronized void connect() throws XMPPException {
		if (connection != null) {
			logger.warn("already connected");
			return;
		}
		final ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration(xmppConfig.getServerHost(), xmppConfig.getServerPort(), "gmail.com");
		// connectionConfiguration.setCompressionEnabled(false);
		// connectionConfiguration.setSASLAuthenticationEnabled(true);
		// connectionConfiguration.setSecurityMode(SecurityMode.disabled);

		connection = new XMPPConnection(connectionConfiguration);
		connection.connect();
		connection.addConnectionListener(new MyConnectionListener());

		logger.debug("isConnected: " + connection.isConnected());
		connection.login(xmppConfig.getUsername(), xmppConfig.getPassword());
		logger.debug("isAuthenticated: " + connection.isAuthenticated());

		final ChatManager chatManager = connection.getChatManager();
		chatManager.addChatListener(new MyChatManagerListener());

		final Roster roster = connection.getRoster();
		roster.addRosterListener(new MyRosterListener());

	}

	public XmppUser getMe() {

		return new XmppUser(connection.getUser());
	}

	public void disconnect() {
		if (connection != null)
			connection.disconnect();
	}

	public void sendMessage(final XmppUser user, final String message) throws XMPPException {
		final ChatManager chatManager = connection.getChatManager();
		final MessageListener messageListener = new MyMessageListener();
		final Chat chat = chatManager.createChat(user.getUid(), messageListener);
		chat.sendMessage(message);
	}

	public XmppUser getUserByName(final String name) {
		final List<XmppUser> users = getUsers();
		for (final XmppUser user : users) {
			if (user.getName().equals(name)) {
				return user;
			}
		}
		return null;
	}

	public List<XmppUser> getUsers() {
		final List<XmppUser> users = new ArrayList<XmppUser>();
		final Roster roster = connection.getRoster();
		final Collection<RosterEntry> entries = roster.getEntries();
		for (final RosterEntry entry : entries) {
			users.add(new XmppUser(entry));
			logger.debug(String.format("Buddy: " + entry.getName() + " Status: " + entry.getStatus() + " id: " + entry.getUser()));
		}
		return users;
	}

	private final class MyRosterListener implements RosterListener {

		@Override
		public void presenceChanged(final Presence presence) {
			logger.debug("presenceChanged");
		}

		@Override
		public void entriesUpdated(final Collection<String> addresses) {
			logger.debug("entriesUpdated");
		}

		@Override
		public void entriesDeleted(final Collection<String> addresses) {
			logger.debug("entriesDeleted");
		}

		@Override
		public void entriesAdded(final Collection<String> addresses) {
			logger.debug("entriesAdded");
		}
	}

	private final class MyConnectionListener implements ConnectionListener {

		@Override
		public void reconnectionSuccessful() {
			logger.debug("reconnectionSuccessful");
		}

		@Override
		public void reconnectionFailed(final Exception e) {
			logger.debug("reconnectionFailed");
		}

		@Override
		public void reconnectingIn(final int seconds) {
			logger.debug("reconnectingIn");
		}

		@Override
		public void connectionClosedOnError(final Exception e) {
			logger.debug("connectionClosedOnError");
		}

		@Override
		public void connectionClosed() {
			logger.debug("connectionClosed");
		}
	}

	private final class MyChatManagerListener implements ChatManagerListener {

		@Override
		public void chatCreated(final Chat chat, final boolean createdLocally) {
			logger.debug("chatCreated chat: " + chat.getParticipant() + " local: " + createdLocally);
		}
	}

	private final class MyMessageListener implements MessageListener {

		@Override
		public void processMessage(final Chat chat, final Message message) {
			logger.debug("processMessage - chat: " + chat.getParticipant() + " message: " + message);
			// logger.debug("getBody: " + message.getBody());
			// logger.debug("getFrom: " + message.getFrom());
			// logger.debug("getLanguage: " + message.getLanguage());
			// logger.debug("getPacketID: " + message.getPacketID());
			// logger.debug("getSubject: " + message.getSubject());
			// logger.debug("getThread: " + message.getThread());
			// logger.debug("getTo: " + message.getTo());
			// logger.debug("getType: " + message.getType());
			final String from = message.getFrom();
			final String body = message.getBody();
			if (Type.chat.equals(message.getType()))
				logger.debug(String.format("Received message '%1$s' from %2$s", body, from));
		}

	}
}
