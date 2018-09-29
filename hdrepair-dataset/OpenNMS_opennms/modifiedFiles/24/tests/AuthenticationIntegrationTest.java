/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2011 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.springframework.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.netmgt.config.UserFactory;
import org.opennms.netmgt.dao.db.JUnitConfigurationEnvironment;
import org.opennms.netmgt.dao.db.JUnitTemporaryDatabase;
import org.opennms.netmgt.model.OnmsUser;
import org.opennms.test.ThrowableAnticipator;
import org.opennms.test.mock.MockLogAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.dao.DaoAuthenticationProvider;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.Assert;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations={
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath*:/META-INF/opennms/component-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-daemon.xml",
        "classpath:/META-INF/opennms/mockEventIpcManager.xml",
        "classpath:/META-INF/opennms/applicationContext-mock-usergroup.xml",
        "classpath:/META-INF/opennms/applicationContext-minimal-conf.xml",
        "classpath:/org/opennms/web/springframework/security/AuthenticationIntegrationTest-context.xml"
})
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
public class AuthenticationIntegrationTest implements InitializingBean {

	@Autowired
	private DaoAuthenticationProvider m_provider; 

	@Before
	public void setUp() {
	    MockLogAppender.setupLogging(true, "DEBUG");
	}
	
	@Test
	public void testAuthenticateAdmin() {
	    Authentication authentication = new UsernamePasswordAuthenticationToken("admin", "admin");
		Authentication authenticated = m_provider.authenticate(authentication);
		assertNotNull("authenticated Authentication object not null", authenticated);
		GrantedAuthority[] authorities = authenticated.getAuthorities();
		assertNotNull("GrantedAuthorities should not be null", authorities);
		assertEquals("GrantedAuthorities size", 2, authorities.length);
		assertEquals("GrantedAuthorities zero role", "ROLE_USER", authorities[0].getAuthority());
		assertEquals("GrantedAuthorities two name", "ROLE_ADMIN", authorities[1].getAuthority());
	}
	
	@Test
	public void testAuthenticateRtc() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("rtc", "rtc");
		Authentication authenticated = m_provider.authenticate(authentication);
		assertNotNull("authenticated Authentication object not null", authenticated);
		GrantedAuthority[] authorities = authenticated.getAuthorities();
		assertNotNull("GrantedAuthorities should not be null", authorities);
		assertEquals("GrantedAuthorities size", 1, authorities.length);
		assertEquals("GrantedAuthorities one name", "ROLE_RTC", authorities[0].getAuthority());
	}
	
	@Test
	public void testAuthenticateTempUser() throws Exception {
        OnmsUser user = new OnmsUser("tempuser");
	    user.setFullName("Temporary User");
	    user.setPassword("18126E7BD3F84B3F3E4DF094DEF5B7DE");
	    user.setDutySchedule(Arrays.asList("MoTuWeThFrSaSu800-2300"));
	    UserFactory.getInstance().save(user);

		Authentication authentication = new UsernamePasswordAuthenticationToken("tempuser", "mike");
		Authentication authenticated = m_provider.authenticate(authentication);
		assertNotNull("authenticated Authentication object not null", authenticated);
		GrantedAuthority[] authorities = authenticated.getAuthorities();
		assertNotNull("GrantedAuthorities should not be null", authorities);
		assertEquals("GrantedAuthorities size", 1, authorities.length);
		assertEquals("GrantedAuthorities zero role", "ROLE_USER", authorities[0].getAuthority());
	}
	
	@Test
	public void testAuthenticateBadUsername() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("badUsername", "admin");
		
		ThrowableAnticipator ta = new ThrowableAnticipator();
		ta.anticipate(new BadCredentialsException("Bad credentials"));
		try {
			m_provider.authenticate(authentication);
		} catch (Throwable t) {
			ta.throwableReceived(t);
		}
		ta.verifyAnticipated();
	}
	
	@Test
	public void testAuthenticateBadPassword() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("admin", "badPassword");

		ThrowableAnticipator ta = new ThrowableAnticipator();
		ta.anticipate(new BadCredentialsException("Bad credentials"));
		try {
			m_provider.authenticate(authentication);
		} catch (Throwable t) {
			ta.throwableReceived(t);
		}
		ta.verifyAnticipated();
	}

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(m_provider);
    }
}
