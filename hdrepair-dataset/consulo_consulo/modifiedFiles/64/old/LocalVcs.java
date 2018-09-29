package com.intellij.history.core;

import com.intellij.history.Clock;
import com.intellij.history.FileRevisionTimestampComparator;
import com.intellij.history.core.changes.*;
import com.intellij.history.core.revisions.RecentChange;
import com.intellij.history.core.revisions.Revision;
import com.intellij.history.core.revisions.RevisionAfterChange;
import com.intellij.history.core.revisions.RevisionBeforeChange;
import com.intellij.history.core.storage.Content;
import com.intellij.history.core.storage.Storage;
import com.intellij.history.core.tree.Entry;
import com.intellij.history.core.tree.RootEntry;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocalVcs implements ILocalVcs {
  protected Storage myStorage;

  private ChangeList myChangeList;
  private Entry myRoot;
  private int myEntryCounter;

  private Change myLastChange;
  private boolean wasModifiedAfterLastSave;
  private int myChangeSetDepth;

  public LocalVcs(Storage s) {
    myStorage = s;
    load();
  }

  private void load() {
    Memento m = myStorage.load();

    myRoot = m.myRoot;
    myEntryCounter = m.myEntryCounter;
    myChangeList = m.myChangeList;
  }

  public void save() {
    if (!wasModifiedAfterLastSave) return;

    Memento m = new Memento();
    m.myRoot = myRoot;
    m.myEntryCounter = myEntryCounter;
    m.myChangeList = myChangeList;

    myStorage.store(m);
    myStorage.save();

    wasModifiedAfterLastSave = false;
  }

  public void purgeObsoleteAndSave(long period) {
    purgeObsolete(period);
    save();
  }

  private void purgeObsolete(long period) {
    List<Content> contentsToPurge = myChangeList.purgeObsolete(period);
    myStorage.purgeContents(contentsToPurge);
  }

  public boolean hasEntry(String path) {
    return myRoot.hasEntry(path);
  }

  public Entry getEntry(String path) {
    return myRoot.getEntry(path);
  }

  public Entry findEntry(String path) {
    return myRoot.findEntry(path);
  }

  public List<Entry> getRoots() {
    return myRoot.getChildren();
  }

  public void beginChangeSet() {
    myChangeList.beginChangeSet();
    myChangeSetDepth++;
  }

  public void endChangeSet(String name) {
    myChangeList.endChangeSet(name);

    // we must call Storage.save to make it flush all the changes made during changesed.
    // otherwise the ContentStorage may become corrupted if IDEA is forced to shutdown.
    myChangeSetDepth--;
    if (myChangeSetDepth == 0) {
      myStorage.save();
    }
  }

  public void createFile(String path, ContentFactory f, long timestamp, boolean isReadOnly) {
    doCreateFile(getNextId(), path, f, timestamp, isReadOnly);
  }

  protected Content createContentFrom(ContentFactory f) {
    return f.createContent(myStorage);
  }

  public void createDirectory(String path) {
    doCreateDirectory(getNextId(), path);
  }

  private int getNextId() {
    return myEntryCounter++;
  }

  public void restoreFile(int id, String path, ContentFactory f, long timestamp, boolean isReadOnly) {
    doCreateFile(id, path, f, timestamp, isReadOnly);
  }

  private void doCreateFile(int id, String path, ContentFactory f, long timestamp, boolean isReadOnly) {
    Content c = createContentFrom(f);
    applyChange(new CreateFileChange(id, path, c, timestamp, isReadOnly));
  }

  public void restoreDirectory(int id, String path) {
    doCreateDirectory(id, path);
  }

  private void doCreateDirectory(int id, String path) {
    applyChange(new CreateDirectoryChange(id, path));
  }

  public void changeFileContent(String path, ContentFactory f, long timestamp) {
    if (contentWasNotChanged(path, f)) return;
    Content c = createContentFrom(f);
    applyChange(new ContentChange(path, c, timestamp));
  }

  protected boolean contentWasNotChanged(String path, ContentFactory f) {
    return f.equalsTo(getEntry(path).getContent());
  }

  public void rename(String path, String newName) {
    applyChange(new RenameChange(path, newName));
  }

  public void changeROStatus(String path, boolean isReadOnly) {
    applyChange(new ROStatusChange(path, isReadOnly));
  }

  public void move(String path, String newParentPath) {
    applyChange(new MoveChange(path, newParentPath));
  }

  public void delete(String path) {
    applyChange(new DeleteChange(path));
  }

  public void putSystemLabel(String name, int color) {
    applyLabel(new PutSystemLabelChange(name, color, getCurrentTimestamp()));
  }

  public void putUserLabel(String name) {
    applyLabel(new PutLabelChange(name, getCurrentTimestamp()));
  }

  public void putUserLabel(String path, String name) {
    applyLabel(new PutEntryLabelChange(path, name, getCurrentTimestamp()));
  }

  private void applyLabel(PutLabelChange c) {
    c.applyTo(myRoot);
    addChangeToChangeList(c);
  }

  private void applyChange(Change c) {
    c.applyTo(myRoot);

    // todo get rid of wrapping changeset here
    beginChangeSet();
    addChangeToChangeList(c);
    endChangeSet(null);

    myLastChange = c;
  }

  private void addChangeToChangeList(Change c) {
    myChangeList.addChange(c);
    wasModifiedAfterLastSave = true;
  }

  @TestOnly
  public ChangeList getChangeList() {
    return myChangeList;
  }

  public Change getLastChange() {
    return myLastChange;
  }

  public boolean isBefore(Change before, Change after, boolean canBeEqual) {
    return myChangeList.isBefore(before, after, canBeEqual);
  }

  public List<Change> getChain(Change initialChange) {
    return myChangeList.getChain(initialChange);
  }

  public List<Revision> getRevisionsFor(String path) {
    return new RevisionsCollector(this, path, myRoot, myChangeList).getResult();
  }

  public byte[] getByteContent(String path, FileRevisionTimestampComparator c) {
    return new ByteContentRetriever(this, path, c).getResult();
  }

  public List<RecentChange> getRecentChanges() {
    List<RecentChange> result = new ArrayList<RecentChange>();

    List<Change> cc = myChangeList.getChanges();

    for (int i = 0; i < cc.size() && result.size() < 20; i++) {
      Change c = cc.get(i);
      if (c.isFileContentChange()) continue;
      if (c.isLabel()) continue;
      if (c.getName() == null) continue;

      Revision before = new RevisionBeforeChange(myRoot, myRoot, myChangeList, c);
      Revision after = new RevisionAfterChange(myRoot, myRoot, myChangeList, c);
      result.add(new RecentChange(before, after));
    }

    return result;
  }

  public void accept(ChangeVisitor v) throws IOException {
    myChangeList.accept(myRoot, v);
  }

  protected long getCurrentTimestamp() {
    return Clock.getCurrentTimestamp();
  }

  public static class Memento {
    public Entry myRoot = new RootEntry();
    public int myEntryCounter = 0;
    public ChangeList myChangeList = new ChangeList();
  }
}
