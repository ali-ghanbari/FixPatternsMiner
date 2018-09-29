/*
 * @author max
 */
package org.jetbrains.jet.cfg;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBase;
import org.jetbrains.jet.lang.cfg.LoopInfo;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class JetControlFlowTest extends JetTestCaseBase {
    static {
        System.setProperty("idea.platform.prefix", "Idea");
    }

    public JetControlFlowTest(String dataPath, String name) {
        super(dataPath, name);
    }

    @Override
    protected void runTest() throws Throwable {
        configureByFile(getTestFilePath());
        JetFile file = (JetFile) getFile();

        final Map<JetElement, Pseudocode> data = new LinkedHashMap<JetElement, Pseudocode>();
        final JetPseudocodeTrace pseudocodeTrace = new JetPseudocodeTrace() {

            @Override
            public void recordControlFlowData(@NotNull JetElement element, @NotNull Pseudocode pseudocode) {
                data.put(element, pseudocode);
            }

            @Override
            public void close() {
                for (Pseudocode pseudocode : data.values()) {
                    pseudocode.postProcess();
                }
            }

            @Override
            public void recordLoopInfo(JetExpression expression, LoopInfo blockInfo) {
            }

            @Override
            public void recordRepresentativeInstruction(@NotNull JetElement element, @NotNull Instruction instruction) {
            }

        };

        AnalyzerFacade.analyzeNamespace(file.getRootNamespace(), new JetControlFlowDataTraceFactory() {
            @NotNull
            @Override
            public JetPseudocodeTrace createTrace(JetElement element) {
                return pseudocodeTrace;
            }
        });

        try {
            processCFData(name, data);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if ("true".equals(System.getProperty("jet.control.flow.test.dump.graphs"))) {
                dumpDot(name, data.values());
            }
        }
    }

    private void processCFData(String name, Map<JetElement, Pseudocode> data) throws IOException {
        Collection<Pseudocode> pseudocodes = data.values();

        StringBuilder instructionDump = new StringBuilder();
        int i = 0;
        for (Pseudocode pseudocode : pseudocodes) {
            JetElement correspondingElement = pseudocode.getCorrespondingElement();
            String label;
            assert correspondingElement instanceof JetNamedDeclaration;
            if (correspondingElement instanceof JetFunctionLiteral) {
                label = "anonymous_" + i++;
            }
            else {
                JetNamedDeclaration namedDeclaration = (JetNamedDeclaration) correspondingElement;
                label = namedDeclaration.getName();
            }

            instructionDump.append("== ").append(label).append(" ==\n");

            instructionDump.append(correspondingElement.getText());
            instructionDump.append("\n---------------------\n");
            dumpInstructions(pseudocode, instructionDump);
            instructionDump.append("=====================\n");
            
            //check edges directions
            Collection<Instruction> instructions = pseudocode.getInstructions();
            for (Instruction instruction : instructions) {
                if (!((InstructionImpl) instruction).isDead()) {
                    for (Instruction nextInstruction : instruction.getNextInstructions()) {
                        assertTrue("instruction '" + instruction + "' has '" + nextInstruction + "' among next instructions list, but not vice versa",
                                   nextInstruction.getPreviousInstructions().contains(instruction));
                    }
                    for (Instruction prevInstruction : instruction.getPreviousInstructions()) {
                        assertTrue("instruction '" + instruction + "' has '" + prevInstruction + "' among previous instructions list, but not vice versa",
                                   prevInstruction.getNextInstructions().contains(instruction));
                    }
                }
            }
        }

        String expectedInstructionsFileName = getTestDataPath() + "/" + getTestFilePath().replace(".jet", ".instructions");
        File expectedInstructionsFile = new File(expectedInstructionsFileName);
        if (!expectedInstructionsFile.exists()) {
            FileUtil.writeToFile(expectedInstructionsFile, instructionDump.toString());
            fail("No expected instructions for " + name + " generated result is written into " + expectedInstructionsFileName);
        }
        String expectedInstructions = StringUtil.convertLineSeparators(FileUtil.loadFile(expectedInstructionsFile));

        assertEquals(expectedInstructions, instructionDump.toString());

//                        StringBuilder graphDump = new StringBuilder();
//                        for (Pseudocode pseudocode : pseudocodes) {
//                            topOrderDump(pseudocode.)
//                        }
    }

    public void dfsDump(Pseudocode pseudocode, StringBuilder nodes, StringBuilder edges, Map<Instruction, String> nodeNames) {
        dfsDump(nodes, edges, pseudocode.getInstructions().get(0), nodeNames);
    }

    private void dfsDump(StringBuilder nodes, StringBuilder edges, Instruction instruction, Map<Instruction, String> nodeNames) {
        if (nodeNames.containsKey(instruction)) return;
        String name = "n" + nodeNames.size();
        nodeNames.put(instruction, name);
        nodes.append(name).append(" := ").append(renderName(instruction));

    }

    private String renderName(Instruction instruction) {
        throw new UnsupportedOperationException(); // TODO
    }

    private static String formatInstruction(Instruction instruction, int maxLength) {
        String[] parts = instruction.toString().split("\n");
        if (parts.length == 1) {
            return "    " + String.format("%1$-" + maxLength + "s", instruction);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, partsLength = parts.length; i < partsLength; i++) {
            String part = parts[i];
            sb.append("    ").append(String.format("%1$-" + maxLength + "s", part));
            if (i < partsLength - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private static String formatInstructionList(Collection<Instruction> instructions) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Iterator<Instruction> iterator = instructions.iterator(); iterator.hasNext(); ) {
            Instruction instruction = iterator.next();
            String instructionText = instruction.toString();
            String[] parts = instructionText.split("\n");
            if (parts.length > 1) {
                StringBuilder instructionSb = new StringBuilder();
                for (String part : parts) {
                    instructionSb.append(part.trim()).append(' ');
                }
                if (instructionSb.toString().length() > 30) {
                    sb.append(instructionSb.substring(0, 28)).append("..)");
                }
                else {
                    sb.append(instructionSb);
                }
            }
            else {
                sb.append(instruction);
            }
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb.toString();
    }

    public void dumpInstructions(Pseudocode pseudocode, @NotNull StringBuilder out) {
        List<Instruction> instructions = pseudocode.getInstructions();
        List<Pseudocode.PseudocodeLabel> labels = pseudocode.getLabels();
        List<Pseudocode> locals = new ArrayList<Pseudocode>();
        int maxLength = 0;
        int maxNextLength = 0;
        for (Instruction instruction : instructions) {
            String instuctionText = instruction.toString();
            if (instuctionText.length() > maxLength) {
                String[] parts = instuctionText.split("\n");
                if (parts.length > 1) {
                    for (String part : parts) {
                        if (part.length() > maxLength) {
                            maxLength = part.length();
                        }
                    }
                }
                else {
                    maxLength = instuctionText.length();
                }
            }
            String instructionListText = formatInstructionList(instruction.getNextInstructions());
            if (instructionListText.length() > maxNextLength) {
                maxNextLength = instructionListText.length();
            }
        }
        for (int i = 0, instructionsSize = instructions.size(); i < instructionsSize; i++) {
            Instruction instruction = instructions.get(i);
            if (instruction instanceof LocalDeclarationInstruction) {
                LocalDeclarationInstruction localDeclarationInstruction = (LocalDeclarationInstruction) instruction;
                locals.add(localDeclarationInstruction.getBody());
            }
            for (Pseudocode.PseudocodeLabel label: labels) {
                if (label.getTargetInstructionIndex() == i) {
                    out.append(label.getName()).append(":\n");
                }
            }

            out.append(formatInstruction(instruction, maxLength)).
                    append("    NEXT:").append(String.format("%1$-" + maxNextLength + "s", formatInstructionList(instruction.getNextInstructions()))).
                    append("    PREV:").append(formatInstructionList(instruction.getPreviousInstructions())).append("\n");
        }
        for (Pseudocode local : locals) {
            dumpInstructions(local, out);
        }
    }

    public void dumpEdges(List<Instruction> instructions,  final PrintStream out, final int[] count, final Map<Instruction, String> nodeToName) {
        for (final Instruction fromInst : instructions) {
            fromInst.accept(new InstructionVisitor() {
                @Override
                public void visitFunctionLiteralValue(LocalDeclarationInstruction instruction) {
                    int index = count[0];
//                    instruction.getBody().dumpSubgraph(out, "subgraph cluster_" + index, count, "color=blue;\nlabel = \"f" + index + "\";", nodeToName);
                    printEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getBody().getInstructions().get(0)), null);
                    visitInstructionWithNext(instruction);
                }

                @Override
                public void visitUnconditionalJump(UnconditionalJumpInstruction instruction) {
                    // Nothing
                }

                @Override
                public void visitJump(AbstractJumpInstruction instruction) {
                    printEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getResolvedTarget()), null);
                }

                @Override
                public void visitNondeterministicJump(NondeterministicJumpInstruction instruction) {
                    //todo print edges
                    visitInstruction(instruction);
                    printEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getNext()), null);
                }

                @Override
                public void visitReturnValue(ReturnValueInstruction instruction) {
                    super.visitReturnValue(instruction);
                }

                @Override
                public void visitReturnNoValue(ReturnNoValueInstruction instruction) {
                    super.visitReturnNoValue(instruction);
                }

                @Override
                public void visitConditionalJump(ConditionalJumpInstruction instruction) {
                    String from = nodeToName.get(instruction);
                    printEdge(out, from, nodeToName.get(instruction.getNextOnFalse()), "no");
                    printEdge(out, from, nodeToName.get(instruction.getNextOnTrue()), "yes");
                }

                @Override
                public void visitInstructionWithNext(InstructionWithNext instruction) {
                    printEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getNext()), null);
                }

                @Override
                public void visitSubroutineExit(SubroutineExitInstruction instruction) {
                    // Nothing
                }

                @Override
                public void visitInstruction(Instruction instruction) {
                    throw new UnsupportedOperationException(instruction.toString());
                }
            });
        }
    }

    public void dumpNodes(List<Instruction> instructions, PrintStream out, int[] count, Map<Instruction, String> nodeToName) {
        for (Instruction node : instructions) {
            if (node instanceof UnconditionalJumpInstruction) {
                continue;
            }
            String name = "n" + count[0]++;
            nodeToName.put(node, name);
            String text = node.toString();
            int newline = text.indexOf("\n");
            if (newline >= 0) {
                text = text.substring(0, newline);
            }
            String shape = "box";
            if (node instanceof ConditionalJumpInstruction) {
                shape = "diamond";
            }
            else if (node instanceof NondeterministicJumpInstruction) {
                shape = "Mdiamond";
            }
            else if (node instanceof UnsupportedElementInstruction) {
                shape = "box, fillcolor=red, style=filled";
            }
            else if (node instanceof LocalDeclarationInstruction) {
                shape = "Mcircle";
            }
            else if (node instanceof SubroutineEnterInstruction || node instanceof SubroutineExitInstruction) {
                shape = "roundrect, style=rounded";
            }
            out.println(name + "[label=\"" + text + "\", shape=" + shape + "];");
        }
    }

    private void printEdge(PrintStream out, String from, String to, String label) {
        if (label != null) {
            label = "[label=\"" + label + "\"]";
        }
        else {
            label = "";
        }
        out.println(from + " -> " + to + label + ";");
    }

    private void dumpDot(String name, Collection<Pseudocode> pseudocodes) throws FileNotFoundException {
        String graphFileName = getTestDataPath() + "/" + getTestFilePath().replace(".jet", ".dot");
        File target = new File(graphFileName);

        PrintStream out = new PrintStream(target);

        out.println("digraph " + name + " {");
        int[] count = new int[1];
        Map<Instruction, String> nodeToName = new HashMap<Instruction, String>();
        for (Pseudocode pseudocode : pseudocodes) {
            dumpNodes(pseudocode.getInstructions(), out, count, nodeToName);
        }
        int i = 0;
        for (Pseudocode pseudocode : pseudocodes) {
            String label;
            JetElement correspondingElement = pseudocode.getCorrespondingElement();
            if (correspondingElement instanceof JetNamedDeclaration) {
                JetNamedDeclaration namedDeclaration = (JetNamedDeclaration) correspondingElement;
                label = namedDeclaration.getName();
            }
            else {
                label = "anonymous_" + i;
            }
            out.println("subgraph cluster_" + i + " {\n" +
                        "label=\"" + label + "\";\n" +
                        "color=blue;\n");
            dumpEdges(pseudocode.getInstructions(), out, count, nodeToName);
            out.println("}");
            i++;
        }
        out.println("}");
        out.close();
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JetTestCaseBase.suiteForDirectory(getTestDataPathBase(), "/cfg/", true, new JetTestCaseBase.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return new JetControlFlowTest(dataPath, name);
            }
        }));
        return suite;
    }

}
