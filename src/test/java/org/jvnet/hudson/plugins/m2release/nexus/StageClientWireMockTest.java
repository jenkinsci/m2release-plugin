package org.jvnet.hudson.plugins.m2release.nexus;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.Issue;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;


class StageClientWireMockTest {

    @RegisterExtension
    private static WireMockExtension wireMock = WireMockExtension.newInstance().options(options().dynamicPort()).failOnUnmatchedRequests(true).build();

    @Issue("SECURITY-1681")
    @Test
    void testNexusXXE() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/malicious.xml")).
                withBasicAuth("admin", "admin123").
                willReturn(aResponse()
                        .withHeader("Content-Type", "application/xml")
                        .withBody(replacePlaceholderURL(getFileContents("malicious.xml")))));
        wireMock.stubFor(get(urlEqualTo("/evil.dtd")).
                willReturn(aResponse()
                        .withHeader("Content-Type", "application/xml")
                        .withBody(replaceLocalFile(replacePlaceholderURL(getFileContents("evil.dtd"))))));
        wireMock.stubFor(get(urlEqualTo("/thisIsTheContentsOfMyLocalFile")).
                willReturn(aResponse()
                        .withHeader("Content-Type", "application/xml")
                        .withBody("")));

        StageClient client = new StageClient(new URL(wireMock.baseUrl()), "admin", "admin123");
        URL url = new URL(wireMock.baseUrl() + "/malicious.xml");
        System.out.println("url is: " + url);

        StageException ex = assertThrows(StageException.class, () -> client.getDocument(url));
        assertThat(ex.getMessage(), not(containsString("thisIsTheContentsOfMyLocalFile")));
        wireMock.verify(0, getRequestedFor(urlEqualTo("/thisIsTheContentsOfMyLocalFile")));
    }

    private String replacePlaceholderURL(String string) {
        return string.replaceAll("BASE_URL", wireMock.baseUrl());
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
