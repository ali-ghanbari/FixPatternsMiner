package edu.utdallas.fpm.commons;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.Assert.*;

public class TestUtil {
    @Test
    public void testProjectName1() {
        final File file = new File("/Users/ali/java-workspace/FixPatternsMiner/hdrepair-dataset/apache_tika/modifiedFiles/10/old/something.java");
        assertTrue(Util.computeProjectName(file, "pppp").equals("apache_tika"));
    }

    @Test
    public void testProjectName2() {
        final File file = new File("/media/disk6TV1/ali/larger-dataset/2011/V1/3379/buggy-version/something.java");
        assertTrue(Util.computeProjectName(file, "pp").equals("3379"));
    }

    @Test
    public void testProjectName3() {
        final File file = new File("/media/disk6TV1/ali/larger-dataset/2011/V1/3379/buggy-version/something.java");
        assertTrue(Util.computeProjectName(file, "").equals("something.java"));
    }
}
