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
        try (PrintWriter pw = new PrintWriter(filesList)) {
            final ClassLoader cl = getClass().getClassLoader();
            final String buggyFileName = (new File("buggy", "a.java")).getPath();
            final File buggyVersion = Paths.get(cl.getResource("buggy/a.java").toURI()).toFile();
            final String fixedFileName = (new File("fixed", "a.java")).getPath();
            final File fixedVersion = Paths.get(cl.getResource(fixedFileName).toURI()).toFile();
            pw.printf("%s,%s%n", buggyVersion.getAbsolutePath(), fixedVersion.getAbsolutePath());
        }
    }

    @Test
    public void acceptanceTest() {
        Main.main(new String[] {this.filesList.getAbsolutePath()});
    }
}