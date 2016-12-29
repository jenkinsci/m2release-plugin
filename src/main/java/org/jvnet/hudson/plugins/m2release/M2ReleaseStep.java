package org.jvnet.hudson.plugins.m2release;

import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BooleanParameterValue;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.TopLevelItem;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Enables scheduling release as build step in every type of build.
 *
 * @author Adam Molewko
 */
public class M2ReleaseStep extends Builder {

  private MavenModuleSet project;

  private String injectParameters;

  private String jobName;

  @Extension
  public static class Descriptor extends BuildStepDescriptor<Builder> {

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      //prevents builds from endless loop
      return !MavenModuleSet.class.isAssignableFrom(jobType);
    }

    @Override
    public String getDisplayName() {
      return "Perform Maven Release";
    }

    public ListBoxModel doFillJobNameItems(@QueryParameter String jobName) {
      final ListBoxModel items = new ListBoxModel();

      for (final MavenModuleSet item : Jenkins.getInstance().getAllItems(MavenModuleSet.class)) {
        for (final BuildWrapper buildWrapper : item.getBuildWrappersList()) {
          //determines if the job has Maven Release enabled
          if (buildWrapper instanceof M2ReleaseBuildWrapper) {
            if (jobName.trim().isEmpty() || item.getName().contains(jobName)) {
              items.add(item.getName());
              break;
            }
          }
        }
      }
      return items;
    }

  }
  @DataBoundConstructor
  public M2ReleaseStep(MavenModuleSet project, String injectParameters, String jobName) {
    this.project = project;
    this.injectParameters = injectParameters;
    this.jobName = jobName;
  }

  public String getInjectParameters() {
    return injectParameters;
  }

  public String getJobName() {
    return jobName;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    final TopLevelItem item = Jenkins.getInstance().getItem(jobName);
    if (!(item instanceof MavenModuleSet)) {
      return false;
    }

    final MavenModuleSet project = (MavenModuleSet) item;

    final M2ReleaseArgumentsAction arguments = new M2ReleaseArgumentsAction();
    arguments.setDryRun(false);

    final String releaseVersion = M2ReleaseInfoProvider.computeReleaseVersion(project.getRootModule());
    final String developmentVersion = M2ReleaseInfoProvider.computeNextVersion(project.getRootModule());

    arguments.setReleaseVersion(releaseVersion);
    arguments.setDevelopmentVersion(developmentVersion);
    arguments.setHudsonUserName(Hudson.getAuthentication().getName());

    final List<ParameterValue> values = new ArrayList<ParameterValue>();
    values.add(new StringParameterValue(M2ReleaseBuildWrapper.DescriptorImpl.DEFAULT_RELEASE_VERSION_ENVVAR, releaseVersion));
    values.add(new StringParameterValue(M2ReleaseBuildWrapper.DescriptorImpl.DEFAULT_DEV_VERSION_ENVVAR, developmentVersion));
    values.add(new BooleanParameterValue(M2ReleaseBuildWrapper.DescriptorImpl.DEFAULT_DRYRUN_ENVVAR, false));

    this.injectVariables(values, this.injectParameters);

    final ParametersAction parameters = new ParametersAction(values);

    return project.scheduleBuild2(0, new ReleaseCause(), parameters, arguments).isDone();
  }

  private void injectVariables(List<ParameterValue> values, String variables) {
    if (variables != null && !variables.trim().isEmpty()) {
      variables = this.fixCrLf(variables);
      final String[] varsByLine = variables.split("\n");
      for (String line : varsByLine) {
        final String[] keyValue = line.split("=");
        if(keyValue.length == 2){
          values.add(new StringParameterValue(keyValue[0], keyValue[1]));
        }
      }
    }
  }

  /**
   * Fix CR/LF and always make it Unix style.
   * @return String with fixed line endings. May return {@code null} only for {@code null} input
   */
  private String fixCrLf(String s) {
    if (s == null) {
      return null;
    }

    // eliminate CR
    int idx;
    while ((idx = s.indexOf("\r\n")) != -1)
      s = s.substring(0, idx) + s.substring(idx + 1);
    return s;
  }
}
