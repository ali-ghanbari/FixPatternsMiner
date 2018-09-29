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
package hudson.maven;

import hudson.FilePath;
import hudson.EnvVars;
import hudson.slaves.WorkspaceList;
import hudson.slaves.WorkspaceList.Lease;
import hudson.maven.agent.AbortException;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.Environment;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.model.Executor;
import hudson.remoting.Channel;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildWrapper;
import hudson.util.ArgumentListBuilder;
import org.apache.maven.BuildFailureException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.project.MavenProject;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link Run} for {@link MavenModule}.
 * 
 * @author Kohsuke Kawaguchi
 */
public class MavenBuild extends AbstractMavenBuild<MavenModule,MavenBuild> {
    /**
     * {@link MavenReporter}s that will contribute project actions.
     * Can be null if there's none.
     */
    /*package*/ List<MavenReporter> projectActionReporters;

    /**
     * {@link ExecutedMojo}s that record what was run.
     * Null until some time before the build completes,
     * or if this build is performed in earlier versions of Hudson.
     * @since 1.98.
     */
    private List<ExecutedMojo> executedMojos;

    public MavenBuild(MavenModule job) throws IOException {
        super(job);
    }

    public MavenBuild(MavenModule job, Calendar timestamp) {
        super(job, timestamp);
    }

    public MavenBuild(MavenModule project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    @Override
    public String getUpUrl() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if(req!=null) {
            List<Ancestor> ancs = req.getAncestors();
            for( int i=1; i<ancs.size(); i++) {
                if(ancs.get(i).getObject()==this) {
                    if(ancs.get(i-1).getObject() instanceof MavenModuleSetBuild) {
                        // if under MavenModuleSetBuild, "up" means MMSB
                        return ancs.get(i-1).getUrl()+'/';
                    }
                }
            }
        }
        return super.getUpUrl();
    }

    @Override
    public String getDisplayName() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if(req!=null) {
            List<Ancestor> ancs = req.getAncestors();
            for( int i=1; i<ancs.size(); i++) {
                if(ancs.get(i).getObject()==this) {
                    if(ancs.get(i-1).getObject() instanceof MavenModuleSetBuild) {
                        // if under MavenModuleSetBuild, display the module name
                        return getParent().getDisplayName();
                    }
                }
            }
        }
        return super.getDisplayName();
    }

    /**
     * Gets the {@link MavenModuleSetBuild} that has the same build number.
     *
     * @return
     *      null if no such build exists, which happens when the module build
     *      is manually triggered.
     * @see #getModuleSetBuild()
     */
    public MavenModuleSetBuild getParentBuild() {
        return getParent().getParent().getBuildByNumber(getNumber());
    }

    /**
     * Gets the "governing" {@link MavenModuleSet} that has set
     * the workspace for this build.
     *
     * @return
     *      null if no such build exists, which happens if the build
     *      is manually removed.
     * @see #getParentBuild()
     */
    public MavenModuleSetBuild getModuleSetBuild() {
        return getParent().getParent().getNearestOldBuild(getNumber());
    }

    @Override
    public ChangeLogSet<? extends Entry> getChangeSet() {
        return new FilteredChangeLogSet(this);
    }

    /**
     * We always get the changeset from {@link MavenModuleSetBuild}.
     */
    @Override
    public boolean hasChangeSetComputed() {
        return true;
    }

    /**
     * Exposes {@code MAVEN_OPTS} to forked processes.
     *
     * <p>
     * See {@link MavenModuleSetBuild#getEnvironment(TaskListener)}  for discussion.
     */
    @Override
    public EnvVars getEnvironment(TaskListener log) throws IOException, InterruptedException {
        EnvVars envs = super.getEnvironment(log);
        String opts = project.getParent().getMavenOpts();
        if(opts!=null)
            envs.put("MAVEN_OPTS", opts);
        return envs;
    }

    public void registerAsProjectAction(MavenReporter reporter) {
        if(projectActionReporters==null)
            projectActionReporters = new ArrayList<MavenReporter>();
        projectActionReporters.add(reporter);
    }

    public List<ExecutedMojo> getExecutedMojos() {
        if(executedMojos==null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(executedMojos);
    }
    
    @Override
    public void run() {
        run(new RunnerImpl());

        getProject().updateTransientActions();

        MavenModuleSetBuild parentBuild = getModuleSetBuild();
        if(parentBuild!=null)
            parentBuild.notifyModuleBuild(this);
    }

    /**
     * If the parent {@link MavenModuleSetBuild} is kept, keep this record, too.
     */
    @Override
    public String getWhyKeepLog() {
        MavenModuleSetBuild pb = getParentBuild();
        if(pb!=null && pb.getWhyKeepLog()!=null)
            return Messages.MavenBuild_KeptBecauseOfParent(pb);
        return super.getWhyKeepLog();
    }


    // used by executedMojos.jelly
    public static ExecutedMojo.Cache createExecutedMojoCache() {
        return new ExecutedMojo.Cache();
    }

    /**
     * Backdoor for {@link MavenModuleSetBuild} to assign workspaces for modules.
     */
    protected void setWorkspace(FilePath path) {
        super.setWorkspace(path);
    }

    /**
     * Runs Maven and builds the project.
     */
    private static final class Builder extends MavenBuilder {
        private final MavenBuildProxy buildProxy;
        private final MavenReporter[] reporters;

        /**
         * Records of what was executed.
         */
        private final List<ExecutedMojo> executedMojos = new ArrayList<ExecutedMojo>();

        private long startTime;

        public Builder(BuildListener listener,MavenBuildProxy buildProxy,MavenReporter[] reporters, List<String> goals, Map<String,String> systemProps) {
            super(listener,goals,systemProps);
            this.buildProxy = new FilterImpl(buildProxy);
            this.reporters = reporters;
        }

        private class FilterImpl extends MavenBuildProxy.Filter<MavenBuildProxy> implements Serializable {
            public FilterImpl(MavenBuildProxy buildProxy) {
                super(buildProxy);
            }

            public void executeAsync(final BuildCallable<?,?> program) throws IOException {
                futures.add(Channel.current().callAsync(new AsyncInvoker(core,program)));
            }

            private static final long serialVersionUID = 1L;
        }

        @Override
        void preBuild(MavenSession session, ReactorManager rm, EventDispatcher dispatcher) throws BuildFailureException, LifecycleExecutionException, IOException, InterruptedException {
            for (MavenReporter r : reporters)
                r.preBuild(buildProxy,rm.getTopLevelProject(),listener);
        }

        @Override
        void postBuild(MavenSession session, ReactorManager rm, EventDispatcher dispatcher) throws BuildFailureException, LifecycleExecutionException, IOException, InterruptedException {
            buildProxy.setExecutedMojos(executedMojos);
            for (MavenReporter r : reporters)
                r.postBuild(buildProxy,rm.getTopLevelProject(),listener);
        }

        @Override
        void preExecute(MavenProject project, MojoInfo info) throws IOException, InterruptedException, AbortException {
            for (MavenReporter r : reporters)
                if(!r.preExecute(buildProxy,project,info,listener))
                    throw new AbortException(r+" failed");

            startTime = System.currentTimeMillis();
        }

        @Override
        void postExecute(MavenProject project, MojoInfo info, Exception exception) throws IOException, InterruptedException, AbortException {
            executedMojos.add(new ExecutedMojo(info,System.currentTimeMillis()-startTime));

            for (MavenReporter r : reporters)
                if(!r.postExecute(buildProxy,project,info,listener,exception))
                    throw new AbortException(r+" failed");
        }

        @Override
        void onReportGenerated(MavenProject project, MavenReportInfo report) throws IOException, InterruptedException, AbortException {
            for (MavenReporter r : reporters)
                if(!r.reportGenerated(buildProxy,project,report,listener))
                    throw new AbortException(r+" failed");
        }

        @Override
        void preModule(MavenProject project) throws InterruptedException, IOException, AbortException {
            for (MavenReporter r : reporters)
                if(!r.enterModule(buildProxy,project,listener))
                    throw new AbortException(r+" failed");
        }

        @Override
        void postModule(MavenProject project) throws InterruptedException, IOException, AbortException {
            for (MavenReporter r : reporters)
                if(!r.leaveModule(buildProxy,project,listener))
                    throw new AbortException(r+" failed");
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * {@link MavenBuildProxy} implementation.
     */
    class ProxyImpl implements MavenBuildProxy, Serializable {
        public <V, T extends Throwable> V execute(BuildCallable<V, T> program) throws T, IOException, InterruptedException {
            return program.call(MavenBuild.this);
        }

        /**
         * This method is implemented by the remote proxy before the invocation
         * gets to this. So correct code shouldn't be invoking this method on the master ever.
         *
         * @deprecated
         *      This helps IDE find coding mistakes when someone tries to call this method.
         */
        public final void executeAsync(BuildCallable<?,?> program) throws IOException {
            throw new AssertionError();
        }

        public FilePath getRootDir() {
            return new FilePath(MavenBuild.this.getRootDir());
        }

        public FilePath getProjectRootDir() {
            return new FilePath(MavenBuild.this.getParent().getRootDir());
        }

        public FilePath getModuleSetRootDir() {
            return new FilePath(MavenBuild.this.getParent().getParent().getRootDir());
        }

        public FilePath getArtifactsDir() {
            return new FilePath(MavenBuild.this.getArtifactsDir());
        }

        public void setResult(Result result) {
            MavenBuild.this.setResult(result);
        }

        public Calendar getTimestamp() {
            return MavenBuild.this.getTimestamp();
        }

        public long getMilliSecsSinceBuildStart() {
            return System.currentTimeMillis()-getTimestamp().getTimeInMillis();
        }

        public void registerAsProjectAction(MavenReporter reporter) {
            MavenBuild.this.registerAsProjectAction(reporter);
        }

        public void registerAsAggregatedProjectAction(MavenReporter reporter) {
            MavenModuleSetBuild pb = getParentBuild();
            if(pb!=null)
                pb.registerAsProjectAction(reporter);
        }

        public void setExecutedMojos(List<ExecutedMojo> executedMojos) {
            MavenBuild.this.executedMojos = executedMojos;
        }

        private Object writeReplace() {
            return Channel.current().export(MavenBuildProxy.class,this);
        }
    }

    class ProxyImpl2 extends ProxyImpl implements MavenBuildProxy2 {
        private final SplittableBuildListener listener;
        long startTime;
        private final OutputStream log;
        private final MavenModuleSetBuild parentBuild;

        ProxyImpl2(MavenModuleSetBuild parentBuild,SplittableBuildListener listener) throws FileNotFoundException {
            this.parentBuild = parentBuild;
            this.listener = listener;
            log = new FileOutputStream(getLogFile()); // no buffering so that AJAX clients can see the log live
        }

        public void start() {
            onStartBuilding();
            startTime = System.currentTimeMillis();
            try {
                listener.setSideOutputStream(log);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void end() {
            if(result==null)
                setResult(Result.SUCCESS);
            onEndBuilding();
            duration = System.currentTimeMillis()- startTime;
            parentBuild.notifyModuleBuild(MavenBuild.this);
            try {
                listener.setSideOutputStream(null);
                save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Sends the accumuldated log in {@link SplittableBuildListener} to the log of this build.
         */
        public void appendLastLog() {
            try {
                listener.setSideOutputStream(log);
                listener.setSideOutputStream(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Performs final clean up. Invoked after the entire aggregator build is completed.
         */
        protected void close() {
            try {
                log.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(hasntStartedYet()) {
                // Mark the build as aborted. This method is used when the aggregated build
                // failed before it didn't even get to this module.
                run(new Runner() {
                    public Result run(BuildListener listener) {
                        listener.getLogger().println(Messages.MavenBuild_FailedEarlier());
                        return Result.NOT_BUILT;
                    }

                    public void post(BuildListener listener) {
                    }

                    public void cleanUp(BuildListener listener) {
                    }
                });
            }
        }

        /**
         * Gets the build for which this proxy is created.
         */
        public MavenBuild owner() {
            return MavenBuild.this;
        }

        private Object writeReplace() {
            // when called from remote, methods need to be executed in the proper Executor's context.
            return Channel.current().export(MavenBuildProxy2.class,
                Executor.currentExecutor().newImpersonatingProxy(MavenBuildProxy2.class,this));
        }
    }

    private class RunnerImpl extends AbstractRunner {
        private List<MavenReporter> reporters;

        @Override
        protected Lease decideWorkspace(Node n, WorkspaceList wsl) throws InterruptedException, IOException {
            return wsl.allocate(getModuleSetBuild().getModuleRoot().child(getProject().getRelativePath()));
        }

        protected Result doRun(BuildListener listener) throws Exception {
            // pick up a list of reporters to run
            reporters = getProject().createReporters();
            MavenModuleSet mms = getProject().getParent();
            if(debug)
                listener.getLogger().println("Reporters="+reporters);

            buildEnvironments = new ArrayList<Environment>();
            for (BuildWrapper w : mms.getBuildWrappers()) {
                Environment e = w.setUp(MavenBuild.this, launcher, listener);
                if (e == null) {
                    return Result.FAILURE;
                }
                buildEnvironments.add(e);
            }

            EnvVars envVars = getEnvironment(listener); // buildEnvironments should be set up first
            
            ProcessCache.MavenProcess process = mavenProcessCache.get(launcher.getChannel(), listener,
                new MavenProcessFactory(getParent().getParent(),launcher,envVars,null));

            ArgumentListBuilder margs = new ArgumentListBuilder("-N","-B");
            if(mms.usesPrivateRepository())
                // use the per-project repository. should it be per-module? But that would cost too much in terms of disk
                // the workspace must be on this node, so getRemote() is safe.
                margs.add("-Dmaven.repo.local="+getWorkspace().child(".repository").getRemote());
            margs.add("-f",getModuleRoot().child("pom.xml").getRemote());
            margs.addTokenized(getProject().getGoals());

            Map<String,String> systemProps = new HashMap<String, String>(envVars);
            // backward compatibility
            systemProps.put("hudson.build.number",String.valueOf(getNumber()));

            boolean normalExit = false;
            try {
                Result r = process.call(new Builder(
                    listener,new ProxyImpl(),
                    reporters.toArray(new MavenReporter[reporters.size()]), margs.toList(), systemProps));
                normalExit = true;
                return r;
            } finally {
                if(normalExit)  process.recycle();
                else            process.discard();

                // tear down in reverse order
                boolean failed=false;
                for( int i=buildEnvironments.size()-1; i>=0; i-- ) {
                    if (!buildEnvironments.get(i).tearDown(MavenBuild.this,listener)) {
                        failed=true;
                    }                    
                }
                buildEnvironments = null;
                // WARNING The return in the finally clause will trump any return before
                if (failed) return Result.FAILURE;
            }
        }

        public void post2(BuildListener listener) throws Exception {
            for (MavenReporter reporter : reporters)
                reporter.end(MavenBuild.this,launcher,listener);
        }

        public void cleanUp(BuildListener listener) throws Exception {
            if(getResult().isBetterOrEqualTo(Result.SUCCESS))
                scheduleDownstreamBuilds(listener);
        }
    }

    private static final int MAX_PROCESS_CACHE = 5;

    protected static final ProcessCache mavenProcessCache = new ProcessCache(MAX_PROCESS_CACHE);

    /**
     * Set true to produce debug output.
     */
    public static boolean debug = false;
    
    @Override
    public MavenModule getParent() {// don't know why, but javac wants this
        return super.getParent();
    }
}
