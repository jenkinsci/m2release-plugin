/*
 * The MIT License
 *
 * Copyright (c) 2013, Robert Kleinschmager
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

import hudson.util.ArgumentListBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Robert Kleinschmager
 */
class M2ReleaseArgumentInterceptorActionTest {

    private M2ReleaseArgumentInterceptorAction sut;
    private ArgumentListBuilder listBuilder;

    @BeforeEach
    void setUp() {
        sut = new M2ReleaseArgumentInterceptorAction("", null);
        listBuilder = new ArgumentListBuilder();
    }

    /**
     * Test method for {@link org.jvnet.hudson.plugins.m2release.M2ReleaseArgumentInterceptorAction#intercept(hudson.util.ArgumentListBuilder, hudson.maven.MavenModuleSetBuild)}.
     */
    @Test
    void interceptorShouldNotTouchArgumentList() {
        //GIVEN
        listBuilder.add("argument1");
        listBuilder.add("argument2");
        listBuilder.add("argument3", true);
        listBuilder.add("argument4", false);

        //WHEN
        ArgumentListBuilder resultingListBuilder = sut.internalIntercept(listBuilder, true);

        //THEN
        assertEquals("argument1", resultingListBuilder.toList().get(0));
        assertFalse(resultingListBuilder.toMaskArray()[0]);

        assertEquals("argument2", resultingListBuilder.toList().get(1));
        assertFalse(resultingListBuilder.toMaskArray()[1]);

        assertEquals("argument3", resultingListBuilder.toList().get(2));
        assertTrue(resultingListBuilder.toMaskArray()[2]);

        assertEquals("argument4", resultingListBuilder.toList().get(3));
        assertFalse(resultingListBuilder.toMaskArray()[3]);
    }

    /**
     * Test method for {@link org.jvnet.hudson.plugins.m2release.M2ReleaseArgumentInterceptorAction#intercept(hudson.util.ArgumentListBuilder, hudson.maven.MavenModuleSetBuild)}.
     */
    @Test
    void interceptorShouldFilterElements() {
        //GIVEN
        listBuilder.add("argument1");
        listBuilder.add("argument2");

        // incremental build arguments, which should be filtered out
        listBuilder.add("-amd");
        listBuilder.add("-pl");
        listBuilder.add("foo,bar");

        listBuilder.add("argument3", true);
        listBuilder.add("argument4", false);

        //WHEN
        ArgumentListBuilder resultingListBuilder = sut.internalIntercept(listBuilder, true);

        //THEN
        assertEquals("argument1", resultingListBuilder.toList().get(0));
        assertFalse(resultingListBuilder.toMaskArray()[0]);

        assertEquals("argument2", resultingListBuilder.toList().get(1));
        assertFalse(resultingListBuilder.toMaskArray()[1]);

        assertEquals("argument3", resultingListBuilder.toList().get(2));
        assertTrue(resultingListBuilder.toMaskArray()[2]);

        assertEquals("argument4", resultingListBuilder.toList().get(3));
        assertFalse(resultingListBuilder.toMaskArray()[3]);
    }

    /**
     * Test method for {@link org.jvnet.hudson.plugins.m2release.M2ReleaseArgumentInterceptorAction#intercept(hudson.util.ArgumentListBuilder, hudson.maven.MavenModuleSetBuild)}.
     */
    @Test
    void interceptorSkipIfOrderIsNotCorrect() {
        //GIVEN
        listBuilder.add("argument1");
        listBuilder.add("argument2");

        // wrong order of the incrementalBuild arguments, means, that they are not added by jenkins incremental feature
        listBuilder.add("-amd");
        listBuilder.add("foo,bar");
        listBuilder.add("-pl");

        listBuilder.add("argument3", true);
        listBuilder.add("argument4", false);

        //WHEN
        ArgumentListBuilder resultingListBuilder = sut.internalIntercept(listBuilder, true);

        //THEN
        assertEquals("argument1", resultingListBuilder.toList().get(0));
        assertFalse(resultingListBuilder.toMaskArray()[0]);

        assertEquals("argument2", resultingListBuilder.toList().get(1));
        assertFalse(resultingListBuilder.toMaskArray()[1]);

        assertEquals("-amd", resultingListBuilder.toList().get(2));
        assertFalse(resultingListBuilder.toMaskArray()[2]);

        assertEquals("foo,bar", resultingListBuilder.toList().get(3));
        assertFalse(resultingListBuilder.toMaskArray()[3]);

        assertEquals("-pl", resultingListBuilder.toList().get(4));
        assertFalse(resultingListBuilder.toMaskArray()[4]);

        assertEquals("argument3", resultingListBuilder.toList().get(5));
        assertTrue(resultingListBuilder.toMaskArray()[5]);

        assertEquals("argument4", resultingListBuilder.toList().get(6));
        assertFalse(resultingListBuilder.toMaskArray()[6]);
    }

    /**
     * Test method for {@link org.jvnet.hudson.plugins.m2release.M2ReleaseArgumentInterceptorAction#intercept(hudson.util.ArgumentListBuilder, hudson.maven.MavenModuleSetBuild)}.
     */
    @Test
    void interceptorShouldSkipIfNoIncrementalBuild() {
        //GIVEN
        listBuilder.add("argument1");
        listBuilder.add("argument2");

        // incremental build arguments, which should be filtered out
        listBuilder.add("-amd");
        listBuilder.add("-pl");
        listBuilder.add("foo,bar");

        listBuilder.add("argument3", true);
        listBuilder.add("argument4", false);

        //WHEN
        ArgumentListBuilder resultingListBuilder = sut.internalIntercept(listBuilder, false);

        //THEN
        assertEquals("argument1", resultingListBuilder.toList().get(0));
        assertFalse(resultingListBuilder.toMaskArray()[0]);

        assertEquals("argument2", resultingListBuilder.toList().get(1));
        assertFalse(resultingListBuilder.toMaskArray()[1]);

        assertEquals("-amd", resultingListBuilder.toList().get(2));
        assertFalse(resultingListBuilder.toMaskArray()[2]);

        assertEquals("-pl", resultingListBuilder.toList().get(3));
        assertFalse(resultingListBuilder.toMaskArray()[3]);

        assertEquals("foo,bar", resultingListBuilder.toList().get(4));
        assertFalse(resultingListBuilder.toMaskArray()[4]);

        assertEquals("argument3", resultingListBuilder.toList().get(5));
        assertTrue(resultingListBuilder.toMaskArray()[5]);


        assertEquals("argument4", resultingListBuilder.toList().get(6));
        assertFalse(resultingListBuilder.toMaskArray()[6]);
    }

    @Test
    void password() {
        M2ReleaseArgumentInterceptorAction xceptor = new M2ReleaseArgumentInterceptorAction("-Dusername=bob release:prepare release:perform", "s3cr3t");
        ArgumentListBuilder args = xceptor.internalIntercept(new ArgumentListBuilder().add("-B").addTokenized(xceptor.getGoalsAndOptions(null)), false);
        assertEquals("[-B, -Dusername=bob, release:prepare, release:perform, -Dpassword=s3cr3t]", Arrays.toString(args.toCommandArray()));
        assertEquals("-B -Dusername=bob release:prepare release:perform ******", args.toString());
    }
}
