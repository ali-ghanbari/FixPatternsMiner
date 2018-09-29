/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.itest.datasources;

import java.util.Date;

import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.enonic.cms.framework.util.JDOMUtil;
import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.command.UpdateContentCommand;
import com.enonic.cms.core.content.contentdata.ContentData;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.contentkeybased.RelatedContentDataEntry;
import com.enonic.cms.core.content.contentdata.custom.relationdataentrylistbased.RelatedContentsDataEntry;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeConfigBuilder;
import com.enonic.cms.core.portal.datasource.DataSourceContext;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.service.DataSourceServiceImpl;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.structure.menuitem.AddContentToSectionCommand;
import com.enonic.cms.core.structure.menuitem.MenuItemAccessEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemService;
import com.enonic.cms.core.time.MockTimeService;
import com.enonic.cms.itest.AbstractSpringTest;
import com.enonic.cms.itest.util.AssertTool;
import com.enonic.cms.itest.util.DomainFactory;
import com.enonic.cms.itest.util.DomainFixture;
import com.enonic.cms.store.dao.UserDao;

import static org.junit.Assert.*;

@TransactionConfiguration(defaultRollback = true)
@DirtiesContext
@Transactional
public class DataSourceServiceImpl_relatedContentTest
    extends AbstractSpringTest
{
    private DomainFactory factory;

    @Autowired
    private DomainFixture fixture;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserDao userDao;

    private DataSourceServiceImpl dataSourceService;

    @Autowired
    private ContentService contentService;

    private static final DateTime DATE_TIME_2010_01_01 = new DateTime( 2010, 1, 1, 0, 0, 0, 0 );

    @Before
    public void setUp()
    {

        factory = fixture.getFactory();

        // setup needed common data for each test
        fixture.initSystemData();

        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );

        MockHttpServletRequest httpRequest = new MockHttpServletRequest( "GET", "/" );
        ServletRequestAccessor.setRequest( httpRequest );

        dataSourceService = new DataSourceServiceImpl();
        dataSourceService.setContentService( contentService );
        dataSourceService.setSecurityService( securityService );
        dataSourceService.setTimeService( new MockTimeService( new DateTime( 2010, 7, 1, 12, 0, 0, 0 ) ) );
        dataSourceService.setUserDao( userDao );

        fixture.createAndStoreNormalUserWithUserGroup( "content-creator", "Creator", "testuserstore" );
        fixture.createAndStoreNormalUserWithUserGroup( "content-querier", "Querier", "testuserstore" );

        // setup content type
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "MyContent", "title" );
        ctyconf.startBlock( "MyContent" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addRelatedContentInput( "myRelatedContent", "contentdata/myRelatedContent", "My related content", false, true );
        ctyconf.endBlock();
        Document configAsXmlBytes = XMLDocumentFactory.create( ctyconf.toString() ).getAsJDOMDocument();

        fixture.save(
            factory.createContentType( "MyRelatedType", ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsXmlBytes ) );
        fixture.save( factory.createUnit( "MyUnit", "en" ) );
        fixture.save(
            factory.createCategory( "MyCategory", null, "MyRelatedType", "MyUnit", User.ANONYMOUS_UID, User.ANONYMOUS_UID, false ) );
        fixture.save(
            factory.createCategory( "MyOtherCategory", null, "MyRelatedType", "MyUnit", User.ANONYMOUS_UID, User.ANONYMOUS_UID, false ) );

        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "content-creator", "read, create, approve, admin_browse" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyCategory", "content-querier", "read, admin_browse" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyOtherCategory", "content-creator", "read, create, approve, admin_browse" ) );
        fixture.save( factory.createCategoryAccessForUser( "MyOtherCategory", "content-querier", "read, admin_browse" ) );

        fixture.flushAndClearHibernateSesssion();
        fixture.flushIndexTransaction();
    }

    @Test
    public void common_content_related_to_between_two_content_is_listed_both_contents_relatedcontentkeys()
    {
        // setup: create same content in two different categories
        ContentKey commonChildContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Common child" ), "content-creator" ) );

        ContentKey contentA = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Content A", commonChildContentKey ),
                                        "content-creator" ) );

        ContentKey contentB = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Content B", commonChildContentKey ),
                                        "content-creator" ) );

        fixture.flushIndexTransaction();

        // setup: verify that 2 content is created
        assertEquals( 3, fixture.countAllContent() );

        // exercise
        DataSourceContext context = new DataSourceContext();
        context.setUser( fixture.findUserByName( "content-querier" ) );

        String query = "title STARTS WITH 'Content '";
        String orderBy = "@title asc";
        int index = 0;
        int count = 10;
        boolean includeData = true;
        int childrenLevel = 1;
        int parentLevel = 0;

        XMLDocument xmlDocResult =
            dataSourceService.getContentByQuery( context, query, orderBy, index, count, includeData, childrenLevel, parentLevel );

        // verify
        Document jdomDocResult = xmlDocResult.getAsJDOMDocument();

        AssertTool.assertSingleXPathValueEquals( "/contents/@totalcount", jdomDocResult, "2" );
        AssertTool.assertXPathEquals( "/contents/content/@key", jdomDocResult, contentA.toString(), contentB.toString() );
        AssertTool.assertXPathEquals( "/contents/content[ title = 'Content A']/relatedcontentkeys/relatedcontentkey/@key", jdomDocResult,
                                      commonChildContentKey.toString() );
        AssertTool.assertXPathEquals( "/contents/content[ title = 'Content B']/relatedcontentkeys/relatedcontentkey/@key", jdomDocResult,
                                      commonChildContentKey.toString() );
        AssertTool.assertSingleXPathValueEquals( "/contents/relatedcontents/@count", jdomDocResult, "1" );
        AssertTool.assertSingleXPathValueEquals( "/contents/relatedcontents/content/@key", jdomDocResult,
                                                 commonChildContentKey.toString() );
    }

    @Test
    public void content_queried_with_related_children_having_children_existing_as_the_queried_content_is_listed_as_related_content_too()
    {
        // setup: create same content in two different categories
        ContentKey grandChildContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Grand child" ), "content-creator" ) );

        ContentKey sonContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Son", grandChildContentKey ), "content-creator" ) );

        ContentKey daughterContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Daughter" ), "content-creator" ) );

        ContentKey fatherContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Father", sonContentKey, daughterContentKey ),
                                        "content-creator" ) );

        fixture.flushIndexTransaction();

        // setup: verify that the content was created
        assertEquals( 4, fixture.countAllContent() );

        // exercise
        DataSourceContext context = new DataSourceContext();
        context.setUser( fixture.findUserByName( "content-querier" ) );

        String query = "categorykey = " + fixture.findCategoryByName( "MyCategory" ).getKey();
        String orderyBy = "@key desc";
        int index = 0;
        int count = 10;
        boolean includeData = true;
        int childrenLevel = 10;
        int parentLevel = 0;

        XMLDocument xmlDocResult =
            dataSourceService.getContentByQuery( context, query, orderyBy, index, count, includeData, childrenLevel, parentLevel );

        // verify
        Document jdomDocResult = xmlDocResult.getAsJDOMDocument();

        AssertTool.assertSingleXPathValueEquals( "/contents/@totalcount", jdomDocResult, "4" );
        AssertTool.assertXPathEquals( "/contents/content/@key", jdomDocResult, fatherContentKey.toString(), daughterContentKey.toString(),
                                      sonContentKey.toString(), grandChildContentKey.toString() );

        AssertTool.assertXPathEquals( "/contents/content[ title = 'Father']/relatedcontentkeys/relatedcontentkey/@key", jdomDocResult,
                                      sonContentKey.toString(), daughterContentKey.toString() );
        AssertTool.assertXPathEquals( "/contents/content[ title = 'Son']/relatedcontentkeys/relatedcontentkey/@key", jdomDocResult,
                                      grandChildContentKey.toString() );
        AssertTool.assertSingleXPathValueEquals( "/contents/relatedcontents/@count", jdomDocResult, "3" );
        AssertTool.assertXPathEquals( "/contents/relatedcontents/content/@key", jdomDocResult, grandChildContentKey.toString(),
                                      sonContentKey.toString(), daughterContentKey.toString() );
    }

    @Test
    public void content_queried_with_related_parent_having_parent_existing_as_the_queried_content_is_listed_as_related_content_too()
    {
        // setup: create same content in two different categories
        ContentKey grandChildContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Grand child" ), "content-creator" ) );
        fixture.flushIndexTransaction();

        ContentKey sonContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Son", grandChildContentKey ), "content-creator" ) );
        fixture.flushIndexTransaction();

        ContentKey daughterContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Daughter" ), "content-creator" ) );
        fixture.flushIndexTransaction();

        ContentKey fatherContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Father", sonContentKey, daughterContentKey ),
                                        "content-creator" ) );

        fixture.flushIndexTransaction();

        // setup: verify that the content was created
        assertEquals( 4, fixture.countAllContent() );

        // exercise
        DataSourceContext context = new DataSourceContext();
        context.setUser( fixture.findUserByName( "content-querier" ) );

        String query = "categorykey = " + fixture.findCategoryByName( "MyCategory" ).getKey();
        String orderyBy = "@key desc";
        int index = 0;
        int count = 10;
        boolean includeData = true;
        int childrenLevel = 0;
        int parentLevel = 10;

        XMLDocument xmlDocResult =
            dataSourceService.getContentByQuery( context, query, orderyBy, index, count, includeData, childrenLevel, parentLevel );

        // verify
        Document jdomDocResult = xmlDocResult.getAsJDOMDocument();

        XMLOutputter outputter = new XMLOutputter( Format.getPrettyFormat() );
        System.out.println( outputter.outputString( jdomDocResult ) );

        AssertTool.assertSingleXPathValueEquals( "/contents/@totalcount", jdomDocResult, "4" );
        AssertTool.assertXPathEquals( "/contents/content/@key", jdomDocResult, fatherContentKey.toString(), daughterContentKey.toString(),
                                      sonContentKey.toString(), grandChildContentKey.toString() );

        AssertTool.assertXPathEquals( "/contents/content[ title = 'Daughter']/relatedcontentkeys/relatedcontentkey/@key", jdomDocResult,
                                      fatherContentKey.toString() );
        AssertTool.assertXPathEquals( "/contents/content[ title = 'Son']/relatedcontentkeys/relatedcontentkey/@key", jdomDocResult,
                                      fatherContentKey.toString() );
        AssertTool.assertXPathEquals( "/contents/content[ title = 'Grand child']/relatedcontentkeys/relatedcontentkey/@key", jdomDocResult,
                                      sonContentKey.toString() );
        AssertTool.assertSingleXPathValueEquals( "/contents/relatedcontents/@count", jdomDocResult, "2" );
        AssertTool.assertXPathEquals( "/contents/relatedcontents/content/@key", jdomDocResult, sonContentKey.toString(),
                                      fatherContentKey.toString() );
    }

    @Test
    public void content_queried_with_both_related_child_and_parent_having_related_content__existing_as_the_queried_content_is_still_listed_as_related_content()
    {
        // setup: create same content in two different categories
        ContentKey grandChildContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Grand child" ), "content-creator" ) );
        fixture.flushIndexTransaction();

        ContentKey sonContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Son", grandChildContentKey ), "content-creator" ) );
        fixture.flushIndexTransaction();

        ContentKey daughterContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Daughter" ), "content-creator" ) );
        fixture.flushIndexTransaction();

        ContentKey fatherContentKey = contentService.createContent(
            createCreateContentCommand( "MyCategory", createMyRelatedContentData( "Father", sonContentKey, daughterContentKey ),
                                        "content-creator" ) );

        fixture.flushIndexTransaction();

        // setup: verify that the content was created
        assertEquals( 4, fixture.countAllContent() );

        // exercise
        DataSourceContext context = new DataSourceContext();
        context.setUser( fixture.findUserByName( "content-querier" ) );

        String query = "categorykey = " + fixture.findCategoryByName( "MyCategory" ).getKey();
        String orderyBy = "@key desc";
        int index = 0;
        int count = 10;
        boolean includeData = true;
        int childrenLevel = 10;
        int parentLevel = 10;

        XMLDocument xmlDocResult =
            dataSourceService.getContentByQuery( context, query, orderyBy, index, count, includeData, childrenLevel, parentLevel );

        // verify
        Document jdomDocResult = xmlDocResult.getAsJDOMDocument();

        XMLOutputter outputter = new XMLOutputter( Format.getPrettyFormat() );
        System.out.println( outputter.outputString( jdomDocResult ) );

        AssertTool.assertSingleXPathValueEquals( "/contents/@totalcount", jdomDocResult, "4" );
        AssertTool.assertXPathEquals( "/contents/content/@key", jdomDocResult, fatherContentKey.toString(), daughterContentKey.toString(),
                                      sonContentKey.toString(), grandChildContentKey.toString() );

        AssertTool.assertXPathEquals( "/contents/content[title = 'Father']/relatedcontentkeys/relatedcontentkey [@level = 1]/@key",
                                      jdomDocResult, sonContentKey.toString(), daughterContentKey.toString() );

        AssertTool.assertXPathEquals( "/contents/content[title = 'Daughter']/relatedcontentkeys/relatedcontentkey[@level = -1]/@key",
                                      jdomDocResult, fatherContentKey.toString() );
        AssertTool.assertXPathEquals( "/contents/content[title = 'Son']/relatedcontentkeys/relatedcontentkey[@level = -1]/@key",
                                      jdomDocResult, fatherContentKey.toString() );
        AssertTool.assertXPathEquals( "/contents/content[title = 'Son']/relatedcontentkeys/relatedcontentkey[@level = 1]/@key",
                                      jdomDocResult, grandChildContentKey.toString() );
        AssertTool.assertXPathEquals( "/contents/content[title = 'Grand child']/relatedcontentkeys/relatedcontentkey[@level = -1]/@key",
                                      jdomDocResult, sonContentKey.toString() );
        AssertTool.assertSingleXPathValueEquals( "/contents/relatedcontents/@count", jdomDocResult, "4" );
        AssertTool.assertXPathEquals( "/contents/relatedcontents/content/@key", jdomDocResult, grandChildContentKey.toString(),
                                      sonContentKey.toString(), daughterContentKey.toString(), fatherContentKey.toString() );
    }

    @Test
    public void content_queried_with_related_children_having_different_read_permissions()
    {
        // set up 2 users
        fixture.createAndStoreNormalUserWithUserGroup( "user_a", "User A", "testuserstore" );
        fixture.createAndStoreNormalUserWithUserGroup( "user_b", "User B", "testuserstore" );

        // content type with a related content type, for example "link_list" and "link" as related content
        ContentTypeConfigBuilder ctyconf = new ContentTypeConfigBuilder( "Link_List", "title" );
        ctyconf.startBlock( "Link_List" );
        ctyconf.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconf.addRelatedContentInput( "link", "contentdata/link", "Link", false, true );
        ctyconf.endBlock();
        Document configAsXmlBytes = XMLDocumentFactory.create( ctyconf.toString() ).getAsJDOMDocument();

        fixture.save( factory.createContentType( "Link_List", ContentHandlerName.CUSTOM.getHandlerClassShortName(), configAsXmlBytes ) );

        // setup related content type
        ContentTypeConfigBuilder ctyconfRel = new ContentTypeConfigBuilder( "Link", "title" );
        ctyconfRel.startBlock( "Link" );
        ctyconfRel.addInput( "title", "text", "contentdata/title", "Title", true );
        ctyconfRel.endBlock();
        Document relConfigAsXmlBytes = XMLDocumentFactory.create( ctyconfRel.toString() ).getAsJDOMDocument();

        fixture.save( factory.createContentType( "Link", ContentHandlerName.CUSTOM.getHandlerClassShortName(), relConfigAsXmlBytes ) );

        // create categories and set user rights
        fixture.save( factory.createUnit( "Link_ListUnit", "en" ) );
        fixture.save(
            factory.createCategory( "Link_List_Category", null, "Link_List", "Link_ListUnit", User.ANONYMOUS_UID, User.ANONYMOUS_UID,
                                    false ) );

        fixture.save( factory.createUnit( "LinkUnit", "en" ) );
        fixture.save( factory.createCategory( "Link_Category", null, "Link", "LinkUnit", User.ANONYMOUS_UID, User.ANONYMOUS_UID, false ) );

        fixture.save( factory.createCategoryAccessForUser( "Link_Category", "user_a", "read, create, approve" ) );
        fixture.save( factory.createCategoryAccessForUser( "Link_Category", "user_b", "read, create, approve" ) );

        fixture.save( factory.createCategoryAccessForUser( "Link_List_Category", "user_a", "read, create, approve" ) );
        fixture.save( factory.createCategoryAccessForUser( "Link_List_Category", "user_b", "read, create, approve" ) );

        fixture.flushAndClearHibernateSesssion();

        // Add a set of links with only reading rights for user A, and another set of links with only reading rights for user B
        CreateContentCommand.AccessRightsStrategy useGivenRights = CreateContentCommand.AccessRightsStrategy.USE_GIVEN;
        ContentKey relCont1ContentKey = contentService.createContent(
            createCreateContentCommand( "Link_Category", createMyRelatedContentData( "link_1" ), "user_a", useGivenRights ) );
        ContentKey relCont2ContentKey = contentService.createContent(
            createCreateContentCommand( "Link_Category", createMyRelatedContentData( "link_2" ), "user_a", useGivenRights ) );
        ContentKey relCont3ContentKey = contentService.createContent(
            createCreateContentCommand( "Link_Category", createMyRelatedContentData( "link_3" ), "user_b", useGivenRights ) );
        ContentKey relCont4ContentKey = contentService.createContent(
            createCreateContentCommand( "Link_Category", createMyRelatedContentData( "link_4" ), "user_b", useGivenRights ) );

        final UserEntity userA = fixture.findUserByName( "user_a" );
        final UserEntity userB = fixture.findUserByName( "user_b" );
        fixture.save( factory.createContentAccess( relCont1ContentKey, userA, "read, create, approve" ) );
        fixture.save( factory.createContentAccess( relCont2ContentKey, userA, "read, create, approve" ) );
        fixture.save( factory.createContentAccess( relCont3ContentKey, userB, "read, create, approve" ) );
        fixture.save( factory.createContentAccess( relCont4ContentKey, userB, "read, create, approve" ) );

        CustomContentData contentData_A = new CustomContentData( fixture.findContentTypeByName( "Link_List" ).getContentTypeConfig() );
        contentData_A.add( new TextDataEntry( contentData_A.getInputConfig( "title" ), "Title content A" ) );
        ContentKey contentA = contentService.createContent( createCreateContentCommand( "Link_List_Category",
                                                                                        createMyRelatedContentData( "Content A",
                                                                                                                    relCont1ContentKey,
                                                                                                                    relCont2ContentKey ),
                                                                                        "user_a" ) );

        CustomContentData contentData_B = new CustomContentData( fixture.findContentTypeByName( "Link_List" ).getContentTypeConfig() );
        contentData_B.add( new TextDataEntry( contentData_B.getInputConfig( "title" ), "Title content B" ) );
        ContentKey contentB = contentService.createContent( createCreateContentCommand( "Link_List_Category",
                                                                                        createMyRelatedContentData( "Content B",
                                                                                                                    relCont3ContentKey,
                                                                                                                    relCont4ContentKey ),
                                                                                        "user_b" ) );

        // publish the link_list contents in a section page with reading rights for both users
        fixture.save( factory.createSite( "My Links Site", new Date(), null, "en" ) );
        MenuItemEntity section = createSection( "MyLinks", "My Links Site", "admin", false );
        fixture.save( section );
        fixture.save( createMenuItemAccess( "MyLinks", "user_a", "read, create, add, publish" ) );
        fixture.save( createMenuItemAccess( "MyLinks", "user_b", "read, create, add, publish" ) );

        AddContentToSectionCommand command = new AddContentToSectionCommand();
        command.setAddOnTop( true );
        command.setApproveInSection( true );
        command.setContributor( userA.getKey() );
        command.setSection( fixture.findMenuItemByName( "MyLinks" ).getKey() );
        command.setContent( contentA );
        menuItemService.execute( command );

        AddContentToSectionCommand command2 = new AddContentToSectionCommand();
        command2.setAddOnTop( true );
        command2.setApproveInSection( true );
        command2.setContributor( userB.getKey() );
        command2.setSection( fixture.findMenuItemByName( "MyLinks" ).getKey() );
        command2.setContent( contentB );
        menuItemService.execute( command2 );

        fixture.flushIndexTransaction();

        // setup: verify that the content was created
        assertEquals( 6, fixture.countAllContent() );

        // exercise
        DataSourceContext context = new DataSourceContext();
        context.setUser( userB );

        String query = "contenttype = 'Link_List'";
        String orderyBy = "";
        int index = 0;
        int count = 10;
        int levels = 1;
        boolean includeData = true;
        int childrenLevel = 1;
        int parentLevel = 0;
        int[] menuItemKeys = new int[]{section.getKey().toInt()};

        XMLDocument xmlDocResult =
            dataSourceService.getContentBySection( context, menuItemKeys, levels, query, orderyBy, index, count, includeData, childrenLevel,
                                                   parentLevel );
        // verify
        Document jdomDocResult = xmlDocResult.getAsJDOMDocument();

        String relCntCount = JDOMUtil.evaluateSingleXPathValueAsString( "/contents/relatedcontents/@count", jdomDocResult );
        int relatedContentCount = Integer.parseInt( relCntCount );
        assertEquals( "number of related contents returned", 2, relatedContentCount );

        String titleContent3 =
            JDOMUtil.evaluateSingleXPathValueAsString( "/contents/relatedcontents/content[1]/contentdata/title", jdomDocResult );
        assertEquals( "related content #1 title", "link_3", titleContent3 );

        String titleContent4 =
            JDOMUtil.evaluateSingleXPathValueAsString( "/contents/relatedcontents/content[2]/contentdata/title", jdomDocResult );
        assertEquals( "related content #2 title", "link_4", titleContent4 );
    }

    private MenuItemEntity createSection( String name, String siteName, String username, boolean isOrdered )
    {
        return factory.createSectionMenuItem( name, 0, null, name, siteName, username, username, "en", null, null, isOrdered, null, false,
                                              null );
    }

    private MenuItemAccessEntity createMenuItemAccess( String menuItemName, String userName, String accesses )
    {
        return factory.createMenuItemAccess( fixture.findMenuItemByName( menuItemName ), fixture.findUserByName( userName ).getUserGroup(),
                                             accesses );
    }

    private ContentData createMyRelatedContentData( String title, ContentKey... relatedContents )
    {
        CustomContentData contentData = new CustomContentData( fixture.findContentTypeByName( "MyRelatedType" ).getContentTypeConfig() );
        if ( title != null )
        {
            contentData.add( new TextDataEntry( contentData.getInputConfig( "title" ), title ) );
        }
        if ( relatedContents != null && relatedContents.length > 0 )
        {
            RelatedContentsDataEntry relatedContentsDataEntry =
                new RelatedContentsDataEntry( contentData.getInputConfig( "myRelatedContent" ) );
            for ( ContentKey relatedKey : relatedContents )
            {
                relatedContentsDataEntry.add( new RelatedContentDataEntry( contentData.getInputConfig( "myRelatedContent" ), relatedKey ) );
            }
            contentData.add( relatedContentsDataEntry );
        }
        return contentData;
    }

    private CreateContentCommand createCreateContentCommand( String categoryName, ContentData contentData, String creatorUid )
    {
        return createCreateContentCommand( categoryName, contentData, creatorUid,
                                           CreateContentCommand.AccessRightsStrategy.INHERIT_FROM_CATEGORY );
    }

    private CreateContentCommand createCreateContentCommand( String categoryName, ContentData contentData, String creatorUid,
                                                             CreateContentCommand.AccessRightsStrategy accessRights )
    {
        CreateContentCommand createContentCommand = new CreateContentCommand();
        createContentCommand.setCategory( fixture.findCategoryByName( categoryName ) );
        createContentCommand.setCreator( fixture.findUserByName( creatorUid ).getKey() );
        createContentCommand.setLanguage( fixture.findLanguageByCode( "en" ) );
        createContentCommand.setStatus( ContentStatus.APPROVED );
        createContentCommand.setPriority( 0 );
        createContentCommand.setAccessRightsStrategy( accessRights );
        createContentCommand.setContentData( contentData );
        createContentCommand.setAvailableFrom( DATE_TIME_2010_01_01.toDate() );
        createContentCommand.setAvailableTo( null );
        createContentCommand.setContentName( "testcontent" );
        return createContentCommand;
    }

    private UpdateContentCommand updateContentCommand( ContentKey contentKeyToUpdate, ContentData contentData, String updaterUid )
    {
        ContentEntity contentToUpdate = fixture.findContentByKey( contentKeyToUpdate );

        UpdateContentCommand command = UpdateContentCommand.storeNewVersionEvenIfUnchanged( contentToUpdate.getMainVersion().getKey() );
        command.setUpdateAsMainVersion( true );
        command.setSyncAccessRights( false );
        command.setSyncRelatedContent( true );
        command.setContentKey( contentToUpdate.getKey() );
        command.setUpdateStrategy( UpdateContentCommand.UpdateStrategy.MODIFY );
        command.setModifier( fixture.findUserByName( updaterUid ).getKey() );
        command.setPriority( 0 );
        command.setLanguage( fixture.findLanguageByCode( "en" ) );
        command.setStatus( ContentStatus.APPROVED );
        command.setContentData( contentData );
        command.setAvailableFrom( DATE_TIME_2010_01_01.toDate() );
        return command;
    }
}
