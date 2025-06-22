/*
 * Copyright (c) NDS Limited 2013.
 * All rights reserved.
 * No part of this program may be reproduced, translated or transmitted,
 * in any form or by any means, electronic, mechanical, photocopying,
 * recording or otherwise, or stored in any retrieval system of any nature,
 * without written permission of the copyright holder.
 */

/*
 * Created on 13 Jun 2013 by nordj
 */
package org.jvnet.hudson.plugins.m2release.nexus;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasXPath;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class StageClientTest {

    private static final Stage TEST_STAGE = new Stage("profile-1", "stage-1");
    private static URL testURL;

    @BeforeAll
    static void setUp() throws Exception {
        testURL = new URL("http://127.0.1.2:3456/nexus/");
    }

    /**
     * Tests that the wait successfully blocks until the repository is no longer transitioning
     */
    @Test
    void waitForActionToCompleteTest() throws Exception {
        final Document transitioning = getDocument("stageClientTest/repository__transitioning.xml");
        final Document transitioned = getDocument("stageClientTest/repository__transitioned.xml");

        StageClient spy = spy(new StageClient(testURL, "username", "password"));

        doAnswer(new Answer<Document>() {
            private int calls = 1;

            public Document answer(InvocationOnMock invocation) {
                return (calls++ % 3 == 0) ? transitioned : transitioning;
            }
        }).when(spy).getDocument(any(URL.class));
        spy.waitForActionToComplete(TEST_STAGE);

        verify(spy, times(3)).getDocument(any(URL.class));
    }

    /**
     * Test when a close fails that the appropriate StageException is thrown. Does not test the performing of
     * the action - but the parsing of the results.
     */
    @Test
    void closeFailureThrowsExceptionTest() throws Exception {
        Document doc = getDocument("stageClientTest/activity__closed_failed.xml");

        StageClient spy = spy(new StageClient(new URL("http://localhost:1234/nexus/"), "username", "password"));

        doNothing().when(spy).performStageAction(any(StageAction.class), same(TEST_STAGE), any(String.class));
        doNothing().when(spy).waitForActionToComplete(TEST_STAGE);
        doReturn(doc).when(spy).getDocument(any(URL.class));
        doReturn(Boolean.TRUE).when(spy).isAsyncClose();

        StageException ex = assertThrows(StageException.class, () -> spy.closeStage(TEST_STAGE, "myDescription"));
        assertThat("Cause should not be present as this should be a rule failure.", ex.getCause(),
                is(nullValue()));
        assertThat(ex.getMessage(), startsWith("Closing stage stage-1 failed."));
        assertThat(ex.getMessage(), containsString("One or more rules have failed"));
        assertThat(ex.getMessage(),
                containsString("Artifact is not unique: 'asd:asd:123' exists in repository 'releases'"));
    }

    /**
     * Test when a close fails that the appropriate StageException is thrown. Does not test the performing of
     * the action - but the parsing of the results.
     */
    @Test
    void closeSuccessTest() throws Exception {
        Document doc = getDocument("stageClientTest/activity__closed_ok.xml");

        StageClient spy = spy(new StageClient(new URL("http://localhost:1234/nexus"), "username", "password"));

        doNothing().when(spy).performStageAction(any(StageAction.class), same(TEST_STAGE), any(String.class));
        doNothing().when(spy).waitForActionToComplete(TEST_STAGE);
        doReturn(doc).when(spy).getDocument(any(URL.class));
        doReturn(Boolean.TRUE).when(spy).isAsyncClose();

        // no exception should be thrown here!
        spy.closeStage(TEST_STAGE, "myDescription");
    }

    @Test
    void getServerVersionTest() throws Exception {
        final Document okPerms = getDocument("stageClientTest/status__ok_perms.xml");

        StageClient spy = spy(new StageClient(testURL, "username", "password"));

        doReturn(okPerms).when(spy).getDocument(any(URL.class));

        String version = spy.getServerVersion();
        assertThat("Incorrect version", version, is("2.5.0-04"));
    }

    @Test
    void isAsyncCloseTest() {
        StageClient sc = new StageClient(testURL, "username", "password");
        assertThat("2.4.0-02 should be async", sc.isAsyncClose("2.4.0-03"), is(true));
        assertThat("2.5.0-04 should be async", sc.isAsyncClose("2.5.0-04"), is(true));
        assertThat("3.1.0-07 should be async", sc.isAsyncClose("3.1.0-07"), is(true));
        assertThat("2.3.23-02 should not be async", sc.isAsyncClose("2.3.23-02"), is(false));
    }

    /**
     * Tests that the getDocument function works correctly when authorized.
     */
    @Test
    void getDocumentTest() throws Exception {
        String response = "<hello>James was here</hello>";
        HttpServer httpServer = createAuthenticatingHttpServer(response, "testuser", "testpassword", "/nexus/");
        try {
            httpServer.start();
            URL url =
                    new URL("http", httpServer.getAddress().getHostName(), httpServer.getAddress().getPort(),
                            "/nexus/");
            StageClient client = new StageClient(url, "testuser", "testpassword");
            Document doc = client.getDocument(url);
            // check we have the correct document returned.
            assertThat(doc.getFirstChild(), is(notNullValue()));
            assertThat(doc.getFirstChild().getNodeName(), is("hello"));
            assertThat(doc.getFirstChild().getTextContent(), is("James was here"));
        } finally {
            httpServer.stop(0);
        }
    }

    /**
     * Tests that the getDocument function works correctly when unauthorized.
     */
    @Test
    void getDocumentUnauthorized() throws Exception {
        String response = "<hello>James was here</hello>";
        HttpServer httpServer = createAuthenticatingHttpServer(response, "testuser", "testpassword", "/nexus/");
        try {
            httpServer.start();
            URL url =
                    new URL("http", httpServer.getAddress().getHostName(), httpServer.getAddress().getPort(),
                            "/nexus/");
            StageClient client = new StageClient(url, "wronguser", "wrongpassword");

            StageException ex = assertThrows(StageException.class, () -> client.getDocument(url));
            assertThat(ex.getMessage(), containsString("Incorrect username / password"));
        } finally {
            httpServer.stop(0);
        }
    }

    @Test
    void testPromotionEscaping() throws Exception {
        String text = "<A Test ]]> String &wibble";
        StageClient spy = spy(new StageClient(testURL, "ignored", "ignored"));

        String xmlStr = spy.createPromoteRequestPayload(new Stage("profile-1234", "stage-1234"), text, Boolean.FALSE);

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8)));
        NodeList list = doc.getElementsByTagName("description");
        assertThat(list.getLength(), is(1));
        assertThat(list.item(0).getTextContent(), is(text));
    }

    @Test
    void testPromotionAsync() throws Exception {
        StageClient spy = spy(new StageClient(testURL, "ignored", "ignored"));

        String xmlStr = spy.createPromoteRequestPayload(new Stage("profile-1234", "stage-1234"), "description", Boolean.FALSE);

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8)));
        assertThat(doc, hasXPath("/promoteRequest/data/autoDropAfterRelease", is("false")));
    }

    @Test
    void testPromotionNonAsync() throws Exception {
        StageClient spy = spy(new StageClient(testURL, "ignored", "ignored"));

        String xmlStr = spy.createPromoteRequestPayload(new Stage("profile-1234", "stage-1234"), "description", null);

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8)));
        assertThat(doc, not(hasXPath("/promoteRequest/data/autoDropAfterRelease")));
    }

    /**
     * Tests that the successful authentication works correctly.
     */
    @Test
    void authenticationPassWithCorrectPermissionsTest() throws Exception {
        final Document okPerms = getDocument("stageClientTest/status__ok_perms.xml");

        StageClient spy = spy(new StageClient(testURL, "username", "password"));

        doReturn(okPerms).when(spy).getDocument(any(URL.class));

        spy.checkAuthentication();
    }

    /**
     * Tests that the successful authentication with not enough privileges works correctly.
     */
    @Test
    void authenticationPassWithIncorrectPermissionsTest() throws Exception {
        final Document okPerms = getDocument("stageClientTest/status__bad_perms.xml");

        StageClient spy = spy(new StageClient(testURL, "username", "password"));

        doReturn(okPerms).when(spy).getDocument(any(URL.class));

        StageException ex = assertThrows(StageException.class, spy::checkAuthentication);
        assertThat(ex.getMessage(), containsString("insufficient privileges to perform staging actions"));
    }

    /**
     * Tests that the successful authentication with not enough privileges works correctly.
     */
    @Test
    void authenticationFailTest() throws Exception {
        HttpServer httpServer =
                createAuthenticatingHttpServer("bogus", "testuser", "testpassword", "/nexus/service/local/status");

        try {
            httpServer.start();
            URL url =
                    new URL("http", httpServer.getAddress().getHostName(), httpServer.getAddress().getPort(),
                            "/nexus/");
            StageClient client = new StageClient(url, "testuser", "wrongpassword");
            StageException ex = assertThrows(StageException.class, client::checkAuthentication);
            assertThat(ex.getMessage(), containsString("Incorrect username / password"));
        } finally {
            httpServer.stop(0);
        }
    }

    @Test
    void openStagesMethodShouldReturnCorrectStages() throws Exception {
        Stage expectedStage1 = new Stage("3e1e1bad64f", "test-001");
        Stage expectedStage2 = new Stage("3e1e1bad64f", "test-005");

        Document doc = getDocument("stageClientTest/profile_repositories.xml");

        StageClient spy = spy(new StageClient(testURL, "username", "password"));

        doReturn(doc).when(spy).getDocument(any(URL.class));

        List<Stage> stages = spy.getOpenStageIDs();

        assertThat(stages, hasSize(2));
        assertThat(stages, hasItems(expectedStage1, expectedStage2));
    }

    @Test
    void checkStageForGAVReturnsCorrectStage() throws Exception {
        List<Stage> stages = new ArrayList<>();
        stages.add(new Stage("profile1", "profile1-1001"));
        stages.add(new Stage("profile1", "profile1-1002"));
        stages.add(new Stage("profile2", "profile2-1001"));
        stages.add(new Stage("profile2", "profile2-1002"));
        Stage targetStage = stages.get(2);

        HttpServer httpServer =
                createAuthenticatingHttpServer("", "username", "password",
                        "/nexus/service/local/repositories/profile2-1001/content/org/example/test/test/1.2.3-4/");

        try {
            httpServer.start();
            URL url =
                    new URL("http", httpServer.getAddress().getHostName(), httpServer.getAddress().getPort(),
                            "/nexus/");
            StageClient sc = new StageClient(url, "username", "password");
            for (Stage stage : stages) {
                boolean found = sc.checkStageForGAV(stage, "org.example.test", "test", "1.2.3-4");
                assertEquals((stage == targetStage), found, "Incorrect stage match (" + stage + ").");
            }
        } finally {
            httpServer.stop(0);
        }
    }

    /**
     * Creates an HTTP Server bound to a random port on 127.0.0.1. The Caller must start and stop this server
     * when it is no longer required.
     *
     * @param text        the text to return with the request.
     * @param username    the username that must be sent to match authentication.
     * @param password    the password that must be sent to match authentication.
     * @param requestPath the path for which the text should be returned - other paths will result in a 404
     *                    response.
     * @return The newly created (and started) HTTP Server.
     */
    private HttpServer createAuthenticatingHttpServer(final String text,
                                                      final String username,
                                                      final String password,
                                                      final String requestPath) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 1);

        HttpContext ctx = httpServer.createContext("/");

        BasicAuthenticator authenticator = new BasicAuthenticator("my realm") {

            @Override
            public boolean checkCredentials(String user, String pass) {
                return (username.equals(user) && password.equals(pass));
            }

        };

        ctx.setAuthenticator(authenticator);

        HttpHandler handler = exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals(requestPath)) {
                byte[] data = text.getBytes(StandardCharsets.UTF_8);
                Headers headers = exchange.getResponseHeaders();

                if (exchange.getRequestMethod().equals("POST")) {
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_CREATED, -1);
                    OutputStream os = exchange.getResponseBody();
                    os.close();
                } else {
                    headers.add("Content-Type", "application/xml; charset=UTF-8");
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, data.length);
                    if (exchange.getRequestMethod().equals("HEAD")) {
                        // bug in sun HTTP Server - warning produces here should not exist.
                        OutputStream os = exchange.getResponseBody();
                        os.close();
                    } else {
                        OutputStream os = exchange.getResponseBody();
                        os.write(data);
                        os.close();
                    }

                }
            } else {
                Headers headers = exchange.getResponseHeaders();
                headers.add("Content-Type", "application/xml; charset=UTF-8");
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
                OutputStream os = exchange.getResponseBody();
                os.close();
            }
        };
        ctx.setHandler(handler);
        return httpServer;
    }

    private static Document getDocument(String testResource) throws ParserConfigurationException,
            SAXException, IOException {
        URL url = StageClientTest.class.getResource(testResource);
        assertThat("resource not found for: " + testResource, url, is(notNullValue()));

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.parse(url.openStream());
    }
}
