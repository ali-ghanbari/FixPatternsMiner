package org.multibit.hd.core.network;

import com.google.bitcoin.core.*;
import com.google.common.base.Optional;
import org.multibit.hd.core.api.BitcoinNetworkSummary;
import org.multibit.hd.core.api.WalletData;
import org.multibit.hd.core.events.CoreEvents;
import org.multibit.hd.core.events.TransactionSeenEvent;
import org.multibit.hd.core.managers.WalletManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MultiBitPeerEventListener implements PeerEventListener {

  private static final Logger log = LoggerFactory.getLogger(MultiBitPeerEventListener.class);
  private int startingBlock = -1;
  private int downloadPercent = 0;
  private int numberOfConnectedPeers;

  public MultiBitPeerEventListener() {
    numberOfConnectedPeers = 0;
  }

  @Override
  public void onBlocksDownloaded(Peer peer, Block block, int blocksLeft) {

    log.trace("Number of blocks left = {}", blocksLeft);

    // Keep track of the download progress
    updateDownloadPercent(blocksLeft);

    // Keep the progress updated
    CoreEvents.fireBitcoinNetworkChangedEvent(BitcoinNetworkSummary.newChainDownloadProgress(downloadPercent));
  }

  @Override
  public void onChainDownloadStarted(Peer peer, int blocksLeft) {

    log.debug("Chain download started with number of blocks left = {}", blocksLeft);

    // Reset the starting block
    startingBlock = -1;

    // Keep track of the download progress
    updateDownloadPercent(blocksLeft);

    // Keep the progress updated
    CoreEvents.fireBitcoinNetworkChangedEvent(BitcoinNetworkSummary.newChainDownloadProgress(downloadPercent));
  }

  @Override
  public void onPeerConnected(Peer peer, int peerCount) {

    numberOfConnectedPeers = peerCount;

    // Only show peers after synchronization to avoid confusion
    if (downloadPercent == 100) {
      CoreEvents.fireBitcoinNetworkChangedEvent(
        BitcoinNetworkSummary.newNetworkReady(numberOfConnectedPeers)
      );
    }
  }

  @Override
  public void onPeerDisconnected(Peer peer, int peerCount) {

    numberOfConnectedPeers = peerCount;

    // Only show peers after synchronization to avoid confusion
    if (downloadPercent == 100) {
      CoreEvents.fireBitcoinNetworkChangedEvent(
        BitcoinNetworkSummary.newNetworkReady(numberOfConnectedPeers)
      );
    }
  }

  @Override
  public Message onPreMessageReceived(Peer peer, Message message) {
    return message;
  }

  @Override
  public void onTransaction(Peer peer, Transaction transaction) {

    // Loop through all the wallets, seeing if the transaction is relevant and adding them as pending if so.
    if (transaction != null) {
      // TODO - want to iterate over all open wallets
      Optional<WalletData> currentWalletData = WalletManager.INSTANCE.getCurrentWalletData();
      if (currentWalletData.isPresent()) {
        if (currentWalletData.get() != null) {
          Wallet currentWallet = currentWalletData.get().getWallet();
          if (currentWallet != null) {
            try {
              if (currentWallet.isTransactionRelevant(transaction)) {
                if (!(transaction.isTimeLocked() && transaction.getConfidence().getSource() != TransactionConfidence.Source.SELF)) {
                  if (currentWallet.getTransaction(transaction.getHash()) == null) {
                    log.debug("MultiBitHD adding a new pending transaction for the wallet '"
                      + currentWalletData.get().getWalletId() + "'\n" + transaction.toString());
                    // The perWalletModelData is marked as dirty.
                    // TODO - mark wallet as dirty ?
                    currentWallet.receivePending(transaction, null);

                    // Emit an event so that GUI elements can update as required
                    CoreEvents.fireTransactionSeenEvent(new TransactionSeenEvent(transaction));
                  }
                }
              }
            } catch (ScriptException se) {
              // Cannot understand this transaction - carry on
            }
          }
        }
      }

    }
  }

  @Override
  public List<Message> getData(Peer peer, GetDataMessage m) {
    return null;
  }

  public int getNumberOfConnectedPeers() {
    return numberOfConnectedPeers;
  }

  /**
   * <p>Calculate an appropriate download percent</p>
   *
   * @param blocksLeft The number of blocks left to download
   */
  private void updateDownloadPercent(int blocksLeft) {

    if (startingBlock == -1) {
      startingBlock = blocksLeft;
    }

    downloadPercent = (int) ((1 - ((double) blocksLeft / startingBlock)) * 100);

  }
}

