/*
 * The MIT License
 * 
 * Copyright (c) 2009, NDS Group Ltd., James Nord
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

import hudson.model.Cause.UserIdCause;

public class ReleaseCause extends UserIdCause {

    // Kept for backwards compatibility with saved builds from older versions of the plugin.
    // Should be removed in the future!
    @Deprecated
    private String authenticationName;
    
    @Override
    public String getUserName() {
        if (this.authenticationName != null) {
            return authenticationName;
        } else {
            return super.getUserName();
        }
    }
    
	@Override
	public String getShortDescription() {
		return Messages.ReleaseCause_ShortDescription(getUserName());
	}
	
	@Override
	public boolean equals(Object o) {
		// generally this is bad but the parent uses instanceof and does not check classes directly and we add nothing
		// and it keeps spotbugs happy.  We should check authenticationName but that has been deprecated since before the dinosaurs
		return super.equals(o);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
