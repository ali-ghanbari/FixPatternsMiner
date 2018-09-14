package org.mudebug.fpm.main;

import java.util.List;

import org.mudebug.fpm.commons.Crawler;
import org.mudebug.fpm.commons.FilePair;

import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;

public class Main {

    public static void main(String[] args) {
        final Crawler c = new Crawler("/home/selab/New FixRuleMiner/FixRuleMiner/dataset/hdrepair-dataset");
        int fakeBugs = 0;
        for (final FilePair fp : c.ls()) {
            final AstComparator ac = new AstComparator();
            try {
                System.out.println("Diffing:");
                System.out.printf("\t%s%n\t%s%n", fp.getBuggy().getAbsolutePath(), fp.getFixed().getAbsolutePath());
                final Diff diff = ac.compare(fp.getBuggy(), fp.getFixed());
                final List<Operation> ops = diff.getRootOperations();
                if (ops.isEmpty()) {
                    fakeBugs++;
                } else {

                }
            } catch (Exception e) {
                System.err.println("OOPS! Something went wrong: " + e.getMessage());
                System.out.println("Warning: ignored \n" + fp.toString());
            }
        }
        System.out.println("Number of fake bugs: " + fakeBugs);
    }

}
