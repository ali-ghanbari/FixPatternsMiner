/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.joda.time.DateTime.now;
import static org.rhq.server.metrics.DateTimeService.ONE_MONTH;
import static org.rhq.server.metrics.DateTimeService.ONE_YEAR;
import static org.rhq.server.metrics.DateTimeService.SEVEN_DAYS;
import static org.rhq.server.metrics.DateTimeService.TWO_WEEKS;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.chrono.GregorianChronology;
import org.joda.time.field.DividedDateTimeField;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.cassandra.CassandraException;
import org.rhq.cassandra.ClusterInitService;
import org.rhq.cassandra.bundle.DeploymentOptions;
import org.rhq.cassandra.bundle.EmbeddedDeployer;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;

import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHost;
import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.cassandra.service.KeyIterator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

/**
 * @author John Sanda
 */
public class MetricsServerTest {

    private final long SECOND = 1000;

    private final long MINUTE = 60 * SECOND;

    private final String RAW_METRIC_DATA_CF = "raw_metrics";

    private final String ONE_HOUR_METRIC_DATA_CF = "one_hour_metric_data";

    private final String SIX_HOUR_METRIC_DATA_CF = "six_hour_metric_data";

    private final String TWENTY_FOUR_HOUR_METRIC_DATA_CF = "twenty_four_hour_metric_data";

    private final String METRICS_WORK_QUEUE_CF = "metrics_work_queue";

    private final String TRAITS_CF = "traits";

    private final String RESOURCE_TRAITS_CF = "resource_traits";

    private MetricsServerStub metricsServer;

    private Keyspace keyspace;

    private static class MetricsServerStub extends MetricsServer {
        private DateTime currentHour;

        public void setCurrentHour(DateTime currentHour) {
            this.currentHour = currentHour;
        }

        @Override
        protected DateTime getCurrentHour() {
            if (currentHour == null) {
                return super.getCurrentHour();
            }
            return currentHour;
        }
    }

    @BeforeClass
    public void deployCluster() throws CassandraException {
        File basedir = new File("target");
        File clusterDir = new File(basedir, "cassandra");
        int numNodes = 2;

        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setClusterDir(clusterDir.getAbsolutePath());
        deploymentOptions.setNumNodes(numNodes);
        deploymentOptions.setLoggingLevel("DEBUG");

        EmbeddedDeployer deployer = new EmbeddedDeployer();
        deployer.setDeploymentOptions(deploymentOptions);
        deployer.deploy();

        List<CassandraHost> hosts = asList(new CassandraHost("127.0.0.1", 9160), new CassandraHost("127.0.0.2", 9160));
        ClusterInitService initService = new ClusterInitService();

        initService.waitForClusterToStart(hosts);
        initService.waitForSchemaAgreement("rhq", hosts);
    }

    @BeforeMethod
    public void initServer() throws Exception {
        Cluster cluster = HFactory.getOrCreateCluster("rhq", "127.0.0.1:9160");
        keyspace = HFactory.createKeyspace("rhq", cluster);

        metricsServer = new MetricsServerStub();
        metricsServer.setCluster(cluster);
        metricsServer.setKeyspace(keyspace);
        metricsServer.setRawMetricsDataCF(RAW_METRIC_DATA_CF);
        metricsServer.setOneHourMetricsDataCF(ONE_HOUR_METRIC_DATA_CF);
        metricsServer.setSixHourMetricsDataCF(SIX_HOUR_METRIC_DATA_CF);
        metricsServer.setTwentyFourHourMetricsDataCF(TWENTY_FOUR_HOUR_METRIC_DATA_CF);
        metricsServer.setMetricsQueueCF(METRICS_WORK_QUEUE_CF);
        metricsServer.setTraitsCF(TRAITS_CF);
        metricsServer.setResourceTraitsCF(RESOURCE_TRAITS_CF);
        purgeDB();
    }

    private void purgeDB() {
        deleteAllRows(METRICS_WORK_QUEUE_CF, StringSerializer.get());
        deleteAllRows(RAW_METRIC_DATA_CF, IntegerSerializer.get());
        deleteAllRows(ONE_HOUR_METRIC_DATA_CF, IntegerSerializer.get());
        deleteAllRows(SIX_HOUR_METRIC_DATA_CF, IntegerSerializer.get());
        deleteAllRows(TWENTY_FOUR_HOUR_METRIC_DATA_CF, IntegerSerializer.get());
        deleteAllRows(TRAITS_CF, IntegerSerializer.get());
        deleteAllRows(RESOURCE_TRAITS_CF, IntegerSerializer.get());
    }

    private <K> MutationResult deleteAllRows(String columnFamily, Serializer<K> keySerializer) {
        KeyIterator<K> keyIterator = new KeyIterator<K>(keyspace, columnFamily, keySerializer);
        Mutator<K> rowMutator = HFactory.createMutator(keyspace, keySerializer);
        rowMutator.addDeletion(keyIterator, columnFamily);

        return rowMutator.execute();
    }

    @Test
    public void insertMultipleRawNumericDataForOneSchedule() {
        int scheduleId = 123;

        //DateTime hour0 = now.hourOfDay().roundFloorCopy().minusHours(now.hourOfDay().get());
        DateTime threeMinutesAgo = now().minusMinutes(3);
        DateTime twoMinutesAgo = now().minusMinutes(2);
        DateTime oneMinuteAgo = now().minusMinutes(1);

        int sevenDays = Duration.standardDays(7).toStandardSeconds().getSeconds();

        String scheduleName = getClass().getName() + "_SCHEDULE";
        long interval = MINUTE * 10;
        boolean enabled = true;
        DataType dataType = DataType.MEASUREMENT;
        MeasurementScheduleRequest request = new MeasurementScheduleRequest(scheduleId, scheduleName, interval,
            enabled, dataType);

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(threeMinutesAgo.getMillis(), request, 3.2));
        data.add(new MeasurementDataNumeric(twoMinutesAgo.getMillis(), request, 3.9));
        data.add(new MeasurementDataNumeric(oneMinuteAgo.getMillis(), request, 2.6));

        metricsServer.addNumericData(data);

        SliceQuery<Integer, Long, Double> query = HFactory.createSliceQuery(keyspace, IntegerSerializer.get(),
            LongSerializer.get(), DoubleSerializer.get());
        query.setColumnFamily(RAW_METRIC_DATA_CF);
        query.setKey(scheduleId);
        query.setRange(null, null, false, 10);

        QueryResult<ColumnSlice<Long, Double>> queryResult = query.execute();
        List<HColumn<Long, Double>> actual = queryResult.get().getColumns();

        List<HColumn<Long, Double>> expected = asList(
            HFactory.createColumn(threeMinutesAgo.getMillis(), 3.2, sevenDays, LongSerializer.get(),
                DoubleSerializer.get()),
            HFactory.createColumn(twoMinutesAgo.getMillis(), 3.9, sevenDays, LongSerializer.get(),
                DoubleSerializer.get()),
            HFactory.createColumn(oneMinuteAgo.getMillis(), 2.6, sevenDays, LongSerializer.get(),
                DoubleSerializer.get())
        );

        for (int i = 0; i < expected.size(); ++i) {
            assertPropertiesMatch("The returned columns do not match", expected.get(i), actual.get(i),
                "clock");
        }

        DateTime theHour = now().hourOfDay().roundFloorCopy();
        Composite expectedComposite = new Composite();
        expectedComposite.addComponent(theHour.getMillis(), LongSerializer.get());
        expectedComposite.addComponent(scheduleId, IntegerSerializer.get());

        assert1HourMetricsQueueEquals(asList(HFactory.createColumn(expectedComposite, 0, CompositeSerializer.get(),
            IntegerSerializer.get())));
    }

    @Test
    public void calculateAggregatesForOneScheduleWhenDBIsEmpty() {
        int scheduleId = 123;

        DateTime hour0 = now().hourOfDay().roundFloorCopy().minusHours(now().hourOfDay().get());
        DateTime hour6 = hour0.plusHours(6);
        DateTime lastHour = hour6.minusHours(1);
        DateTime firstMetricTime = hour6.minusMinutes(3);
        DateTime secondMetricTime = hour6.minusMinutes(2);
        DateTime thirdMetricTime = hour6.minusMinutes(1);

        String scheduleName = getClass().getName() + "_SCHEDULE";
        long interval = MINUTE * 15;
        boolean enabled = true;
        DataType dataType = DataType.MEASUREMENT;
        MeasurementScheduleRequest request = new MeasurementScheduleRequest(scheduleId, scheduleName, interval,
            enabled, dataType);

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(firstMetricTime.getMillis(), request, 3.2));
        data.add(new MeasurementDataNumeric(secondMetricTime.getMillis(), request, 3.9));
        data.add(new MeasurementDataNumeric(thirdMetricTime.getMillis(), request, 2.6));

        metricsServer.setCurrentHour(hour6);
        metricsServer.addNumericData(data);
        metricsServer.calculateAggregates();

        // verify one hour metric data is calculated
        // The ttl for 1 hour data is 14 days.
        int ttl = Days.days(14).toStandardSeconds().getSeconds();
        List<HColumn<Composite, Double>> expected1HourData = asList(
            HFactory.createColumn(createAggregateKey(lastHour, AggregateType.MAX), 3.9, ttl, CompositeSerializer.get(),
                DoubleSerializer.get()),
            HFactory.createColumn(createAggregateKey(lastHour, AggregateType.MIN), 2.6, ttl, CompositeSerializer.get(),
                DoubleSerializer.get()),
            HFactory.createColumn(createAggregateKey(lastHour, AggregateType.AVG), (3.9 + 3.2 + 2.6) / 3, ttl,
                CompositeSerializer.get(), DoubleSerializer.get())
        );

        assert1HourDataEquals(scheduleId, expected1HourData);

        // verify six hour metric data is calculated
        // the ttl for 6 hour data is 31 days
        ttl = Days.days(31).toStandardSeconds().getSeconds();
        List<HColumn<Composite, Double>> expected6HourData = asList(
            HFactory.createColumn(createAggregateKey(hour0, AggregateType.MAX), 3.9, ttl, CompositeSerializer.get(),
                DoubleSerializer.get()),
            HFactory.createColumn(createAggregateKey(hour0, AggregateType.MIN), 2.6, ttl, CompositeSerializer.get(),
                DoubleSerializer.get()),
            HFactory.createColumn(createAggregateKey(hour0, AggregateType.AVG), (3.9 + 3.2 + 2.6) / 3, ttl,
                CompositeSerializer.get(), DoubleSerializer.get())
        );

        assert6HourDataEquals(scheduleId, expected6HourData);
    }

    @Test
    public void aggregateRawDataDuring9thHour() {
        int scheduleId = 123;

        DateTime now = new DateTime();
        DateTime hour0 = now.hourOfDay().roundFloorCopy().minusHours(now.hourOfDay().get());
        DateTime hour9 = hour0.plusHours(9);
        DateTime hour8 = hour9.minusHours(1);

        DateTime firstMetricTime = hour8.plusMinutes(5);
        DateTime secondMetricTime = hour8.plusMinutes(10);
        DateTime thirdMetricTime = hour8.plusMinutes(15);

        double firstValue = 1.1;
        double secondValue = 2.2;
        double thirdValue = 3.3;

        // insert raw data to be aggregated
        Mutator<Integer> rawMetricsMutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
        rawMetricsMutator.addInsertion(scheduleId, RAW_METRIC_DATA_CF, createRawDataColumn(firstMetricTime,
            firstValue));
        rawMetricsMutator.addInsertion(scheduleId, RAW_METRIC_DATA_CF,
            createRawDataColumn(secondMetricTime, secondValue));
        rawMetricsMutator.addInsertion(scheduleId, RAW_METRIC_DATA_CF, createRawDataColumn(thirdMetricTime,
            thirdValue));

        rawMetricsMutator.execute();

        // update the one hour queue
        Mutator<String> queueMutator = HFactory.createMutator(keyspace, StringSerializer.get());
        Composite key = createQueueColumnName(hour8, scheduleId);
        HColumn<Composite, Integer> oneHourQueueColumn = HFactory.createColumn(key, 0, CompositeSerializer.get(),
            IntegerSerializer.get());
        queueMutator.addInsertion(ONE_HOUR_METRIC_DATA_CF, METRICS_WORK_QUEUE_CF, oneHourQueueColumn);

        queueMutator.execute();

        metricsServer.setCurrentHour(hour9);
        metricsServer.calculateAggregates();

        // verify that the 1 hour aggregates are calculated

        assert1HourDataEquals(scheduleId, asList(
            create1HourColumn(hour8, AggregateType.MAX, thirdValue),
            create1HourColumn(hour8, AggregateType.MIN, firstValue),
            create1HourColumn(hour8, AggregateType.AVG, (firstValue + secondValue + thirdValue) / 3)
        ));

        Chronology chronology = GregorianChronology.getInstance();
        DateTimeField hourField = chronology.hourOfDay();
        DividedDateTimeField dividedField = new DividedDateTimeField(hourField, DateTimeFieldType.clockhourOfDay(), 6);
        long timestamp = dividedField.roundFloor(hour9.getMillis());
        DateTime sixHourSlice = new DateTime(timestamp);

        // verify that the 6 hour queue is updated
        assert6HourMetricsQueueEquals(asList(HFactory.createColumn(createQueueColumnName(sixHourSlice, scheduleId), 0,
            CompositeSerializer.get(), IntegerSerializer.get())));

        // The 6 hour data should not get aggregated since the current 6 hour time slice
        // has not passed yet. More specifically, the aggregation job is running at 09:00
        // which means that the current 6 hour slice is from 06:00 to 12:00.
        assert6HourDataEmpty(scheduleId);

        // verify that the 24 hour queue is empty
        assert24HourMetricsQueueEmpty(scheduleId);

        // verify that the 1 hour queue has been purged
        assert1HourMetricsQueueEmpty(scheduleId);
    }

    @Test
    public void aggregate1HourDataDuring12thHour() {
        // set up the test fixture
        int scheduleId = 123;

        DateTime now = new DateTime();
        DateTime hour0 = now.hourOfDay().roundFloorCopy().minusHours(now.hourOfDay().get());
        DateTime hour12 = hour0.plusHours(12);
        DateTime hour6 = hour0.plusHours(6);
        DateTime hour7 = hour0.plusHours(7);
        DateTime hour8 = hour0.plusHours(8);

        double min1 = 1.1;
        double avg1 = 2.2;
        //double max1 = 3.3;
        double max1 = 9.9;

        double min2 = 4.4;
        double avg2 = 5.5;
        double max2 = 6.6;

        // insert one hour data to be aggregated
        Mutator<Integer> oneHourMutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
        oneHourMutator.addInsertion(scheduleId, ONE_HOUR_METRIC_DATA_CF, create1HourColumn(hour7, AggregateType.MAX,
            max1));
        oneHourMutator.addInsertion(scheduleId, ONE_HOUR_METRIC_DATA_CF, create1HourColumn(hour7, AggregateType.MIN,
            min1));
        oneHourMutator.addInsertion(scheduleId, ONE_HOUR_METRIC_DATA_CF, create1HourColumn(hour7, AggregateType.AVG,
            avg1));
        oneHourMutator.addInsertion(scheduleId, ONE_HOUR_METRIC_DATA_CF, create1HourColumn(hour8, AggregateType.MAX,
            max2));
        oneHourMutator.addInsertion(scheduleId, ONE_HOUR_METRIC_DATA_CF, create1HourColumn(hour8, AggregateType.MIN,
            min2));
        oneHourMutator.addInsertion(scheduleId, ONE_HOUR_METRIC_DATA_CF, create1HourColumn(hour8, AggregateType.AVG,
            avg2));
        oneHourMutator.execute();

        // update the 6 hour queue
        Mutator<String> queueMutator = HFactory.createMutator(keyspace, StringSerializer.get());
        Composite key = createQueueColumnName(hour6, scheduleId);
        HColumn<Composite, Integer> sixHourQueueColumn = HFactory.createColumn(key, 0, CompositeSerializer.get(),
            IntegerSerializer.get());
        queueMutator.addInsertion(SIX_HOUR_METRIC_DATA_CF, METRICS_WORK_QUEUE_CF, sixHourQueueColumn);

        queueMutator.execute();

        // execute the system under test
        metricsServer.setCurrentHour(hour12);
        metricsServer.calculateAggregates();

        // verify the results
        // verify that the one hour data has been aggregated
        assert6HourDataEquals(scheduleId, asList(
            create6HourColumn(hour6, AggregateType.MAX, max1),
            create6HourColumn(hour6, AggregateType.MIN, min1),
            create6HourColumn(hour6, AggregateType.AVG, (avg1 + avg2) / 2)
        ));

        // verify that the 6 hour queue has been updated
        assert6HourMetricsQueueEmpty(scheduleId);

        // verify that the 24 hour queue is updated
        assert24HourMetricsQueueEquals(asList(HFactory.createColumn(createQueueColumnName(hour0, scheduleId), 0,
            CompositeSerializer.get(), IntegerSerializer.get())));

        // verify that 6 hour data is not rolled up into the 24 hour bucket
        assert24HourDataEmpty(scheduleId);
    }

    @Test
    public void aggregate6HourDataDuring24thHour() {
        // set up the test fixture
        int scheduleId = 123;

        DateTime now = new DateTime();
        DateTime hour0 = now.hourOfDay().roundFloorCopy().minusHours(now.hourOfDay().get());
        DateTime hour12 = hour0.plusHours(12);
        DateTime hour6 = hour0.plusHours(6);
        DateTime hour24 = hour0.plusHours(24);

        double min1 = 1.1;
        double avg1 = 2.2;
        double max1 = 3.3;

        double min2 = 4.4;
        double avg2 = 5.5;
        double max2 = 6.6;

        // insert 6 hour data to be aggregated
        Mutator<Integer> sixHourMutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
        sixHourMutator.addInsertion(scheduleId, SIX_HOUR_METRIC_DATA_CF, create6HourColumn(hour6, AggregateType.MAX,
            max1));
        sixHourMutator.addInsertion(scheduleId, SIX_HOUR_METRIC_DATA_CF, create6HourColumn(hour6, AggregateType.MIN,
            min1));
        sixHourMutator.addInsertion(scheduleId, SIX_HOUR_METRIC_DATA_CF, create6HourColumn(hour6, AggregateType.AVG,
            avg1));
        sixHourMutator.addInsertion(scheduleId, SIX_HOUR_METRIC_DATA_CF, create6HourColumn(hour12, AggregateType.MAX,
            max2));
        sixHourMutator.addInsertion(scheduleId, SIX_HOUR_METRIC_DATA_CF, create6HourColumn(hour12, AggregateType.MIN,
            min2));
        sixHourMutator.addInsertion(scheduleId, SIX_HOUR_METRIC_DATA_CF, create6HourColumn(hour12, AggregateType.AVG,
            avg2));
        sixHourMutator.execute();

        // update the 24 queue
        Mutator<String> queueMutator = HFactory.createMutator(keyspace, StringSerializer.get());
        Composite key = createQueueColumnName(hour0, scheduleId);
        HColumn<Composite, Integer> twentyFourHourQueueColumn = HFactory.createColumn(key, 0, CompositeSerializer.get(),
            IntegerSerializer.get());
        queueMutator.addInsertion(TWENTY_FOUR_HOUR_METRIC_DATA_CF, METRICS_WORK_QUEUE_CF, twentyFourHourQueueColumn);

        queueMutator.execute();

        // execute the system under test
        metricsServer.setCurrentHour(hour24);
        metricsServer.calculateAggregates();

        // verify the results
        // verify that the 6 hour data is aggregated
        assert24HourDataEquals(scheduleId, asList(
            create24HourColumn(hour0, AggregateType.MAX, max2),
            create24HourColumn(hour0, AggregateType.MIN, min1),
            create24HourColumn(hour0, AggregateType.AVG, (avg1 + avg2) / 2)
        ));

        // verify that the 24 hour queue is updated
        assert24HourMetricsQueueEmpty(scheduleId);
    }

    private HColumn<Long, Double> createRawDataColumn(DateTime timestamp, double value) {
        return HFactory.createColumn(timestamp.getMillis(), value, SEVEN_DAYS, LongSerializer.get(),
            DoubleSerializer.get());
    }

    private Composite createQueueColumnName(DateTime dateTime, int scheduleId) {
        Composite composite = new Composite();
        composite.addComponent(dateTime.getMillis(), LongSerializer.get());
        composite.addComponent(scheduleId, IntegerSerializer.get());

        return composite;
    }

    private void assert1HourMetricsQueueEquals(List<HColumn<Composite, Integer>> expected) {
        assertMetricsQueueEquals(ONE_HOUR_METRIC_DATA_CF, expected);
    }

    private void assert6HourMetricsQueueEquals(List<HColumn<Composite, Integer>> expected) {
        assertMetricsQueueEquals(SIX_HOUR_METRIC_DATA_CF, expected);
    }

    private void assert24HourMetricsQueueEquals(List<HColumn<Composite, Integer>> expected) {
        assertMetricsQueueEquals(TWENTY_FOUR_HOUR_METRIC_DATA_CF, expected);
    }

    private void assertMetricsQueueEquals(String columnFamily, List<HColumn<Composite, Integer>> expected) {
        SliceQuery<String,Composite, Integer> sliceQuery = HFactory.createSliceQuery(keyspace, StringSerializer.get(),
            new CompositeSerializer().get(), IntegerSerializer.get());
        sliceQuery.setColumnFamily(METRICS_WORK_QUEUE_CF);
        sliceQuery.setKey(columnFamily);

        ColumnSliceIterator<String, Composite, Integer> iterator = new ColumnSliceIterator<String, Composite, Integer>(
            sliceQuery, (Composite) null, (Composite) null, false);

        List<HColumn<Composite, Integer>> actual = new ArrayList<HColumn<Composite, Integer>>();
        while (iterator.hasNext()) {
            actual.add(iterator.next());
        }

        assertEquals(actual.size(), expected.size(), "The number of entries in the queue do not match.");
        int i = 0;
        for (HColumn<Composite, Integer> expectedColumn :  expected) {
            HColumn<Composite, Integer> actualColumn = actual.get(i++);
            assertEquals(getTimestamp(actualColumn.getName()), getTimestamp(expectedColumn.getName()),
                "The timestamp does not match the expected value.");
            assertEquals(getScheduleId(actualColumn.getName()), getScheduleId(expectedColumn.getName()),
                "The schedule id does not match the expected value.");
        }
    }

    private void assert1HourDataEquals(int scheduleId, List<HColumn<Composite, Double>> expected) {
        assertMetricDataEquals(scheduleId, ONE_HOUR_METRIC_DATA_CF, expected);
    }

    private void assert6HourDataEquals(int scheduleId, List<HColumn<Composite, Double>> expected) {
        assertMetricDataEquals(scheduleId, SIX_HOUR_METRIC_DATA_CF, expected);
    }

    private void assert24HourDataEquals(int scheduleId, List<HColumn<Composite, Double>> expected) {
        assertMetricDataEquals(scheduleId, TWENTY_FOUR_HOUR_METRIC_DATA_CF, expected);
    }

    private void assertMetricDataEquals(int scheduleId, String columnFamily, List<HColumn<Composite,
        Double>> expected) {
        SliceQuery<Integer, Composite, Double> query = HFactory.createSliceQuery(keyspace, IntegerSerializer.get(),
            CompositeSerializer.get(), DoubleSerializer.get());
        query.setColumnFamily(columnFamily);
        query.setKey(scheduleId);

        ColumnSliceIterator<Integer, Composite, Double> iterator = new ColumnSliceIterator<Integer, Composite, Double>(
            query, (Composite) null, (Composite) null, false);

        List<HColumn<Composite, Double>> actual = new ArrayList<HColumn<Composite, Double>>();
        while (iterator.hasNext()) {
            actual.add(iterator.next());
        }

        String prefix;
        if (columnFamily.equals(ONE_HOUR_METRIC_DATA_CF)) {
            prefix = "The one hour data for schedule id " + scheduleId + " is wrong.";
        } else if (columnFamily.equals(SIX_HOUR_METRIC_DATA_CF)) {
            prefix = "The six hour data for schedule id " + scheduleId + " is wrong.";
        } else if (columnFamily.equals(TWENTY_FOUR_HOUR_METRIC_DATA_CF)) {
            prefix = "The twenty-four hour data for schedule id " + scheduleId + " is wrong.";
        } else {
            throw new IllegalArgumentException(columnFamily + " is not a recognized column family");
        }

        assertEquals(actual.size(), expected.size(), prefix + " The number of columns do not match.");
        int i = 0;
        for (HColumn<Composite, Double> expectedColumn : expected) {
            HColumn<Composite, Double> actualColumn = actual.get(i++);
            assertEquals(getTimestamp(actualColumn.getName()), getTimestamp(expectedColumn.getName()),
                prefix + " The timestamp does not match the expected value.");
            assertEquals(getAggregateType(actualColumn.getName()), getAggregateType(expectedColumn.getName()),
                prefix + " The column data type does not match the expected value");
            assertEquals(actualColumn.getValue(), expectedColumn.getValue(), "The column value is wrong");
            assertEquals(actualColumn.getTtl(), expectedColumn.getTtl(), "The ttl for the column is wrong.");
        }
    }

    private void assert6HourDataEmpty(int scheduleId) {
        assertMetricDataEmpty(scheduleId, SIX_HOUR_METRIC_DATA_CF);
    }

    private void assert24HourDataEmpty(int scheduleId) {
        assertMetricDataEmpty(scheduleId, TWENTY_FOUR_HOUR_METRIC_DATA_CF);
    }

    private void assertMetricDataEmpty(int scheduleId, String columnFamily) {
        SliceQuery<Integer, Composite, Double> query = HFactory.createSliceQuery(keyspace, IntegerSerializer.get(),
            CompositeSerializer.get(), DoubleSerializer.get());
        query.setColumnFamily(columnFamily);
        query.setKey(scheduleId);

        ColumnSliceIterator<Integer, Composite, Double> iterator = new ColumnSliceIterator<Integer, Composite, Double>(
            query, (Composite) null, (Composite) null, false);

        List<HColumn<Composite, Double>> actual = new ArrayList<HColumn<Composite, Double>>();
        while (iterator.hasNext()) {
            actual.add(iterator.next());
        }

        String prefix;
        if (columnFamily.equals(ONE_HOUR_METRIC_DATA_CF)) {
            prefix = "The one hour data for schedule id " + scheduleId + " is wrong.";
        } else if (columnFamily.equals(SIX_HOUR_METRIC_DATA_CF)) {
            prefix = "The six hour data for schedule id " + scheduleId + " is wrong.";
        } else if (columnFamily.equals(TWENTY_FOUR_HOUR_METRIC_DATA_CF)) {
            prefix = "The twenty-four hour data for schedule id " + scheduleId + " is wrong.";
        } else {
            throw new IllegalArgumentException(columnFamily + " is not a recognized column family");
        }

        assertEquals(actual.size(), 0, prefix + " Expected the row to be empty.");
    }

    private void assert1HourMetricsQueueEmpty(int scheduleId) {
        assertMetricsQueueEmpty(scheduleId, ONE_HOUR_METRIC_DATA_CF);
    }

    private void assert6HourMetricsQueueEmpty(int scheduleId) {
        assertMetricsQueueEmpty(scheduleId, SIX_HOUR_METRIC_DATA_CF);
    }

    private void assert24HourMetricsQueueEmpty(int scheduleId) {
        assertMetricsQueueEmpty(scheduleId, TWENTY_FOUR_HOUR_METRIC_DATA_CF);
    }

    private void assertMetricsQueueEmpty(int scheduleId, String columnFamily) {
        SliceQuery<String,Composite, Integer> sliceQuery = HFactory.createSliceQuery(keyspace, StringSerializer.get(),
            new CompositeSerializer().get(), IntegerSerializer.get());
        sliceQuery.setColumnFamily(METRICS_WORK_QUEUE_CF);
        sliceQuery.setKey(columnFamily);

        ColumnSliceIterator<String, Composite, Integer> iterator = new ColumnSliceIterator<String, Composite, Integer>(
            sliceQuery, (Composite) null, (Composite) null, false);

        List<HColumn<Composite, Integer>> actual = new ArrayList<HColumn<Composite, Integer>>();
        while (iterator.hasNext()) {
            actual.add(iterator.next());
        }

        String queueName;
        if (columnFamily.equals(ONE_HOUR_METRIC_DATA_CF)) {
            queueName = "1 hour";
        } else if (columnFamily.equals(SIX_HOUR_METRIC_DATA_CF)) {
            queueName = "6 hour";
        } else if (columnFamily.equals(TWENTY_FOUR_HOUR_METRIC_DATA_CF)) {
            queueName = "24 hour";
        } else {
            throw new IllegalArgumentException(columnFamily + " is not a recognized metric data column family.");
        }

        assertEquals(actual.size(), 0, "Expected the " + queueName + " queue to be empty for schedule id " +
            scheduleId);
    }

    private Integer getScheduleId(Composite composite) {
        return composite.get(1, IntegerSerializer.get());
    }

    private Long getTimestamp(Composite composite) {
        return composite.get(0, LongSerializer.get());
    }

    private AggregateType getAggregateType(Composite composite) {
        Integer type = composite.get(1, IntegerSerializer.get());
        return AggregateType.valueOf(type);
    }

    private HColumn<Composite, Double> create1HourColumn(DateTime dateTime, AggregateType type, double value) {
        return HFactory.createColumn(createAggregateKey(dateTime, type), value, TWO_WEEKS, CompositeSerializer.get(),
            DoubleSerializer.get());
    }

    private HColumn<Composite, Double> create6HourColumn(DateTime dateTime, AggregateType type, double value) {
        return HFactory.createColumn(createAggregateKey(dateTime, type), value, ONE_MONTH, CompositeSerializer.get(),
            DoubleSerializer.get());
    }

    private HColumn<Composite, Double> create24HourColumn(DateTime dateTime, AggregateType type, double value) {
        return HFactory.createColumn(createAggregateKey(dateTime, type), value, ONE_YEAR, CompositeSerializer.get(),
            DoubleSerializer.get());
    }

    private Composite createAggregateKey(DateTime dateTime, AggregateType type) {
        Composite composite = new Composite();
        composite.addComponent(dateTime.getMillis(), LongSerializer.get());
        composite.addComponent(type.ordinal(), IntegerSerializer.get());

        return composite;
    }
}
