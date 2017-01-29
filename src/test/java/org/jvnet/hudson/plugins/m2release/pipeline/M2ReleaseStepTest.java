package org.jvnet.hudson.plugins.m2release.pipeline;

import static org.jvnet.hudson.test.ToolInstallations.configureMaven3;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.plugins.m2release.M2ReleaseActionTest;
import org.jvnet.hudson.plugins.m2release.M2ReleaseBuildWrapper;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;

import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.ParameterizedJobMixIn;

/**
 * @author Alexey Merezhin
 */
public class M2ReleaseStepTest {
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule public LoggerRule logging = new LoggerRule();
    private MavenModuleSet ds;

    @Before
    public void setUp() throws Exception {
        ds = j.createProject(MavenModuleSet.class, "ds");
        ds.setRootPOM("maven3-project/pom.xml");
        ds.setMaven(configureMaven3().getName());
        ds.setScm(new ExtractResourceSCM(M2ReleaseActionTest.class.getResource("maven3-project.zip")));
        ds.setGoals("compile"); // build would fail with this goal

        M2ReleaseBuildWrapper wrapper = new M2ReleaseBuildWrapper(
                M2ReleaseBuildWrapper.DescriptorImpl.DEFAULT_RELEASE_GOALS, M2ReleaseBuildWrapper.DescriptorImpl.DEFAULT_DRYRUN_GOALS, false,
                false, false, "ENV", "USERENV", "PWDENV", M2ReleaseBuildWrapper.DescriptorImpl.DEFAULT_NUMBER_OF_RELEASE_BUILDS_TO_KEEP);
        ds.getBuildWrappersList().add(wrapper);
    }

    @Test
    public void releaseProjectFromPipeline() throws Exception {
        WorkflowJob us = j.jenkins.createProject(WorkflowJob.class, "us");
        us.setDefinition(new CpsFlowDefinition(
                "m2release job: 'ds', releaseVersion: '1.2.3', developmentVersion: '2.0.0', dryRun: true\n"
                + " echo \"release's done\""
                , true));
        WorkflowRun workflowRun = j.buildAndAssertSuccess(us);
        j.assertLogContains("its only a dryRun, no need to mark it for keep", ds.getBuildByNumber(1));
        j.assertLogContains("-DreleaseVersion=1.2.3 ", ds.getBuildByNumber(1));
        j.assertLogContains("-DdevelopmentVersion=2.0.0", ds.getBuildByNumber(1));

        j.assertBuildStatusSuccess(ds.getBuildByNumber(1));
        j.assertLogContains("release's done", workflowRun);
    }

    @Test
    public void testDefaultParameters() throws Exception {
        /* job should be built at least once to have modules initialized */
        j.buildAndAssertSuccess(ds);
        ds.getBuildByNumber(1).delete();

        WorkflowJob us = j.jenkins.createProject(WorkflowJob.class, "us");
        us.setDefinition(new CpsFlowDefinition(
                "m2release job: 'ds', dryRun: true\n"
                        + " echo \"release's done\""
                , true));

        WorkflowRun workflowRun = j.buildAndAssertSuccess(us);
        j.assertLogContains("release's done", workflowRun);

        j.assertLogContains("its only a dryRun, no need to mark it for keep", ds.getBuildByNumber(2));
        j.assertLogContains("-DdevelopmentVersion=1.7-SNAPSHOT", ds.getBuildByNumber(2));
        j.assertLogContains("-DreleaseVersion=1.6", ds.getBuildByNumber(2));
    }
}