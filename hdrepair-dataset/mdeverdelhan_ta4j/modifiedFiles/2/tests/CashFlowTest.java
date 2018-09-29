package eu.verdelhan.ta4j.analysis;

import eu.verdelhan.ta4j.Operation;
import eu.verdelhan.ta4j.OperationType;
import eu.verdelhan.ta4j.TATestsUtils;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.Trade;
import eu.verdelhan.ta4j.mocks.MockTick;
import eu.verdelhan.ta4j.mocks.MockTimeSeries;
import eu.verdelhan.ta4j.series.ConstrainedTimeSeries;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

public class CashFlowTest {

	@Test
	public void testCashFlowSize() {
		TimeSeries sampleTimeSeries = new MockTimeSeries(new double[] { 1d, 2d, 3d, 4d, 5d });
		CashFlow cashFlow = new CashFlow(sampleTimeSeries, new ArrayList<Trade>());
		assertThat(cashFlow.getSize()).isEqualTo(5);
	}

	@Test
	public void testCashFlowBuyWithOnlyOneTrade() {
		TimeSeries sampleTimeSeries = new MockTimeSeries(new double[] { 1d, 2d });

		List<Trade> trades = new ArrayList<Trade>();
		trades.add(new Trade(new Operation(0, OperationType.BUY), new Operation(1, OperationType.SELL)));

		CashFlow cashFlow = new CashFlow(sampleTimeSeries, trades);

		assertThat(cashFlow.getValue(0)).isEqualTo(1d);
		assertThat(cashFlow.getValue(1)).isEqualTo(2d);
	}

	@Test
	public void testCashFlowWithSellAndBuyOperations() {
		TimeSeries sampleTimeSeries = new MockTimeSeries(2, 1, 3, 5, 6, 3, 20);

		List<Trade> trades = new ArrayList<Trade>();
		trades.add(new Trade(new Operation(0, OperationType.BUY), new Operation(1, OperationType.SELL)));
		trades.add(new Trade(new Operation(3, OperationType.BUY), new Operation(4, OperationType.SELL)));
		trades.add(new Trade(new Operation(5, OperationType.SELL), new Operation(6, OperationType.BUY)));

		CashFlow cashFlow = new CashFlow(sampleTimeSeries, trades);

		assertThat(cashFlow.getValue(0)).isEqualTo(1d);
		assertThat(cashFlow.getValue(1)).isEqualTo(.5d);
		assertThat(cashFlow.getValue(2)).isEqualTo(.5d);
		assertThat(cashFlow.getValue(3)).isEqualTo(.5d);
		assertThat(cashFlow.getValue(4)).isEqualTo(.6d);
		assertThat(cashFlow.getValue(5)).isEqualTo(.6d);
		assertThat(cashFlow.getValue(6)).isEqualTo(.09d);
	}


	@Test
	public void testCashFlowSell() {
		TimeSeries sampleTimeSeries = new MockTimeSeries(1, 2, 4, 8, 16, 32);

		List<Trade> trades = new ArrayList<Trade>();
		trades.add(new Trade(new Operation(2, OperationType.SELL), new Operation(3, OperationType.BUY)));

		CashFlow cashFlow = new CashFlow(sampleTimeSeries, trades);

		assertThat(cashFlow.getValue(0)).isEqualTo(1d);
		assertThat(cashFlow.getValue(1)).isEqualTo(1d);
		assertThat(cashFlow.getValue(2)).isEqualTo(1d);
		assertThat(cashFlow.getValue(3)).isEqualTo(.5d);
		assertThat(cashFlow.getValue(4)).isEqualTo(.5d);
		assertThat(cashFlow.getValue(5)).isEqualTo(.5d);
	}

	@Test
	public void testCashFlowShortSell() {
		TimeSeries sampleTimeSeries = new MockTimeSeries(1, 2, 4, 8, 16, 32);

		List<Trade> trades = new ArrayList<Trade>();
		trades.add(new Trade(new Operation(0, OperationType.BUY), new Operation(2, OperationType.SELL)));
		trades.add(new Trade(new Operation(2, OperationType.SELL), new Operation(4, OperationType.BUY)));
		trades.add(new Trade(new Operation(4, OperationType.BUY), new Operation(5, OperationType.SELL)));

		CashFlow cashFlow = new CashFlow(sampleTimeSeries, trades);

		assertThat(cashFlow.getValue(0)).isEqualTo(1d);
		assertThat(cashFlow.getValue(1)).isEqualTo(2d);
		assertThat(cashFlow.getValue(2)).isEqualTo(4d);
		assertThat(cashFlow.getValue(3)).isEqualTo(2d);
		assertThat(cashFlow.getValue(4)).isEqualTo(1d);
		assertThat(cashFlow.getValue(5)).isEqualTo(2d);
	}

	@Test
	public void testCashFlowValueWithOnlyOneTradeAndAGapBefore() {
		TimeSeries sampleTimeSeries = new MockTimeSeries(new double[] { 1d, 1d, 2d });

		List<Trade> trades = new ArrayList<Trade>();
		trades.add(new Trade(new Operation(1, OperationType.BUY), new Operation(2, OperationType.SELL)));

		CashFlow cashFlow = new CashFlow(sampleTimeSeries, trades);

		assertThat(cashFlow.getValue(0)).isEqualTo(1d);
		assertThat(cashFlow.getValue(1)).isEqualTo(1d);
		assertThat(cashFlow.getValue(2)).isEqualTo(2d);
	}

	@Test
	public void testCashFlowValueWithOnlyOneTradeAndAGapAfter() {
		TimeSeries sampleTimeSeries = new MockTimeSeries(new double[] { 1d, 2d, 2d });

		List<Trade> trades = new ArrayList<Trade>();
		trades.add(new Trade(new Operation(0, OperationType.BUY), new Operation(1, OperationType.SELL)));

		CashFlow cashFlow = new CashFlow(sampleTimeSeries, trades);

		assertThat(cashFlow.getSize()).isEqualTo(3);
		assertThat(cashFlow.getValue(0)).isEqualTo(1d);
		assertThat(cashFlow.getValue(1)).isEqualTo(2d);
		assertThat(cashFlow.getValue(2)).isEqualTo(2d);
	}

	@Test
	public void testCashFlowValueWithTwoTradesAndLongTimeWithoutOperations() {
		TimeSeries sampleTimeSeries = new MockTimeSeries(new double[] { 1d, 2d, 4d, 8d, 16d, 32d });

		List<Trade> trades = new ArrayList<Trade>();
		trades.add(new Trade(new Operation(1, OperationType.BUY), new Operation(2, OperationType.SELL)));
		trades.add(new Trade(new Operation(4, OperationType.BUY), new Operation(5, OperationType.SELL)));

		CashFlow cashFlow = new CashFlow(sampleTimeSeries, trades);

		assertThat(cashFlow.getValue(0)).isEqualTo(1d);
		assertThat(cashFlow.getValue(1)).isEqualTo(1d);
		assertThat(cashFlow.getValue(2)).isEqualTo(2d);
		assertThat(cashFlow.getValue(3)).isEqualTo(2d);
		assertThat(cashFlow.getValue(4)).isEqualTo(2d);
		assertThat(cashFlow.getValue(5)).isEqualTo(4d);
		assertThat(cashFlow.getValue(5)).isEqualTo(4d);
	}

	@Test
	public void testCashFlowValue() {

		TimeSeries sampleTimeSeries = new MockTimeSeries(new double[] { 3d, 2d, 5d, 1000d, 5000d, 0.0001d, 4d, 7d,
				6d, 7d, 8d, 5d, 6d });

		List<Trade> trades = new ArrayList<Trade>();
		trades.add(new Trade(new Operation(0, OperationType.BUY), new Operation(2, OperationType.SELL)));
		trades.add(new Trade(new Operation(6, OperationType.BUY), new Operation(8, OperationType.SELL)));
		trades.add(new Trade(new Operation(9, OperationType.BUY), new Operation(11, OperationType.SELL)));

		CashFlow cashFlow = new CashFlow(sampleTimeSeries, trades);

		assertThat(cashFlow.getValue(0)).isEqualTo(1d);
		assertThat(cashFlow.getValue(1)).isEqualTo(2d / 3);
		assertThat(cashFlow.getValue(2)).isEqualTo(5d / 3);
		assertThat(cashFlow.getValue(3)).isEqualTo(5d / 3);
		assertThat(cashFlow.getValue(4)).isEqualTo(5d / 3);
		assertThat(cashFlow.getValue(5)).isEqualTo(5d / 3);
		assertThat(cashFlow.getValue(6)).isEqualTo(5d / 3);
		assertThat(cashFlow.getValue(7)).isEqualTo(5d / 3 * 7d / 4);
		assertThat(cashFlow.getValue(8)).isEqualTo(5d / 3 * 6d / 4);
		assertThat(cashFlow.getValue(9)).isEqualTo(5d / 3 * 6d / 4);
		assertThat(cashFlow.getValue(10)).isEqualTo(5d / 3 * 6d / 4 * 8d / 7, TATestsUtils.LONG_OFFSET);
		assertThat(cashFlow.getValue(11)).isEqualTo(5d / 3 * 6d / 4 * 5d / 7, TATestsUtils.LONG_OFFSET);
		assertThat(cashFlow.getValue(12)).isEqualTo(5d / 3 * 6d / 4 * 5d / 7, TATestsUtils.LONG_OFFSET);
	}

	@Test
	public void testCashFlowValueWithNoTrades() {
		TimeSeries sampleTimeSeries = new MockTimeSeries(new double[] { 3d, 2d, 5d, 4d, 7d, 6d, 7d, 8d, 5d, 6d });
		List<Trade> trades = new ArrayList<Trade>();

		CashFlow cashFlow = new CashFlow(sampleTimeSeries, trades);

		assertThat(cashFlow.getValue(4)).isEqualTo(1d);
		assertThat(cashFlow.getValue(7)).isEqualTo(1d);
		assertThat(cashFlow.getValue(9)).isEqualTo(1d);
	}

	@Test(expected = RuntimeException.class)
	public void testCashFlowWithIllegalArgument() {
		TimeSeries sampleTimeSeries = new MockTimeSeries(new double[] { 3d, 2d, 5d, 4d, 7d, 6d, 7d, 8d, 5d, 6d });
		List<Trade> trades = new ArrayList<Trade>();

		CashFlow cashFlow = new CashFlow(sampleTimeSeries, trades);

		cashFlow.getValue(10);
	}

	@Test
	public void testCashFlowWithConstrainedSeries() {
		MockTimeSeries series = new MockTimeSeries(new double[] { 5d, 6d, 3d, 7d, 8d, 6d, 10d, 15d, 6d });
		ConstrainedTimeSeries constrained = new ConstrainedTimeSeries(series, 4, 8);
		List<Trade> trades = new ArrayList<Trade>();
		trades.add(new Trade(new Operation(4, OperationType.BUY), new Operation(5, OperationType.SELL)));
		trades.add(new Trade(new Operation(6, OperationType.BUY), new Operation(8, OperationType.SELL)));
		CashFlow flow = new CashFlow(constrained, trades);
		assertThat(flow.getValue(0)).isEqualTo(1d);
		assertThat(flow.getValue(1)).isEqualTo(1d);
		assertThat(flow.getValue(2)).isEqualTo(1d);
		assertThat(flow.getValue(3)).isEqualTo(1d);
		assertThat(flow.getValue(4)).isEqualTo(1d);
		assertThat(flow.getValue(5)).isEqualTo(6d / 8);
		assertThat(flow.getValue(6)).isEqualTo(6d / 8);
		assertThat(flow.getValue(7)).isEqualTo(6d / 8 * 15d / 10);
		assertThat(flow.getValue(8)).isEqualTo(6d / 8 * 6d / 10, TATestsUtils.LONG_OFFSET);
	}

	@Test
	public void testReallyLongCashFlow() {
		int size = 1000000;
		TimeSeries sampleTimeSeries = new MockTimeSeries(Collections.nCopies(size, (Tick) new MockTick(10)));
		List<Trade> trades = new ArrayList<Trade>();
		trades.add(new Trade(new Operation(0, OperationType.BUY), new Operation(size - 1, OperationType.SELL)));
		CashFlow cashFlow = new CashFlow(sampleTimeSeries, trades);
		assertThat(cashFlow.getValue(size - 1)).isEqualTo(1d);
	}

}
