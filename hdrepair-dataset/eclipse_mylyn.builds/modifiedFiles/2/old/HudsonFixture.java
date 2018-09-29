/*******************************************************************************
 * Copyright (c) 2010 Markus Knittig and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Markus Knittig - initial API and implementation
 *     Benjamin Muskalla - bug 324039: [build] tests fail with NPE
 *******************************************************************************/

package org.eclipse.mylyn.hudson.tests.support;

import java.net.Proxy;

import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.commons.net.IProxyProvider;
import org.eclipse.mylyn.commons.net.WebLocation;
import org.eclipse.mylyn.internal.hudson.core.HudsonCorePlugin;
import org.eclipse.mylyn.internal.hudson.core.client.HudsonConfigurationCache;
import org.eclipse.mylyn.internal.hudson.core.client.RestfulHudsonClient;
import org.eclipse.mylyn.tests.util.TestFixture;
import org.eclipse.mylyn.tests.util.TestUtil;
import org.eclipse.mylyn.tests.util.TestUtil.Credentials;
import org.eclipse.mylyn.tests.util.TestUtil.PrivilegeLevel;

/**
 * Initializes Hudson repositories to a defined state. This is done once per test run, since cleaning and initializing
 * the repository for each test method would take too long.
 * 
 * @author Markus Knittig
 */
public class HudsonFixture extends TestFixture {

	public static final String SERVER = System.getProperty("mylyn.hudson.server", "mylyn.eclipse.org/hudson");

	public final static String HUDSON_TEST_URL = "http://" + SERVER;

	private static HudsonFixture current;

	private static final HudsonFixture DEFAULT = new HudsonFixture(HUDSON_TEST_URL, "1.339", "REST");

	/**
	 * Standard configurations for running all test against.
	 */
	public static final HudsonFixture[] ALL = new HudsonFixture[] { DEFAULT };

	public static HudsonFixture current() {
		return current(DEFAULT);
	}

	public static HudsonFixture current(HudsonFixture fixture) {
		if (current == null) {
			fixture.activate();
		}
		return current;
	}

	private String version;

	public HudsonFixture() {
		super(HudsonCorePlugin.CONNECTOR_KIND, HUDSON_TEST_URL);
	}

	public HudsonFixture(String url, String version, String info) {
		super(HudsonCorePlugin.CONNECTOR_KIND, url);
		this.version = version;
		setInfo("Hudson", version, info);
	}

	@Override
	protected TestFixture activate() {
		current = this;
		setUpFramework();
		return this;
	}

	public RestfulHudsonClient connect() throws Exception {
		return connect(getRepositoryUrl());
	}

	public RestfulHudsonClient connect(PrivilegeLevel level) throws Exception {
		return connect(repositoryUrl, Proxy.NO_PROXY, level);
	}

	public RestfulHudsonClient connect(String url) throws Exception {
		return connect(url, Proxy.NO_PROXY, PrivilegeLevel.USER);
	}

	public RestfulHudsonClient connect(String url, Proxy proxy, PrivilegeLevel level) throws Exception {
		Credentials credentials = TestUtil.readCredentials(level);
		return connect(url, credentials.username, credentials.password, proxy);
	}

	public RestfulHudsonClient connect(String url, String username, String password) throws Exception {
		return connect(url, username, password, Proxy.NO_PROXY);
	}

	public RestfulHudsonClient connect(String url, String username, String password, final Proxy proxy)
			throws Exception {
		WebLocation location = new WebLocation(url, username, password, new IProxyProvider() {
			public Proxy getProxyForHost(String host, String proxyType) {
				return proxy;
			}
		});
		if (username != null && password != null) {
			location.setCredentials(AuthenticationType.HTTP, username, password);
		}
		RestfulHudsonClient hudsonClient = new RestfulHudsonClient(location, new HudsonConfigurationCache());
		return hudsonClient;
	}

	@Override
	protected HudsonFixture getDefault() {
		return DEFAULT;
	}

	public String getVersion() {
		return version;
	}

}
