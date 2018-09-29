/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portalweb.portal.ldap;

import com.liferay.portalweb.portal.BaseTests;
import com.liferay.portalweb.portal.StopSeleniumTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * <a href="LDAPTests.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 */
public class LDAPTests extends BaseTests {

	public static Test suite() {
		TestSuite testSuite = new TestSuite();

		testSuite.addTestSuite(AssertNoLDAPUsersTest.class);
		testSuite.addTestSuite(EnableLDAPTest.class);
		testSuite.addTestSuite(AssertLDAPConnectionTest.class);
		testSuite.addTestSuite(AssertLDAPUsersTest.class);
		testSuite.addTestSuite(LogoutTest.class);
		testSuite.addTestSuite(LoginJaneLDAPTest.class);
		testSuite.addTestSuite(LogoutTest.class);
		testSuite.addTestSuite(LoginLukeLDAPTest.class);
		testSuite.addTestSuite(LogoutTest.class);
		testSuite.addTestSuite(LoginMartinLDAPTest.class);
		testSuite.addTestSuite(LogoutTest.class);
		testSuite.addTestSuite(LoginAdminTest.class);
		testSuite.addTestSuite(AssertLDAPUsersPresentTest.class);

		testSuite.addTestSuite(StopSeleniumTest.class);

		return testSuite;
	}

}