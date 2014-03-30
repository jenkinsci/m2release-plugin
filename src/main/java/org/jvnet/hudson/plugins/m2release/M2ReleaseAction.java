/*
 * The MIT License
 * 
 * Copyright (c) 2009, NDS Group Ltd., James Nord, CloudBees, Inc.
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
package org.jvnet.hudson.plugins.m2release;

import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.ParameterValue;
import hudson.model.BooleanParameterValue;
import hudson.model.Hudson;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PasswordParameterValue;
import hudson.model.PermalinkProjectAction;
import hudson.model.StringParameterValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.shared.release.versions.DefaultVersionInfo;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * The action appears as the link in the side bar that users will click on in
 * order to start the release process.
 * 
 * @author James Nord
 * @author Dominik Bartholdi
 * @version 0.3
 */
public class M2ReleaseAction implements PermalinkProjectAction {

	private MavenModuleSet project;
	private boolean selectCustomScmCommentPrefix;
	private boolean selectCustomScmTag = false;
	private boolean selectAppendHudsonUsername;
	private boolean selectScmCredentials;

	public M2ReleaseAction(MavenModuleSet project, boolean selectCustomScmCommentPrefix, boolean selectAppendHudsonUsername, boolean selectScmCredentials) {
		this.project = project;
		this.selectCustomScmCommentPrefix = selectCustomScmCommentPrefix;
		this.selectAppendHudsonUsername = selectAppendHudsonUsername;
		this.selectScmCredentials = selectScmCredentials;
		if (getRootModule() == null) {
			// if the root module is not available, the user should be informed
			// about the stuff we are not able to compute
			this.selectCustomScmTag = true;
		}
	}

	public List<ParameterDefinition> getParameterDefinitions() {
		ParametersDefinitionProperty pdp = project.getProperty(ParametersDefinitionProperty.class);
		List<ParameterDefinition> pds = Collections.emptyList();
		if (pdp != null) {
			pds = pdp.getParameterDefinitions();
		}
		return pds;
	}

	public List<Permalink> getPermalinks() {
		return PERMALINKS;
	}

	public String getDisplayName() {
		return Messages.ReleaseAction_perform_release_name();
	}

	public String getIconFileName() {
		if (M2ReleaseBuildWrapper.hasReleasePermission(project)) {
			return "installer.gif"; //$NON-NLS-1$
		}
		// by returning null the link will not be shown.
		return null;
	}

	public String getUrlName() {
		return "m2release"; //$NON-NLS-1$
	}

	public boolean isSelectScmCredentials() {
		return selectScmCredentials;
	}

	public boolean isSelectCustomScmCommentPrefix() {
		return selectCustomScmCommentPrefix;
	}

	public void setSelectCustomScmCommentPrefix(boolean selectCustomScmCommentPrefix) {
		this.selectCustomScmCommentPrefix = selectCustomScmCommentPrefix;
	}

	public boolean isSelectAppendHudsonUsername() {
		return selectAppendHudsonUsername;
	}

	public void setSelectAppendHudsonUsername(boolean selectAppendHudsonUsername) {
		this.selectAppendHudsonUsername = selectAppendHudsonUsername;
	}

	public boolean isSelectCustomScmTag() {
		return selectCustomScmTag;
	}

	public Collection<MavenModule> getModules() {
		return project.getModules();
	}

	public MavenModule getRootModule() {
		return project.getRootModule();
	}

	public String computeReleaseVersion() {
		String version = "NaN";
		final MavenModule rootModule = getRootModule();
		if (rootModule != null && StringUtils.isNotBlank(rootModule.getVersion())) {
			try {
				DefaultVersionInfo dvi = new DefaultVersionInfo(rootModule.getVersion());
				version = dvi.getReleaseVersionString();
			} catch (VersionParseException vpEx) {
				Logger logger = Logger.getLogger(this.getClass().getName());
				logger.log(Level.WARNING, "Failed to compute next version.", vpEx);
				version = rootModule.getVersion().replace("-SNAPSHOT", "");
			}
		}
		return version;
	}

	public String computeRepoDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append(project.getRootModule().getName());
		sb.append(':');
		sb.append(computeReleaseVersion());
		return sb.toString();
	}

	public String computeScmTag() {
		// maven default is artifact-version
		String artifactId = getRootModule() == null ? "M2RELEASE-TAG" : getRootModule().getModuleName().artifactId;
		StringBuilder sb = new StringBuilder();
		sb.append(artifactId);
		sb.append('-');
		sb.append(computeReleaseVersion());
		return sb.toString();
	}

	public String computeNextVersion() {
		String version = "NaN-SNAPSHOT";
		final MavenModule rootModule = getRootModule();
		if (rootModule != null && StringUtils.isNotBlank(rootModule.getVersion())) {
			try {
				DefaultVersionInfo dvi = new DefaultVersionInfo(rootModule.getVersion());
				version = dvi.getNextVersion().getSnapshotVersionString();
			} catch (Exception vpEx) {
				Logger logger = Logger.getLogger(this.getClass().getName());
				logger.log(Level.WARNING, "Failed to compute next version.", vpEx);
			}
		}
		return version;
	}

	public boolean isNexusSupportEnabled() {
		return project.getBuildWrappersList().get(M2ReleaseBuildWrapper.class).getDescriptor().isNexusSupport();
	}

	public void doSubmit(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
		M2ReleaseBuildWrapper.checkReleasePermission(project);
		M2ReleaseBuildWrapper m2Wrapper = project.getBuildWrappersList().get(M2ReleaseBuildWrapper.class);

		// JSON collapses everything in the dynamic specifyVersions section so
		// we need to fall back to
		// good old http...
		Map<?, ?> httpParams = req.getParameterMap();

		final boolean closeNexusStage = httpParams.containsKey("closeNexusStage"); //$NON-NLS-1$
		final String repoDescription = closeNexusStage ? getString("repoDescription", httpParams) : ""; //$NON-NLS-1$
		final boolean specifyScmCredentials = httpParams.containsKey("specifyScmCredentials"); //$NON-NLS-1$
		final String scmUsername = specifyScmCredentials ? getString("scmUsername", httpParams) : null; //$NON-NLS-1$
		final String scmPassword = specifyScmCredentials ? getString("scmPassword", httpParams) : null; //$NON-NLS-1$
		final boolean specifyScmCommentPrefix = httpParams.containsKey("specifyScmCommentPrefix"); //$NON-NLS-1$
		final String scmCommentPrefix = specifyScmCommentPrefix ? getString("scmCommentPrefix", httpParams) : null; //$NON-NLS-1$
		final boolean specifyScmTag = httpParams.containsKey("specifyScmTag"); //$NON-NLS-1$
		final String scmTag = specifyScmTag ? getString("scmTag", httpParams) : null; //$NON-NLS-1$

		final boolean appendHusonUserName = specifyScmCommentPrefix && httpParams.containsKey("appendHudsonUserName"); //$NON-NLS-1$
		final boolean isDryRun = httpParams.containsKey("isDryRun"); //$NON-NLS-1$

		final String releaseVersion = getString("releaseVersion", httpParams); //$NON-NLS-1$
		final String developmentVersion = getString("developmentVersion", httpParams); //$NON-NLS-1$

		// TODO make this nicer by showing a html error page.
		// this will throw an exception so control will terminate if the dev
		// version is not a "SNAPSHOT".
		enforceDeveloperVersion(developmentVersion);

		// get the normal job parameters (adapted from
		// hudson.model.ParametersDefinitionProperty._doBuild(StaplerRequest,
		// StaplerResponse))
		List<ParameterValue> values = new ArrayList<ParameterValue>();
		JSONObject formData = req.getSubmittedForm();
		JSONArray a = JSONArray.fromObject(formData.get("parameter"));
		for (Object o : a) {
			if (o instanceof JSONObject) {
				JSONObject jo = (JSONObject) o;
				if (jo != null && !jo.isNullObject()) {
					String name = jo.optString("name");
					if (name != null) {
						ParameterDefinition d = getParameterDefinition(name);
						if (d == null) {
							throw new IllegalArgumentException("No such parameter definition: " + name);
						}
						ParameterValue parameterValue = d.createValue(req, jo);
						values.add(parameterValue);
					}
				}
			}
		}

		// if configured, expose the SCM credentails as additional parameters
		if (StringUtils.isNotBlank(m2Wrapper.getScmPasswordEnvVar())) {
			String scmPasswordVal = StringUtils.isEmpty(scmPassword) ? "" : scmPassword;
			values.add(new PasswordParameterValue(m2Wrapper.getScmPasswordEnvVar(), scmPasswordVal));
		}
		if (StringUtils.isNotBlank(m2Wrapper.getScmUserEnvVar())) {
			String scmUsernameVal = StringUtils.isEmpty(scmUsername) ? "" : scmUsername;
			values.add(new StringParameterValue(m2Wrapper.getScmUserEnvVar(), scmUsernameVal));
		}
		values.add(new StringParameterValue(M2ReleaseBuildWrapper.DescriptorImpl.DEFAULT_RELEASE_VERSION_ENVVAR, releaseVersion));
		values.add(new StringParameterValue(M2ReleaseBuildWrapper.DescriptorImpl.DEFAULT_DEV_VERSION_ENVVAR, developmentVersion));
		values.add(new BooleanParameterValue(M2ReleaseBuildWrapper.DescriptorImpl.DEFAULT_DRYRUN_ENVVAR, isDryRun));

		// schedule release build
		ParametersAction parameters = new ParametersAction(values);

		M2ReleaseArgumentsAction arguments = new M2ReleaseArgumentsAction();
		arguments.setDryRun(isDryRun);

		arguments.setReleaseVersion(releaseVersion);
		arguments.setDevelopmentVersion(developmentVersion);
		// TODO - re-implement versions on specific modules.

		arguments.setCloseNexusStage(closeNexusStage);
		arguments.setRepoDescription(repoDescription);
		arguments.setScmUsername(scmUsername);
		arguments.setScmPassword(scmPassword);
		arguments.setScmTagName(scmTag);
		arguments.setScmCommentPrefix(scmCommentPrefix);
		arguments.setAppendHusonUserName(appendHusonUserName);
		arguments.setHudsonUserName(Hudson.getAuthentication().getName());

		
		if (project.scheduleBuild(0, new ReleaseCause(), parameters, arguments)) {
			resp.sendRedirect(req.getContextPath() + '/' + project.getUrl());
		} else {
			// redirect to error page.
			// TODO try and get this to go back to the form page with an
			// error at the top.
			resp.sendRedirect(req.getContextPath() + '/' + project.getUrl() + '/' + getUrlName() + "/failed");
		}
	}

	/**
	 * Gets the {@link ParameterDefinition} of the given name, if any.
	 */
	public ParameterDefinition getParameterDefinition(String name) {
		for (ParameterDefinition pd : getParameterDefinitions()) {
			if (pd.getName().equals(name)) {
				return pd;
			}
		}
		return null;
	}

	/**
	 * returns the value of the key as a String. if multiple values have been
	 * submitted, the first one will be returned.
	 * 
	 * @param key
	 * @param httpParams
	 * @return
	 */
	private String getString(String key, Map<?, ?> httpParams) {
		return (String) (((Object[]) httpParams.get(key))[0]);
	}

	/**
	 * Enforces that the developer version is actually a developer version and
	 * ends with "-SNAPSHOT".
	 * 
	 * @throws IllegalArgumentException
	 *             if the version does not end with "-SNAPSHOT"
	 */
	private void enforceDeveloperVersion(String version) throws IllegalArgumentException {
		if (!version.endsWith("-SNAPSHOT")) {
			throw new IllegalArgumentException(String.format(Locale.ENGLISH, "Developer Version (%s) is not a valid version (it must end with \"-SNAPSHOT\")",
					version));
		}
	}

	private static final List<Permalink> PERMALINKS = Collections.singletonList(LastReleasePermalink.INSTANCE);
}
