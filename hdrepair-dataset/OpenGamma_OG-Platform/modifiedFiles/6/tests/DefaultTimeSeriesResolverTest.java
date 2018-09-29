/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.timeseries.config;


import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.config.ConfigDocument;
import com.opengamma.engine.config.ConfigSource;
import com.opengamma.engine.config.MockConfigSource;
import com.opengamma.engine.security.MockSecuritySource;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.financial.timeseries.TimeSeriesMetaData;
import com.opengamma.financial.timeseries.TimeSeriesMetaDataResolver;
import com.opengamma.financial.timeseries.exchange.DefaultExchangeDataProvider;
import com.opengamma.financial.timeseries.exchange.ExchangeDataProvider;
import com.opengamma.id.IdentifierBundle;

/**
 * Test DefaultTimeSeriesResolver.
 */
public class DefaultTimeSeriesResolverTest {

  private static final Logger s_logger = LoggerFactory.getLogger(DefaultTimeSeriesResolverTest.class);
  private TimeSeriesMetaDataResolver _metaDataResolver;
  private MockSecuritySource _mockSecuritySource;

  @Before
  public void setUp() throws Exception {
    _mockSecuritySource = new MockSecuritySource();
    ExchangeDataProvider exchangeDataProvider = new DefaultExchangeDataProvider();
    ConfigSource configsource = setUpConfigSource();
    DefaultTimeSeriesResolver defaultResolver = new DefaultTimeSeriesResolver(_mockSecuritySource, exchangeDataProvider, configsource);
    _metaDataResolver = defaultResolver;
  }

  @After
  public void tearDown() throws Exception {
    _metaDataResolver = null;
    _mockSecuritySource = null;
  }

  private ConfigSource setUpConfigSource() {
    MockConfigSource cfgSource = new MockConfigSource();
    //add tsmetadata configuration
    ConfigDocument<TimeSeriesMetaDataConfiguration> doc = new ConfigDocument<TimeSeriesMetaDataConfiguration>();
    //set up config for equity security
    TimeSeriesMetaDataConfiguration definition = new TimeSeriesMetaDataConfiguration("EQUITY", "BLOOMBERG", "PX_LAST", "EXCH");
    definition.addDataSource("REUTERS");
    definition.addDataField("VOLUME");
    doc.setName("EQUITY");
    doc.setValue(definition);
    cfgSource.add(doc);
    //set up config for bond security
    doc = new ConfigDocument<TimeSeriesMetaDataConfiguration>();
    definition = new TimeSeriesMetaDataConfiguration("BOND", "BLOOMBERG", "PX_LAST", "CMPL");
    definition.addDataSource("REUTERS");
    definition.addDataField("VOLUME");
    definition.addDataProvider("CMPN");
    definition.addDataProvider("CMPT");
    doc.setName("BOND");
    doc.setValue(definition);
    cfgSource.add(doc);
    return cfgSource;
  }

//  @Test
//  public void testBondSecurity() {
//    CorporateBondSecurity security = TimeseriesMasterTestUtils.makeExpectedCorporateBondSecurity();
//    IdentifierBundle identifierBundle = security.getIdentifiers();
//    TimeSeriesMetaData metaData = _metaDataResolver.getDefaultMetaData(identifierBundle);
//    assertEquals("BLOOMBERG", metaData.getDataSource());
//    assertEquals("PX_LAST", metaData.getDataField());
//    assertEquals("CMPL", metaData.getDataProvider());
//    assertEquals("LONDON_CLOSE", metaData.getObservationTime());
//    
//    Set<String> expectedDataFields = Sets.newHashSet("PX_LAST", "VOLUME");
//    Set<String> expectedDataSources = Sets.newHashSet("BLOOMBERG", "REUTERS");
//    Set<String> expectedDataProviders = Sets.newHashSet("CMPL", "CMPN", "CMPT");
//    Collection<TimeSeriesMetaData> availableMetaData = _metaDataResolver.getAvailableMetaData(identifierBundle);
//    assertNotNull(availableMetaData);
//    assertTrue(availableMetaData.size() == 12);
//    for (TimeSeriesMetaData timeSeriesMetaData : availableMetaData) {
//      assertTrue(expectedDataFields.contains(timeSeriesMetaData.getDataField()));
//      assertTrue(expectedDataSources.contains(timeSeriesMetaData.getDataSource()));
//      String dataProvider = timeSeriesMetaData.getDataProvider();
//      assertTrue(expectedDataProviders.contains(dataProvider));
//      String expectedObservationTime = DefaultTimeSeriesResolver.NON_EXCHANGE_DATA_MAP.get(dataProvider);
//      assertEquals(expectedObservationTime, timeSeriesMetaData.getObservationTime());
//    }
//  }

  @Test
  public void testEquitySecurity() {
    EquitySecurity equitySecurity = TimeseriesMasterTestUtils.makeExpectedAAPLEquitySecurity();
    _mockSecuritySource.addSecurity(equitySecurity);
    IdentifierBundle identifierBundle = equitySecurity.getIdentifiers();
    s_logger.debug("sec exchange={} for ID={}", equitySecurity.getExchangeCode(), identifierBundle);
    TimeSeriesMetaData metaData = _metaDataResolver.getDefaultMetaData(identifierBundle);
    assertEquals("BLOOMBERG", metaData.getDataSource());
    assertEquals("PX_LAST", metaData.getDataField());
    String expectedDataProvider = DefaultTimeSeriesResolver.EXCH_PREFIX + equitySecurity.getExchangeCode();
    assertEquals(expectedDataProvider , metaData.getDataProvider());
    assertEquals("NEWYORK_CLOSE", metaData.getObservationTime());
    
//    Set<String> expectedDataFields = Sets.newHashSet("PX_LAST", "VOLUME");
//    Set<String> expectedDataSources = Sets.newHashSet("BLOOMBERG", "REUTERS");
//    Collection<TimeSeriesMetaData> availableMetaData = _metaDataResolver.getAvailableMetaData(identifierBundle);
//    assertNotNull(availableMetaData);
//    
//    assertTrue(availableMetaData.size() == 4);
//    for (TimeSeriesMetaData timeSeriesMetaData : availableMetaData) {
//      assertTrue(expectedDataFields.contains(timeSeriesMetaData.getDataField()));
//      assertTrue(expectedDataSources.contains(timeSeriesMetaData.getDataSource()));
//      assertEquals(expectedDataProvider , metaData.getDataProvider());
//      assertEquals("NEWYORK_CLOSE", metaData.getObservationTime());
//    }
  }

}
