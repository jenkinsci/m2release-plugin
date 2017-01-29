package org.jvnet.hudson.plugins.m2release.pipeline;

import static java.util.logging.Level.WARNING;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import hudson.AbortException;
import hudson.Extension;
import hudson.console.ModelHyperlinkNote;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

/**
 * copied from org.jenkinsci.plugins.workflow.support.steps.build.BuildTriggerListener
 */
@Extension
public class M2ReleaseTriggerListener extends RunListener<Run<?,?>>{

    private static final Logger LOGGER = Logger.getLogger(M2ReleaseTriggerListener.class.getName());

    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {
        for (M2ReleaseTriggerAction.Trigger trigger : M2ReleaseTriggerAction.triggersFor(run)) {
            StepContext stepContext = trigger.context;
            if (stepContext != null && stepContext.isReady()) {
                LOGGER.log(Level.FINE, "started releasing {0} from #{1} in {2}", new Object[] {run, run.getQueueId(), stepContext});
                try {
                    TaskListener taskListener = stepContext.get(TaskListener.class);
                    // encodeTo(Run) calls getDisplayName, which does not include the project name.
                    taskListener.getLogger().println("Starting releasing: " + ModelHyperlinkNote.encodeTo("/" + run.getUrl(), run.getFullDisplayName()));
                } catch (Exception e) {
                    LOGGER.log(WARNING, null, e);
                }
            } else {
                LOGGER.log(Level.FINE, "{0} unavailable in {1}", new Object[] {stepContext, run});
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation") // TODO 2.30+ use removeAction
    public void onCompleted(Run<?,?> run, @Nonnull TaskListener listener) {
        for (M2ReleaseTriggerAction.Trigger trigger : M2ReleaseTriggerAction.triggersFor(run)) {
            LOGGER.log(Level.FINE, "completing {0} for {1}", new Object[] {run, trigger.context});
            if (run.getResult() == Result.SUCCESS) {
                if (trigger.interruption == null) {
                    trigger.context.onSuccess(new RunWrapper(run, false));
                } else {
                    trigger.context.onFailure(trigger.interruption);
                }
            } else {
                trigger.context.onFailure(new AbortException(run.getFullDisplayName() + " completed with status " + run.getResult()));
            }
        }
        run.getActions().removeAll(run.getActions(M2ReleaseTriggerAction.class));
    }

    @Override
    public void onDeleted(Run<?,?> run) {
        for (M2ReleaseTriggerAction.Trigger trigger : M2ReleaseTriggerAction.triggersFor(run)) {
            trigger.context.onFailure(new AbortException(run.getFullDisplayName() + " was deleted"));
        }
    }
}
