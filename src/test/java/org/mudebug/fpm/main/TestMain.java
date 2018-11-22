package org.mudebug.fpm.main;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class TestMain {
    private File filesList;

    @BeforeEach
    public void initFilesList() throws Exception {
        filesList = File.createTempFile("filesList", "csv");
        final String[] fileNames = new String[] {
                "BOD.java",
                "CaseRemoval.java",
                "NakedMethodCall.java",
                "BinOpMeth.java",
                "FieldMethod.java",
                "UnaryOpMeth.java",
                "IfThenBranch.java",
                "IfElseBranch.java",
                "SimpleIfThen.java",
                "ArgPropagation.java",
                "FieldLocalWrite.java",
                "LocalMethod.java",
                "CaseBreakerBrk.java",
                "CaseBreakerRet.java",
                "InvertNegsAddNeg.java",
                "InvertNegsDelNeg.java",
                "FieldInitRemoval.java",
                "COTF.java",
                "LocalFieldRead.java",
                "LocalFieldWrite.java",
                "DerefGuard.java",
                "RetMethodGuard.java",
                "MethGuard.java",
                "Postconditions.java",
                "MethodLocal.java",
                "MethodToField.java",
                "FieldInitRemoval.java",
                "Preconditions.java",
                "RetDerefGuard.java",
                "ArgList.java",
                "BOR.java",
                "ConstantReplacement.java",
                "CtorName.java",
                "FieldName.java",
                "LocalName.java",
                "MethodName.java",
                "UnaryOp.java",
                "FieldToMethod.java",
                "FieldLocal.java",
                "SimpleMethCallGuard.java",
                "IncrementsRemoval.java",
                "VoidMethCallRemoval.java",
                "IfRemoval.java",
                "Constantify.java"
                //,
                //"SwitchCase.java"
        };
        try (PrintWriter pw = new PrintWriter(filesList)) {
            final ClassLoader cl = getClass().getClassLoader();
            for (final String fileName : fileNames) {
                final String buggyFileName = (new File("buggy", fileName)).getPath();
                final File buggyVersion = Paths.get(cl.getResource(buggyFileName).toURI()).toFile();
                final String fixedFileName = (new File("fixed", fileName)).getPath();
                final File fixedVersion = Paths.get(cl.getResource(fixedFileName).toURI()).toFile();
                pw.printf("%s,%s%n", buggyVersion.getAbsolutePath(), fixedVersion.getAbsolutePath());
            }
        }
    }

    @Test
    public void acceptanceTest() {
        Main.main(new String[] {this.filesList.getAbsolutePath()});
    }
}