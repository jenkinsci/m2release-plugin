package org.jvnet.hudson.plugins.m2release;

import hudson.Extension;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.util.ComboBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * @author Adam.Molewko
 */
public class M2ReleaseStep extends Builder {

  private String jobName;

  private String envVars;

  @Extension
  public static class Descriptor extends BuildStepDescriptor<Builder> {

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Perform Maven Release";
    }

    public ComboBoxModel doFillJobNameItems(@QueryParameter String jobName) {
      final ComboBoxModel items = new ComboBoxModel();

      for (final MavenModuleSet item : Jenkins.getInstance().getAllItems(MavenModuleSet.class)) {
        for (final BuildWrapper buildWrapper : item.getBuildWrappersList()) {
          //determines if the job has Maven Release enabled
          if (buildWrapper instanceof M2ReleaseBuildWrapper) {
            if (jobName.isEmpty() || item.getName().contains(jobName)) {
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
  public M2ReleaseStep(String jobName, final String envVars) {
    this.jobName = jobName;
    this.envVars = envVars;
  }

  public String getJobName() {
    return jobName;
  }
}
