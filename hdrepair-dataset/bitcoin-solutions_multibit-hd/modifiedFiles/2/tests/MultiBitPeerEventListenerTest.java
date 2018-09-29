package org.multibit.hd.core.network;

import com.google.common.eventbus.Subscribe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multibit.hd.core.events.BitcoinNetworkChangedEvent;
import org.multibit.hd.core.services.CoreServices;

import static org.fest.assertions.api.Assertions.assertThat;

public class MultiBitPeerEventListenerTest {
  boolean test = false;

  @Before
  public void setUp() {

    CoreServices.uiEventBus.register(this);
    test =false;

  }

  @After
  public void tearDown() {

    CoreServices.uiEventBus.unregister(this);
  }

  @Test
  public void testDownloadEvents() throws Exception {

    MultiBitPeerEventListener testObject = new MultiBitPeerEventListener();

    test = false;
    // Set up the initial block count
    testObject.onChainDownloadStarted(null, 200);

    test = true;

    // Simulate blocks left of 150/200 (expect 25% event to be emitted)
    testObject.onBlocksDownloaded(null, null, 150);

  }

  @Subscribe
  public void onBitcoinNetworkChangeEvent(BitcoinNetworkChangedEvent event) {

    // Progress 25%
    if (test) {
      assertThat(event.getSummary().getPercent()).isEqualTo(25);
    }
  }

}
