//   Copyright 2013 Palantir Technologies
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
package com.palantir.stash.stashbot.managers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;

import com.atlassian.stash.hook.repository.RepositoryHookService;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;
import com.atlassian.stash.util.Page;
import com.atlassian.stash.util.PageRequest;
import com.atlassian.stash.util.PageRequestImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;
import com.palantir.stash.stashbot.config.ConfigurationPersistenceManager;
import com.palantir.stash.stashbot.config.JenkinsServerConfiguration;
import com.palantir.stash.stashbot.config.RepositoryConfiguration;
import com.palantir.stash.stashbot.jenkins.JenkinsJobXmlFormatter;
import com.palantir.stash.stashbot.jenkins.JenkinsJobXmlFormatter.JenkinsBuildParam;
import com.palantir.stash.stashbot.logger.StashbotLoggerFactory;

public class JenkinsManager {

    // NOTE: this is the key used in the atlassian-plugin.xml
    private static final String TRIGGER_JENKINS_BUILD_HOOK_KEY =
        "com.palantir.stash.stashbot:triggerJenkinsBuildHook";

    // Tacking this onto the end of the build command makes it print out "BUILD SUCCESS0" on success and
    // "BUILD FAILURE1" on failure.
    private static final String BUILD_COMMAND_POSTFIX =
        "&& echo \"BUILD SUCCESS$?\" || /bin/false || (echo \"BUILD FAILURE$?\" && /bin/false)";

    private final NavBuilder navBuilder;
    private final ConfigurationPersistenceManager cpm;
    private final JenkinsJobXmlFormatter xmlFormatter;
    private final JenkinsClientManager jenkinsClientManager;
    private final RepositoryService repositoryService;
    private final RepositoryHookService rhs;
    private final Logger log;
    private final StashbotLoggerFactory lf;

    public JenkinsManager(NavBuilder navBuilder, RepositoryService repositoryService, RepositoryHookService rhs,
        ConfigurationPersistenceManager cpm, JenkinsJobXmlFormatter xmlFormatter,
        JenkinsClientManager jenkisnClientManager, StashbotLoggerFactory lf) {
        this.navBuilder = navBuilder;
        this.repositoryService = repositoryService;
        this.rhs = rhs;
        this.cpm = cpm;
        this.xmlFormatter = xmlFormatter;
        this.jenkinsClientManager = jenkisnClientManager;
        this.lf = lf;
        this.log = lf.getLoggerForThis(this);
    }

    public void updateRepo(Repository repo) {
        try {
            Callable<Void> visit = new UpdateAllRepositoryVisitor(rhs, jenkinsClientManager, cpm, repo, lf);
            visit.call();
        } catch (Exception e) {
            log.error("Exception while attempting to create missing jobs for a repo: ", e);
        }
    }

    public void createJob(Repository repo, JenkinsBuildTypes buildType) {
        try {
            final RepositoryConfiguration rc = cpm.getRepositoryConfigurationForRepository(repo);
            final JenkinsServerConfiguration jsc = cpm.getJenkinsServerConfiguration(rc.getJenkinsServerName());
            final JenkinsServer jenkinsServer = jenkinsClientManager.getJenkinsServer(jsc, rc);
            final String jobName = buildType.getBuildNameFor(repo);

            // If we try to create a job which already exists, we still get a 200... so we should check first to make
            // sure it doesn't already exist
            Map<String, Job> jobMap = jenkinsServer.getJobs();

            if (jobMap.containsKey(jobName)) {
                throw new IllegalArgumentException("Job " + jobName + " already exists");
            }

            String xml = calculateXml(repo, buildType, jsc, rc);

            log.trace("Sending XML to jenkins to create job: " + xml);
            jenkinsServer.createJob(buildType.getBuildNameFor(repo), xml);
        } catch (IOException e) {
            // TODO: something other than just rethrow?
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String calculateXml(Repository repo, JenkinsBuildTypes buildType, final JenkinsServerConfiguration jsc,
        final RepositoryConfiguration rc) {
        String repositoryUrl = navBuilder.repo(repo).clone(repo.getScmId()).buildAbsoluteWithoutUsername();
        // manually insert the username and pw we are configured to use
        repositoryUrl =
            repositoryUrl.replace("://", "://" + jsc.getStashUsername() + ":" + jsc.getStashPassword() + "@");

        String startedCommand = "/usr/bin/curl -s -i " + buildUrl(repositoryUrl, jsc, "inprogress");
        String successCommand = "/usr/bin/curl -s -i " + buildUrl(repositoryUrl, jsc, "successful");
        String failedCommand = "/usr/bin/curl -s -i " + buildUrl(repositoryUrl, jsc, "failed");

        String prebuildCommand = rc.getPrebuildCommand();

        String buildCommand;
        if (buildType == JenkinsBuildTypes.PUBLISH) {
            buildCommand = rc.getPublishBuildCommand();
        } else if (buildType == JenkinsBuildTypes.VERIFICATION) {
            buildCommand = rc.getVerifyBuildCommand();
        } else {
            buildCommand = "/bin/true";
        }

        buildCommand = "(" + buildCommand + ") " + BUILD_COMMAND_POSTFIX;

        // URL looks like: "BASE_URL/REPO_ID/TYPE/STATE/BUILD_NUMBER/BUILD_HEAD[/MERGE_HEAD/PULLREQUEST_ID]";
        Collection<JenkinsBuildParam> params =
            ImmutableList.<JenkinsBuildParam> of(
                new JenkinsBuildParam("repoId",
                    JenkinsJobXmlFormatter.JenkinsBuildParamType.StringParameterDefinition, "stash repository Id",
                    "unknown"),
                new JenkinsBuildParam("type",
                    JenkinsJobXmlFormatter.JenkinsBuildParamType.StringParameterDefinition, "build type",
                    "unknown"),
                new JenkinsBuildParam("buildHead",
                    JenkinsJobXmlFormatter.JenkinsBuildParamType.StringParameterDefinition, "the change to build",
                    "head"),
                new JenkinsBuildParam("mergeHead",
                    JenkinsJobXmlFormatter.JenkinsBuildParamType.StringParameterDefinition,
                    "branch to merge in before build", ""),
                new JenkinsBuildParam("pullRequestId",
                    JenkinsJobXmlFormatter.JenkinsBuildParamType.StringParameterDefinition,
                    "stash pull request id", ""));

        String repositoryLink = navBuilder.repo(repo).browse().buildAbsolute();
        String repositoryName = repo.getProject().getName() + " " + repo.getName();
        String xml =
            xmlFormatter.getJobXml(repositoryUrl, prebuildCommand, buildCommand, startedCommand, successCommand,
                failedCommand, repositoryLink, repositoryName, params);
        return xml;
    }

    /**
     * This method IGNORES the current job XML, and regenerates it from scratch, and posts it. If any changes were made
     * to the job directly via jenkins UI, this will overwrite those changes.
     * 
     * @param repo
     * @param buildType
     */
    public void updateJob(Repository repo, JenkinsBuildTypes buildType) {
        try {
            final RepositoryConfiguration rc = cpm.getRepositoryConfigurationForRepository(repo);
            final JenkinsServerConfiguration jsc = cpm.getJenkinsServerConfiguration(rc.getJenkinsServerName());
            final JenkinsServer jenkinsServer = jenkinsClientManager.getJenkinsServer(jsc, rc);
            final String jobName = buildType.getBuildNameFor(repo);

            // If we try to create a job which already exists, we still get a 200... so we should check first to make
            // sure it doesn't already exist
            Map<String, Job> jobMap = jenkinsServer.getJobs();

            if (!jobMap.containsKey(jobName)) {
                throw new IllegalArgumentException("Job " + jobName + " must already exist to update");
            }

            String xml = calculateXml(repo, buildType, jsc, rc);

            log.trace("Sending XML to jenkins to create job: " + xml);
            jenkinsServer.updateJob(buildType.getBuildNameFor(repo), xml);
        } catch (IOException e) {
            // TODO: something other than just rethrow?
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void triggerBuild(Repository repo, JenkinsBuildTypes type, String hashToBuild) {
        triggerBuild(repo, type, hashToBuild, null, null);
    }

    public void triggerBuild(Repository repo, JenkinsBuildTypes type, String hashToBuild, String hashToMerge,
        String pullRequestId) {
        if (type == JenkinsBuildTypes.NOOP) {
            return;
        }

        try {
            RepositoryConfiguration rc = cpm.getRepositoryConfigurationForRepository(repo);
            JenkinsServerConfiguration jsc = cpm.getJenkinsServerConfiguration(rc.getJenkinsServerName());

            String jenkinsBuildId = type.getBuildNameFor(repo);
            String url = jsc.getUrl();
            String user = jsc.getUsername();
            String password = jsc.getPassword();

            log.info("Triggering jenkins build id " + jenkinsBuildId + " on hash " + hashToBuild
                + " (" + user + "@" + url + " pw: " + password.replaceAll(".", "*") + ")");

            final JenkinsServer js = jenkinsClientManager.getJenkinsServer(jsc, rc);
            Map<String, Job> jobMap = js.getJobs();
            String key = type.getBuildNameFor(repo);

            if (!jobMap.containsKey(key)) {
                throw new RuntimeException("Build doesn't exist: " + key);
            }

            Builder<String, String> builder = ImmutableMap.builder();
            builder.put("buildHead", hashToBuild);
            builder.put("repoId", repo.getId().toString());
            builder.put("type", type.toString());
            if (hashToMerge != null) {
                log.debug("Determined merge head " + hashToMerge);
                builder.put("mergeHead", hashToMerge);
            }
            if (pullRequestId != null) {
                log.debug("Determined pullRequestId " + pullRequestId);
                builder.put("pullRequestId", pullRequestId);
            }

            jobMap.get(key).build(builder.build());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (HttpResponseException e) { // subclass of IOException thrown by client
            if (e.getStatusCode() == 302) {
                // BUG in client - this isn't really an error, assume the build triggered ok and this is just a redirect
                // to some URL after the fact.
                return;
            }
            // For other HTTP errors, log it for easier debugging
            log.error("HTTP Error (resp code " + Integer.toString(e.getStatusCode()) + ")", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Code to ensure a given repository has plans that exist in jenkins.
     * 
     * @author cmyers
     */
    class CreateMissingRepositoryVisitor implements Callable<Void> {

        private final RepositoryHookService rhs;
        private final JenkinsClientManager jcm;
        private final ConfigurationPersistenceManager cpm;
        private final Repository r;
        private final Logger log;

        public CreateMissingRepositoryVisitor(RepositoryHookService rhs, JenkinsClientManager jcm,
            ConfigurationPersistenceManager cpm, Repository r, StashbotLoggerFactory lf) {
            this.rhs = rhs;
            this.jcm = jcm;
            this.cpm = cpm;
            this.r = r;
            this.log = lf.getLoggerForThis(this);
        }

        @Override
        public Void call() throws Exception {
            RepositoryConfiguration rc = cpm.getRepositoryConfigurationForRepository(r);
            // may someday require repo also...
            JenkinsServerConfiguration jsc = cpm.getJenkinsServerConfiguration(rc.getJenkinsServerName());

            if (!rc.getCiEnabled())
                return null;

            // Ensure hook is enabled
            try {
                rhs.enable(r, TRIGGER_JENKINS_BUILD_HOOK_KEY);
            } catch (Exception e) {
                log.error("Exception thrown while trying to enable hook", e);
            }

            // make sure jobs exist
            JenkinsServer js = jcm.getJenkinsServer(jsc, rc);
            Map<String, Job> jobs = js.getJobs();
            for (JenkinsBuildTypes type : ImmutableList.of(JenkinsBuildTypes.VERIFICATION, JenkinsBuildTypes.PUBLISH)) {
                if (!jobs.containsKey(type.getBuildNameFor(r))) {
                    log.info("Creating " + type.toString() + " job for repo " + r.toString());
                    createJob(r, type);
                }
            }
            return null;
        }
    }

    public void createMissingJobs() {

        ExecutorService es = Executors.newCachedThreadPool();
        List<Future<Void>> futures = new LinkedList<Future<Void>>();

        PageRequest pageReq = new PageRequestImpl(0, 500);
        Page<? extends Repository> p = repositoryService.findAll(pageReq);
        while (true) {
            for (Repository r : p.getValues()) {
                Future<Void> f = es.submit(new CreateMissingRepositoryVisitor(rhs, jenkinsClientManager, cpm, r, lf));
                futures.add(f);
            }
            if (p.getIsLastPage())
                break;
            pageReq = p.getNextPageRequest();
            p = repositoryService.findAll(pageReq);
        }
        for (Future<Void> f : futures) {
            try {
                f.get(); // don't care about return, just catch exceptions
            } catch (ExecutionException e) {
                log.error("Exception while attempting to create missing jobs for a repo: ", e);
            } catch (InterruptedException e) {
                log.error("Interrupted: this shouldn't happen", e);
            }
        }
    }

    /**
     * Code to ensure a given repository has plans that exist in jenkins.
     * 
     * @author cmyers
     */
    class UpdateAllRepositoryVisitor implements Callable<Void> {

        private final RepositoryHookService rhs;
        private final JenkinsClientManager jcm;
        private final ConfigurationPersistenceManager cpm;
        private final Repository r;
        private final Logger log;

        public UpdateAllRepositoryVisitor(RepositoryHookService rhs, JenkinsClientManager jcm,
            ConfigurationPersistenceManager cpm, Repository r, StashbotLoggerFactory lf) {
            this.rhs = rhs;
            this.jcm = jcm;
            this.cpm = cpm;
            this.r = r;
            this.log = lf.getLoggerForThis(this);
        }

        @Override
        public Void call() throws Exception {
            RepositoryConfiguration rc = cpm.getRepositoryConfigurationForRepository(r);
            // may someday require repo also...
            JenkinsServerConfiguration jsc = cpm.getJenkinsServerConfiguration(rc.getJenkinsServerName());

            if (!rc.getCiEnabled())
                return null;

            // Ensure hook is enabled
            try {
                rhs.enable(r, TRIGGER_JENKINS_BUILD_HOOK_KEY);
            } catch (Exception e) {
                log.error("Exception thrown while trying to enable hook", e);
            }

            // make sure jobs are up to date
            JenkinsServer js = jcm.getJenkinsServer(jsc, rc);
            Map<String, Job> jobs = js.getJobs();
            for (JenkinsBuildTypes type : ImmutableList.of(JenkinsBuildTypes.VERIFICATION, JenkinsBuildTypes.PUBLISH)) {
                if (!jobs.containsKey(type.getBuildNameFor(r))) {
                    log.info("Creating " + type.toString() + " job for repo " + r.toString());
                    createJob(r, type);
                } else {
                    // update job
                    log.info("Updating " + type.toString() + " job for repo " + r.toString());
                    updateJob(r, type);
                }
            }
            return null;
        }
    }

    public void updateAllJobs() {

        ExecutorService es = Executors.newCachedThreadPool();
        List<Future<Void>> futures = new LinkedList<Future<Void>>();

        PageRequest pageReq = new PageRequestImpl(0, 500);
        Page<? extends Repository> p = repositoryService.findAll(pageReq);
        while (true) {
            for (Repository r : p.getValues()) {
                Future<Void> f = es.submit(new UpdateAllRepositoryVisitor(rhs, jenkinsClientManager, cpm, r, lf));
                futures.add(f);
            }
            if (p.getIsLastPage())
                break;
            pageReq = p.getNextPageRequest();
            p = repositoryService.findAll(pageReq);
        }
        for (Future<Void> f : futures) {
            try {
                f.get(); // don't care about return, just catch exceptions
            } catch (ExecutionException e) {
                log.error("Exception while attempting to create missing jobs for a repo: ", e);
            } catch (InterruptedException e) {
                log.error("Interrupted: this shouldn't happen", e);
            }
        }
    }

    private String buildUrl(String repositoryUrl, JenkinsServerConfiguration jsc, String status) {
        // Look at the BuildSuccessReportinServlet if you change this:
        // "BASE_URL/REPO_ID/TYPE/STATE/BUILD_NUMBER/BUILD_HEAD[/MERGE_HEAD/PULLREQUEST_ID]";
        // SEE ALSO:
        // https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-JenkinsSetEnvironmentVariables
        String url =
            navBuilder.buildAbsolute().concat(
                "/plugins/servlet/stashbot/build-reporting/$repoId/$type/" + status
                    + "/$BUILD_NUMBER/$buildHead/$mergeHead/$pullRequestId");
        url = url.replace("://", "://" + jsc.getStashUsername() + ":" + jsc.getStashPassword() + "@");
        return url;
    }
}
