/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.action;

import static org.jboss.seam.international.StatusMessage.Severity.ERROR;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.faces.event.ValueChangeEvent;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.framework.EntityHome;
import org.zanata.async.AsyncTaskHandle;
import org.zanata.async.SimpleAsyncTask;
import org.zanata.dao.TransMemoryDAO;
import org.zanata.exception.EntityMissingException;
import org.zanata.model.tm.TransMemory;
import org.zanata.rest.service.TranslationMemoryResourceService;
import org.zanata.service.AsyncTaskManagerService;
import org.zanata.service.SlugEntityService;
import org.zanata.util.ServiceLocator;

/**
 * Controller class for the Translation Memory UI.
 *
 * @author Carlos Munoz <a
 *         href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Name("translationMemoryAction")
@Restrict("#{s:hasRole('admin')}")
@Slf4j
public class TranslationMemoryAction extends EntityHome<TransMemory> {
    @In
    private TransMemoryDAO transMemoryDAO;

    @In
    private TranslationMemoryResourceService translationMemoryResource;

    @In
    private SlugEntityService slugEntityServiceImpl;

    @In
    private AsyncTaskManagerService asyncTaskManagerServiceImpl;

    private List<TransMemory> transMemoryList;

    /**
     * Stores the last process handle, in page scope (ie for this user).
     */
    @In(scope = ScopeType.PAGE, required = false)
    @Out(scope = ScopeType.PAGE, required = false)
    private AsyncTaskHandle myTaskHandle;

    /**
     * Stores the last process error, but only for the duration of the event.
     */
    private String myProcessError;

    public List<TransMemory> getAllTranslationMemories() {
        if (transMemoryList == null) {
            transMemoryList = transMemoryDAO.findAll();
        }
        return transMemoryList;
    }

    public void verifySlugAvailable(ValueChangeEvent e) {
        String slug = (String) e.getNewValue();
        validateSlug(slug, e.getComponent().getId());
    }

    public boolean validateSlug(String slug, String componentId) {
        if (!slugEntityServiceImpl.isSlugAvailable(slug, TransMemory.class)) {
            FacesMessages.instance().addToControl(componentId,
                    "This Id is not available");
            return false;
        }
        return true;
    }

    public void clearTransMemory(final String transMemorySlug) {
        String name = "TranslationMemoryAction.clearTransMemory: "+transMemorySlug;
        asyncTaskManagerServiceImpl.startTask(new SimpleAsyncTask<Void>(name) {
            @Override
            public Void call() throws Exception {
                TranslationMemoryResourceService tmResource =
                        ServiceLocator.instance().getInstance(
                                "translationMemoryResource",
                                TranslationMemoryResourceService.class);
                String msg =
                        tmResource.deleteTranslationUnitsUnguarded(
                                transMemorySlug).toString();
                return null;
            }
        }, new ClearTransMemoryProcessKey(transMemorySlug));

        transMemoryList = null; // Force refresh next time list is requested
    }

    private boolean isProcessing() {
        return myTaskHandle != null;
    }

    public boolean isProcessErrorPollEnabled() {
        // No need to poll just for process erorrs if we are already polling the
        // table
        return isProcessing() && !isTablePollEnabled();
    }

    /**
     * Gets the error (if any) for this user's last Clear operation, if it
     * finished since the last poll. NB: If the process has just finished, this
     * method will return the error only until the event scope exists.
     *
     * @return
     */
    public String getProcessError() {
        if (myProcessError != null)
            return myProcessError;
        if (myTaskHandle != null && myTaskHandle.isDone()) {
            try {
                myTaskHandle.get();
            } catch (InterruptedException e) {
                // no error, just interrupted
            } catch (ExecutionException e) {
                // remember the result, just until this event finishes
                this.myProcessError =
                        e.getCause() != null ? e.getCause().getMessage() : "";
            }
            myTaskHandle = null;
            return myProcessError;
        }
        return "";
    }

    @Transactional
    public void deleteTransMemory(String transMemorySlug) {
        try {
            translationMemoryResource.deleteTranslationMemory(transMemorySlug);
            transMemoryList = null; // Force refresh next time list is requested
        } catch (EntityMissingException e) {
            FacesMessages.instance().addFromResourceBundle(ERROR,
                    "jsf.transmemory.TransMemoryNotFound");
        }
    }

    public boolean isTransMemoryBeingCleared(String transMemorySlug) {
        AsyncTaskHandle<Void> handle =
                asyncTaskManagerServiceImpl
                        .getHandleByKey(new ClearTransMemoryProcessKey(
                                transMemorySlug));
        return handle != null && !handle.isDone();
    }

    public boolean deleteTransMemoryDisabled(String transMemorySlug) {
        // Translation memories have to be cleared before deleting them
        return getTranslationMemorySize(transMemorySlug) > 0;
    }

    public boolean isTablePollEnabled() {
        // Poll is enabled only when there is something being cleared
        for (TransMemory tm : transMemoryList) {
            if (isTransMemoryBeingCleared(tm.getSlug())) {
                return true;
            }
        }
        return false;
    }

    public long getTranslationMemorySize(String tmSlug) {
        return transMemoryDAO.getTranslationMemorySize(tmSlug);
    }

    public String cancel() {
        // Navigation logic in pages.xml
        return "cancel";
    }

    @Override
    @Transactional
    public String persist() {
        if (!validateSlug(getInstance().getSlug(), "slug")) {
            return null;
        }

        return super.persist();
    }

    /**
     * Represents a key to index a translation memory clear process.
     *
     * NB: Eventually this class might need to live outside if there are other
     * services that need to control this process.
     */
    @AllArgsConstructor
    @EqualsAndHashCode
    private class ClearTransMemoryProcessKey implements Serializable {
        private String slug;
    }
}
