package org.geogit.test.integration;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.util.ServiceLoader;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.RevCommit;
import org.geogit.api.hooks.CannotRunGeogitOperationException;
import org.geogit.api.hooks.CommandHook;
import org.geogit.api.hooks.Scripting;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class HooksTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
        File hooksFolder = new File(geogit.getPlatform().pwd(), ".geogit/hooks");
        File[] files = hooksFolder.listFiles();
        for (File file : files) {
            file.delete();
            assertFalse(file.exists());
        }
    }

    @Test
    public void testHookWithError() throws Exception {
        CharSequence wrongHookCode = "this is a syntactically wrong sentence";
        File hooksFolder = new File(geogit.getPlatform().pwd(), ".geogit/hooks");
        File commitPreHookFile = new File(hooksFolder, "pre_commit.js");

        Files.write(wrongHookCode, commitPreHookFile, Charsets.UTF_8);

        insertAndAdd(points1);
        try {
            geogit.command(CommitOp.class).setMessage("A message").call();
            fail();
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testHook() throws Exception {
        // a hook that only accepts commit messages longer with at least 4 words, and converts
        // message to lower case
        CharSequence commitPreHookCode = "exception = Packages.org.geogit.api.hooks.CannotRunGeogitOperationException;\n"
                + "msg = params.get(\"message\");\n"
                + "if (msg.length() < 30){\n"
                + "\tthrow new exception(\"Commit messages must have at least 30 characters\");\n}"
                + "params.put(\"message\", msg.toLowerCase());";

        File hooksFolder = new File(geogit.getPlatform().pwd(), ".geogit/hooks");
        File commitPreHookFile = new File(hooksFolder, "pre_commit.js");

        Files.write(commitPreHookCode, commitPreHookFile, Charsets.UTF_8);

        insertAndAdd(points1);
        try {
            geogit.command(CommitOp.class).setMessage("A short message").call();
            fail();
        } catch (Exception e) {
            String javaVersion = System.getProperty("java.version");
            // Rhino in jdk6 throws a different exception
            if (javaVersion.startsWith("1.6")) {
                String expected = "Script " + commitPreHookFile + " threw an exception";
                assertTrue(e.getMessage(), e.getMessage().contains(expected));
            } else {
                assertTrue(
                        e.getMessage(),
                        e.getMessage().startsWith(
                                "Commit messages must have at least 30 characters"));
            }
        }

        String longMessage = "THIS IS A LONG UPPERCASE COMMIT MESSAGE";
        RevCommit commit = geogit.command(CommitOp.class).setMessage(longMessage).call();
        assertEquals(longMessage.toLowerCase(), commit.getMessage());

    }

    @Test
    public void testExecutableScriptFileHook() throws Exception {
        File hooksFolder = new File(geogit.getPlatform().pwd(), ".geogit/hooks");
        File commitPreHookFile;
        String commitPreHookCode;
        // a hook that returns non-zero
        if (Scripting.isWindows()) {
            commitPreHookCode = "exit 1";
        } else {
            commitPreHookCode = "#!/bin/sh\nexit 1";
        }
        commitPreHookFile = new File(hooksFolder, "pre_commit.bat");
        Files.write(commitPreHookCode, commitPreHookFile, Charsets.UTF_8);
        commitPreHookFile.setExecutable(true);

        insertAndAdd(points1);
        try {
            geogit.command(CommitOp.class).setMessage("Message").call();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof CannotRunGeogitOperationException);
        }

        // a hook that returns zero
        if (Scripting.isWindows()) {
            commitPreHookCode = "exit 0";
        } else {
            commitPreHookCode = "#!/bin/sh\nexit 0";
        }
        commitPreHookFile = new File(hooksFolder, "pre_commit.bat");
        Files.write(commitPreHookCode, commitPreHookFile, Charsets.UTF_8);
        commitPreHookFile.setExecutable(true);

        geogit.command(CommitOp.class).setMessage("Message").call();

    }

    @Test
    public void testFailingPostPostProcessHook() throws Exception {
        CharSequence postHookCode = "exception = Packages.org.geogit.api.hooks.CannotRunGeogitOperationException;\n"
                + "throw new exception();";
        File hooksFolder = new File(geogit.getPlatform().pwd(), ".geogit/hooks");
        File commitPostHookFile = new File(hooksFolder, "post_commit.js");

        Files.write(postHookCode, commitPostHookFile, Charsets.UTF_8);

        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("A message").call();

    }

    @Test
    public void testClasspathHook() throws Exception {
        ClasspathHookTest.ENABLED = true;
        try {
            ClasspathHookTest.CAPTURE_CLASS = AddOp.class;
            insertAndAdd(points1);
            assertEquals(AddOp.class, ClasspathHookTest.PRE_OP.getClass());
            assertEquals(AddOp.class, ClasspathHookTest.POST_OP.getClass());
        } finally {
            ClasspathHookTest.reset();
        }
    }

    @Test
    public void testClasspathHookPreFail() throws Exception {
        ClasspathHookTest.ENABLED = true;
        try {
            ClasspathHookTest.PRE_FAIL = true;
            ClasspathHookTest.CAPTURE_CLASS = AddOp.class;
            try {
                insertAndAdd(points1);
                fail("Expected pre hook exception");
            } catch (CannotRunGeogitOperationException e) {
                assertEquals("expected", e.getMessage());
            }
            assertEquals(AddOp.class, ClasspathHookTest.PRE_OP.getClass());
            assertEquals(AddOp.class, ClasspathHookTest.PRE_OP.getClass());
        } finally {
            ClasspathHookTest.reset();
        }
    }

    @Test
    public void testClasspathHookPostFail() throws Exception {
        ClasspathHookTest.ENABLED = true;
        try {
            ClasspathHookTest.POST_FAIL = true;
            ClasspathHookTest.CAPTURE_CLASS = AddOp.class;

            insertAndAdd(points1);
            // post hook errors should not forbid the operation to return successfully
            assertTrue(ClasspathHookTest.POST_EXCEPTION_THROWN);

            assertEquals(AddOp.class, ClasspathHookTest.PRE_OP.getClass());
            assertEquals(AddOp.class, ClasspathHookTest.PRE_OP.getClass());
        } finally {
            ClasspathHookTest.reset();
        }
    }

    /**
     * This command hook is discoverable through the {@link ServiceLoader} SPI as there's a
     * {@code org.geogit.api.hooks.CommandHook} file in {@code src/test/resources/META-INF/services}
     * but the static ENABLED flag must be set by the test case for it to be run.
     */
    public static final class ClasspathHookTest implements CommandHook {
        private static boolean ENABLED = false;

        private static boolean PRE_FAIL = false;

        private static boolean POST_FAIL = false;

        private static boolean POST_EXCEPTION_THROWN;

        private static Class<? extends AbstractGeoGitOp> CAPTURE_CLASS = AbstractGeoGitOp.class;

        private static AbstractGeoGitOp<?> PRE_OP, POST_OP;

        private static void reset() {
            ENABLED = false;
            PRE_FAIL = false;
            POST_FAIL = false;
            CAPTURE_CLASS = AbstractGeoGitOp.class;
            PRE_OP = null;
            POST_OP = null;
            POST_EXCEPTION_THROWN = false;
        }

        @Override
        public boolean appliesTo(Class<? extends AbstractGeoGitOp> clazz) {
            boolean enabled = ENABLED;
            Class<? extends AbstractGeoGitOp> captureClass = CAPTURE_CLASS;
            checkNotNull(clazz);
            checkNotNull(captureClass);
            boolean applies = enabled && CAPTURE_CLASS.equals(clazz);
            return applies;
        }

        @Override
        public <C extends AbstractGeoGitOp<?>> C pre(C command)
                throws CannotRunGeogitOperationException {
            checkState(ENABLED);
            PRE_OP = command;
            if (PRE_FAIL) {
                throw new CannotRunGeogitOperationException("expected");
            }
            return command;
        }

        @Override
        public <T> T post(AbstractGeoGitOp<T> command, Object retVal, boolean success)
                throws Exception {
            checkState(ENABLED);
            POST_OP = command;
            if (POST_FAIL) {
                POST_EXCEPTION_THROWN = true;
                throw new RuntimeException("expected");
            }
            return (T) retVal;
        }

    }

}
