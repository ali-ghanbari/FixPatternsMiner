/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.triggers;

import hudson.FilePath;
import hudson.Launcher;
import hudson.util.OneShotEvent;
import hudson.util.StreamTaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.scm.NullSCM;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

/**
 * @author Alan Harder
 */
public class SCMTriggerTest extends HudsonTestCase {
    /**
     * Make sure that SCMTrigger doesn't trigger another build when a build has just started,
     * but not yet completed its SCM update.
     */
    @Bug(2671)
    public void testSimultaneousPollAndBuild() throws Exception {
        FreeStyleProject p = createFreeStyleProject();

        // used to coordinate polling and check out
        final OneShotEvent checkoutStarted = new OneShotEvent();

        p.setScm(new TestSCM(checkoutStarted));

        Future<FreeStyleBuild> build = p.scheduleBuild2(0, new Cause.UserCause());
        checkoutStarted.block();
        assertFalse("SCM-poll after build has started should wait until that build finishes SCM-update", p.pollSCMChanges(new StreamTaskListener(System.out)));
        build.get();  // let mock build finish
    }

    private static class TestSCM extends NullSCM {
        private volatile int myRev = 1;
        private final OneShotEvent checkoutStarted;

        public TestSCM(OneShotEvent checkoutStarted) {
            this.checkoutStarted = checkoutStarted;
        }

        @Override synchronized
        public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath dir, TaskListener listener) throws IOException {
            return myRev < 2;
        }

        @Override
        public boolean checkout(AbstractBuild<?,?> build, Launcher launcher, FilePath remoteDir, BuildListener listener, File changeLogFile) throws IOException, InterruptedException {
            checkoutStarted.signal();
            Thread.sleep(400);  // processing time for mock update
            synchronized (this) { if (myRev < 2) myRev = 2; }
            return super.checkout(build, launcher, remoteDir, listener, changeLogFile);
        }
    }
}
