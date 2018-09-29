package org.slc.sli.api.security;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slc.sli.api.security.openam.OpenamRestTokenResolver;
import org.slc.sli.api.security.resolve.RolesToRightsResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import org.slc.sli.api.security.enums.Right;
import org.slc.sli.api.security.mock.Mocker;
import org.slc.sli.api.test.WebContextTestExecutionListener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the OpenamRestTokenResolver.
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring/applicationContext-test.xml" })
@TestExecutionListeners({ WebContextTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class })
public class OpenamRestTokenResolverTest {


    private OpenamRestTokenResolver resolver;
    
    private RolesToRightsResolver rightsResolver;
    
    @Before
    public void init() {
        resolver = new OpenamRestTokenResolver();
        resolver.setTokenServiceUrl(Mocker.MOCK_URL);
        resolver.setRest(Mocker.mockRest());
        resolver.setLocator(Mocker.getLocator());
        rightsResolver = mock(RolesToRightsResolver.class);
        resolver.setResolver(rightsResolver);

        Set<GrantedAuthority> rights = new HashSet<GrantedAuthority>();
        rights.add(Right.READ_GENERAL);
        when(rightsResolver.resolveRoles(Arrays.asList(new String[] {"IT Administrator", "parent", "teacher"}))).thenReturn(rights);
    }
    
    
    @Test
    public void testResolveSuccess() {

        Authentication auth = resolver.resolve(Mocker.VALID_TOKEN);
        Assert.assertNotNull(auth);
        Assert.assertTrue(auth.getAuthorities().contains(Right.READ_GENERAL));
    }
    
    @Test
    public void testResolveFailure() {
        when(rightsResolver.resolveRoles(null)).thenReturn(null);
        Authentication auth = resolver.resolve(Mocker.INVALID_TOKEN);
        Assert.assertNull(auth);
    }
}
