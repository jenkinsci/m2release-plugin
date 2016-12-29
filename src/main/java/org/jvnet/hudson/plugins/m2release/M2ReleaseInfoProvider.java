package org.jvnet.hudson.plugins.m2release;

import hudson.maven.MavenModule;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionParseException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides common information needed for release, computes new versions, etc.
 *
 * @author Adam Molewko
 */
public class M2ReleaseInfoProvider {

    private static final Logger LOGGER = Logger.getLogger(M2ReleaseInfoProvider.class.getName());

    public static String computeNextVersion(MavenModule rootModule) {
        String version = "NaN-SNAPSHOT";
        if (rootModule != null && StringUtils.isNotBlank(rootModule.getVersion())) {
            try {
                DefaultVersionInfo dvi = new DefaultVersionInfo(rootModule.getVersion());
                version = dvi.getNextVersion().getSnapshotVersionString();
            } catch (Exception vpEx) {
                LOGGER.log(Level.WARNING, "Failed to compute next version.", vpEx);
            }
        }
        return version;
    }

    public static String computeScmTag(MavenModule rootModule) {
        // maven default is artifact-version
        String artifactId = rootModule == null ? "M2RELEASE-TAG" : rootModule.getModuleName().artifactId;
        StringBuilder sb = new StringBuilder();
        sb.append(artifactId);
        sb.append('-');
        sb.append(computeReleaseVersion(rootModule));
        return sb.toString();
    }

    public static String computeReleaseVersion(MavenModule rootModule) {
        String version = "NaN";
        if (rootModule != null && StringUtils.isNotBlank(rootModule.getVersion())) {
            try {
                DefaultVersionInfo dvi = new DefaultVersionInfo(rootModule.getVersion());
                version = dvi.getReleaseVersionString();
            } catch (VersionParseException vpEx) {
                LOGGER.log(Level.WARNING, "Failed to compute next version.", vpEx);
                version = rootModule.getVersion().replace("-SNAPSHOT", "");
            }
        }
        return version;
    }

    public static String computeRepoDescription(MavenModule rootModule) {
        StringBuilder sb = new StringBuilder();
        sb.append(rootModule.getName());
        sb.append(':');
        sb.append(computeReleaseVersion(rootModule));
        return sb.toString();
    }
}
