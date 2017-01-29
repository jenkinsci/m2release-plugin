package org.jvnet.hudson.plugins.m2release.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.InvisibleAction;
import hudson.model.Queue;
import hudson.model.queue.FoldableAction;

/**
 * copied from org.jenkinsci.plugins.workflow.support.steps.build.BuildTriggerAction
 */
@SuppressWarnings("SynchronizeOnNonFinalField")
class M2ReleaseTriggerAction extends InvisibleAction implements FoldableAction {

    private static final Logger LOGGER = Logger.getLogger(M2ReleaseTriggerAction.class.getName());

    @Deprecated
    private StepContext context;

    /** Record of one upstream build step. */
    static class Trigger {

        final StepContext context;


        /** Record of cancellation cause passed to {@link M2ReleaseStep$Execution#stop}, if any. */
        @CheckForNull Throwable interruption;

        Trigger(StepContext context) {
            this.context = context;
        }

    }

    private /* final */ List<Trigger> triggers;

    M2ReleaseTriggerAction(StepContext context) {
        triggers = new ArrayList<>();
        triggers.add(new Trigger(context));
    }

    private Object readResolve() {
        if (triggers == null) {
            triggers = new ArrayList<>();
            triggers.add(new Trigger(context));
            context = null;
        }
        return this;
    }

    static Iterable<Trigger> triggersFor(Actionable actionable) {
        List<Trigger> triggers = new ArrayList<>();
        for (M2ReleaseTriggerAction action : actionable.getActions(M2ReleaseTriggerAction.class)) {
            synchronized (action.triggers) {
                triggers.addAll(action.triggers);
            }
        }
        return triggers;
    }

    @Override public void foldIntoExisting(Queue.Item item, Queue.Task owner, List<Action> otherActions) {
        // there may be >1 upstream builds (or other unrelated causes) for a single downstream build
        M2ReleaseTriggerAction existing = item.getAction(M2ReleaseTriggerAction.class);
        if (existing == null) {
            item.addAction(this);
        } else {
            synchronized (existing.triggers) {
                existing.triggers.addAll(triggers);
            }
        }
        LOGGER.log(Level.FINE, "coalescing actions for {0}", item);
    }

}
