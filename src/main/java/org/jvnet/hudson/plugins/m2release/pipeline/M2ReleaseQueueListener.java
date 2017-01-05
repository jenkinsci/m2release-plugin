package org.jvnet.hudson.plugins.m2release.pipeline;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;

/**
 * copied from org.jenkinsci.plugins.workflow.support.steps.build.BuildQueueListener
 */
@Extension
public class M2ReleaseQueueListener extends QueueListener {
    @Override
    public void onLeft(Queue.LeftItem li) {
        if(li.isCancelled()){
            for (M2ReleaseTriggerAction.Trigger trigger : M2ReleaseTriggerAction.triggersFor(li)) {
                trigger.context.onFailure(new AbortException("Build of " + li.task.getFullDisplayName() + " was cancelled"));
            }
        }
    }


}
