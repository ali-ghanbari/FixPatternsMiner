/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.subtree;


import org.apache.ldap.server.AbstractAdminTestCase;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.exception.LdapNoSuchAttributeException;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.Map;
import java.util.HashMap;


/**
 * Testcases for the SubentryService.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubentryServiceTest extends AbstractAdminTestCase
{
    public Attributes getTestEntry( String cn )
    {
        Attributes subentry = new LockableAttributesImpl();
        Attribute objectClass = new LockableAttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "person" );
        subentry.put( objectClass );
        subentry.put( "cn", cn );
        subentry.put( "sn", "testentry" );
        return subentry;
    }


    public Attributes getTestSubentry()
    {
        Attributes subentry = new LockableAttributesImpl();
        Attribute objectClass = new LockableAttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        subentry.put( objectClass );
        subentry.put( "subtreeSpecification", "{ base \"ou=configuration\" }" );
        subentry.put( "cn", "testsubentry" );
        return subentry;
    }


    public void addAdministrativeRole( String role ) throws NamingException
    {
        Attribute attribute = new LockableAttributeImpl( "administrativeRole" );
        attribute.add( role );
        ModificationItem item = new ModificationItem( DirContext.ADD_ATTRIBUTE, attribute );
        super.sysRoot.modifyAttributes( "", new ModificationItem[] { item } );
    }


    public Map getAllEntries() throws NamingException
    {
        Map resultMap = new HashMap();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[] { "+", "*" } );
        NamingEnumeration results = super.sysRoot.search( "", "(objectClass=*)", controls );
        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            resultMap.put( result.getName(), result.getAttributes() );
        }
        return resultMap;
    }


    public void testEntryAdd() throws NamingException
    {
        addAdministrativeRole( "autonomousArea" );
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        super.sysRoot.createSubcontext( "cn=unmarked", getTestEntry( "unmarked" ) );
        super.sysRoot.createSubcontext( "cn=marked,ou=configuration", getTestEntry( "marked" ) );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes marked = ( Attributes ) results.get( "cn=marked,ou=configuration,ou=system" );
        Attribute autonomousSubentry = marked.get( SubentryService.AUTONOUMOUS_AREA_SUBENTRY );
        assertNotNull( "ou=marked,ou=configuration,ou=system should be marked", autonomousSubentry );
        assertEquals( "cn=testsubentry,ou=system", autonomousSubentry.get() );
        assertEquals( 1, autonomousSubentry.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes unmarked = ( Attributes ) results.get( "cn=unmarked,ou=system" );
        assertNull( "cn=unmarked,ou=system should not be marked",
                unmarked.get( SubentryService.AUTONOUMOUS_AREA_SUBENTRY ) );
    }


    public void testSubentryAdd() throws NamingException
    {
        try
        {
            super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
            fail( "should never get here: cannot create subentry under regular entries" );
        }
        catch ( LdapNoSuchAttributeException e ) {}

        addAdministrativeRole( "autonomousArea" );
        super.sysRoot.createSubcontext( "cn=testsubentry", getTestSubentry() );
        Map results = getAllEntries();

        // --------------------------------------------------------------------
        // Make sure entries selected by the subentry do have the mark
        // --------------------------------------------------------------------

        Attributes configuration = ( Attributes ) results.get( "ou=configuration,ou=system" );
        Attribute autonomousSubentry = configuration.get( SubentryService.AUTONOUMOUS_AREA_SUBENTRY );
        assertNotNull( "ou=configuration,ou=system should be marked", autonomousSubentry );
        assertEquals( "cn=testsubentry,ou=system", autonomousSubentry.get() );
        assertEquals( 1, autonomousSubentry.size() );

        Attributes interceptors = ( Attributes ) results.get( "ou=interceptors,ou=configuration,ou=system" );
        autonomousSubentry = interceptors.get( SubentryService.AUTONOUMOUS_AREA_SUBENTRY );
        assertNotNull( "ou=interceptors,ou=configuration,ou=system should be marked", autonomousSubentry );
        assertEquals( "cn=testsubentry,ou=system", autonomousSubentry.get() );
        assertEquals( 1, autonomousSubentry.size() );

        Attributes partitions = ( Attributes ) results.get( "ou=partitions,ou=configuration,ou=system" );
        autonomousSubentry = partitions.get( SubentryService.AUTONOUMOUS_AREA_SUBENTRY );
        assertNotNull( "ou=partitions,ou=configuration,ou=system should be marked", autonomousSubentry );
        assertEquals( "cn=testsubentry,ou=system", autonomousSubentry.get() );
        assertEquals( 1, autonomousSubentry.size() );

        Attributes services = ( Attributes ) results.get( "ou=services,ou=configuration,ou=system" );
        autonomousSubentry = services.get( SubentryService.AUTONOUMOUS_AREA_SUBENTRY );
        assertNotNull( "ou=services,ou=configuration,ou=system should be marked", autonomousSubentry );
        assertEquals( "cn=testsubentry,ou=system", autonomousSubentry.get() );
        assertEquals( 1, autonomousSubentry.size() );

        // --------------------------------------------------------------------
        // Make sure entries not selected by subentry do not have the mark
        // --------------------------------------------------------------------

        Attributes system = ( Attributes ) results.get( "ou=system" );
        assertNull( "ou=system should not be marked",
                system.get( SubentryService.AUTONOUMOUS_AREA_SUBENTRY ) );

        Attributes users = ( Attributes ) results.get( "ou=users,ou=system" );
        assertNull( "ou=users,ou=system should not be marked",
                users.get( SubentryService.AUTONOUMOUS_AREA_SUBENTRY ) );

        Attributes groups = ( Attributes ) results.get( "ou=groups,ou=system" );
        assertNull( "ou=groups,ou=system should not be marked",
                groups.get( SubentryService.AUTONOUMOUS_AREA_SUBENTRY ) );

        Attributes admin = ( Attributes ) results.get( "uid=admin,ou=system" );
        assertNull( "uid=admin,ou=system should not be marked",
                admin.get( SubentryService.AUTONOUMOUS_AREA_SUBENTRY ) );

        Attributes sysPrefRoot = ( Attributes ) results.get( "prefNodeName=sysPrefRoot,ou=system" );
        assertNull( "prefNode=sysPrefRoot,ou=system should not be marked",
                sysPrefRoot.get( SubentryService.AUTONOUMOUS_AREA_SUBENTRY ) );

    }
}
