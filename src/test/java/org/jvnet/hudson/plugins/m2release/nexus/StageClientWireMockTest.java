package org.jvnet.hudson.plugins.m2release.nexus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import com.github.tomakehurst.wiremock.junit.WireMockRule;



public class StageClientWireMockTest {

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort(), true);

	@Issue("SECURITY-1681")
	@Test
	public void testNexusXXE() throws Exception {
		wireMockRule.stubFor(get(urlEqualTo("/malicious.xml")).
		                     withBasicAuth("admin", "admin123").
		                     willReturn(aResponse()
		                                .withHeader("Content-Type", "application/xml")
		                                .withBody(replacePlaceholderURL(getFileContents("malicious.xml")))));
		wireMockRule.stubFor(get(urlEqualTo("/evil.dtd")).
		                     willReturn(aResponse()
		                                .withHeader("Content-Type", "application/xml")
		                                .withBody(replaceLocalFile(replacePlaceholderURL(getFileContents("evil.dtd"))))));
		wireMockRule.stubFor(get(urlEqualTo("/thisIsTheContentsOfMyLocalFile")).
		                     willReturn(aResponse()
		                                .withHeader("Content-Type", "application/xml")
		                                .withBody("")));
		
		StageClient client = new StageClient(new URL(wireMockRule.baseUrl()), "admin", "admin123");
		URL url = new URL(wireMockRule.baseUrl() + "/malicious.xml");
		System.out.println("url is: " + url);
		try {
			client.getDocument(url);
			wireMockRule.verify(0, getRequestedFor(urlEqualTo("/thisIsTheContentsOfMyLocalFile")));
		} catch (StageException sex) {
			assertThat(sex.getMessage(), not(containsString("thisIsTheContentsOfMyLocalFile")));
		}
	}
	
	private String replacePlaceholderURL(String string) {
		return string.replaceAll("BASE_URL", wireMockRule.baseUrl());
	}

	private String replaceLocalFile(String string) {
		URL url = this.getClass().getResource(getClass().getSimpleName() + "/payload.txt");
		return string.replaceAll("LOCAL_FILE", url.toString());
	}
	
	private String getFileContents(String resourceName) throws Exception {
		String toLoad = getClass().getSimpleName() + '/' + resourceName;
		try (InputStream resource = this.getClass().getResourceAsStream(toLoad)) {
			assertThat("could not load resource " + toLoad, resource, notNullValue());
			return IOUtils.toString(resource, StandardCharsets.UTF_8);
		}
	}
}
