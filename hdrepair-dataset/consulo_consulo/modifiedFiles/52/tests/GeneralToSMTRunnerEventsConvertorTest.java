package org.jetbrains.plugins.ruby.testing.sm.runner;

import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.plugins.ruby.Marker;
import org.jetbrains.plugins.ruby.testing.sm.runner.ui.SMTRunnerConsoleView;
import org.jetbrains.plugins.ruby.testing.sm.runner.ui.SMTRunnerTestTreeView;
import org.jetbrains.plugins.ruby.testing.sm.runner.ui.SMTestRunnerResultsForm;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import java.util.List;

/**
 * @author Roman Chernyatchik
 */
public class GeneralToSMTRunnerEventsConvertorTest extends BaseSMTRunnerTestCase {
  private SMTRunnerConsoleView myConsole;
  private GeneralToSMTRunnerEventsConvertor myEventsProcessor;
  private TreeModel myTreeModel;
  private SMTestRunnerResultsForm myResultsViewer;
  private TestConsoleProperties myConsoleProperties;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myConsoleProperties = createConsoleProperties();
    TestConsoleProperties.HIDE_PASSED_TESTS.set(myConsoleProperties, false);
    TestConsoleProperties.OPEN_FAILURE_LINE.set(myConsoleProperties, false);
    TestConsoleProperties.SCROLL_TO_SOURCE.set(myConsoleProperties, false);
    TestConsoleProperties.SELECT_FIRST_DEFECT.set(myConsoleProperties, false);
    TestConsoleProperties.TRACK_RUNNING_TEST.set(myConsoleProperties, false);

    myResultsViewer = (SMTestRunnerResultsForm)createResultsViewer(myConsoleProperties);

    myConsole = new SMTRunnerConsoleView(myConsoleProperties, myResultsViewer);
    myEventsProcessor = new GeneralToSMTRunnerEventsConvertor(myResultsViewer.getTestsRootNode());
    myEventsProcessor.addEventsListener(myResultsViewer);
    myTreeModel = myResultsViewer.getTreeView().getModel();

    myEventsProcessor.onStartTesting();
  }

  @Override
  protected void tearDown() throws Exception {
    Disposer.dispose(myEventsProcessor);
    Disposer.dispose(myConsole);

    super.tearDown();
  }

  public void testOnStartedTesting() {
    final Object rootTreeNode = myTreeModel.getRoot();
    assertEquals(0, myTreeModel.getChildCount(rootTreeNode));

    final SMTRunnerNodeDescriptor nodeDescriptor =
        (SMTRunnerNodeDescriptor)((DefaultMutableTreeNode)rootTreeNode).getUserObject();
    assertFalse(nodeDescriptor.expandOnDoubleClick());

    final SMTestProxy rootProxy = SMTRunnerTestTreeView.getTestProxyFor(rootTreeNode);
    assertNotNull(rootProxy);

    assertTrue(rootProxy.wasLaunched());
    assertTrue(rootProxy.isInProgress());
    assertTrue(rootProxy.isLeaf());

    assertEquals("[root]", rootTreeNode.toString());
  }

  public void testOnTestStarted() throws InterruptedException {
    onTestStarted("some_test");
    final String fullName = myEventsProcessor.getFullTestName("some_test");
    final SMTestProxy proxy = myEventsProcessor.getProxyByFullTestName(fullName);

    assertNotNull(proxy);
    assertTrue(proxy.isInProgress());

    final Object rootTreeNode = (myTreeModel.getRoot());
    assertEquals(1, myTreeModel.getChildCount(rootTreeNode));
    final SMTestProxy rootProxy = SMTRunnerTestTreeView.getTestProxyFor(rootTreeNode);
    assertNotNull(rootProxy);
    assertSameElements(rootProxy.getChildren(), proxy);


    onTestStarted("some_test2");
    final String fullName2 = myEventsProcessor.getFullTestName("some_test2");
    final SMTestProxy proxy2 = myEventsProcessor.getProxyByFullTestName(fullName2);
    assertSameElements(rootProxy.getChildren(), proxy, proxy2);
  }

  public void testOnTestStarted_Twice() {
    onTestStarted("some_test");
    onTestStarted("some_test");

    assertEquals(1, myEventsProcessor.getRunningTestsQuantity());
  }

  public void testOnTestFailure() {
    onTestStarted("some_test");
    myEventsProcessor.onTestFailure("some_test", "", "", false);

    final String fullName = myEventsProcessor.getFullTestName("some_test");
    final SMTestProxy proxy = myEventsProcessor.getProxyByFullTestName(fullName);

    assertTrue(proxy.isDefect());
    assertFalse(proxy.isInProgress());
  }

  public void testOnTestFailure_Twice() {
    onTestStarted("some_test");
    myEventsProcessor.onTestFailure("some_test", "", "", false);
    myEventsProcessor.onTestFailure("some_test", "", "", false);

    assertEquals(1, myEventsProcessor.getRunningTestsQuantity());
    assertEquals(1, myEventsProcessor.getFailedTestsSet().size());
  }

   public void testOnTestError() {
    onTestStarted("some_test");
    myEventsProcessor.onTestFailure("some_test", "", "", true);

    final String fullName = myEventsProcessor.getFullTestName("some_test");
    final SMTestProxy proxy = myEventsProcessor.getProxyByFullTestName(fullName);

    assertTrue(proxy.isDefect());
    assertFalse(proxy.isInProgress());
  }

  public void testOnTestIgnored() {
    onTestStarted("some_test");
    myEventsProcessor.onTestIgnored("some_test", "");

    final String fullName = myEventsProcessor.getFullTestName("some_test");
    final SMTestProxy proxy = myEventsProcessor.getProxyByFullTestName(fullName);

    assertTrue(proxy.isDefect());
    assertFalse(proxy.isInProgress());
  }

  public void testOnTestFinished() {
    onTestStarted("some_test");
    final String fullName = myEventsProcessor.getFullTestName("some_test");
    final SMTestProxy proxy = myEventsProcessor.getProxyByFullTestName(fullName);
    myEventsProcessor.onTestFinished("some_test", 10);

    assertEquals(0, myEventsProcessor.getRunningTestsQuantity());
    assertEquals(0, myEventsProcessor.getFailedTestsSet().size());


    assertFalse(proxy.isDefect());
    assertFalse(proxy.isInProgress());

    //Tree
    final Object rootTreeNode = myTreeModel.getRoot();
    assertEquals(1, myTreeModel.getChildCount(rootTreeNode));
    final SMTestProxy rootProxy = SMTRunnerTestTreeView.getTestProxyFor(rootTreeNode);
    assertNotNull(rootProxy);
    assertSameElements(rootProxy.getChildren(), proxy);
  }

  //TODO[romeo] catch assertion
  //public void testFinished_Twice() {
  //  myEventsProcessor.onTestStarted("some_test");
  //  myEventsProcessor.onTestFinished("some_test");
  //  myEventsProcessor.onTestFinished("some_test");
  //
  //  assertEquals(1, myEventsProcessor.getTestsCurrentCount());
  //  assertEquals(0, myEventsProcessor.getRunningTestsFullNameToProxy().size());
  //  assertEquals(0, myEventsProcessor.getFailedTestsSet().size());
  //
  //}

  public void testOnTestFinished_EmptySuite() {
    myEventsProcessor.onFinishTesting();

    //Tree
    final Object rootTreeNode = myTreeModel.getRoot();
    assertEquals(0, myTreeModel.getChildCount(rootTreeNode));
    final SMTestProxy rootProxy = SMTRunnerTestTreeView.getTestProxyFor(rootTreeNode);
    assertNotNull(rootProxy);

    assertFalse(rootProxy.isInProgress());
    assertFalse(rootProxy.isDefect());
  }

  public void testOnFinishedTesting_WithFailure() {
    onTestStarted("test");
    myEventsProcessor.onTestFailure("test", "", "", false);
    myEventsProcessor.onTestFinished("test", 10);
    myEventsProcessor.onFinishTesting();

    //Tree
    final Object rootTreeNode = myTreeModel.getRoot();
    assertEquals(1, myTreeModel.getChildCount(rootTreeNode));
    final SMTestProxy rootProxy = SMTRunnerTestTreeView.getTestProxyFor(rootTreeNode);
    assertNotNull(rootProxy);

    assertFalse(rootProxy.isInProgress());
    assertTrue(rootProxy.isDefect());
  }

  public void testOnFinishedTesting_WithError() {
    onTestStarted("test");
    myEventsProcessor.onTestFailure("test", "", "", true);
    myEventsProcessor.onTestFinished("test", 10);
    myEventsProcessor.onFinishTesting();

    //Tree
    final Object rootTreeNode = myTreeModel.getRoot();
    assertEquals(1, myTreeModel.getChildCount(rootTreeNode));
    final SMTestProxy rootProxy = SMTRunnerTestTreeView.getTestProxyFor(rootTreeNode);
    assertNotNull(rootProxy);

    assertFalse(rootProxy.isInProgress());
    assertTrue(rootProxy.isDefect());
  }

  public void testOnFinishedTesting_WithIgnored() {
    onTestStarted("test");
    myEventsProcessor.onTestIgnored("test", "");
    myEventsProcessor.onTestFinished("test", 10);
    myEventsProcessor.onFinishTesting();

    //Tree
    final Object rootTreeNode = myTreeModel.getRoot();
    assertEquals(1, myTreeModel.getChildCount(rootTreeNode));
    final SMTestProxy rootProxy = SMTRunnerTestTreeView.getTestProxyFor(rootTreeNode);
    assertNotNull(rootProxy);

    assertFalse(rootProxy.isInProgress());
    assertTrue(rootProxy.isDefect());
  }

  public void testOnFinishedTesting_twice() {
    myEventsProcessor.onFinishTesting();

    final Marker finishedMarker = new Marker();
    myEventsProcessor.addEventsListener(new SMTRunnerEventsAdapter(){
      @Override
      public void onTestingFinished() {
        finishedMarker.set();
      }
    });
    myEventsProcessor.onFinishTesting();
    assertFalse(finishedMarker.isSet());
  }

  public void testOnSuiteStarted() {
    onTestSuiteStarted("suite1");

    //lets check that new tests have right parent
    onTestStarted("test1");
    final SMTestProxy test1 =
        myEventsProcessor.getProxyByFullTestName(myEventsProcessor.getFullTestName("test1"));
    assertEquals("suite1", test1.getParent().getName());

    //lets check that new suits have righ parent
    onTestSuiteStarted("suite2");
    onTestSuiteStarted("suite3");
    onTestStarted("test2");
    final SMTestProxy test2 =
        myEventsProcessor.getProxyByFullTestName(myEventsProcessor.getFullTestName("test2"));
    assertEquals("suite3", test2.getParent().getName());
    assertEquals("suite2", test2.getParent().getParent().getName());

    myEventsProcessor.onTestFinished("test2", 10);

    //check that after finishing suite (suite3), current will be parent of finished suite (i.e. suite2)
    myEventsProcessor.onSuiteFinished("suite3");
    onTestStarted("test3");
    final SMTestProxy test3 =
        myEventsProcessor.getProxyByFullTestName(myEventsProcessor.getFullTestName("test3"));
    assertEquals("suite2", test3.getParent().getName());

    //clean up
    myEventsProcessor.onSuiteFinished("suite2");
    myEventsProcessor.onSuiteFinished("suite1");
  }

  public void testGetCurrentTestSuite() {
    assertEquals(myResultsViewer.getTestsRootNode(), myEventsProcessor.getCurrentSuite());

    onTestSuiteStarted("my_suite");
    assertEquals("my_suite", myEventsProcessor.getCurrentSuite().getName());
  }

  public void testConcurrentSuite_intersected() {
    myEventsProcessor.onSuiteStarted("suite1");
    myEventsProcessor.onTestStarted("suite2.test1");

    final SMTestProxy test1 =
        myEventsProcessor.getProxyByFullTestName(myEventsProcessor.getFullTestName("suite2.test1"));

    myEventsProcessor.onSuiteFinished("suite1");

    myEventsProcessor.onSuiteStarted("suite2");
    myEventsProcessor.onTestFinished("suite2.test1", 10);
    myEventsProcessor.onSuiteFinished("suite2");

    assertEquals("suite1", test1.getParent().getName());

    final List<? extends SMTestProxy> children =
        myResultsViewer.getTestsRootNode().getChildren();
    assertEquals(2, children.size());
    assertEquals("suite1", children.get(0).getName());
    assertEquals(1, children.get(0).getChildren().size());
    assertEquals("suite2", children.get(1).getName());
    assertEquals(0, children.get(1).getChildren().size());
  }

  public void testRuby_1767() throws InterruptedException {
    TestConsoleProperties.HIDE_PASSED_TESTS.set(myConsoleProperties, true);

    myEventsProcessor.onSuiteStarted("suite");
    myResultsViewer.performUpdate();

    myEventsProcessor.onTestStarted("test_failed");
    myResultsViewer.performUpdate();
    myEventsProcessor.onTestFailure("test_failed", "", "", false);
    myResultsViewer.performUpdate();
    myEventsProcessor.onTestFinished("test_failed", 10);
    myResultsViewer.performUpdate();

    myEventsProcessor.onTestStarted("test");
    myResultsViewer.performUpdate();
    assertEquals(2, myTreeModel.getChildCount(myTreeModel.getChild(myTreeModel.getRoot(), 0)));

    myEventsProcessor.onTestFinished("test", 10);
    assertEquals(2, myTreeModel.getChildCount(myTreeModel.getChild(myTreeModel.getRoot(), 0)));

    myEventsProcessor.onSuiteFinished("suite");
    myEventsProcessor.onFinishTesting();

    assertEquals(1, myTreeModel.getChildCount(myTreeModel.getChild(myTreeModel.getRoot(), 0)));
  }


  private void onTestStarted(final String testName) {
    myEventsProcessor.onTestStarted(testName);
    myResultsViewer.performUpdate();
  }

  private void onTestSuiteStarted(final String suiteName) {
    myEventsProcessor.onSuiteStarted(suiteName);
    myResultsViewer.performUpdate();
  }
}
