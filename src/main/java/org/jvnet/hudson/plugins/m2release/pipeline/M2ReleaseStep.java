package org.jvnet.hudson.plugins.m2release.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.util.StaplerReferer;
import org.jvnet.hudson.plugins.m2release.M2ReleaseAction;
import org.jvnet.hudson.plugins.m2release.M2ReleaseArgumentsAction;
import org.jvnet.hudson.plugins.m2release.ReleaseCause;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.google.inject.Inject;

import hudson.AbortException;
import hudson.Extension;
import hudson.console.ModelHyperlinkNote;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Hudson;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;

/**
 * Created by e3cmea on 1/3/17.
 *
 * @author Alexey Merezhin
 */
public class M2ReleaseStep extends AbstractStepImpl {
    private static final Logger LOGGER = Logger.getLogger(M2ReleaseStep.class.getName());

    private String job;
    private String releaseVersion = "";
    private String developmentVersion = "";
    private Boolean isDryRun = false;

    @DataBoundConstructor
    public M2ReleaseStep(String job) {
        this.job = job;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    @DataBoundSetter public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public String getDevelopmentVersion() {
        return developmentVersion;
    }

    @DataBoundSetter public void setDevelopmentVersion(String developmentVersion) {
        this.developmentVersion = developmentVersion;
    }

    public Boolean isDryRun() {
        return isDryRun;
    }

    @DataBoundSetter public void setDryRun(Boolean dryRun) {
        isDryRun = dryRun;
    }

    public static class Execution extends AbstractStepExecutionImpl {
        @StepContextParameter private transient Run<?,?> invokingRun;
        @StepContextParameter private transient TaskListener listener;

        @Inject(optional=true) transient M2ReleaseStep step;

        @Override
        public boolean start() throws Exception {
            if (step.getJob() == null) {
                throw new AbortException("Job name is not defined.");
            }

            final MavenModuleSet project = Jenkins.getActiveInstance()
                                                  .getItem(step.getJob(), invokingRun.getParent(), MavenModuleSet.class);
            if (project == null) {
                throw new AbortException("No parametrized job named " + step.getJob() + " found");
            }
            listener.getLogger().println("Releasing project: " + ModelHyperlinkNote.encodeTo(project));

            List<ParameterValue> values = new ArrayList<>();

            // schedule release build
            ParametersAction parameters = new ParametersAction(values);

            M2ReleaseArgumentsAction arguments = new M2ReleaseArgumentsAction();
            M2ReleaseAction releaseAction = new M2ReleaseAction(project, false, false, false);
            arguments.setDryRun(step.isDryRun());
            arguments.setReleaseVersion(step.getReleaseVersion());
            if (StringUtils.isBlank(arguments.getReleaseVersion())) {
                arguments.setReleaseVersion(releaseAction.computeReleaseVersion());
            }
            arguments.setDevelopmentVersion(step.getDevelopmentVersion());
            if (StringUtils.isBlank(arguments.getDevelopmentVersion())) {
                arguments.setDevelopmentVersion(releaseAction.computeNextVersion());
            }

            List<Action> actions = new ArrayList<>();
            actions.add(new M2ReleaseTriggerAction(getContext()));
            actions.add(parameters);
            actions.add(arguments);
            actions.add(new CauseAction(new ReleaseCause()));

            QueueTaskFuture<?> task = project.scheduleBuild2(0, new Cause.UpstreamCause(invokingRun), actions);
            if (task == null) {
                throw new AbortException("Failed to trigger build of " + project.getFullName());
            }

            return false;
        }

        @Override
        public void stop(@Nonnull Throwable cause) throws Exception {
            getContext().onFailure(cause);
        }

        private static final long serialVersionUID = 1L;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "m2release";
        }

        @Override
        public String getDisplayName() {
            return "Trigger M2 release for the job";
        }

        public AutoCompletionCandidates doAutoCompleteJob(@AncestorInPath ItemGroup<?> context, @QueryParameter String value) {
            return AutoCompletionCandidates.ofJobNames(ParameterizedJobMixIn.ParameterizedJob.class, value, context);
        }

        @Restricted(DoNotUse.class) // for use from config.jelly
        public String getContext() {
            Job<?,?> job = StaplerReferer.findItemFromRequest(Job.class);
            return job != null ? job.getFullName() : null;
        }
    }
}
