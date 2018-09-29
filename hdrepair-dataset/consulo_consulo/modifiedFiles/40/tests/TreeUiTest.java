package com.intellij.ide.util.treeView;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.util.Time;
import com.intellij.util.WaitFor;
import com.intellij.util.ui.UIUtil;
import junit.framework.TestSuite;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class TreeUiTest extends AbstractTreeBuilderTest {

  public TreeUiTest(boolean passthrougth) {
    super(passthrougth);
  }

  public TreeUiTest(boolean yieldingUiBuild, boolean bgStructureBuilding) {
    super(yieldingUiBuild, bgStructureBuilding);
  }

  public void testEmptyInvisibleRoot() throws Exception {
    myTree.setRootVisible(false);
    showTree();
    assertTree("+/\n");

    updateFromRoot();
    assertTree("+/\n");


    buildNode("/", false);
    assertTree("+/\n");

    myTree.setRootVisible(true);
    buildNode("/", false);
    assertTree("/\n");
  }

  public void testVisibleRoot() throws Exception {
    myTree.setRootVisible(true);
    buildStructure(myRoot);
    assertTree("+/\n");

    updateFromRoot();
    assertTree("+/\n");
  }

  public void testInvisibleRoot() throws Exception {
    myTree.setRootVisible(false);
    buildStructure(myRoot);
    assertTree("-/\n"
               + " +com\n"
               + " +jetbrains\n"
               + " +org\n"
               + " +xunit\n");

    collapsePath(new TreePath(myTreeModel.getRoot()));
    assertTree("+/\n");

    updateFromRoot();
    assertTree("-/\n"
               + " +com\n"
               + " +jetbrains\n"
               + " +org\n"
               + " +xunit\n");

    buildNode("com", true);
    assertTree("-/\n"
               + " +[com]\n"
               + " +jetbrains\n"
               + " +org\n"
               + " +xunit\n");

    myRoot.removeAll();
    updateFromRoot();

    assertTree("+/\n");

  }

  public void testAutoExpand() throws Exception {
    buildStructure(myRoot);
    assertTree("+/\n");

    myAutoExpand.add(new NodeElement("/"));
    buildStructure(myRoot);

    assertTree("-/\n"
               + " +com\n"
               + " +jetbrains\n"
               + " +org\n"
               + " +xunit\n");


    myAutoExpand.add(new NodeElement("jetbrains"));
    updateFromRoot();

    assertTree("-/\n"
               + " +com\n"
               + " -jetbrains\n"
               + "  +fabrique\n"
               + " +org\n"
               + " +xunit\n");

    collapsePath(getPath("jetbrains"));
    assertTree("-/\n"
               + " +com\n"
               + " +jetbrains\n"
               + " +org\n"
               + " +xunit\n");

    updateFrom(new NodeElement("org"));
    assertTree("-/\n"
               + " +com\n"
               + " +jetbrains\n"
               + " +org\n"
               + " +xunit\n");

    updateFrom(new NodeElement("jetbrains"));
    assertTree("-/\n"
               + " +com\n"
               + " -jetbrains\n"
               + "  +fabrique\n"
               + " +org\n"
               + " +xunit\n");
  }

  public void testAutoExpandDeep() throws Exception {
    myTree.setRootVisible(false);
    //myAutoExpand.add(new NodeElement("jetbrains"));
    myAutoExpand.add(new NodeElement("fabrique"));


    buildStructure(myRoot);
    //assertTree("+/\n");

    expand(getPath("/"));
    expand(getPath("jetbrains"));
    assertTree("-/\n"
               + " +com\n"
               + " -jetbrains\n"
               + "  -fabrique\n"
               + "   ide\n"
               + " +org\n"
               + " +xunit\n");

    collapsePath(getPath("/"));
    assertTree("+/\n");

    expand(getPath("/"));
    expand(getPath("jetbrains"));

    assertTree("-/\n"
               + " +com\n"
               + " -jetbrains\n"
               + "  -fabrique\n"
               + "   ide\n"
               + " +org\n"
               + " +xunit\n");

    collapsePath(getPath("jetbrains"));
    assertTree("-/\n"
               + " +com\n"
               + " +jetbrains\n"
               + " +org\n"
               + " +xunit\n");

    expand(getPath("jetbrains"));
    assertTree("-/\n"
               + " +com\n"
               + " -jetbrains\n"
               + "  -fabrique\n"
               + "   ide\n"
               + " +org\n"
               + " +xunit\n");

  }


  public void testAutoExpandInNonVisibleNode() throws Exception {
    myAutoExpand.add(new NodeElement("fabrique"));
    buildStructure(myRoot);

    expand(getPath("/"));
    assertTree("-/\n"
               + " +com\n"
               + " +jetbrains\n"
               + " +org\n"
               + " +xunit\n");
  }

  public void testSmartExpand() throws Exception {
    mySmartExpand = true;
    buildStructure(myRoot);
    assertTree("+/\n");

    expand(getPath("/"));
    assertTree("-/\n"
               + " +com\n"
               + " +jetbrains\n"
               + " +org\n"
               + " +xunit\n");

    expand(getPath("jetbrains"));
    assertTree("-/\n"
               + " +com\n"
               + " -jetbrains\n"
               + "  -fabrique\n"
               + "   ide\n"
               + " +org\n"
               + " +xunit\n");

    collapsePath(getPath("jetbrains"));
    assertTree("-/\n"
               + " +com\n"
               + " +jetbrains\n"
               + " +org\n"
               + " +xunit\n");

    updateFromRoot();
    assertTree("-/\n"
               + " +com\n"
               + " +jetbrains\n"
               + " +org\n"
               + " +xunit\n");

    mySmartExpand = false;
    collapsePath(getPath("jetbrains"));
    assertTree("-/\n"
               + " +com\n"
               + " +jetbrains\n"
               + " +org\n"
               + " +xunit\n");

    expand(getPath("jetbrains"));
    assertTree("-/\n"
               + " +com\n"
               + " -jetbrains\n"
               + "  +fabrique\n"
               + " +org\n"
               + " +xunit\n");
  }


  public void testClear() throws Exception {
    getBuilder().getUi().setClearOnHideDelay(10 * Time.SECOND);

    buildStructure(myRoot);

    assertTree("+/\n");

    final DefaultMutableTreeNode openApiNode = findNode("openapi", false);
    final DefaultMutableTreeNode ideNode = findNode("ide", false);
    final DefaultMutableTreeNode runnerNode = findNode("runner", false);
    final DefaultMutableTreeNode rcpNode = findNode("rcp", false);

    assertNull(openApiNode);
    assertNull(ideNode);
    assertNull(runnerNode);
    assertNull(rcpNode);

    buildNode(myOpenApi, true);
    buildNode(myIde, true);
    buildNode(myRunner, false);

    hideTree();

    assertNull(findNode("openapi", true));
    assertNull(findNode("ide", true));
    assertNull(findNode("runner", false));
    assertNull(findNode("rcp", false));


    showTree();

    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   [openapi]\n" +
               " -jetbrains\n" +
               "  -fabrique\n" +
               "   [ide]\n" +
               " +org\n" +
               " -xunit\n" +
               "  runner\n");

    getMyBuilder().myWasCleanedUp = false;
    hideTree();
    showTree();

    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   [openapi]\n" +
               " -jetbrains\n" +
               "  -fabrique\n" +
               "   [ide]\n" +
               " +org\n" +
               " -xunit\n" +
               "  runner\n");


    buildNode(myFabrique.myElement, true, false);
    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   openapi\n" +
               " -jetbrains\n" +
               "  -[fabrique]\n" +
               "   ide\n" +
               " +org\n" +
               " -xunit\n" +
               "  runner\n");
  }

  public void testUpdateRestoresState() throws Exception {
    buildStructure(myRoot);

    buildNode(myOpenApi, true);
    buildNode(myIde, true);
    buildNode(myRunner, false);

    waitBuilderToCome();

    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   [openapi]\n" +
               " -jetbrains\n" +
               "  -fabrique\n" +
               "   [ide]\n" +
               " +org\n" +
               " -xunit\n" +
               "  runner\n");

    myRoot.removeAll();
    myStructure.clear();

    final AbstractTreeBuilderTest.Node newRoot = myRoot.addChild("newRoot");

    buildStructure(newRoot);

    updateFromRoot();
    assertTree("-/\n" +
               " -newRoot\n" +
               "  -com\n" +
               "   -intellij\n" +
               "    [openapi]\n" +
               "  -jetbrains\n" +
               "   -fabrique\n" +
               "    [ide]\n" +
               "  +org\n" +
               "  -xunit\n" +
               "   runner\n");
  }


  public void testSelect() throws Exception {
    buildStructure(myRoot);
    assertTree(
      "+/\n");


    buildNode(myOpenApi, true);
    assertTree(
      "-/\n" +
      " -com\n" +
      "  -intellij\n" +
      "   [openapi]\n" +
      " +jetbrains\n" +
      " +org\n" +
      " +xunit\n");

    buildNode("fabrique", true);

    assertTree(
      "-/\n" +
      " -com\n" +
      "  -intellij\n" +
      "   [openapi]\n" +
      " -jetbrains\n" +
      "  +[fabrique]\n" +
      " +org\n" +
      " +xunit\n");
  }

  public void testCallbackOnceOnSelect() throws Exception {
    buildStructure(myRoot);

    assertCallbackOnce(new TreeAction() {
      public void run(Runnable onDone) {
        getMyBuilder().select(new Object[] {new NodeElement("intellij"), new NodeElement("fabrique")}, onDone);
      }
    });

    assertTree(
      "-/\n" +
      " -com\n" +
      "  +[intellij]\n" +
      " -jetbrains\n" +
      "  +[fabrique]\n" +
      " +org\n" +
      " +xunit\n");

  }

  public void testCallbackOnceOnExpand() throws Exception {
    buildStructure(myRoot);

    assertCallbackOnce(new TreeAction() {
      public void run(Runnable onDone) {
        getMyBuilder().expand(new Object[] {new NodeElement("intellij"), new NodeElement("fabrique")}, onDone);
      }
    });

    assertTree(
      "-/\n" +
      " -com\n" +
      "  -intellij\n" +
      "   openapi\n" +
      " -jetbrains\n" +
      "  -fabrique\n" +
      "   ide\n" +
      " +org\n" +
      " +xunit\n");

  }


  public void testNoInfiniteAutoExpand() throws Exception {
    mySmartExpand = false;

    assertNoInfiniteAutoExpand(new Runnable() {
      public void run() {
        myAutoExpand.add(new NodeElement("level2"));
        myAutoExpand.add(new NodeElement("level3"));
        myAutoExpand.add(new NodeElement("level4"));
        myAutoExpand.add(new NodeElement("level5"));
        myAutoExpand.add(new NodeElement("level6"));
        myAutoExpand.add(new NodeElement("level7"));
        myAutoExpand.add(new NodeElement("level8"));
        myAutoExpand.add(new NodeElement("level9"));
        myAutoExpand.add(new NodeElement("level10"));
        myAutoExpand.add(new NodeElement("level11"));
        myAutoExpand.add(new NodeElement("level12"));
        myAutoExpand.add(new NodeElement("level13"));
        myAutoExpand.add(new NodeElement("level14"));
        myAutoExpand.add(new NodeElement("level15"));
      }
    });
  }

  public void testNoInfiniteSmartExpand() throws Exception {
    mySmartExpand = false;

    assertNoInfiniteAutoExpand(new Runnable() {
      public void run() {
        mySmartExpand = true;
      }
    });
  }

  private void assertNoInfiniteAutoExpand(final Runnable enableExpand) throws Exception {
    class Level extends Node {

      int myLevel;

      Level(Node parent, int level) {
        super(parent, "level" + level);
        myLevel = level;
      }

      @Override
      public Object[] getChildElements() {
        if (super.getChildElements().length == 0) {
          addChild(new Level(this, myLevel + 1));
        }

        return super.getChildElements();
      }
    }

    myRoot.addChild(new Level(myRoot, 0));

    activate();
    buildNode("level0", false);

    assertTree("-/\n" +
               " -level0\n" +
               "  +level1\n");

    enableExpand.run();

    expand(getPath("level1"));

    assertTree("-/\n" +
               " -level0\n" +
               "  -level1\n" +
               "   -level2\n" +
               "    -level3\n" +
               "     -level4\n" +
               "      -level5\n" +
               "       +level6\n");

    expand(getPath("level6"));
    assertTree("-/\n" +
               " -level0\n" +
               "  -level1\n" +
               "   -level2\n" +
               "    -level3\n" +
               "     -level4\n" +
               "      -level5\n" +
               "       -level6\n" +
               "        -level7\n" +
               "         -level8\n" +
               "          -level9\n" +
               "           -level10\n" + 
               "            +level11\n");
  }

  private void assertCallbackOnce(final TreeAction action) {
    final int[] notifyCount = new int[1];
    final boolean[] done = new boolean[1];
    invokeLaterIfNeeded(new Runnable() {
      public void run() {
        action.run(new Runnable() {
          public void run() {
            notifyCount[0]++;
            done[0] = true;
          }
        });
      }
    });

    new WaitFor(60000) {
      @Override
      protected boolean condition() {
        return done[0] && getMyBuilder().getUi().isReady();
      }
    };

    assertTrue(done[0]);
    assertEquals(1, notifyCount[0]);
  }

  public void testSelectMultiple() throws Exception {
    buildStructure(myRoot);
    assertTree(
      "+/\n");

    select(new Object[] {new NodeElement("openapi"), new NodeElement("fabrique")}, false);
    assertTree(
      "-/\n" +
      " -com\n" +
      "  -intellij\n" +
      "   [openapi]\n" +
      " -jetbrains\n" +
      "  +[fabrique]\n" +
      " +org\n" +
      " +xunit\n");
  }

  public void testUnsuccessfulSelect() throws Exception {
    buildStructure(myRoot);
    select(new Object[] {new NodeElement("openapi"), new NodeElement("fabrique")}, false);

    assertTree(
      "-/\n" +
      " -com\n" +
      "  -intellij\n" +
      "   [openapi]\n" +
      " -jetbrains\n" +
      "  +[fabrique]\n" +
      " +org\n" +
      " +xunit\n");

    select(new Object[] {new NodeElement("whatever1"), new NodeElement("whatever2")}, false);

    assertTree(
      "-/\n" +
      " -com\n" +
      "  -intellij\n" +
      "   [openapi]\n" +
      " -jetbrains\n" +
      "  +[fabrique]\n" +
      " +org\n" +
      " +xunit\n");
  }


  public void testSelectionWhenChildMoved() throws Exception {
    buildStructure(myRoot);
    assertTree("+/\n");

    final Node refactoring = myCom.getChildNode("intellij").addChild("refactoring");

    buildNode("refactoring", true);

    assertTree(
      "-/\n" +
      " -com\n" +
      "  -intellij\n" +
      "   openapi\n" +
      "   [refactoring]\n" +
      " +jetbrains\n" +
      " +org\n" +
      " +xunit\n");

    refactoring.delete();
    myCom.getChildNode("intellij").getChildNode("openapi").addChild("refactoring");

    updateFromRoot();

    assertTree(
      "-/\n" +
      " -com\n" +
      "  -intellij\n" +
      "   -openapi\n" +
      "    [refactoring]\n" +
      " +jetbrains\n" +
      " +org\n" +
      " +xunit\n");
  }


  public void testSelectionGoesToParentWhenOnlyChildRemove() throws Exception {
    buildStructure(myRoot);
    buildNode("openapi", true);

    assertTree(
      "-/\n" +
      " -com\n" +
      "  -intellij\n" +
      "   [openapi]\n" +
      " +jetbrains\n" +
      " +org\n" +
      " +xunit\n");

    myCom.getChildNode("intellij").getChildNode("openapi").delete();

    updateFromRoot();

    assertTree(
      "-/\n" +
      " -com\n" +
      "  [intellij]\n" +
      " +jetbrains\n" +
      " +org\n" +
      " +xunit\n");
  }

  public void testSelectionGoesToParentWhenOnlyChildMoved() throws Exception {
    buildStructure(myRoot);
    buildNode("openapi", true);

    assertTree(
      "-/\n" +
      " -com\n" +
      "  -intellij\n" +
      "   [openapi]\n" +
      " +jetbrains\n" +
      " +org\n" +
      " +xunit\n");

    myCom.getChildNode("intellij").getChildNode("openapi").delete();
    myRoot.getChildNode("xunit").addChild("openapi");

    updateFromRoot();

    assertTree(
      "-/\n" +
      " -com\n" +
      "  intellij\n" +
      " +jetbrains\n" +
      " +org\n" +
      " -xunit\n" +
      "  [openapi]\n" +
      "  runner\n");
  }

  public void testSelectionGoesToParentWhenOnlyChildMoved2() throws Exception {
    buildStructure(myRoot);
    buildNode("openapi", true);

    assertTree(
      "-/\n" +
      " -com\n" +
      "  -intellij\n" +
      "   [openapi]\n" +
      " +jetbrains\n" +
      " +org\n" +
      " +xunit\n");

    myCom.getChildNode("intellij").getChildNode("openapi").delete();
    myRoot.getChildNode("xunit").addChild("openapi");

    getBuilder().addSubtreeToUpdateByElement(new NodeElement("intellij"));
    getBuilder().addSubtreeToUpdateByElement(new NodeElement("xunit"));


    doAndWaitForBuilder(new Runnable() {
      public void run() {
        getBuilder().getUpdater().performUpdate();
      }
    });

    assertTree(
      "-/\n" +
      " -com\n" +
      "  intellij\n" +
      " +jetbrains\n" +
      " +org\n" +
      " -xunit\n" +
      "  [openapi]\n" +
      "  runner\n");
  }

  public void testSelectionGoesToParentWhenChildrenFold() throws Exception {
    buildStructure(myRoot);
    buildNode("openapi", true);

    assertTree(
      "-/\n" +
      " -com\n" +
      "  -intellij\n" +
      "   [openapi]\n" +
      " +jetbrains\n" +
      " +org\n" +
      " +xunit\n");


    final DefaultMutableTreeNode node = findNode("intellij", false);
    collapsePath(new TreePath(node.getPath()));

    assertTree(
      "-/\n" +
      " -com\n" +
      "  +[intellij]\n" +
      " +jetbrains\n" +
      " +org\n" +
      " +xunit\n");
  }

  public void testThrowingProcessCancelledInterruptsUpdate() throws Exception {
    buildStructure(myRoot);

    expand(getPath("/"));
    expand(getPath("com"));
    expand(getPath("jetbrains"));
    expand(getPath("org"));
    expand(getPath("xunit"));

    assertTree("-/\n" +
               " -com\n" +
               "  +intellij\n" +
               " -jetbrains\n" +
               "  +fabrique\n" +
               " -org\n" +
               "  +eclipse\n" +
               " -xunit\n" +
               "  runner\n");
    
    runAndInterrupt(new MyRunnable() {
      public void runSafe() throws Exception {
        updateFrom(new NodeElement("/"));
      }
    }, "update", new NodeElement("jetbrains"));

    runAndInterrupt(new MyRunnable() {
      @Override
      public void runSafe() throws Exception {
        updateFrom(new NodeElement("/"));
      }
    }, "getChildren", new NodeElement("jetbrains"));
  }

  private void runAndInterrupt(final Runnable action, final String interruptAction, final Object interruptElement) throws Exception {
    myElementUpdate.clear();

    final boolean[] wasInterrupted = new boolean[1];
    myElementUpdateHook = new ElementUpdateHook() {
      public void onElementAction(String action, Object element) {
        if (wasInterrupted[0]) {
          if (myCancelRequest == null) {
            myCancelRequest = new AssertionError("Not supposed to be update after interruption request: action=" + action + " element=" + element);
          }
        } else {
          if (element.equals(interruptElement) && action.equals(interruptAction)) {
            wasInterrupted[0] = true;
            throw new ProcessCanceledException();
          }
        }
      }
    };

    action.run();
  }

  public void testQueryStructure() throws Exception {
    buildStructure(myRoot);

    assertTree("+/\n");
    assertUpdates("/: update");

    expand(getPath("/"));
    assertTree("-/\n" +
               " +com\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");
    assertUpdates("/: update getChildren\n" +
                  "com: update getChildren\n" +
                  "eclipse: update\n" +
                  "fabrique: update\n" +
                  "intellij: update\n" +
                  "jetbrains: update getChildren\n" +
                  "org: update getChildren\n" +
                  "runner: update\n" +
                  "xunit: update getChildren");

    collapsePath(getPath("/"));
    assertTree("+/\n");
    assertUpdates("");

    expand(getPath("/"));
    assertTree("-/\n" +
               " +com\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");
    assertUpdates("/: update getChildren\n" +
                  "com: update getChildren\n" +
                  "eclipse: update\n" +
                  "fabrique: update\n" +
                  "intellij: update\n" +
                  "jetbrains: update getChildren\n" +
                  "org: update getChildren\n" +
                  "runner: update\n" +
                  "xunit: update getChildren");

    updateFromRoot();
    assertTree("-/\n" +
               " +com\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");

    assertUpdates("/: update getChildren\n" +
                  "com: update getChildren\n" +
                  "eclipse: update\n" +
                  "fabrique: update\n" +
                  "intellij: update\n" +
                  "jetbrains: update getChildren\n" +
                  "org: update getChildren\n" +
                  "runner: update\n" +
                  "xunit: update getChildren");

  }

  public void testQueryStructureWhenExpand() throws Exception {
    buildStructure(myRoot);

    assertTree("+/\n");
    assertUpdates("/: update");

    buildNode("ide", false);
    assertTree("-/\n" +
               " +com\n" +
               " -jetbrains\n" +
               "  -fabrique\n" +
               "   ide\n" +
               " +org\n" +
               " +xunit\n");

    assertUpdates("/: update getChildren\n" +
                  "com: update getChildren\n" +
                  "eclipse: update\n" +
                  "fabrique: update (2) getChildren\n" +
                  "ide: update getChildren\n" +
                  "intellij: update\n" +
                  "jetbrains: update getChildren\n" +
                  "org: update getChildren\n" +
                  "runner: update\n" +
                  "xunit: update getChildren");

  }

  public void testQueryStructureIsAlwaysLeaf() throws Exception {
    buildStructure(myRoot);
    myStructure.addLeaf(new NodeElement("openapi"));

    buildNode("jetbrains", false);
    assertTree("-/\n" +
               " +com\n" +
               " -jetbrains\n" +
               "  +fabrique\n" +
               " +org\n" +
               " +xunit\n");

    assertUpdates("/: update (2) getChildren\n" +
                  "com: update getChildren\n" +
                  "eclipse: update\n" +
                  "fabrique: update (2) getChildren\n" +
                  "ide: update\n" +
                  "intellij: update\n" +
                  "jetbrains: update getChildren\n" +
                  "org: update getChildren\n" +
                  "runner: update\n" +
                  "xunit: update getChildren");

    expand(getPath("fabrique"));
    assertTree("-/\n" +
               " +com\n" +
               " -jetbrains\n" +
               "  -fabrique\n" +
               "   ide\n" +
               " +org\n" +
               " +xunit\n");
    assertUpdates("ide: update getChildren");


    buildNode("com", false);
    assertTree("-/\n" +
               " -com\n" +
               "  +intellij\n" +
               " -jetbrains\n" +
               "  -fabrique\n" +
               "   ide\n" +
               " +org\n" +
               " +xunit\n");

    myElementUpdate.clear();

    expand(getPath("intellij"));
    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   openapi\n" +
               " -jetbrains\n" +
               "  -fabrique\n" +
               "   ide\n" +
               " +org\n" +
               " +xunit\n");

    assertUpdates("");
  }

  public void testToggleIsAlwaysLeaf() throws Exception {
    buildStructure(myRoot);

    buildNode("openapi", true);

    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   [openapi]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");

    myStructure.addLeaf(new NodeElement("intellij"));

    updateFrom(new NodeElement("com"));

    assertTree("-/\n" +
               " -com\n" +
               "  [intellij]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");

    myStructure.removeLeaf(new NodeElement("intellij"));
    updateFrom(new NodeElement("com"));

    assertTree("-/\n" +
               " -com\n" +
               "  +[intellij]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");

    expand(getPath("intellij"));

    assertTree("-/\n" +
               " -com\n" +
               "  -[intellij]\n" +
               "   openapi\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");

  }


  public void testSorting() throws Exception {
    buildStructure(myRoot);
    assertSorted("");

    buildNode("/", false);
    assertTree("-/\n" +
               " +com\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");

    assertSorted("/\n" +
                 "com\n" +
                 "jetbrains\n" +
                 "org\n" +
                 "xunit");

    updateFromRoot();
    assertTree("-/\n" +
               " +com\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");
    assertSorted("/\n" +
                 "com\n" +
                 "jetbrains\n" +
                 "org\n" +
                 "xunit");

    updateFrom(new NodeElement("/"), false);
    assertTree("-/\n" +
               " +com\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");
    assertSorted("");


    expand(getPath("com"));
    assertTree("-/\n" +
               " -com\n" +
               "  +intellij\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");
    assertSorted("intellij");
  }

  public void testResorting() throws Exception {
    final boolean invert[] = new boolean[] {false};
    NodeDescriptor.NodeComparator<NodeDescriptor> c = new NodeDescriptor.NodeComparator<NodeDescriptor>() {
      public int compare(NodeDescriptor o1, NodeDescriptor o2) {
        return invert[0] ? AlphaComparator.INSTANCE.compare(o2, o1) : AlphaComparator.INSTANCE.compare(o1, o2);
      }
    };

    myComparator.setDelegate(c);

    buildStructure(myRoot);

    buildNode("/", false);
    assertTree("-/\n" +
               " +com\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");

    updateFromRoot();
    updateFromRoot();
    updateFromRoot();

    assertTrue(getMyBuilder().getUi().myOwnComparatorStamp > c.getStamp());
    invert[0] = true;
    c.incStamp();

    updateFrom(new NodeElement("/"), false);
    assertTree("-/\n" +
               " +xunit\n" +
               " +org\n" +
               " +jetbrains\n" +
               " +com\n");
  }

  public void testRestoreSelectionOfRemovedElement() throws Exception {
    buildStructure(myRoot);
    buildNode("openapi", true);
    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   [openapi]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");


    removeFromParentButKeepRef(new NodeElement("openapi"));

    updateFromRoot();

    assertTree("-/\n" +
               " -com\n" +
               "  [intellij]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");
  }

  public void testElementMove1() throws Exception {
    assertMove(new Runnable() {
      public void run() {
        getBuilder().getUpdater().addSubtreeToUpdateByElement(new NodeElement("com"));
        getBuilder().getUpdater().addSubtreeToUpdateByElement(new NodeElement("jetbrains"));
      }
    });
  }

  public void testElementMove2() throws Exception {
    assertMove(new Runnable() {
      public void run() {
        getBuilder().getUpdater().addSubtreeToUpdateByElement(new NodeElement("jetbrains"));
        getBuilder().getUpdater().addSubtreeToUpdateByElement(new NodeElement("com"));
      }
    });
  }

  public void testSelectionOnDelete() throws Exception {
    doTestSelectionOnDelete(false);
  }

  public void testSelectionOnDeleteButKeepRef() throws Exception {
    doTestSelectionOnDelete(true);
  }

  private void doTestSelectionOnDelete(boolean keepRef) throws Exception {
    myComparator.setDelegate(new NodeDescriptor.NodeComparator<NodeDescriptor>() {
      public int compare(NodeDescriptor o1, NodeDescriptor o2) {
        boolean isParent1 = myStructure._getChildElements(o1.getElement(), false).length > 0;
        boolean isParent2 = myStructure._getChildElements(o2.getElement(), false).length > 0;

        int result = AlphaComparator.INSTANCE.compare(o1, o2);

        if (isParent1) {
          result -= 1000;
        }

        if (isParent2) {
          result += 1000;
        }

        return result;
      }
    });

    buildStructure(myRoot);
    myRoot.addChild("toDelete");

    select(new NodeElement("toDelete"), false);
    assertTree("-/\n" +
               " +com\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n" +
               " [toDelete]\n");

    if (keepRef) {
      removeFromParentButKeepRef(new NodeElement("toDelete"));
    } else {
      myStructure.getNodeFor(new NodeElement("toDelete")).delete();
    }

    getMyBuilder().addSubtreeToUpdateByElement(new NodeElement("/"));

    assertTree("-/\n" +
               " +com\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +[xunit]\n");
  }

  public void testSelectWhenUpdatesArePending() throws Exception {
    getBuilder().getUpdater().setDelay(1000);

    buildStructure(myRoot);

    buildNode("intellij", false);
    select(new Object[] {new NodeElement("intellij")}, false);
    assertTree("-/\n" +
               " -com\n" +
               "  -[intellij]\n" +
               "   openapi\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");


    myIntellij.addChild("ui");

    DefaultMutableTreeNode intellijNode = findNode("intellij", false);
    assertTrue(myTree.isExpanded(new TreePath(intellijNode.getPath())));
    getMyBuilder().addSubtreeToUpdate(intellijNode);
    assertFalse(getMyBuilder().getUi().isReady());

    select(new Object[] {new NodeElement("ui")}, false);
    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   openapi\n" +
               "   [ui]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");
  }

  public void testAddNewElementToLeafElementAlwaysShowPlus() throws Exception {
    myAlwaysShowPlus.add(new NodeElement("openapi"));

    buildStructure(myRoot);
    select(new Object[] {new NodeElement("openapi")}, false);

    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   +[openapi]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");

    expand(getPath("openapi"));
    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   [openapi]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");


    myOpenApi.addChild("ui");

    getMyBuilder().addSubtreeToUpdate(findNode("openapi", false));

    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   +[openapi]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");
  }

  public void testAddNewElementToLeafElement() throws Exception {
    buildStructure(myRoot);
    select(new Object[] {new NodeElement("openapi")}, false);

    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   [openapi]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");

    expand(getPath("openapi"));
    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   [openapi]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");


    myOpenApi.addChild("ui");

    getMyBuilder().addSubtreeToUpdate(findNode("openapi", false));

    assertTree("-/\n" +
               " -com\n" +
               "  -intellij\n" +
               "   +[openapi]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");
  }


  private void assertMove(Runnable updateRoutine) throws Exception {
    buildStructure(myRoot);

    buildNode("intellij", true);
    assertTree("-/\n" +
               " -com\n" +
               "  +[intellij]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");

    AbstractTreeBuilderTest.Node intellij = removeFromParentButKeepRef(new NodeElement("intellij"));
    myRoot.getChildNode("jetbrains").addChild(intellij);

    updateRoutine.run();

    assertTree("-/\n" +
               " com\n" +
               " -jetbrains\n" +
               "  +fabrique\n" +
               "  +[intellij]\n" +
               " +org\n" +
               " +xunit\n");

  }


  public void testChangeRootElement() throws Exception {
    buildStructure(myRoot);

    select(new NodeElement("com"), false);

    assertTree("-/\n" +
               " +[com]\n" +
               " +jetbrains\n" +
               " +org\n" +
               " +xunit\n");

    myRoot = new Node(null, "root");
    myStructure.reinitRoot(myRoot);

    myRoot.addChild("com");

    updateFromRoot();
    assertTree("+root\n");
  }

  public void testReleaseBuilderDuringUpdate() throws Exception {
    assertReleaseDuringBuilding("update", "fabrique", new Runnable() {
      public void run() {
        try {
          select(new NodeElement("ide"), false);
        }
        catch (Exception e) {
          myCancelRequest = e;
        }
      }
    });
  }

  public void testStickyLoadingNodeIssue() throws Exception {
    buildStructure(myRoot);

    final boolean[] done = new boolean[] {false};
    getBuilder().select(new NodeElement("jetbrains"), new Runnable() {
      public void run() {
        getBuilder().expand(new NodeElement("fabrique"), new Runnable() {
          public void run() {
            done[0] = true;
          }
        });
      }
    });

    waitBuilderToCome(new Condition() {
      public boolean value(Object o) {
        return done[0];
      }
    });

    assertTree("-/\n" +
               " +com\n" +
               " -[jetbrains]\n" +
               "  -fabrique\n" +
               "   ide\n" +
               " +org\n" +
               " +xunit\n");
  }

  public void testReleaseBuilderDuringGetChildren() throws Exception {
    assertReleaseDuringBuilding("getChildren", "fabrique", new Runnable() {
      public void run() {
        try {
          select(new NodeElement("ide"), false);
        }
        catch (Exception e) {
          myCancelRequest = e;
        }
      }
    });
  }

  private void assertReleaseDuringBuilding(final String actionAction, final Object actionElement, Runnable buildAction) throws Exception {
    buildStructure(myRoot);

    myElementUpdateHook = new ElementUpdateHook() {
      public void onElementAction(String action, Object element) {
        if (!element.toString().equals(actionElement.toString())) return;

        if (actionAction.equals(action)) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              myReadyRequest = true;
              Disposer.dispose(getBuilder());
            }
          });
        }
      }
    };

    buildAction.run();

    boolean released = new WaitFor(5000) {
      @Override
      protected boolean condition() {
        return getBuilder().getUi() == null;
      }
    }.isConditionRealized();

    assertTrue(released);
  }

  public static class SyncUpdate extends TreeUiTest {
    public SyncUpdate() {
      super(false, false);
    }
  }

  public static class Passthrough extends TreeUiTest {
    public Passthrough() {
      super(true);
    }

    public void testSelectionGoesToParentWhenOnlyChildMoved2() throws Exception {
      //todo
    }

    public void testQueryStructureWhenExpand() throws Exception {
      //todo
    }


    public void testElementMove1() throws Exception {
      //todo
    }

    @Override
    public void testClear() throws Exception {
      //todo
    }

    @Override
    public void testSelectWhenUpdatesArePending() throws Exception {
      // doesn't make sense in pass-through mode
    }
  }

  public static class YieldingUpdate extends TreeUiTest {
    public YieldingUpdate() {
      super(true, false);
    }

  }

  public static class BgLoadingSyncUpdate extends TreeUiTest {
    public BgLoadingSyncUpdate() {
      super(false, true);
    }

    @Override
    protected int getChildrenLoadingDelay() {
      return 300;
    }

    @Override
    protected int getNodeDescriptorUpdateDelay() {
      return 300;
    }

    @Override
    public void testNoInfiniteSmartExpand() throws Exception {
      //todo
    }
  }

  public static class QuickBgLoadingSyncUpdate extends TreeUiTest {
    public QuickBgLoadingSyncUpdate() {
      super(false, true);
    }


    @Override
    protected int getNodeDescriptorUpdateDelay() {
      return 300;
    }

    @Override
    public void testNoInfiniteSmartExpand() throws Exception {
      //todo
    }

    @Override
    public void testStickyLoadingNodeIssue() throws Exception {
      super.testStickyLoadingNodeIssue();
    }
  }

  public static TestSuite suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(Passthrough.class);
    suite.addTestSuite(SyncUpdate.class);
    suite.addTestSuite(YieldingUpdate.class);
    suite.addTestSuite(BgLoadingSyncUpdate.class);
    suite.addTestSuite(QuickBgLoadingSyncUpdate.class);
    return suite;
  }

  abstract class MyRunnable implements Runnable {
    public final void run() {
      try {
        runSafe();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public abstract void runSafe() throws Exception;
  }
}
