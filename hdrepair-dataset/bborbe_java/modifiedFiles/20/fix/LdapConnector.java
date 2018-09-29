package de.benjaminborbe.authentication.ldap;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.benjaminborbe.authentication.config.AuthenticationConfig;
import de.benjaminborbe.tools.util.ResourceUtil;

@Singleton
public class LdapConnector {

	private static final String KEYSTORE_PATH = "keystore.jks";

	private final AuthenticationConfig authenticationConfig;

	private final Logger logger;

	private final ResourceUtil resourceUtil;

	@Inject
	public LdapConnector(final Logger logger, final AuthenticationConfig authenticationConfig, final ResourceUtil resourceUtil) {
		this.logger = logger;
		this.authenticationConfig = authenticationConfig;
		this.resourceUtil = resourceUtil;
	}

	public boolean verify(final String username, final String password) throws LdapException {
		final Hashtable<String, String> env = getUserEnv(username, password);
		try {
			final DirContext ctx = getDirContext(env);
			logger.debug("login success for " + username);
			final SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			ctx.search("ou=Mitarbeiter,dc=rp,dc=seibert-media,dc=net", "(objectClass=person)", searchControls);
			return true;
		}
		catch (final javax.naming.AuthenticationException e) {
			logger.info("login fail for " + username);
			return false;
		}
		catch (final NamingException e) {
			logger.trace(e.getClass().getName(), e);
			return false;
		}
		catch (final KeyManagementException e) {
			logger.debug(e.getClass().getName(), e);
			throw new LdapException(e);
		}
		catch (final NoSuchAlgorithmException e) {
			logger.debug(e.getClass().getName(), e);
			throw new LdapException(e);
		}
		catch (final KeyStoreException e) {
			logger.debug(e.getClass().getName(), e);
			throw new LdapException(e);
		}
		catch (final CertificateException e) {
			logger.debug(e.getClass().getName(), e);
			throw new LdapException(e);
		}
		catch (final IOException e) {
			logger.debug(e.getClass().getName(), e);
			throw new LdapException(e);
		}

	}

	public String getFullname(final String username) throws LdapException {
		try {
			final Hashtable<String, String> env = getReadEnv();

			final DirContext ctx = getDirContext(env);

			final SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			final NamingEnumeration<SearchResult> enumeration = ctx.search("ou=Mitarbeiter,dc=rp,dc=seibert-media,dc=net", "(objectClass=person)", searchControls);
			while (enumeration.hasMore()) {
				final SearchResult searchResult = enumeration.next();
				final Attributes attrs = searchResult.getAttributes();
				final String cn = String.valueOf(attrs.get("cn").get());

				logger.trace("compare " + username + " <=> " + cn);
				if (cn.equalsIgnoreCase(username)) {
					final String displayName = String.valueOf(attrs.get("displayName").get());
					logger.debug("found user " + username + " => " + displayName);
					return displayName;
				}
			}
			logger.debug("no user found " + username);
			return null;
		}
		catch (final NamingException e) {
			throw new LdapException(e);
		}
		catch (final KeyManagementException e) {
			throw new LdapException(e);
		}
		catch (final NoSuchAlgorithmException e) {
			throw new LdapException(e);
		}
		catch (final KeyStoreException e) {
			throw new LdapException(e);
		}
		catch (final CertificateException e) {
			throw new LdapException(e);
		}
		catch (final IOException e) {
			throw new LdapException(e);
		}
	}

	public Collection<String> getUsernames() throws LdapException {
		try {
			final Set<String> result = new HashSet<String>();
			final Hashtable<String, String> env = getReadEnv();
			final DirContext ctx = getDirContext(env);

			final SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			final NamingEnumeration<SearchResult> enumeration = ctx.search("ou=Mitarbeiter,dc=rp,dc=seibert-media,dc=net", "(objectClass=person)", searchControls);
			while (enumeration.hasMore()) {
				final SearchResult searchResult = enumeration.next();
				final Attributes attrs = searchResult.getAttributes();
				final Attribute cn = attrs.get("cn");
				result.add(String.valueOf(cn));
			}
			return result;
		}
		catch (final NamingException e) {
			throw new LdapException(e);
		}
		catch (final KeyManagementException e) {
			throw new LdapException(e);
		}
		catch (final NoSuchAlgorithmException e) {
			throw new LdapException(e);
		}
		catch (final KeyStoreException e) {
			throw new LdapException(e);
		}
		catch (final CertificateException e) {
			throw new LdapException(e);
		}
		catch (final IOException e) {
			throw new LdapException(e);
		}
	}

	protected DirContext getDirContext(final Hashtable<String, String> env) throws NamingException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
			CertificateException, IOException {
		SSLContext context = null;
		try {
			context = SSLContext.getDefault();
			SSLContext.setDefault(getSSLContext());
			return new InitialDirContext(env);
		}
		finally {
			if (context != null)
				SSLContext.setDefault(context);
		}
	}

	private Hashtable<String, String> getBaseEnv() {
		final Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, authenticationConfig.getProviderUrl());
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		if (authenticationConfig.isSSL()) {
			env.put(Context.SECURITY_PROTOCOL, "ssl");
			// env.put("java.naming.ldap.factory.socket", SSLSocketFactoryBase.class.getName());
		}
		return env;
	}

	private Hashtable<String, String> getReadEnv() {
		final Hashtable<String, String> env = getBaseEnv();
		env.put(Context.SECURITY_PRINCIPAL, "cn=ldaplookup,ou=Extern,ou=Mitarbeiter,dc=rp,dc=seibert-media,dc=net");
		env.put(Context.SECURITY_CREDENTIALS, authenticationConfig.getCredentials());
		return env;
	}

	private Hashtable<String, String> getUserEnv(final String username, final String password) {
		final Hashtable<String, String> env = getBaseEnv();
		env.put(Context.SECURITY_PRINCIPAL, authenticationConfig.getDomain() + "\\" + username);
		env.put(Context.SECURITY_CREDENTIALS, password);
		return env;
	}

	private SSLContext getSSLContext() throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, KeyManagementException {
		// load your key store as a stream and initialize a KeyStore
		final InputStream trustStream = resourceUtil.getResourceContentAsInputStream(KEYSTORE_PATH);
		final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

		// if your store is password protected then declare it (it can be null however)
		final char[] trustPassword = "mazdazx".toCharArray();// new char[0];

		// load the stream to your store
		trustStore.load(trustStream, trustPassword);

		// initialize a trust manager factory with the trusted store
		final TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustFactory.init(trustStore);

		// get the trust managers from the factory
		final TrustManager[] trustManagers = trustFactory.getTrustManagers();

		// initialize an ssl context to use these managers and set as default
		final SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, trustManagers, null);
		return sslContext;
	}
}
