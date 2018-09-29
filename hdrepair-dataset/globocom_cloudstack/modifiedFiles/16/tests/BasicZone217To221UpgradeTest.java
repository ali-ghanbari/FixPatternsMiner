/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.upgrade.dao;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.ConfigurationException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import com.cloud.upgrade.dao.VersionVO.Step;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DbTestUtils;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

public class BasicZone217To221UpgradeTest extends TestCase {
    private static final Logger s_logger = Logger.getLogger(BasicZone217To221UpgradeTest.class);

    @Override
    @Before
    public void setUp() throws Exception {
        VersionVO version = new VersionVO("2.1.7");
        version.setStep(Step.Cleanup);
        DbTestUtils.executeScript("VersionDaoImplTest/clean-db.sql", false, true);
    }
    
    @Override
    @After
    public void tearDown() throws Exception {
    }
    
    public void test217to22Upgrade() {
        s_logger.debug("Finding sample data from 2.1.7");
        DbTestUtils.executeScript("VersionDaoImplTest/2.1.7/2.1.7_sample_basicZone_noSecurityGroups.sql", false, true);
        
        Connection conn = Transaction.getStandaloneConnection();
        PreparedStatement pstmt;
        
        VersionDaoImpl dao = ComponentLocator.inject(VersionDaoImpl.class);
        
        String version = dao.getCurrentVersion();
        
        if (!version.equals("2.1.7")) {
            s_logger.error("Version returned is not 2.1.7 but " + version);
        } else {
            s_logger.debug("Basic zone test version is " + version);
        }
        
        try {
            dao.upgrade("2.1.7", "2.2.3");
        } catch (ConfigurationException e) {
            s_logger.warn("Exception: ", e);
            assert false : "The test failed.  Check logs"; 
        }
        
        conn = Transaction.getStandaloneConnection();
        try {
            
            s_logger.debug("Starting tesing upgrade from 2.1.7 to 2.2.2 for Basic zone...");
            
            //Version check
            pstmt = conn.prepareStatement("SELECT version FROM version");
            ResultSet rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                s_logger.error("ERROR: No version selected");
            } else if (!rs.getString(1).equals("2.2.1")) {
                s_logger.error("ERROR: VERSION stored is not 2.2.1: " + rs.getString(1));
            }
            rs.close();
            pstmt.close();
            
            //Check that default network offerings are present
            pstmt = conn.prepareStatement("SELECT COUNT(*) FROM network_offerings");
            rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                s_logger.error("ERROR: Unable to get the count of network offerings.");
            } else if (rs.getInt(1) != 7) {
                s_logger.error("ERROR: Didn't find 7 network offerings but found " + rs.getInt(1));
            } else {
                s_logger.debug("Network offerings test passed");
            }

            rs.close();
            pstmt.close();
            
            
            //Zone network type check
            pstmt = conn.prepareStatement("SELECT DISTINCT networktype FROM data_center");
            rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                s_logger.error("No zone exists after upgrade");
            } else if (!rs.getString(1).equals("Basic")) {
                s_logger.error("ERROR: Zone type is not Basic");
            } else if (rs.next()) {
                s_logger.error("ERROR: Why do we have more than 1 zone with different types??");
                System.exit(2);
            } else {
                s_logger.debug("Test passed. Zone was updated properly with type Basic");
            }
            rs.close();
            pstmt.close();
            
            //Check that vnet/cidr were set to NULL for basic zone
            pstmt = conn.prepareStatement("SELECT vnet, guest_network_cidr FROM data_center");
            rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                s_logger.error("ERROR: vnet field is missing for the zone");
            } else if (rs.getString(1) != null || rs.getString(2) != null) {
                s_logger.error("ERROR: vnet/guestCidr should be NULL for basic zone; instead it's " + rs.getString(1));
            } else {
                s_logger.debug("Test passed. Vnet and cidr are set to NULL for the basic zone");
            }
            
            rs.close();
            pstmt.close();
            
            //Verify that default Direct guest network got created, and it's Shared and Default
            pstmt = conn.prepareStatement("SELECT traffic_type, guest_type, shared, is_default, id FROM networks WHERE name LIKE '%BasicZoneDirectNetwork%'");
            rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                s_logger.error("Direct network is missing for the Basic zone");
            } else if (!rs.getString(1).equalsIgnoreCase("Guest") || !rs.getString(2).equalsIgnoreCase("Direct") || !rs.getBoolean(3) || !rs.getBoolean(4)) {
                s_logger.error("Direct network for basic zone has incorrect setting");
            } else {
                s_logger.debug("Test passed. Default Direct Basic zone network parameters were set correctly");
            }
            
            long defaultDirectNetworkId = rs.getInt(5);
            rs.close();
            pstmt.close();
            
            //Verify that all vlans in the zone belong to default Direct network
            pstmt = conn.prepareStatement("SELECT network_id FROM vlan");
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                if (rs.getInt(1) != defaultDirectNetworkId) {
                    s_logger.error("ERROR: network_id is set incorrectly for public untagged vlans in Basic zone");
                    System.exit(2);
                }
            }
            
            s_logger.debug("Test passed for vlan table in Basic zone");
            
            rs.close();
            pstmt.close();
            
            //Verify user_ip_address table
            pstmt = conn.prepareStatement("SELECT source_network_id FROM user_ip_address");
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                if (rs.getInt(1) != defaultDirectNetworkId) {
                    s_logger.error("ERROR: network_id is set incorrectly for public Ip addresses (user_ip_address table) in Basic zone");
                    System.exit(2);
                }
            }
            
            s_logger.debug("Test passed for user_ip_address table in Basic zone");
            
            rs.close();
            pstmt.close();
            
            //Verify domain_router table
            pstmt = conn.prepareStatement("SELECT network_id FROM domain_router");
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                if (rs.getInt(1) != defaultDirectNetworkId) {
                    s_logger.error("ERROR: network_id is set incorrectly for domain routers (domain_router table) in Basic zone");
                    System.exit(2);
                }
            }
            
            s_logger.debug("Test passed for domain_router table in Basic zone");
            
            rs.close();
            pstmt.close();
            
            s_logger.debug("Basic zone test is finished");
            
        } catch (SQLException e) {
            throw new CloudRuntimeException("Problem checking upgrade version", e);
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
            }
        }
    }
    
}
