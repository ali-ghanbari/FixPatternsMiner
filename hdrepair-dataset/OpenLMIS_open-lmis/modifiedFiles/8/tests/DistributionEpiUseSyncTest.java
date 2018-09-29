/*
 *
 *  * This program is part of the OpenLMIS logistics management information system platform software.
 *  * Copyright © 2013 VillageReach
 *  *
 *  * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  *  
 *  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *  * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 *
 */

package org.openlmis.functional;


import org.openlmis.UiUtils.TestCaseHelper;
import org.openlmis.UiUtils.TestWebDriver;
import org.openlmis.pageobjects.*;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.selenium.SeleneseTestBase.assertTrue;
import static java.util.Arrays.asList;

public class DistributionEpiUseSyncTest extends TestCaseHelper {

  public static final String USER = "user";
  public static final String PASSWORD = "password";
  public static final String FIRST_DELIVERY_ZONE_CODE = "firstDeliveryZoneCode";
  public static final String SECOND_DELIVERY_ZONE_CODE = "secondDeliveryZoneCode";
  public static final String FIRST_DELIVERY_ZONE_NAME = "firstDeliveryZoneName";
  public static final String SECOND_DELIVERY_ZONE_NAME = "secondDeliveryZoneName";
  public static final String FIRST_FACILITY_CODE = "firstFacilityCode";
  public static final String SECOND_FACILITY_CODE = "secondFacilityCode";
  public static final String VACCINES_PROGRAM = "vaccinesProgram";
  public static final String TB_PROGRAM = "secondProgram";
  public static final String SCHEDULE = "schedule";
  public static final String PRODUCT_GROUP_CODE = "productGroupName";
  LoginPage loginPage;
  FacilityListPage facilityListPage;

  public Map<String, String> epiUseData = new HashMap<String, String>() {{
    put(USER, "fieldCoordinator");
    put(PASSWORD, "Admin123");
    put(FIRST_DELIVERY_ZONE_CODE, "DZ1");
    put(SECOND_DELIVERY_ZONE_CODE, "DZ2");
    put(FIRST_DELIVERY_ZONE_NAME, "Delivery Zone First");
    put(SECOND_DELIVERY_ZONE_NAME, "Delivery Zone Second");
    put(FIRST_FACILITY_CODE, "F10");
    put(SECOND_FACILITY_CODE, "F11");
    put(VACCINES_PROGRAM, "VACCINES");
    put(TB_PROGRAM, "TB");
    put(SCHEDULE, "M");
    put(PRODUCT_GROUP_CODE, "PG1");
  }};

  @BeforeMethod(groups = {"distribution"})
  public void setUp() throws Exception {
    super.setup();
    loginPage = PageFactory.getInstanceOfLoginPage(testWebDriver, baseUrlGlobal);
    facilityListPage = PageFactory.getInstanceOfFacilityListPage(testWebDriver);

    Map<String, String> dataMap = epiUseData;
    setupDataForDistributionTest(dataMap.get(USER), dataMap.get(FIRST_DELIVERY_ZONE_CODE), dataMap.get(SECOND_DELIVERY_ZONE_CODE),
      dataMap.get(FIRST_DELIVERY_ZONE_NAME), dataMap.get(SECOND_DELIVERY_ZONE_NAME), dataMap.get(FIRST_FACILITY_CODE),
      dataMap.get(SECOND_FACILITY_CODE), dataMap.get(VACCINES_PROGRAM), dataMap.get(TB_PROGRAM), dataMap.get(SCHEDULE),
      dataMap.get(PRODUCT_GROUP_CODE));
  }

  @Test(groups = {"distribution"})
  public void testEpiUsePageSync() throws Exception {
    HomePage homePage = loginPage.loginAs(epiUseData.get(USER), epiUseData.get(PASSWORD));
    initiateDistribution(epiUseData.get(FIRST_DELIVERY_ZONE_NAME), epiUseData.get(VACCINES_PROGRAM));
    RefrigeratorPage refrigeratorPage = facilityListPage.selectFacility(epiUseData.get(FIRST_FACILITY_CODE));

    EPIUsePage epiUsePage = refrigeratorPage.navigateToEpiUse();
    epiUsePage.verifyIndicator("RED");
    epiUsePage.verifyProductGroup("PG1-Name", 1);
    epiUsePage.enterData(10, 20, 30, 40, 50, "10/2011", 1);
    epiUsePage.verifyIndicator("GREEN");

    GeneralObservationPage generalObservationPage = epiUsePage.navigateToGeneralObservations();
    generalObservationPage.enterData("some observations", "samuel", "Doe", "Verifier", "XYZ");

    FullCoveragePage fullCoveragePage = generalObservationPage.navigateToFullCoverage();
    fullCoveragePage.enterData(12, 34, 45, "56");

    generalObservationPage.navigateToEpiInventory();
    fillEpiInventoryWithOnlyDeliveredQuantity("2", "4", "6");

    DistributionPage distributionPage = homePage.navigateToDistributionWhenOnline();
    distributionPage.syncDistribution(1);
    assertTrue(distributionPage.getSyncMessage().contains("F10-Village Dispensary"));
    distributionPage.syncDistributionMessageDone();

    verifyEpiUseDataInDatabase(10, 20, 30, 40, 50, "10/2011", epiUseData.get(PRODUCT_GROUP_CODE), epiUseData.get(FIRST_FACILITY_CODE));
  }

  @Test(groups = {"distribution"})
  public void testEpiUseEditSync() throws Exception {
    HomePage homePage = loginPage.loginAs(epiUseData.get(USER), epiUseData.get(PASSWORD));
    initiateDistribution(epiUseData.get(FIRST_DELIVERY_ZONE_NAME), epiUseData.get(VACCINES_PROGRAM));
    RefrigeratorPage refrigeratorPage = facilityListPage.selectFacility(epiUseData.get(FIRST_FACILITY_CODE));

    EPIUsePage epiUsePage = refrigeratorPage.navigateToEpiUse();
    epiUsePage.verifyProductGroup("PG1-Name", 1);
    epiUsePage.verifyIndicator("RED");
    epiUsePage.enterValueInLoss("0", 1);
    epiUsePage.verifyIndicator("AMBER");
    epiUsePage.enterData(10, 20, 30, 40, 50, "10/2011", 1);
    epiUsePage.verifyIndicator("GREEN");

    epiUsePage.navigateToRefrigerators();
    refrigeratorPage.navigateToEpiUse();

    epiUsePage.verifyTotal("30", 1);
    epiUsePage.verifyStockAtFirstOfMonth("10", 1);
    epiUsePage.verifyReceived("20", 1);
    epiUsePage.verifyDistributed("30", 1);
    epiUsePage.verifyLoss("40", 1);
    epiUsePage.verifyStockAtEndOfMonth("50", 1);
    epiUsePage.verifyExpirationDate("10/2011", 1);

    epiUsePage.checkUncheckStockAtFirstOfMonthNotRecorded(1);
    epiUsePage.checkUncheckReceivedNotRecorded(1);
    epiUsePage.checkUncheckDistributedNotRecorded(1);
    epiUsePage.checkUncheckLossNotRecorded(1);
    epiUsePage.checkUncheckStockAtEndOfMonthNotRecorded(1);
    epiUsePage.checkUncheckExpirationDateNotRecorded(1);

    epiUsePage.navigateToRefrigerators();
    refrigeratorPage.navigateToEpiUse();

    epiUsePage.verifyStockAtFirstOfMonthStatus(false, 1);
    epiUsePage.verifyReceivedStatus(false, 1);
    epiUsePage.verifyDistributedStatus(false, 1);
    epiUsePage.verifyLossStatus(false, 1);
    epiUsePage.verifyStockAtEndOfMonthStatus(false, 1);
    epiUsePage.verifyExpirationDateStatus(false, 1);

    epiUsePage.checkUncheckStockAtFirstOfMonthNotRecorded(1);
    epiUsePage.checkUncheckReceivedNotRecorded(1);
    epiUsePage.checkUncheckDistributedNotRecorded(1);
    epiUsePage.checkUncheckLossNotRecorded(1);
    epiUsePage.checkUncheckStockAtEndOfMonthNotRecorded(1);
    epiUsePage.checkUncheckExpirationDateNotRecorded(1);

    epiUsePage.enterData(20, 30, 40, 50, 60, "11/2012", 1);
    epiUsePage.verifyIndicator("GREEN");

    epiUsePage.navigateToRefrigerators();
    refrigeratorPage.navigateToEpiUse();
    epiUsePage.checkApplyNRToAllFields(false);
    epiUsePage.verifyTotal("50", 1);

    epiUsePage.verifyStockAtFirstOfMonth("20", 1);
    epiUsePage.verifyReceived("30", 1);
    epiUsePage.verifyDistributed("40", 1);
    epiUsePage.verifyLoss("50", 1);
    epiUsePage.verifyStockAtEndOfMonth("60", 1);
    epiUsePage.verifyExpirationDate("11/2012", 1);

    epiUsePage.verifyStockAtFirstOfMonthStatus(true, 1);
    epiUsePage.verifyReceivedStatus(true, 1);
    epiUsePage.verifyDistributedStatus(true, 1);
    epiUsePage.verifyLossStatus(true, 1);
    epiUsePage.verifyStockAtEndOfMonthStatus(true, 1);
    epiUsePage.verifyExpirationDateStatus(true, 1);
    epiUsePage.verifyIndicator("GREEN");

    GeneralObservationPage generalObservationPage = epiUsePage.navigateToGeneralObservations();
    generalObservationPage.enterData("some observations", "samuel", "Doe", "Verifier", "XYZ");

    FullCoveragePage fullCoveragePage = generalObservationPage.navigateToFullCoverage();
    fullCoveragePage.enterData(12, 34, 45, "56");

    generalObservationPage.navigateToEpiInventory();
    fillEpiInventoryWithOnlyDeliveredQuantity("2", "4", "6");

    DistributionPage distributionPage = homePage.navigateToDistributionWhenOnline();
    distributionPage.syncDistribution(1);
    assertTrue(distributionPage.getSyncMessage().contains("F10-Village Dispensary"));
    distributionPage.syncDistributionMessageDone();

    verifyEpiUseDataInDatabase(20, 30, 40, 50, 60, "11/2012", epiUseData.get(PRODUCT_GROUP_CODE), epiUseData.get(FIRST_FACILITY_CODE));
  }

  @Test(groups = {"distribution"})
  public void testEpiUsePageSyncWhenSomeFieldsEmpty() throws Exception {
    loginPage.loginAs(epiUseData.get(USER), epiUseData.get(PASSWORD));
    initiateDistribution(epiUseData.get(FIRST_DELIVERY_ZONE_NAME), epiUseData.get(VACCINES_PROGRAM));
    RefrigeratorPage refrigeratorPage = facilityListPage.selectFacility(epiUseData.get(FIRST_FACILITY_CODE));

    EPIUsePage epiUsePage = refrigeratorPage.navigateToEpiUse();
    epiUsePage.verifyProductGroup("PG1-Name", 1);
    epiUsePage.verifyIndicator("RED");
    epiUsePage.enterValueInStockAtFirstOfMonth("10", 1);
    epiUsePage.verifyIndicator("AMBER");

    GeneralObservationPage generalObservationPage = epiUsePage.navigateToGeneralObservations();
    generalObservationPage.enterData("some observations", "samuel", "Doe", "Verifier", "XYZ");

    FullCoveragePage fullCoveragePage = generalObservationPage.navigateToFullCoverage();
    fullCoveragePage.enterData(12, 34, 45, "56");

    generalObservationPage.navigateToEpiInventory();
    fillEpiInventoryWithOnlyDeliveredQuantity("2", "4", "6");

    facilityListPage.selectFacility(epiUseData.get(FIRST_FACILITY_CODE));
    facilityListPage.verifyFacilityIndicatorColor("Overall", "AMBER");

    verifyEpiUseDataInDatabase(null, null, null, null, null, null, epiUseData.get(PRODUCT_GROUP_CODE), epiUseData.get(FIRST_FACILITY_CODE));
  }

  @Test(groups = {"distribution"})
  public void testEpiUsePageSyncWhenNrAppliedToAllFields() throws Exception {
    HomePage homePage = loginPage.loginAs(epiUseData.get(USER), epiUseData.get(PASSWORD));
    initiateDistribution(epiUseData.get(FIRST_DELIVERY_ZONE_NAME), epiUseData.get(VACCINES_PROGRAM));
    RefrigeratorPage refrigeratorPage = facilityListPage.selectFacility(epiUseData.get(FIRST_FACILITY_CODE));

    EPIUsePage epiUsePage = refrigeratorPage.navigateToEpiUse();

    epiUsePage.verifyProductGroup("PG1-Name", 1);
    epiUsePage.verifyIndicator("RED");
    epiUsePage.verifyProductGroup("PG1-Name", 1);
    epiUsePage.enterData(10, 20, 30, 40, 50, "10/2011", 1);
    epiUsePage.verifyIndicator("GREEN");

    epiUsePage.checkApplyNRToAllFields(true);
    epiUsePage.verifyIndicator("GREEN");
    epiUsePage.verifyStockAtFirstOfMonthStatus(false, 1);
    epiUsePage.verifyReceivedStatus(false, 1);
    epiUsePage.verifyDistributedStatus(false, 1);
    epiUsePage.verifyLossStatus(false, 1);
    epiUsePage.verifyStockAtEndOfMonthStatus(false, 1);
    epiUsePage.verifyExpirationDateStatus(false, 1);

    GeneralObservationPage generalObservationPage = epiUsePage.navigateToGeneralObservations();
    generalObservationPage.enterData("some observations", "samuel", "Doe", "Verifier", "XYZ");

    FullCoveragePage fullCoveragePage = generalObservationPage.navigateToFullCoverage();
    fullCoveragePage.enterData(12, 34, 45, "56");

    generalObservationPage.navigateToEpiInventory();
    fillEpiInventoryWithOnlyDeliveredQuantity("2", "4", "6");

    DistributionPage distributionPage = homePage.navigateToDistributionWhenOnline();
    distributionPage.syncDistribution(1);
    assertTrue(distributionPage.getSyncMessage().contains("F10-Village Dispensary"));
    distributionPage.syncDistributionMessageDone();

    distributionPage.clickRecordData(1);
    facilityListPage.selectFacility("F10");
    facilityListPage.verifyFacilityIndicatorColor("Overall", "BLUE");

    verifyEpiUseDataInDatabase(null, null, null, null, null, null, epiUseData.get(PRODUCT_GROUP_CODE), epiUseData.get(FIRST_FACILITY_CODE));
  }

  @Test(groups = {"distribution"})
  public void testEpiUsePageSyncWhenNRAppliedToFewFields() throws Exception {
    dbWrapper.insertProductGroup("PG2");
    dbWrapper.insertProductWithGroup("Product7", "ProductName7", "PG2", true);
    dbWrapper.insertProgramProduct("Product7", epiUseData.get(VACCINES_PROGRAM), "10", "true");

    HomePage homePage = loginPage.loginAs(epiUseData.get(USER), epiUseData.get(PASSWORD));
    initiateDistribution(epiUseData.get(FIRST_DELIVERY_ZONE_NAME), epiUseData.get(VACCINES_PROGRAM));
    RefrigeratorPage refrigeratorPage = facilityListPage.selectFacility(epiUseData.get(FIRST_FACILITY_CODE));

    EPIUsePage epiUsePage = refrigeratorPage.navigateToEpiUse();
    epiUsePage.verifyIndicator("RED");
    epiUsePage.verifyProductGroup("PG1-Name", 1);
    epiUsePage.checkApplyNRToStockAtFirstOfMonth0();
    epiUsePage.verifyIndicator("AMBER");
    epiUsePage.checkApplyNRToReceived0();
    epiUsePage.checkApplyNRToDistributed0();
    epiUsePage.checkApplyNRToLoss0();
    epiUsePage.enterValueInStockAtEndOfMonth("4", 1);
    epiUsePage.enterValueInExpirationDate("12/2031", 1);
    epiUsePage.enterData(10, 20, 30, 40, 50, "10/2011", 2);
    epiUsePage.verifyIndicator("GREEN");

    GeneralObservationPage generalObservationPage = epiUsePage.navigateToGeneralObservations();
    generalObservationPage.enterData("some observations", "samuel", "Doe", "Verifier", "XYZ");

    FullCoveragePage fullCoveragePage = generalObservationPage.navigateToFullCoverage();
    fullCoveragePage.enterData(12, 34, 45, "56");

    EpiInventoryPage epiInventoryPage = generalObservationPage.navigateToEpiInventory();
    fillEpiInventoryWithOnlyDeliveredQuantity("2", "4", "6");
    epiInventoryPage.fillDeliveredQuantity(4, "8");

    DistributionPage distributionPage = homePage.navigateToDistributionWhenOnline();
    distributionPage.syncDistribution(1);
    assertTrue(distributionPage.getSyncMessage().contains("F10-Village Dispensary"));
    distributionPage.syncDistributionMessageDone();

    verifyEpiUseDataInDatabase(null, null, null, null, 4, "12/2031", epiUseData.get(PRODUCT_GROUP_CODE), epiUseData.get(FIRST_FACILITY_CODE));
    verifyEpiUseDataInDatabase(10, 20, 30, 40, 50, "10/2011", "PG2", epiUseData.get(FIRST_FACILITY_CODE));
  }

  @Test(groups = {"distribution"})
  public void shouldDisplayNoProductsAddedMessageOnEpiUsePageWhenNoActiveProducts() throws Exception {
    dbWrapper.updateFieldValue("products", "active", "false", "code", "P10");
    dbWrapper.updateFieldValue("products", "active", "false", "code", "P11");
    dbWrapper.updateFieldValue("products", "active", "false", "code", "Product6");

    HomePage homePage = loginPage.loginAs(epiUseData.get(USER), epiUseData.get(PASSWORD));
    DistributionPage distributionPage = homePage.navigateToDistributionWhenOnline();
    distributionPage.initiate(epiUseData.get(FIRST_DELIVERY_ZONE_NAME), epiUseData.get(VACCINES_PROGRAM));
    FacilityListPage facilityListPage = distributionPage.clickRecordData(1);
    EPIUsePage epiUsePage = facilityListPage.selectFacility(epiUseData.get(FIRST_FACILITY_CODE)).navigateToEpiUse();

    assertTrue(epiUsePage.getNoProductsAddedMessage().contains("No products added"));
    epiUsePage.verifyIndicator("GREEN");

    dbWrapper.updateFieldValue("products", "active", "true", "code", "P10");
    dbWrapper.updateFieldValue("products", "active", "true", "code", "P11");
    dbWrapper.updateFieldValue("products", "active", "true", "code", "Product6");
  }

  public void setupDataForDistributionTest(String userSIC, String deliveryZoneCodeFirst, String deliveryZoneCodeSecond,
                                           String deliveryZoneNameFirst, String deliveryZoneNameSecond,
                                           String facilityCodeFirst, String facilityCodeSecond,
                                           String programFirst, String programSecond, String schedule, String productGroupCode) throws Exception {
    List<String> rightsList = asList("MANAGE_DISTRIBUTION");
    setupTestDataToInitiateRnRAndDistribution(facilityCodeFirst, facilityCodeSecond, true, programFirst, userSIC, "200", rightsList,
      programSecond, "District1", "Ngorongoro", "Ngorongoro");
    setupDataForDeliveryZone(true, deliveryZoneCodeFirst, deliveryZoneCodeSecond, deliveryZoneNameFirst, deliveryZoneNameSecond,
      facilityCodeFirst, facilityCodeSecond, programFirst, programSecond, schedule);
    dbWrapper.insertRoleAssignmentForDistribution(userSIC, "store in-charge", deliveryZoneCodeFirst);
    dbWrapper.insertRoleAssignmentForDistribution(userSIC, "store in-charge", deliveryZoneCodeSecond);
    dbWrapper.insertProductGroup(productGroupCode);
    dbWrapper.insertProductWithGroup("Product5", "ProductName5", productGroupCode, true);
    dbWrapper.insertProductWithGroup("Product6", "ProductName6", productGroupCode, true);
    dbWrapper.insertProgramProduct("Product5", programFirst, "10", "false");
    dbWrapper.insertProgramProduct("Product6", programFirst, "10", "true");
  }

  public void initiateDistribution(String deliveryZoneNameFirst, String programFirst) throws IOException {
    HomePage homePage = PageFactory.getInstanceOfHomePage(testWebDriver);
    DistributionPage distributionPage = homePage.navigateToDistributionWhenOnline();
    distributionPage.selectValueFromDeliveryZone(deliveryZoneNameFirst);
    distributionPage.selectValueFromProgram(programFirst);
    distributionPage.clickInitiateDistribution();
    distributionPage.clickRecordData(1);
  }

  public void fillEpiInventoryWithOnlyDeliveredQuantity(String deliveredQuantity1, String deliveredQuantity2, String deliveredQuantity3) {
    EpiInventoryPage epiInventoryPage = PageFactory.getInstanceOfEpiInventoryPage(testWebDriver);
    epiInventoryPage.applyNRToAll();
    epiInventoryPage.fillDeliveredQuantity(1, deliveredQuantity1);
    epiInventoryPage.fillDeliveredQuantity(2, deliveredQuantity2);
    epiInventoryPage.fillDeliveredQuantity(3, deliveredQuantity3);
  }

  @AfterMethod(groups = "distribution")
  public void tearDown() throws Exception {
    testWebDriver.sleep(500);
    if (!testWebDriver.getElementById("username").isDisplayed()) {
      HomePage homePage = PageFactory.getInstanceOfHomePage(testWebDriver);
      homePage.logout(baseUrlGlobal);
      dbWrapper.deleteData();
      dbWrapper.closeConnection();
    }
    ((JavascriptExecutor) TestWebDriver.getDriver()).executeScript("indexedDB.deleteDatabase('open_lmis');");
  }

}
