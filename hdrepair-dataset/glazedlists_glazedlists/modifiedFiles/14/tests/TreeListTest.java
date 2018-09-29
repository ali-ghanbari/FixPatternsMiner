package ca.odell.glazedlists;

import ca.odell.glazedlists.impl.testing.GlazedListsTests;
import ca.odell.glazedlists.impl.testing.ListConsistencyListener;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Verifies that EventList matches the List API.
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public class TreeListTest extends TestCase {

    /**
     * Can we build a tree list?
     */
    public void testCreate() throws Exception {
        BasicEventList<Method> source = new BasicEventList<Method>();
        source.add(List.class.getMethod("add", new Class[] {Object.class}));
        source.add(List.class.getMethod("add", new Class[] {int.class, Object.class}));
        source.add(List.class.getMethod("set", new Class[] {int.class, Object.class}));
        source.add(List.class.getMethod("remove", new Class[] {int.class}));
        source.add(List.class.getMethod("clear", new Class[] {}));
        source.add(String.class.getMethod("toString", new Class[] {}));
        source.add(Date.class.getMethod("getTime", new Class[] {}));
        source.add(BasicEventList.class.getMethod("add", new Class[] {Object.class}));
        source.add(BasicEventList.class.getMethod("add", new Class[] {int.class, Object.class}));

        TreeList treeList = new TreeList(source, new JavaStructureTreeFormat(), GlazedLists.comparableComparator());
    }

    /**
     * Convert Java methods into paths. For example, {@link Object#toString()}
     * is <code>/java/lang/Object/toString</code>
     */
    static class JavaStructureTreeFormat implements TreeList.Format<Object> {

        public boolean allowsChildren(Object element) {
            return (!(element instanceof Method));
        }

        public void getPath(List<Object> path, Object element) {
            Method javaMethod = (Method)element;
            path.addAll(Arrays.asList(javaMethod.getDeclaringClass().getName().split("\\.")));

            StringBuffer signature = new StringBuffer();
            signature.append(javaMethod.getName());

            // print the arguments list
            signature.append("(");
            Class<?>[] parameterTypes = javaMethod.getParameterTypes();
            for(int c = 0; c < parameterTypes.length; c++) {
                if(c > 0) signature.append(", ");
                String[] parameterClassNameParts = parameterTypes[c].getName().split("\\.");
                signature.append(parameterClassNameParts[parameterClassNameParts.length - 1]);
            }
            signature.append(")");

            path.add(signature.toString());
        }
    }

    /**
     * Convert Strings into paths. For example, PUPPY is <code>/P/U/P/P/Y</code>
     *
     * <p>Lowercase values cannot have children.
     */
    static class CharacterTreeFormat implements TreeList.Format<String> {
        public boolean allowsChildren(String element) {
            return Character.isUpperCase(element.charAt(0));
        }
        public void getPath(List<String> path, String element) {
            for(int c = 0; c < element.length(); c++) {
                path.add(element.substring(c, c + 1));
            }
        }
    }


    public void testAddsAndRemoves() {
        BasicEventList<String> source = new BasicEventList<String>();
        source.add("ABC");
        source.add("ABD");
        source.add("ABEFG");
        source.add("ABEFH");
        source.add("ACD");
        source.add("ACE");
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat(), (Comparator)GlazedLists.comparableComparator());
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "ABD",
                "ABE",
                "ABEF",
                "ABEFG",
                "ABEFH",
                "AC",
                "ACD",
                "ACE",
        });

        // observe that we're firing the right events
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        // make some modifications, they should be handled in place
        source.add("ABF");
        assertEquals(GlazedListsTests.stringToList("ABF"), treeList.getTreeNode(8).path());
        source.add("ABG");
        assertEquals(GlazedListsTests.stringToList("ABG"), treeList.getTreeNode(9).path());
        source.add("ACBA");
        assertEquals(GlazedListsTests.stringToList("ACB"), treeList.getTreeNode(11).path());
        assertEquals(GlazedListsTests.stringToList("ACBA"), treeList.getTreeNode(12).path());

        // now some removes
        source.remove("ABD");
        assertEquals(14, treeList.size());
        source.remove("ACD");
        assertEquals(13, treeList.size());
        source.remove("ABEFH");
        assertEquals(12, treeList.size());
    }

    public void testSubtreeSize() {

        // try a simple hierarchy
        BasicEventList<String> source = new BasicEventList<String>();
        source.add("ABCD");
        source.add("ABEFG");
        source.add("ACDC");
        source.add("ACE");
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat(), (Comparator)GlazedLists.comparableComparator());
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "ABCD",
                "ABE",
                "ABEF",
                "ABEFG",
                "AC",
                "ACD",
                "ACDC",
                "ACE",
        });

        assertEquals(11, treeList.subtreeSize(0, true));
        assertEquals(6, treeList.subtreeSize(1, true));
        assertEquals(2, treeList.subtreeSize(2, true));
        assertEquals(1, treeList.subtreeSize(3, true));
        assertEquals(3, treeList.subtreeSize(4, true));
        assertEquals(2, treeList.subtreeSize(5, true));
        assertEquals(1, treeList.subtreeSize(6, true));
        assertEquals(4, treeList.subtreeSize(7, true));
        assertEquals(2, treeList.subtreeSize(8, true));
        assertEquals(1, treeList.subtreeSize(9, true));
        assertEquals(1, treeList.subtreeSize(10, true));
    }

    public void testVirtualParentsAreCleanedUp() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat(), (Comparator)GlazedLists.comparableComparator());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABG");
        source.add("ACDEF");
        source.add("AHI");
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABG",
                "AC",
                "ACD",
                "ACDE",
                "ACDEF",
                "AH",
                "AHI",
        });

        source.remove("ACDEF");
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABG",
                "AH",
                "AHI",
        });

        source.remove("ABG");
        assertTreeStructure(treeList, new String[] {
                "A",
                "AH",
                "AHI",
        });

        source.remove("AHI");
        assertEquals(0, treeList.size());

        // but only virtual parents are cleaned up
        source.add("ABC");
        source.add("ABCDEF");
        source.remove("ABCDEF");
        assertEquals(3, treeList.size());
    }

    public void testInsertRealOverVirtualParent() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat(), (Comparator)GlazedLists.comparableComparator());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABCDEF");
        source.add("ABC");
        assertEquals(6, treeList.size());
    }

    public void testStructureChangingUpdates() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat(), (Comparator)GlazedLists.comparableComparator());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("A");
        source.add("DEF");
        source.add("GJ");

        // swap dramatically, moving to a new subtree
        source.set(1, "DHI");
        assertTreeStructure(treeList, new String[] {
                "A",
                "D",
                "DH",
                "DHI",
                "G",
                "GJ",
        });

        // now move deeper
        source.set(1, "DHIJK");
        assertTreeStructure(treeList, new String[] {
                "A",
                "D",
                "DH",
                "DHI",
                "DHIJ",
                "DHIJK",
                "G",
                "GJ",
        });

        // now move shallower
        source.set(1, "DH");
        assertTreeStructure(treeList, new String[] {
                "A",
                "D",
                "DH",
                "G",
                "GJ",
        });

        // now move to another subtree after this one
        source.set(1, "GAB");
        assertTreeStructure(treeList, new String[] {
                "A",
                "G",
                "GA",
                "GAB",
                "GJ",
        });

        // now move to another subtree before this one
        source.set(1, "ABC");
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "G",
                "GJ",
        });
    }

    public void testClear() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat(), (Comparator)GlazedLists.comparableComparator());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABC");
        source.add("DEF");
        source.add("ABH");
        source.add("DZK");
        source.clear();
        assertEquals(0, treeList.size());
    }

    public void testSourceChangesOnCollapsedSubtrees() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat(), (Comparator)GlazedLists.comparableComparator());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("A");
        treeList.setExpanded(0, false);
        source.add("ABC");
        assertEquals(1, treeList.size());

        treeList.setExpanded(0, true);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
        });

        treeList.setExpanded(1, false);
        source.add("AD");
        source.addAll(Arrays.asList(new String[] {
                "ABD",
                "ABE",
                "ABF",
                "ABG",
        }));

        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "AD",
        });

        treeList.setExpanded(0, false);
        source.addAll(Arrays.asList(new String[] {
                "ABH",
                "ABI",
        }));
        treeList.setExpanded(0, true);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "AD",
        });

        treeList.setExpanded(1, true);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "ABD",
                "ABE",
                "ABF",
                "ABG",
                "ABH",
                "ABI",
                "AD",
        });

        treeList.setExpanded(0, false);
        source.removeAll(Arrays.asList(new String[] {
                "ABF",
                "ABG",
        }));
        treeList.setExpanded(0, true);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "ABD",
                "ABE",
                "ABH",
                "ABI",
                "AD",
        });
    }

    public void assertTreeStructure(TreeList<String>treeList, String[] structure) {

        // convert the list of TreeElements into a list of Strings
        List<String> treeAsStrings = new ArrayList<String>();
        for(Iterator<TreeList.Node<String>> i = treeList.getNodesList().iterator(); i.hasNext(); ) {
            TreeList.Node<String> node = i.next();
            StringBuffer asString = new StringBuffer(node.path().size());
            for(Iterator<String> n = node.path().iterator(); n.hasNext(); ) {
                asString.append(n.next());
            }
            treeAsStrings.add(asString.toString());
        }

        assertEquals(Arrays.asList(structure), treeAsStrings);
    }

    public void testCollapseExpand() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat(), (Comparator)GlazedLists.comparableComparator());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABC");
        source.add("ABD");
        source.add("ABE");
        source.add("ABFG");
        source.add("ABFH");
        source.add("DEF");
        source.add("GJ");
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "ABD",
                "ABE",
                "ABF",
                "ABFG",
                "ABFH",
                "D",
                "DE",
                "DEF",
                "G",
                "GJ",
        });

        // collapse 'E'
        treeList.setExpanded(9, false);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "ABD",
                "ABE",
                "ABF",
                "ABFG",
                "ABFH",
                "D",
                "DE",
                "G",
                "GJ",
        });

        // collapse 'F'
        treeList.setExpanded(5, false);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "ABD",
                "ABE",
                "ABF",
                "D",
                "DE",
                "G",
                "GJ",
        });

        // collapse 'B'
        treeList.setExpanded(1, false);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "D",
                "DE",
                "G",
                "GJ",
        });

        // collapse 'D'
        treeList.setExpanded(2, false);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "D",
                "G",
                "GJ",
        });

        // expand 'B'
        treeList.setExpanded(1, true);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "ABD",
                "ABE",
                "ABF",
                "D",
                "G",
                "GJ",
        });

        // collapse 'A'
        treeList.setExpanded(0, false);
        assertTreeStructure(treeList, new String[] {
                "A",
                "D",
                "G",
                "GJ",
        });

        // expand 'A', 'F'
        treeList.setExpanded(0, true);
        treeList.setExpanded(5, true);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "ABD",
                "ABE",
                "ABF",
                "ABFG",
                "ABFH",
                "D",
                "G",
                "GJ",
        });
    }

    public void testSourceUpdateEvents() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat(), (Comparator)GlazedLists.comparableComparator());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABC");
        source.add("DEF");
        treeList.setExpanded(0, false);
        source.set(0, "ABD");
        assertTreeStructure(treeList, new String[] {
                "A",
                "D",
                "DE",
                "DEF",
        });

        source.set(1, "ABE");
        assertTreeStructure(treeList, new String[] {
                "A",
        });
    }

    public void testDeletedRealParentIsReplacedByVirtualParent() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat(), (Comparator)GlazedLists.comparableComparator());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("A");
        source.add("ABC");
        source.remove(0);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
        });
    }

    public void testTreeEditing() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat(), (Comparator)GlazedLists.comparableComparator());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABC");
        source.add("ABD");
        source.add("EFG");
        source.add("EFK");
        treeList.setExpanded(0, false);
        assertTreeStructure(treeList, new String[] {
                "A",
                "E",
                "EF",
                "EFG",
                "EFK",
        });

        treeList.set(3, "EFH");
        treeList.set(4, "EFL");
        treeList.add(4, "EFI");
        assertTreeStructure(treeList, new String[] {
                "A",
                "E",
                "EF",
                "EFH",
                "EFI",
                "EFL",
        });
    }

    public void testTreeSortingUnsortedTree() {
        EventList<String> source = new BasicEventList<String>();
        SortedList<String> sortedSource = new SortedList<String>(source);
        TreeList<String> treeList = new TreeList<String>(sortedSource, new CharacterTreeFormat());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABC");
        source.add("ABD");
        source.add("ABE");
        source.add("FGH");
        source.add("FGI");

        sortedSource.setComparator(GlazedLists.reverseComparator());
        assertTreeStructure(treeList, new String[] {
                "F",
                "FG",
                "FGI",
                "FGH",
                "A",
                "AB",
                "ABE",
                "ABD",
                "ABC",
        });
    }

    public void testTreeSorting() {
        EventList<String> source = new BasicEventList<String>();
        SortedList<String> sortedSource = new SortedList<String>(source);
        TreeList<String> treeList = new TreeList<String>(sortedSource, new CharacterTreeFormat(), (Comparator)GlazedLists.comparableComparator());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABc");
        source.add("ABd");
        source.add("ABe");
        source.add("FGh");
        source.add("FGi");

        sortedSource.setComparator(GlazedLists.reverseComparator());
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABe",
                "ABd",
                "ABc",
                "F",
                "FG",
                "FGi",
                "FGh",
        });
    }

    public void testInsertInReverseOrder() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat(), (Comparator)GlazedLists.comparableComparator());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("LBAA");
        source.add("LAAA");
    }

    public void testNonSiblingsBecomeSiblings() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABC");
        source.add("EFG");
        source.add("ABD");
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "E",
                "EF",
                "EFG",
                "A",
                "AB",
                "ABD",
        });

        source.remove("EFG");
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "ABD",
        });
    }

    public void testSiblingsBecomeNonSiblings() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABC");
        source.add("ABD");
        source.add(1, "EFG");
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "E",
                "EF",
                "EFG",
                "A",
                "AB",
                "ABD",
        });
    }

    public void testSiblingsBecomeNonSiblingsWithCollapsedNodes() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABC");
        source.add("ABD");
        treeList.setExpanded(1, false); // collapse 'B'
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
        });

        source.add(1, "EFG");
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "E",
                "EF",
                "EFG",
                "A",
                "AB",
        });

        treeList.setExpanded(6, true);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "E",
                "EF",
                "EFG",
                "A",
                "AB",
                "ABD",
        });
    }

    /**
     * This test validates that when multiple sets of parents are restored, all
     * the appropriate virtual nodes are assigned the appropriate new parents.
     */
    public void testInsertMultipleParents() {
        ExternalNestingEventList<String> source = new ExternalNestingEventList<String>(new BasicEventList<String>());
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.beginEvent(false);
            source.add("ABC");
            source.add("ABD");
            source.add("ABE");
        source.commitEvent();

        source.beginEvent(true);
            source.addAll(0, Arrays.asList(new String[] { "A", "AB" }));
            source.addAll(3, Arrays.asList(new String[] { "A", "AB" }));
        source.commitEvent();

        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "A",
                "AB",
                "ABD",
                "ABE",
        });
    }

    public void testAttachSiblings() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("A");
        source.add("BCD");
        source.add("BCF");
        assertTreeStructure(treeList, new String[] {
                "A",
                "B",
                "BC",
                "BCD",
                "BCF",
        });
    }

    public void testDeleteParentAndOneChild() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("P");
        source.add("PD");
        source.add("PU");
        source.add("PI");
        source.add("PIS");
        source.add("PIU");
        source.add("PY");
        source.removeAll(Arrays.asList(new String[] { "PI", "PIS" }));
        assertTreeStructure(treeList, new String[] {
                "P",
                "PD",
                "PU",
                "PI",
                "PIU",
                "PY",
        });
    }

    public void testReplaceVirtualWithRealWithSiblings() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABC");
        source.add("DEF");
        source.add("GHI");
        source.addAll(1, Arrays.asList(new String[] { "D", "DE" }));
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "D",
                "DE",
                "DEF",
                "G",
                "GH",
                "GHI",
        });
    }


    public void testAddSubtreeForPlusSiblings() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABC");
        source.add("AWX");
        source.addAll(1, Arrays.asList(new String[] { "AE", "AEF", "AW" }));
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "AE",
                "AEF",
                "AW",
                "AWX",
        });
    }

    public void testInsertUpdateDeleteOnCollapsed() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABC");
        source.add("ABD");
        source.add("ABEF");
        treeList.setExpanded(1, false);
        source.add("ABEG");
        source.add(3, "ABH"); // to split siblings ABEF, ABEG
        source.remove(1);
        source.set(0, "ABI");
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
        });

        treeList.setExpanded(1, true);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABI",
                "ABE",
                "ABEF",
                "ABH",
                "ABE",
                "ABEG",
        });

        treeList.setExpanded(1, false);
        source.remove(2); // to join siblings ABEF, ABEG

        treeList.setExpanded(1, true);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABI",
                "ABE",
                "ABEF",
                "ABEG",
        });
    }

    public void testInsertVirtualParentsOnCollapsed() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABE");
        treeList.setExpanded(1, false);
        source.add("ABEFGH");

        treeList.setExpanded(1, true);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABE",
                "ABEF",
                "ABEFG",
                "ABEFGH",
        });
    }

    public void testReplaceHiddenVirtualParentWithReal() {
        EventList<String> source = new BasicEventList<String>();
        TreeList<String> treeList = new TreeList<String>(source, new CharacterTreeFormat());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("ABC");
        treeList.setExpanded(0, false);
        source.addAll(0, Arrays.asList(new String[] {
                "A",
                "AB",
        }));

        treeList.setExpanded(0, true);
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
        });
    }

    public void testSortingSource() {
        EventList<String> source = new BasicEventList<String>();
        SortedList<String> sortedList = new SortedList<String>(source, null);
        TreeList<String> treeList = new TreeList<String>(sortedList, new CharacterTreeFormat());
        ListConsistencyListener<String> listConsistencyListener = ListConsistencyListener.install(treeList);
        listConsistencyListener.setPreviousElementTracked(false);

        source.add("A");
        source.add("DEF");
        source.add("ABC");
        sortedList.setComparator(GlazedLists.comparableComparator());
        assertTreeStructure(treeList, new String[] {
                "A",
                "AB",
                "ABC",
                "D",
                "DE",
                "DEF",
        });

    }
}
