package org.jbpm.task.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class DBUserGroupCallbackImplTest {

    protected static final String DATASOURCE_PROPERTIES = "/datasource.properties";
    private PoolingDataSource pds;
    private Properties props;

    @Before
    public void setup() {

        Properties dsProps = loadDataSourceProperties();

        pds = new PoolingDataSource();
        pds.setUniqueName("jdbc/taskDS");
        pds.setClassName(dsProps.getProperty("className"));
        pds.setMaxPoolSize(Integer.parseInt(dsProps.getProperty("maxPoolSize")));
        pds.setAllowLocalTransactions(Boolean.parseBoolean(dsProps.getProperty("allowLocalTransactions")));
        for (String propertyName : new String[]{"user", "password"}) {
            pds.getDriverProperties().put(propertyName, dsProps.getProperty(propertyName));
        }
        setDatabaseSpecificDataSourceProperties(pds, dsProps);

        pds.init();

        prepareDb();

        props = new Properties();
        props.setProperty(DBUserGroupCallbackImpl.DS_JNDI_NAME, "jdbc/taskDS");
        props.setProperty(DBUserGroupCallbackImpl.PRINCIPAL_QUERY, "select userId from Users where userId = ?");
        props.setProperty(DBUserGroupCallbackImpl.ROLES_QUERY, "select groupId from Groups where groupId = ?");
        props.setProperty(DBUserGroupCallbackImpl.USER_ROLES_QUERY, "select groupId from Groups where userId = ?");
    }

    protected Properties loadDataSourceProperties() {

        InputStream propsInputStream = getClass().getResourceAsStream(DATASOURCE_PROPERTIES);

        Properties dsProps = new Properties();
        if (propsInputStream != null) {
            try {
                dsProps.load(propsInputStream);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return dsProps;
    }

    protected void prepareDb() {
        try {
            Connection conn = pds.getConnection();
            String createUserTableSql = "create table Users (userId varchar(255))";
            PreparedStatement st = conn.prepareStatement(createUserTableSql);
            st.execute();

            String createGroupTableSql = "create table Groups (groupId varchar(255), userId varchar(255))";
            st = conn.prepareStatement(createGroupTableSql);
            st.execute();

            // insert user rows
            String insertUser = "insert into Users (userId) values (?)";
            st = conn.prepareStatement(insertUser);
            st.setString(1, "john");
            st.execute();

            // insert group rows
            String insertGroup = "insert into Groups (groupId, userId) values (?, ?)";
            st = conn.prepareStatement(insertGroup);
            st.setString(1, "PM");
            st.setString(2, "john");
            st.execute();


            st.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    protected void cleanDb() {
        try {
            Connection conn = pds.getConnection();
            String dropUserTableSql = "drop table Users";
            PreparedStatement st = conn.prepareStatement(dropUserTableSql);
            st.execute();

            String dropGroupTableSql = "drop table Groups";
            st = conn.prepareStatement(dropGroupTableSql);

            st.execute();

            st.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    @After
    public void cleanup() {
        cleanDb();
        pds.close();
    }

    @Test
    public void testUserExists() {



        DBUserGroupCallbackImpl callback = new DBUserGroupCallbackImpl(props);
        boolean exists = callback.existsUser("john");
        assertTrue(exists);
    }

    @Test
    public void testGroupExists() {

        DBUserGroupCallbackImpl callback = new DBUserGroupCallbackImpl(props);
        boolean exists = callback.existsGroup("PM");
        assertTrue(exists);
    }

    @Test
    public void testUserGroups() {

        DBUserGroupCallbackImpl callback = new DBUserGroupCallbackImpl(props);
        List<String> groups = callback.getGroupsForUser("john", null, null);
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertEquals("PM", groups.get(0));
    }

    @Test
    public void testUserNotExists() {

        DBUserGroupCallbackImpl callback = new DBUserGroupCallbackImpl(props);
        boolean exists = callback.existsUser("mike");
        assertFalse(exists);
    }

    @Test
    public void testGroupNotExists() {

        DBUserGroupCallbackImpl callback = new DBUserGroupCallbackImpl(props);
        boolean exists = callback.existsGroup("HR");
        assertFalse(exists);
    }

    @Test
    public void testNoUserGroups() {

        DBUserGroupCallbackImpl callback = new DBUserGroupCallbackImpl(props);
        List<String> groups = callback.getGroupsForUser("mike", null, null);
        assertNotNull(groups);
        assertEquals(0, groups.size());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfiguration() {

        Properties invalidProps = new Properties();
        DBUserGroupCallbackImpl callback = new DBUserGroupCallbackImpl(invalidProps);
        callback.getGroupsForUser("mike", null, null);
        fail("Should fail as it does not have valid configuration");

    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidArgument() {

        DBUserGroupCallbackImpl callback = new DBUserGroupCallbackImpl(props);
        callback.getGroupsForUser(null, null, null);
        fail("Should fail as it does not have valid configuration");

    }

    private void setDatabaseSpecificDataSourceProperties(PoolingDataSource pds, Properties dsProps) {
        String driverClass = dsProps.getProperty("driverClassName");
        if (driverClass.startsWith("org.h2")) {
            for (String propertyName : new String[]{"url", "driverClassName"}) {
                pds.getDriverProperties().put(propertyName, dsProps.getProperty(propertyName));
            }
        } else {

            if (driverClass.startsWith("oracle")) {
                pds.getDriverProperties().put("driverType", "thin");
                pds.getDriverProperties().put("URL", dsProps.getProperty("url"));
            } else if (driverClass.startsWith("com.ibm.db2")) {
                for (String propertyName : new String[]{"databaseName", "portNumber", "serverName"}) {
                    pds.getDriverProperties().put(propertyName, dsProps.getProperty(propertyName));
                }
                pds.getDriverProperties().put("driverType", "4");
            } else if (driverClass.startsWith("com.microsoft")) {
                for (String propertyName : new String[]{"serverName", "portNumber", "databaseName"}) {
                    pds.getDriverProperties().put(propertyName, dsProps.getProperty(propertyName));
                }
                pds.getDriverProperties().put("URL", dsProps.getProperty("url"));
                pds.getDriverProperties().put("selectMethod", "cursor");
                pds.getDriverProperties().put("InstanceName", "MSSQL01");
            } else if (driverClass.startsWith("com.mysql")) {
                for (String propertyName : new String[]{"databaseName", "serverName", "portNumber", "url"}) {
                    pds.getDriverProperties().put(propertyName, dsProps.getProperty(propertyName));
                }
            } else if (driverClass.startsWith("com.sybase")) {
                for (String propertyName : new String[]{"databaseName", "portNumber", "serverName"}) {
                    pds.getDriverProperties().put(propertyName, dsProps.getProperty(propertyName));
                }
                pds.getDriverProperties().put("REQUEST_HA_SESSION", "false");
                pds.getDriverProperties().put("networkProtocol", "Tds");
            } else if (driverClass.startsWith("org.postgresql")) {
                for (String propertyName : new String[]{"databaseName", "portNumber", "serverName"}) {
                    pds.getDriverProperties().put(propertyName, dsProps.getProperty(propertyName));
                }
            } else {
                throw new RuntimeException("Unknown driver class: " + driverClass);
            }
        }
    }
}
