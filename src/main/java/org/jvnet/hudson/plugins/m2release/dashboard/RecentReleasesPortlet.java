package org.jvnet.hudson.plugins.m2release.dashboard;

import hudson.Extension;
import hudson.FeedAdapter;
import hudson.model.*;
import hudson.plugins.view.dashboard.DashboardPortlet;
import hudson.tasks.Mailer;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import org.jvnet.hudson.plugins.m2release.M2ReleaseBadgeAction;
import org.jvnet.hudson.plugins.m2release.ReleaseCause;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.*;

public class RecentReleasesPortlet extends DashboardPortlet {
    @DataBoundConstructor
    public RecentReleasesPortlet(String name) {
        super(name);
    }

    @SuppressWarnings("rawtypes")
    public Collection<Run> getRecentReleases(int max) {
        LinkedList<Run> recentReleases = new LinkedList<>();

        for (Job job : getDashboard().getJobs()) {
            for (Run run = job.getLastCompletedBuild(); run != null && (recentReleases.size() < max || run.getTimestamp().compareTo(recentReleases.getLast().getTimestamp()) > 0); run = run.getPreviousBuild()) {

                M2ReleaseBadgeAction mbb = run.getAction(M2ReleaseBadgeAction.class);

                if (mbb != null) {
                    if (!insertRun(run, recentReleases, max)) {
                        break;
                    }
                }
            }
        }

        return recentReleases;
    }


    /**
     * Get the release version from this run
     *
     * @param run Must be a release run - i.e. have a ReleaseBuildBadgeAction
     * @return
     */
    public String getReleaseVersion(@SuppressWarnings("rawtypes")Run run) {
        M2ReleaseBadgeAction rbb = run.getAction(M2ReleaseBadgeAction.class);

        return rbb.getVersionNumber();
    }

    @SuppressWarnings("rawtypes")
    private boolean insertRun(Run run, LinkedList<Run> recentReleases, int max) {
        ListIterator<Run> iter = recentReleases.listIterator();
        Run recentRun = null;

        do {
            if (iter.hasNext()) {
                recentRun = iter.next();
            } else {
                recentRun = null;
            }

            // if we're at the end of the recent releases list and the list has room for another
            // or this run is more recent than current position, then insert
            if ((recentRun == null && recentReleases.size() < max) || (recentRun != null && run.getTimestamp().compareTo(recentRun.getTimestamp()) > 0)) {
                // back up one and add to list
                if (!recentReleases.isEmpty() && recentRun != null) {
                    iter.previous();
                }

                iter.add(run);

                // remove last on list if size == max - might be removing what we just added
                if (recentReleases.size() > max) {
                    recentReleases.removeLast();
                }

                return true;
            }
        } while (recentRun != null);

        return false;
    }

    public void doRssAll(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException, ServletException {
        rss(req, rsp, " all builds", RunList.fromRuns(getRecentReleases(20)));
    }

    public void doRssFailed(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException, ServletException {
        rss(req, rsp, " failed builds", RunList.fromRuns(getRecentReleases(20)).failureOnly());
    }

    @SuppressWarnings("rawtypes")
    private void rss(StaplerRequest2 req, StaplerResponse2 rsp, String suffix, RunList runs) throws IOException, ServletException {
        RSS.forwardToRss(getDisplayName() + suffix, getDashboard().getUrl() + getUrl(),
                runs.newBuilds(), new RelativePathFeedAdapter(getDashboard().getUrl() + getUrl()), req, rsp);
    }

    public static class DescriptorImpl extends Descriptor<DashboardPortlet> {

        @Extension(optional = true)
        public static DescriptorImpl newInstance() {
            if (Jenkins.get().getPlugin("dashboard-view") != null) {
                return new DescriptorImpl();
            } else {
                return null;
            }
        }

        @Override
        public String getDisplayName() {
            return "Recent Maven Releases";
        }
    }

    @SuppressWarnings("rawtypes")
    private class RelativePathFeedAdapter implements FeedAdapter<Run> {
        private String url;

        RelativePathFeedAdapter(String url) {
            this.url = url;
        }

        @Override
		public String getEntryTitle(Run entry) {
            return entry + " (" + entry.getResult() + ")";
        }

        @Override
		public String getEntryUrl(Run entry) {
            return url + entry.getUrl();
        }

        @Override
		public String getEntryID(Run entry) {
            return "tag:" + "hudson.dev.java.net,"
                    + entry.getTimestamp().get(Calendar.YEAR) + ":"
                    + entry.getParent().getName() + ':' + entry.getId();
        }

        @Override
		public String getEntryDescription(Run entry) {
            return getReleaseVersion(entry);
        }

        @Override
		public Calendar getEntryTimestamp(Run entry) {
            return entry.getTimestamp();
        }

        @Override
		public String getEntryAuthor(Run entry) {
            // release builds are manual so get the UserCause
            // and report rss entry as user who kicked off build
            List<Cause> causes = entry.getCauses();
            for (Cause cause : causes) {
                if (cause instanceof ReleaseCause) {
                    return ((ReleaseCause) cause).getUserName();
                }
            }

            // in the unexpected case where there is no user cause, return admin
            return JenkinsLocationConfiguration.get().getAdminAddress();
        }
    }

}
