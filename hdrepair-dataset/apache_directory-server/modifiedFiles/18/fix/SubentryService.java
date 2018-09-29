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


import org.apache.ldap.server.interceptor.BaseInterceptor;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.enumeration.SearchResultFilter;
import org.apache.ldap.server.enumeration.SearchResultFilteringEnumeration;
import org.apache.ldap.server.invocation.InvocationStack;
import org.apache.ldap.server.jndi.ContextFactoryConfiguration;
import org.apache.ldap.server.configuration.InterceptorConfiguration;
import org.apache.ldap.server.partition.ContextPartitionNexus;
import org.apache.ldap.server.schema.ConcreteNameComponentNormalizer;
import org.apache.ldap.common.message.SubentryRequestControl;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.filter.*;
import org.apache.ldap.common.subtree.SubtreeSpecificationParser;
import org.apache.ldap.common.subtree.SubtreeSpecification;
import org.apache.ldap.common.name.DnParser;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.exception.LdapNoSuchAttributeException;
import org.apache.ldap.common.exception.LdapInvalidAttributeValueException;
import org.apache.ldap.common.exception.LdapSchemaViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ldap.LdapContext;
import javax.naming.ldap.Control;
import javax.naming.directory.*;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.Name;
import java.util.*;


/**
 * The Subentry interceptor service which is responsible for filtering
 * out subentries on search operations and injecting operational attributes
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubentryService extends BaseInterceptor
{
    /** the subentry control OID */
    private static final String SUBENTRY_CONTROL = "1.3.6.1.4.1.4203.1.10.1";
    /** the objectClass value for a subentry */
    private static final String SUBENTRY_OBJECTCLASS = "subentry";
    /** the objectClass OID for a subentry */
    private static final String SUBENTRY_OBJECTCLASS_OID = "2.5.17.0";

    public static final String AUTONOUMOUS_AREA = "autonomousArea";
    public static final String AUTONOUMOUS_AREA_SUBENTRY = "autonomousAreaSubentry";

    public static final String AC_AREA = "accessControlSpecificArea";
    public static final String AC_AREA_SUBENTRY = "accessControlAreaSubentries";

    public static final String AC_INNERAREA = "accessControlInnerArea";
    public static final String AC_INNERAREA_SUBENTRY = "accessControlInnerAreaSubentries";

    public static final String SCHEMA_AREA = "subschemaAdminSpecificArea";
    public static final String SCHEMA_AREA_SUBENTRY = "subschemaSubentry";

    public static final String COLLECTIVE_AREA = "collectiveAttributeSpecificArea";
    public static final String COLLECTIVE_ATTRIBUTE_SUBENTRIES = "collectiveAttributeSubentries";

    public static final String COLLECTIVE_INNERAREA = "collectiveAttributeInnerArea";

    public static final String[] SUBENTRY_OPATTRS = {
        AUTONOUMOUS_AREA_SUBENTRY,
        AC_AREA_SUBENTRY,
        AC_INNERAREA_SUBENTRY,
        SCHEMA_AREA_SUBENTRY,
        COLLECTIVE_ATTRIBUTE_SUBENTRIES
    };

    /**
     * the search result filter to filter out subentries based on objectClass values.
     */
    private static final SearchResultFilter SUBENTRY_FILTER = new SearchResultFilter()
    {
        public boolean accept( LdapContext ctx, SearchResult result, SearchControls controls )
        {
            Attribute objectClasses = result.getAttributes().get( "objectClass" );

            if ( objectClasses == null )
            {
                return true;
            }

            if ( objectClasses.contains( SUBENTRY_OBJECTCLASS ) || objectClasses.contains( SUBENTRY_OBJECTCLASS_OID ) )
            {
                return false;
            }
            return true;
        }
    };

    private static final Logger log = LoggerFactory.getLogger( SubentryService.class );

    /** the hash mapping the DN of a subentry to its SubtreeSpecification */
    private final Map subtrees = new HashMap();
    private ContextFactoryConfiguration factoryCfg;
    private DnParser dnParser;
    private SubtreeSpecificationParser ssParser;
    private SubtreeEvaluator evaluator;
    private ContextPartitionNexus nexus;


    public void init( ContextFactoryConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        super.init( factoryCfg, cfg );
        this.nexus = factoryCfg.getPartitionNexus();
        this.factoryCfg = factoryCfg;
        ConcreteNameComponentNormalizer ncn = new ConcreteNameComponentNormalizer(
                factoryCfg.getGlobalRegistries().getAttributeTypeRegistry() );
        ssParser = new SubtreeSpecificationParser( ncn );
        dnParser = new DnParser( ncn );
        evaluator = new SubtreeEvaluator( factoryCfg.getGlobalRegistries().getOidRegistry() );

        // prepare to find all subentries in all namingContexts
        Iterator suffixes = this.nexus.listSuffixes( true );
        ExprNode filter = new SimpleNode( "objectclass", "subentry", LeafNode.EQUALITY );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[] { "subtreeSpecification" } );

        // search each namingContext for subentries
        while ( suffixes.hasNext() )
        {
            Name suffix = dnParser.parse( ( String ) suffixes.next() );
            NamingEnumeration subentries = nexus.search( suffix, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes subentry = result.getAttributes();
                String dn = result.getName();
                String subtree = ( String ) subentry.get( "subtreeSpecification" ).get();
                SubtreeSpecification ss = null;

                try
                {
                    ss = ssParser.parse( subtree );
                }
                catch ( Exception e )
                {
                    log.warn( "Failed while parsing subtreeSpecification for " + dn );
                    continue;
                }

                subtrees.put( dnParser.parse( dn ).toString(), ss );
            }
        }
    }


    // -----------------------------------------------------------------------
    // Methods/Code dealing with Subentry Visibility
    // -----------------------------------------------------------------------


    public NamingEnumeration list( NextInterceptor nextInterceptor, Name base ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.list( base );
        LdapContext ctx = ( LdapContext ) InvocationStack.getInstance().peek().getCaller();

        if ( ! isSubentryVisible( ctx ) )
        {
            return new SearchResultFilteringEnumeration( e, new SearchControls(), ctx, SUBENTRY_FILTER );
        }

        return e;
    }


    public NamingEnumeration search( NextInterceptor nextInterceptor, Name base, Map env, ExprNode filter,
            SearchControls searchCtls ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.search( base, env, filter, searchCtls );
        LdapContext ctx = ( LdapContext ) InvocationStack.getInstance().peek().getCaller();

        // object scope searches by default return subentries
        if ( searchCtls.getSearchScope() == SearchControls.OBJECT_SCOPE )
        {
            return e;
        }

        // for subtree and one level scope we filter
        if ( ! isSubentryVisible( ctx ) )
        {
            return new SearchResultFilteringEnumeration( e, searchCtls, ctx, SUBENTRY_FILTER );
        }

        return e;
    }


    /**
     * Checks to see if subentries for the search and list operations should be
     * made visible based on the availability of the search request control
     *
     * @param ctx the ldap context the search operation was invoked on
     * @return true if subentries should be visible, false otherwise
     * @throws NamingException if there are problems accessing request controls
     */
    private boolean isSubentryVisible( LdapContext ctx ) throws NamingException
    {
        Control[] reqControls = ( Control[] ) ctx.getRequestControls();

        if ( reqControls == null || reqControls.length <= 0 )
        {
            return false;
        }

        // check all request controls to see if subentry control is present
        for ( int ii = 0; ii < reqControls.length; ii++ )
        {
            // found the subentry request control so we return its value
            if ( reqControls[ii].getID().equals( SUBENTRY_CONTROL ) )
            {
                SubentryRequestControl subentryControl = ( SubentryRequestControl ) reqControls[ii];
                return subentryControl.getSubentryVisibility();
            }
        }

        return false;
    }


    // -----------------------------------------------------------------------
    // Methods dealing with entry and subentry addition
    // -----------------------------------------------------------------------


    public void add( NextInterceptor next, String upName, Name normName, Attributes entry ) throws NamingException
    {
        Attribute objectClasses = entry.get( "objectClass" );

        if ( objectClasses.contains( "subentry" ) )
        {
            // get the name of the administrative point and its administrativeRole attributes
            Name apName = ( Name ) normName.clone();
            apName.remove( normName.size() - 1 );
            Attributes ap = nexus.lookup( apName );
            Attribute administrativeRole = ap.get( "administrativeRole" );

            // check that administrativeRole has something valid in it for us
            if ( administrativeRole == null || administrativeRole.size() <= 0 )
            {
                throw new LdapNoSuchAttributeException( "Administration point " + apName
                        + " does not contain an administrativeRole attribute! An"
                        + " administrativeRole attribute in the administrative point is"
                        + " required to add a subordinate subentry." );
            }

            /* ----------------------------------------------------------------
             * Build the set of operational attributes to be injected into
             * entries that are contained within the subtree repesented by this
             * new subentry.  In the process we make sure the proper roles are
             * supported by the administrative point to allow the addition of
             * this new subentry.
             * ----------------------------------------------------------------
             */
            Attributes operational = getSubentryOperatationalAttributes( normName, administrativeRole );

            /* ----------------------------------------------------------------
             * Parse the subtreeSpecification of the subentry and add it to the
             * SubtreeSpecification cache.  If the parse succeeds we continue
             * to add the entry to the DIT.  Thereafter we search out entries
             * to modify the subentry operational attributes of.
             * ----------------------------------------------------------------
             */
            String subtree = ( String ) entry.get( "subtreeSpecification" ).get();
            SubtreeSpecification ss = null;
            try
            {
                ss = ssParser.parse( subtree );
            }
            catch ( Exception e )
            {
                String msg = "Failed while parsing subtreeSpecification for " + upName;
                log.warn( msg );
                throw new LdapInvalidAttributeValueException( msg, ResultCodeEnum.INVALIDATTRIBUTESYNTAX );
            }
            subtrees.put( normName.toString(), ss );
            next.add( upName, normName, entry );

            /* ----------------------------------------------------------------
             * Find the baseDn for the subentry and use that to search the tree
             * while testing each entry returned for inclusion within the
             * subtree of the subentry's subtreeSpecification.  All included
             * entries will have their operational attributes merged with the
             * operational attributes calculated above.
             * ----------------------------------------------------------------
             */
            Name baseDn = ( Name ) apName.clone();
            baseDn.addAll( ss.getBase() );

            ExprNode filter = new PresenceNode( "objectclass" );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[] { "+", "*" } );

            NamingEnumeration subentries = nexus.search( baseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                Name dn = dnParser.parse( result.getName() );

                if ( evaluator.evaluate( ss, apName, dn, candidate.get( "objectClass" ) ) )
                {
                    nexus.modify( dn, getOperationalModsForAdd( candidate, operational ) );
                }
            }
        }
        else
        {
            Iterator list = subtrees.keySet().iterator();
            while ( list.hasNext() )
            {
                String subentryDnStr = ( String ) list.next();
                Name subentryDn = new LdapName( subentryDnStr );
                Name apDn = ( Name ) subentryDn.clone();
                apDn.remove( apDn.size() - 1 );
                SubtreeSpecification ss = ( SubtreeSpecification ) subtrees.get( subentryDn );

                if ( evaluator.evaluate( ss, apDn, normName, objectClasses ) )
                {
                    Attribute administrativeRole = nexus.lookup( apDn ).get( "administrativeRole" );
                    NamingEnumeration roles = administrativeRole.getAll();
                    while ( roles.hasMore() )
                    {
                        Attribute operational = null;
                        String role = ( String ) roles.next();

                        if ( role.equalsIgnoreCase( AUTONOUMOUS_AREA ) )
                        {
                            operational = entry.get( AUTONOUMOUS_AREA_SUBENTRY );
                            if ( operational == null )
                            {
                                operational = new LockableAttributeImpl( AUTONOUMOUS_AREA_SUBENTRY );
                                entry.put( operational );
                            }
                        }
                        else if ( role.equalsIgnoreCase( AC_AREA ) )
                        {
                            operational = ( Attribute ) entry.get( AC_AREA_SUBENTRY ).clone();
                            if ( operational == null )
                            {
                                operational = new LockableAttributeImpl( AC_AREA_SUBENTRY );
                                entry.put( operational );
                            }
                        }
                        else if ( role.equalsIgnoreCase( AC_INNERAREA ) )
                        {
                            operational = ( Attribute ) entry.get( AC_INNERAREA_SUBENTRY ).clone();
                            if ( operational == null )
                            {
                                operational = new LockableAttributeImpl( AC_INNERAREA_SUBENTRY );
                                entry.put( operational );
                            }
                        }
                        else if ( role.equalsIgnoreCase( SCHEMA_AREA ) )
                        {
                            operational = ( Attribute ) entry.get( SCHEMA_AREA_SUBENTRY ).clone();
                            if ( operational == null )
                            {
                                operational = new LockableAttributeImpl( SCHEMA_AREA_SUBENTRY );
                                entry.put( operational );
                            }
                        }
                        else if ( role.equalsIgnoreCase( COLLECTIVE_AREA ) ||
                                  role.equalsIgnoreCase( COLLECTIVE_INNERAREA ) )
                        {
                            operational = ( Attribute ) entry.get( COLLECTIVE_ATTRIBUTE_SUBENTRIES ).clone();
                            if ( operational == null )
                            {
                                operational = new LockableAttributeImpl( COLLECTIVE_ATTRIBUTE_SUBENTRIES );
                                entry.put( operational );
                            }
                        }
                        else
                        {
                            throw new LdapInvalidAttributeValueException( "Encountered invalid administrativeRole '"
                                    + role + "' in administrative point of subentry " + subentryDnStr + ". The values of this attribute"
                                    + " are constrained to autonomousArea, accessControlSpecificArea, accessControlInnerArea,"
                                    + " subschemaAdminSpecificArea, collectiveAttributeSpecificArea, and"
                                    + " collectiveAttributeInnerArea.", ResultCodeEnum.CONSTRAINTVIOLATION );
                        }

                        operational.add( subentryDn.toString() );
                    }
                }
            }

            next.add( upName, normName, entry );
        }
    }


    // -----------------------------------------------------------------------
    // Methods dealing subentry deletion
    // -----------------------------------------------------------------------


    public void delete( NextInterceptor next, Name name ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        Attribute objectClasses = entry.get( "objectClass" );

        if ( objectClasses.contains( "subentry" ) )
        {
            SubtreeSpecification ss = ( SubtreeSpecification ) subtrees.get( name.toString() );
            subtrees.remove( ss );
            next.delete( name );

            /* ----------------------------------------------------------------
             * Find the baseDn for the subentry and use that to search the tree
             * for all entries included by the subtreeSpecification.  Then we
             * check the entry for subentry operational attribute that contain
             * the DN of the subentry.  These are the subentry operational
             * attributes we remove from the entry in a modify operation.
             * ----------------------------------------------------------------
             */
            Name apName = ( Name ) name.clone();
            apName.remove( name.size() - 1 );
            Name baseDn = ( Name ) apName.clone();
            baseDn.addAll( ss.getBase() );

            ExprNode filter = new PresenceNode( "objectclass" );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[] { "+", "*" } );

            NamingEnumeration subentries = nexus.search( baseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                Name dn = dnParser.parse( result.getName() );

                if ( evaluator.evaluate( ss, apName, dn, candidate.get( "objectClass" ) ) )
                {
                    nexus.modify( dn, getOperationalModsForRemove( name, candidate ) );
                }
            }
        }
        else
        {
            next.delete( name );
        }
    }


    // -----------------------------------------------------------------------
    // Methods dealing subentry name changes
    // -----------------------------------------------------------------------


    /**
     * Checks to see if an entry being renamed has a descendant that is an
     * administrative point.
     *
     * @param name the name of the entry which is used as the search base
     * @return true if name is an administrative point or one of its descendants
     * are, false otherwise
     * @throws NamingException if there are errors while searching the directory
     */
    private boolean hasAdministrativeDescendant( Name name ) throws NamingException
    {
        ExprNode filter = new PresenceNode( "administrativeRole" );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration aps = nexus.search( name, factoryCfg.getEnvironment(), filter, controls );
        if ( aps.hasMore() )
        {
            aps.close();
            return true;
        }

        return false;
    }


    private ModificationItem[] getModsOnEntryRdnChange( Name oldName, Name newName, Attributes entry )
        throws NamingException
    {
        Attribute objectClasses = entry.get( "objectClass" );
        List modList = new ArrayList();

        /*
         * There are two different situations warranting action.  Firt if
         * an ss evalutating to true with the old name no longer evalutates
         * to true with the new name.  This would be caused by specific chop
         * exclusions that effect the new name but did not effect the old
         * name. In this case we must remove subentry operational attribute
         * values associated with the dn of that subentry.
         *
         * In the second case an ss selects the entry with the new name when
         * it did not previously with the old name.  Again this situation
         * would be caused by chop exclusions. In this case we must add subentry
         * operational attribute values with the dn of this subentry.
         */
        Iterator subentries = subtrees.keySet().iterator();
        while ( subentries.hasNext() )
        {
            String subentryDn = ( String ) subentries.next();
            Name apDn = new LdapName( subentryDn );
            apDn.remove( apDn.size() - 1 );
            SubtreeSpecification ss = ( SubtreeSpecification ) subtrees.get( subentryDn );
            boolean isOldNameSelected = evaluator.evaluate( ss, apDn, oldName, objectClasses );
            boolean isNewNameSelected = evaluator.evaluate( ss, apDn, newName, objectClasses );

            if ( isOldNameSelected == isNewNameSelected )
            {
                continue;
            }

            // need to remove references to the subentry
            if ( isOldNameSelected && ! isNewNameSelected )
            {
                for ( int ii = 0; ii < SUBENTRY_OPATTRS.length; ii++ )
                {
                    int op = DirContext.REPLACE_ATTRIBUTE;
                    Attribute opAttr = entry.get( SUBENTRY_OPATTRS[ii] );
                    if ( opAttr != null )
                    {
                        opAttr = ( Attribute ) opAttr.clone();
                        opAttr.remove( subentryDn );

                        if ( opAttr.size() < 1 )
                        {
                            op = DirContext.REMOVE_ATTRIBUTE;
                        }

                        modList.add( new ModificationItem( op, opAttr ) );
                    }
                }
            }
            // need to add references to the subentry
            else if ( isNewNameSelected && ! isOldNameSelected )
            {
                for ( int ii = 0; ii < SUBENTRY_OPATTRS.length; ii++ )
                {
                    int op = DirContext.REPLACE_ATTRIBUTE;
                    Attribute opAttr = entry.get( SUBENTRY_OPATTRS[ii] );
                    if ( opAttr != null )
                    {
                        opAttr = ( Attribute ) opAttr.clone();
                    }
                    else
                    {
                        op = DirContext.ADD_ATTRIBUTE;
                        opAttr = new LockableAttributeImpl( SUBENTRY_OPATTRS[ii] );
                    }

                    opAttr.add( subentryDn );
                    modList.add( new ModificationItem( op, opAttr ) );
                }
            }
        }

        ModificationItem[] mods = new ModificationItem[modList.size()];
        mods = ( ModificationItem[] ) modList.toArray( mods );
        return mods;
    }


    public void modifyRn( NextInterceptor next, Name name, String newRn, boolean deleteOldRn ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        Attribute objectClasses = entry.get( "objectClass" );

        if ( objectClasses.contains( "subentry" ) )
        {
            SubtreeSpecification ss = ( SubtreeSpecification ) subtrees.get( name.toString() );
            Name apName = ( Name ) name.clone();
            apName.remove( apName.size() - 1 );
            Name baseDn = ( Name ) apName.clone();
            baseDn.addAll( ss.getBase() );
            Name newName = ( Name ) name.clone();
            newName.remove( newName.size() - 1 );
            Name rdn = dnParser.parse( newRn );
            newName.addAll( rdn );

            subtrees.put( newName.toString(), ss );
            next.modifyRn( name, newRn, deleteOldRn );

            Attributes apAttrs = nexus.lookup( apName );
            Attribute administrativeRole = apAttrs.get( "administrativeRole" );
            ExprNode filter = new PresenceNode( "objectclass" );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[] { "+", "*" } );
            NamingEnumeration subentries = nexus.search( baseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                Name dn = dnParser.parse( result.getName() );

                if ( evaluator.evaluate( ss, apName, dn, candidate.get( "objectClass" ) ) )
                {
                    nexus.modify( dn, getOperationalModsForReplace( name, newName, administrativeRole, candidate ) );
                }
            }
        }
        else
        {
            if ( hasAdministrativeDescendant( name ) )
            {
                String msg = "Will not allow rename operation on entries with administrative descendants.";
                log.warn( msg );
                throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOTALLOWEDONRDN );
            }
            next.modifyRn( name, newRn, deleteOldRn );

            // calculate the new DN now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            Name newName = ( Name ) name.clone();
            newName.remove( newName.size() - 1 );
            newName.add( newRn );
            ModificationItem[] mods = getModsOnEntryRdnChange( name, newName, entry );

            if ( mods.length > 0 )
            {
                nexus.modify( newName, mods );
            }
        }
    }


    public void move( NextInterceptor next, Name oriChildName, Name newParentName, String newRn, boolean deleteOldRn )
            throws NamingException
    {
        Attributes entry = nexus.lookup( oriChildName );
        Attribute objectClasses = entry.get( "objectClass" );

        if ( objectClasses.contains( "subentry" ) )
        {
            SubtreeSpecification ss = ( SubtreeSpecification ) subtrees.get( oriChildName.toString() );
            Name apName = ( Name ) oriChildName.clone();
            apName.remove( apName.size() - 1 );
            Name baseDn = ( Name ) apName.clone();
            baseDn.addAll( ss.getBase() );
            Name newName = ( Name ) newParentName.clone();
            newName.remove( newName.size() - 1 );
            Name rdn = dnParser.parse( newRn );
            newName.addAll( rdn );

            subtrees.put( newName.toString(), ss );
            next.move( oriChildName, newParentName, newRn, deleteOldRn );

            Attributes apAttrs = nexus.lookup( apName );
            Attribute administrativeRole = apAttrs.get( "administrativeRole" );

            ExprNode filter = new PresenceNode( "objectclass" );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[] { "+", "*" } );
            NamingEnumeration subentries = nexus.search( baseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                Name dn = dnParser.parse( result.getName() );

                if ( evaluator.evaluate( ss, apName, dn, candidate.get( "objectClass" ) ) )
                {
                    nexus.modify( dn, getOperationalModsForReplace( oriChildName, newName, administrativeRole, candidate ) );
                }
            }
        }
        else
        {
            if ( hasAdministrativeDescendant( oriChildName ) )
            {
                String msg = "Will not allow rename operation on entries with administrative descendants.";
                log.warn( msg );
                throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOTALLOWEDONRDN );
            }
            next.move( oriChildName, newParentName, newRn, deleteOldRn );

            // calculate the new DN now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            Name newName = ( Name ) newParentName.clone();
            newName.add( newRn );
            ModificationItem[] mods = getModsOnEntryRdnChange( oriChildName, newName, entry );

            if ( mods.length > 0 )
            {
                nexus.modify( newName, mods );
            }
        }
    }


    public void move( NextInterceptor next, Name oriChildName, Name newParentName ) throws NamingException
    {
        Attributes entry = nexus.lookup( oriChildName );
        Attribute objectClasses = entry.get( "objectClass" );

        if ( objectClasses.contains( "subentry" ) )
        {
            SubtreeSpecification ss = ( SubtreeSpecification ) subtrees.get( oriChildName.toString() );
            Name apName = ( Name ) oriChildName.clone();
            apName.remove( apName.size() - 1 );
            Name baseDn = ( Name ) apName.clone();
            baseDn.addAll( ss.getBase() );
            Name newName = ( Name ) newParentName.clone();
            newName.remove( newName.size() - 1 );
            newName.add( newParentName.get( newParentName.size() - 1 ) );

            subtrees.put( newName.toString(), ss );
            next.move( oriChildName, newParentName );

            Attributes apAttrs = nexus.lookup( apName );
            Attribute administrativeRole = apAttrs.get( "administrativeRole" );

            ExprNode filter = new PresenceNode( "objectclass" );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[] { "+", "*" } );
            NamingEnumeration subentries = nexus.search( baseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                Name dn = dnParser.parse( result.getName() );

                if ( evaluator.evaluate( ss, apName, dn, candidate.get( "objectClass" ) ) )
                {
                    nexus.modify( dn, getOperationalModsForReplace( oriChildName, newName, administrativeRole, candidate ) );
                }
            }
        }
        else
        {
            if ( hasAdministrativeDescendant( oriChildName ) )
            {
                String msg = "Will not allow rename operation on entries with administrative descendants.";
                log.warn( msg );
                throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOTALLOWEDONRDN );
            }
            next.move( oriChildName, newParentName );

            // calculate the new DN now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            Name newName = ( Name ) newParentName.clone();
            newName.add( oriChildName.get( oriChildName.size() - 1 ) );
            ModificationItem[] mods = getModsOnEntryRdnChange( oriChildName, newName, entry );

            if ( mods.length > 0 )
            {
                nexus.modify( newName, mods );
            }
        }
    }


    // -----------------------------------------------------------------------
    // Methods dealing subentry modification
    // -----------------------------------------------------------------------


    public void modify( NextInterceptor next, Name name, int modOp, Attributes mods ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        Attribute objectClasses = entry.get( "objectClass" );

        if ( objectClasses.contains( "subentry" ) && mods.get( "subtreeSpecification" ) != null )
        {
            SubtreeSpecification ssOld = ( SubtreeSpecification ) subtrees.remove( name.toString() );
            SubtreeSpecification ssNew = null;

            try
            {
                ssNew = ssParser.parse( ( String ) mods.get( "subtreeSpecification" ).get() );
            }
            catch ( Exception e )
            {
                String msg = "failed to parse the new subtreeSpecification";
                log.error( msg, e );
                throw new LdapInvalidAttributeValueException( msg, ResultCodeEnum.INVALIDATTRIBUTESYNTAX );
            }

            subtrees.put( name.toString(), ssNew );
            next.modify( name, modOp, mods );

            // search for all entries selected by the old SS and remove references to subentry
            Name apName = ( Name ) name.clone();
            apName.remove( apName.size() - 1 );
            Name oldBaseDn = ( Name ) apName.clone();
            oldBaseDn.addAll( ssOld.getBase() );
            ExprNode filter = new PresenceNode( "objectClass" );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[] { "+", "*" } );
            NamingEnumeration subentries = nexus.search( oldBaseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                Name dn = dnParser.parse( result.getName() );

                if ( evaluator.evaluate( ssOld, apName, dn, candidate.get( "objectClass" ) ) )
                {
                    nexus.modify( dn, getOperationalModsForRemove( name, candidate ) );
                }
            }

            // search for all selected entries by the new SS and add references to subentry
            Attributes apAttrs = nexus.lookup( apName );
            Attribute administrativeRole = apAttrs.get( "administrativeRole" );
            Attributes operational = getSubentryOperatationalAttributes( name, administrativeRole );
            Name newBaseDn = ( Name ) apName.clone();
            newBaseDn.addAll( ssNew.getBase() );
            subentries = nexus.search( newBaseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                Name dn = dnParser.parse( result.getName() );

                if ( evaluator.evaluate( ssOld, apName, dn, candidate.get( "objectClass" ) ) )
                {
                    nexus.modify( dn, getOperationalModsForAdd( candidate, operational ) );
                }
            }
        }
        else
        {
            next.modify( name, modOp, mods );
        }
    }


    public void modify( NextInterceptor next, Name name, ModificationItem[] mods ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        Attribute objectClasses = entry.get( "objectClass" );
        boolean isSubtreeSpecificationModification = false;
        ModificationItem subtreeMod = null;

        for ( int ii = 0; ii < mods.length; ii++ )
        {
            if ( ( ( String ) mods[ii].getAttribute().getID() ).equalsIgnoreCase( "subtreeSpecification" ) )
            {
                isSubtreeSpecificationModification = true;
                subtreeMod = mods[ii];
            }
        }

        if ( objectClasses.contains( "subentry" ) && isSubtreeSpecificationModification )
        {
            SubtreeSpecification ssOld = ( SubtreeSpecification ) subtrees.remove( name.toString() );
            SubtreeSpecification ssNew = null;

            try
            {
                ssNew = ssParser.parse( ( String ) subtreeMod.getAttribute().get() );
            }
            catch ( Exception e )
            {
                String msg = "failed to parse the new subtreeSpecification";
                log.error( msg, e );
                throw new LdapInvalidAttributeValueException( msg, ResultCodeEnum.INVALIDATTRIBUTESYNTAX );
            }

            subtrees.put( name.toString(), ssNew );
            next.modify( name, mods );

            // search for all entries selected by the old SS and remove references to subentry
            Name apName = ( Name ) name.clone();
            apName.remove( apName.size() - 1 );
            Name oldBaseDn = ( Name ) apName.clone();
            oldBaseDn.addAll( ssOld.getBase() );
            ExprNode filter = new PresenceNode( "objectClass" );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[] { "+", "*" } );
            NamingEnumeration subentries = nexus.search( oldBaseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                Name dn = dnParser.parse( result.getName() );

                if ( evaluator.evaluate( ssOld, apName, dn, candidate.get( "objectClass" ) ) )
                {
                    nexus.modify( dn, getOperationalModsForRemove( name, candidate ) );
                }
            }

            // search for all selected entries by the new SS and add references to subentry
            Attributes apAttrs = nexus.lookup( apName );
            Attribute administrativeRole = apAttrs.get( "administrativeRole" );
            Attributes operational = getSubentryOperatationalAttributes( name, administrativeRole );
            Name newBaseDn = ( Name ) apName.clone();
            newBaseDn.addAll( ssNew.getBase() );
            subentries = nexus.search( newBaseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                Name dn = dnParser.parse( result.getName() );

                if ( evaluator.evaluate( ssOld, apName, dn, candidate.get( "objectClass" ) ) )
                {
                    nexus.modify( dn, getOperationalModsForAdd( candidate, operational ) );
                }
            }
        }
        else
        {
            next.modify( name, mods );
        }
    }


    // -----------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------


    private ModificationItem[] getOperationalModsForReplace( Name oldName, Name newName, Attribute administrativeRole,
                                                             Attributes entry ) throws NamingException
    {
        List modList = new ArrayList();
        NamingEnumeration roles = administrativeRole.getAll();
        while ( roles.hasMore() )
        {
            Attribute operational = null;
            String role = ( String ) roles.next();

            if ( role.equalsIgnoreCase( AUTONOUMOUS_AREA ) )
            {
                operational = ( Attribute ) entry.get( AUTONOUMOUS_AREA_SUBENTRY ).clone();
                if ( operational == null )
                {
                    operational = new LockableAttributeImpl( AUTONOUMOUS_AREA_SUBENTRY );
                    operational.add( newName.toString() );
                }
                else
                {
                    operational.remove( oldName.toString() );
                    operational.add( newName.toString() );
                }
            }
            else if ( role.equalsIgnoreCase( AC_AREA ) )
            {
                operational = ( Attribute ) entry.get( AC_AREA_SUBENTRY ).clone();
                if ( operational == null )
                {
                    operational = new LockableAttributeImpl( AC_AREA_SUBENTRY );
                    operational.add( newName.toString() );
                }
                else
                {
                    operational.remove( oldName.toString() );
                    operational.add( newName.toString() );
                }
            }
            else if ( role.equalsIgnoreCase( AC_INNERAREA ) )
            {
                operational = ( Attribute ) entry.get( AC_INNERAREA_SUBENTRY ).clone();
                if ( operational == null )
                {
                    operational = new LockableAttributeImpl( AC_INNERAREA_SUBENTRY );
                    operational.add( newName.toString() );
                }
                else
                {
                    operational.remove( oldName.toString() );
                    operational.add( newName.toString() );
                }
            }
            else if ( role.equalsIgnoreCase( SCHEMA_AREA ) )
            {
                operational = ( Attribute ) entry.get( SCHEMA_AREA_SUBENTRY ).clone();
                if ( operational == null )
                {
                    operational = new LockableAttributeImpl( SCHEMA_AREA_SUBENTRY );
                    operational.add( newName.toString() );
                }
                else
                {
                    operational.remove( oldName.toString() );
                    operational.add( newName.toString() );
                }
            }
            else if ( role.equalsIgnoreCase( COLLECTIVE_AREA ) ||
                      role.equalsIgnoreCase( COLLECTIVE_INNERAREA ) )
            {
                operational = ( Attribute ) entry.get( COLLECTIVE_ATTRIBUTE_SUBENTRIES ).clone();
                if ( operational == null )
                {
                    operational = new LockableAttributeImpl( COLLECTIVE_ATTRIBUTE_SUBENTRIES );
                    operational.add( newName.toString() );
                }
                else
                {
                    operational.remove( oldName.toString() );
                    operational.add( newName.toString() );
                }
            }
            else
            {
                throw new LdapInvalidAttributeValueException( "Encountered invalid administrativeRole '"
                        + role + "' in administrative point of subentry " + oldName + ". The values of this attribute"
                        + " are constrained to autonomousArea, accessControlSpecificArea, accessControlInnerArea,"
                        + " subschemaAdminSpecificArea, collectiveAttributeSpecificArea, and"
                        + " collectiveAttributeInnerArea.", ResultCodeEnum.CONSTRAINTVIOLATION );
            }

            modList.add( new ModificationItem( DirContext.REPLACE_ATTRIBUTE, operational ) );
        }

        ModificationItem[] mods = new ModificationItem[modList.size()];
        return ( ModificationItem[] ) modList.toArray( mods );
    }


    /**
     * Gets the subschema operational attributes to be added to or removed from
     * an entry selected by a subentry's subtreeSpecification.
     *
     * @param name the normalized distinguished name of the subentry (the value of op attrs)
     * @param administrativeRole the roles the administrative point participates in
     * @return the set of attributes to be added or removed from entries
     * @throws NamingException if there are problems accessing attributes
     */
    private Attributes getSubentryOperatationalAttributes( Name name, Attribute administrativeRole )
            throws NamingException
    {
        Attributes operational = new LockableAttributesImpl();
        NamingEnumeration roles = administrativeRole.getAll();
        while ( roles.hasMore() )
        {
            String role = ( String ) roles.next();

            if ( role.equalsIgnoreCase( AUTONOUMOUS_AREA ) )
            {
                if ( operational.get( AUTONOUMOUS_AREA_SUBENTRY ) == null )
                {
                    operational.put( AUTONOUMOUS_AREA_SUBENTRY, name.toString() );
                }
                else
                {
                    operational.get( AUTONOUMOUS_AREA_SUBENTRY ).add( name.toString() );
                }
            }
            else if ( role.equalsIgnoreCase( AC_AREA ) )
            {
                if ( operational.get( AC_AREA_SUBENTRY ) == null )
                {
                    operational.put( AC_AREA_SUBENTRY, name.toString() );
                }
                else
                {
                    operational.get( AC_AREA_SUBENTRY ).add( name.toString() );
                }
            }
            else if ( role.equalsIgnoreCase( AC_INNERAREA ) )
            {
                if ( operational.get( AC_INNERAREA_SUBENTRY ) == null )
                {
                    operational.put( AC_INNERAREA_SUBENTRY, name.toString() );
                }
                else
                {
                    operational.get( AC_INNERAREA_SUBENTRY ).add( name.toString() );
                }
            }
            else if ( role.equalsIgnoreCase( SCHEMA_AREA ) )
            {
                if ( operational.get( SCHEMA_AREA_SUBENTRY ) == null )
                {
                    operational.put( SCHEMA_AREA_SUBENTRY, name.toString() );
                }
                else
                {
                    operational.get( SCHEMA_AREA_SUBENTRY ).add( name.toString() );
                }
            }
            else if ( role.equalsIgnoreCase( COLLECTIVE_AREA ) ||
                      role.equalsIgnoreCase( COLLECTIVE_INNERAREA ) )
            {
                if ( operational.get( COLLECTIVE_ATTRIBUTE_SUBENTRIES ) == null )
                {
                    operational.put( COLLECTIVE_ATTRIBUTE_SUBENTRIES, name.toString() );
                }
                else
                {
                    operational.get( COLLECTIVE_ATTRIBUTE_SUBENTRIES ).add( name.toString() );
                }
            }
            else
            {
                throw new LdapInvalidAttributeValueException( "Encountered invalid administrativeRole '"
                        + role + "' in administrative point of subentry " + name + ". The values of this attribute are"
                        + " constrained to autonomousArea, accessControlSpecificArea, accessControlInnerArea,"
                        + " subschemaAdminSpecificArea, collectiveAttributeSpecificArea, and"
                        + " collectiveAttributeInnerArea.", ResultCodeEnum.CONSTRAINTVIOLATION );
            }
        }

        return operational;
    }


    /**
     * Calculates the subentry operational attributes to remove from a candidate
     * entry selected by a subtreeSpecification.  When we remove a subentry we
     * must remove the operational attributes in the entries that were once selected
     * by the subtree specification of that subentry.  To do so we must perform
     * a modify operation with the set of modifications to perform.  This method
     * calculates those modifications.
     *
     * @param subentryDn the distinguished name of the subentry
     * @param candidate the candidate entry to removed from the
     * @return the set of modifications required to remove an entry's reference to
     * a subentry
     */
    private ModificationItem[] getOperationalModsForRemove( Name subentryDn, Attributes candidate )
    {
        List modList = new ArrayList();
        String dn = subentryDn.toString();

        for ( int ii = 0; ii < SUBENTRY_OPATTRS.length; ii++ )
        {
            String opAttrId = SUBENTRY_OPATTRS[ii];
            Attribute opAttr = candidate.get( opAttrId );

            if ( opAttr != null && opAttr.contains( dn ) )
            {
                Attribute attr = new LockableAttributeImpl( SUBENTRY_OPATTRS[ii] );
                attr.add( dn );
                modList.add( new ModificationItem( DirContext.REMOVE_ATTRIBUTE, attr ) );
            }
        }

        ModificationItem[] mods = new ModificationItem[modList.size()];
        return ( ModificationItem[] ) modList.toArray( mods );
    }


    /**
     * Calculates the subentry operational attributes to add or replace from
     * a candidate entry selected by a subtree specification.  When a subentry
     * is added or it's specification is modified some entries must have new
     * operational attributes added to it to point back to the associated
     * subentry.  To do so a modify operation must be performed on entries
     * selected by the subtree specification.  This method calculates the
     * modify operation to be performed on the entry.
     *
     * @param entry the entry being modified
     * @param operational the set of operational attributes supported by the AP
     * of the subentry
     * @return the set of modifications needed to update the entry
     */
    public ModificationItem[] getOperationalModsForAdd( Attributes entry, Attributes operational ) throws NamingException
    {
        List modList = new ArrayList();

        NamingEnumeration opAttrIds = operational.getIDs();
        while ( opAttrIds.hasMore() )
        {
            int op = DirContext.REPLACE_ATTRIBUTE;
            String opAttrId = ( String ) opAttrIds.next();
            Attribute result = new LockableAttributeImpl( opAttrId );
            Attribute opAttrAdditions = operational.get( opAttrId );
            Attribute opAttrInEntry = entry.get( opAttrId );

            for ( int ii = 0; ii < opAttrAdditions.size(); ii++ )
            {
                result.add( opAttrAdditions.get( ii ) );
            }

            if ( opAttrInEntry != null && opAttrInEntry.size() > 0 )
            {
                for ( int ii = 0; ii < opAttrInEntry.size(); ii++ )
                {
                    result.add( opAttrInEntry.get( ii ) );
                }
            }
            else
            {
                op = DirContext.ADD_ATTRIBUTE;
            }

            modList.add( new ModificationItem( op, result ) );
        }

        ModificationItem[] mods = new ModificationItem[modList.size()];
        mods = ( ModificationItem[] ) modList.toArray( mods );
        return mods;
    }
}
