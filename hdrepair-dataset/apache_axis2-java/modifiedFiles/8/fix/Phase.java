/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package org.apache.axis2.engine;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.PhaseRule;
import org.apache.axis2.phaseresolver.PhaseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A Phase is an ordered collection of Handlers.
 */
public class Phase implements Handler {

	private static final long serialVersionUID = -3352439587370050957L;

	/**
     * Field BOTH_BEFORE_AFTER
     */
    private static final int BOTH_BEFORE_AFTER = 0;

    /**
     * Field BEFORE
     */
    private static final int BEFORE = 1;

    /**
     * Field ANYWHERE
     */
    private static final int ANYWHERE = 3;

    /**
     * Field AFTER
     */
    private static final int AFTER = 2;

    /**
     * Field log
     */
    private Log log = LogFactory.getLog(getClass());

    /**
     * Field handlers
     */
    private ArrayList handlers;

    /**
     * this is want if the phaseFirst and phaseLst same hanlder
     * that is for this phase there is only one phase
     */
    private boolean isOneHanlder;

    /**
     * Field phaseName
     */
    private String phaseName;

    /**
     * Field phasefirstset
     */
    private boolean phasefirstset;

    /**
     * Field phaselastset
     */
    private boolean phaselastset;

    public Phase() {
        this(null);
    }

    /**
     * Constructor Phase.
     *
     * @param phaseName
     */
    public Phase(String phaseName) {
        handlers = new ArrayList();
        this.phaseName = phaseName;
    }

    /**
     * Adds handler to the collection.
     *
     * @param handler
     */
    public void addHandler(Handler handler) {
        log.debug("Handler " + handler.getName() + " added to Phase " + phaseName);

        if (phaselastset) {
            handlers.add(handlers.size() - 2, handler);    // add before phaseLast
        } else {
            handlers.add(handler);
        }
    }

    /**
     * Method addHandler.
     *
     * @param handler
     * @throws PhaseException
     */
    public void addHandler(HandlerDescription handler) throws PhaseException {
        Iterator handlers_itr = getHandlers().iterator();

        while (handlers_itr.hasNext()) {
            Handler hand = (Handler) handlers_itr.next();
            HandlerDescription handlerDesc = hand.getHandlerDesc();

            if (handler.getName().getLocalPart().equals(handlerDesc.getName().getLocalPart())) {
                return;
            }
        }

        if (isOneHanlder) {

            // TODO : should we allow both phaseFirst and phaseLast to be true for one Handler??
            throw new PhaseException(this.getPhaseName()
                    + "can only have one handler, since there is a "
                    + "handler with both phaseFirst and PhaseLast true ");
        }

        if (handler.getRules().isPhaseFirst() && handler.getRules().isPhaseLast()) {
            if (handlers.size() > 0) {
                throw new PhaseException(this.getPhaseName()
                        + " can not have more than one handler "
                        + handler.getName()
                        + " is invalid or incorrect phase rules");
            } else {
                handlers.add(handler.getHandler());
                isOneHanlder = true;
            }
        } else if (handler.getRules().isPhaseFirst()) {
            setPhaseFirst(handler.getHandler());
        } else if (handler.getRules().isPhaseLast()) {
            setPhaseLast(handler.getHandler());
        } else {
            insertHandler(handler);
        }
    }

    /**
     * Method addHandler.
     *
     * @param handler
     * @param index
     */
    public void addHandler(Handler handler, int index) {
        log.debug("Handler " + handler.getName() + "Added to place " + index + " At the Phase "
                + phaseName);
        handlers.add(index, handler);
    }

    public void checkPostConditions(MessageContext msgContext) throws AxisFault {

        // Default version does nothing
    }

    public void checkPreconditions(MessageContext msgContext) throws AxisFault {

        // Default version does nothing
    }

    public void cleanup() throws AxisFault {

        // Default version does nothing
    }

    public void init(HandlerDescription handlerdesc) {

        // Default version does nothing
    }

    /**
     * Method insertAfter.
     *
     * @param handler
     */
    private void insertAfter(Handler handler) throws PhaseException {
        String afterName = handler.getHandlerDesc().getRules().getAfter();

        for (int i = 0; i < handlers.size(); i++) {
            Handler temphandler = (Handler) handlers.get(i);

            if (temphandler.getName().getLocalPart().equals(afterName)) {
                if (phaselastset && (i == handlers.size() - 1)) {
                    throw new PhaseException("Can't insert handler after handler '"
                            + temphandler.getName()
                            + "', which is marked phaseLast");
                }

                handlers.add(i + 1, handler);

                return;
            }
        }

        if (handlers.size() > 0) {
            handlers.add(0, handler);
        } else {
            handlers.add(handler);
        }
    }

    /**
     * Method insertBefore.
     *
     * @param handler
     */
    private void insertBefore(Handler handler) throws PhaseException {
        String beforename = handler.getHandlerDesc().getRules().getBefore();

        for (int i = 0; i < handlers.size(); i++) {
            Handler temphandler = (Handler) handlers.get(i);

            if (temphandler.getName().getLocalPart().equals(beforename)) {
                if (i == 0) {
                    if (phasefirstset) {
                        throw new PhaseException("Can't insert handler before handler '"
                                + temphandler.getName()
                                + "', which is marked phaseFirst");
                    }

                    handlers.add(0, handler);

                    return;
                }

                handlers.add(i - 1, handler);
            }
        }

        // added as last handler
        addHandler(handler);
    }

    /**
     * This method assume that both the before and after cant be a same hander
     * that does not check inside this , it should check befor calling this method
     *
     * @param handler
     * @throws PhaseException
     */
    private void insertBeforeandAfter(Handler handler) throws PhaseException {
        int before = -1;
        int after = -1;
        String beforeName = handler.getHandlerDesc().getRules().getBefore();
        String afterName = handler.getHandlerDesc().getRules().getAfter();

        for (int i = 0; i < handlers.size(); i++) {
            Handler temphandler = (Handler) handlers.get(i);

            if (afterName.equals(temphandler.getName().getLocalPart())) {
                after = i;
            } else {
                if (beforeName.equals(temphandler.getName().getLocalPart())) {
                    before = i;
                }
            }

            if ((after >= 0) && (before >= 0)) {
                break;
            }
        }

        // no point of continue since both the before and after index has found
        if (after > before) {

            // TODO fix me Deepal , (have to check this)
            throw new PhaseException("incorrect handler order for "
                    + handler.getHandlerDesc().getName());
        }

        if ((before == -1) && (after == -1)) {
            addHandler(handler);

            return;
        }

        if (before == -1) {
            addHandler(handler);

            return;
        }

        if (after == -1) {
            if (phasefirstset && (before == 0)) {
                throw new PhaseException("Can't insert handler before handler '"
                        + ((Handler) handlers.get(0)).getName()
                        + "', which is marked phaseFirst");
            }
        }

        handlers.add(before, handler);
    }

    private void insertHandler(HandlerDescription handlerDesc) throws PhaseException {
        Handler handler = handlerDesc.getHandler();
        int type = getBeforeAfter(handler);

        switch (type) {
            case BOTH_BEFORE_AFTER : {
                insertBeforeandAfter(handler);

                break;
            }

            case BEFORE : {
                insertBefore(handler);

                break;
            }

            case AFTER : {
                insertAfter(handler);

                break;
            }

            case ANYWHERE : {
                addHandler(handler);

                break;
            }
        }
    }

    /**
     * invokes all the handlers in this Phase
     *
     * @param msgctx
     * @throws org.apache.axis2.AxisFault
     */
    public final void invoke(MessageContext msgctx) throws AxisFault {
        if (log.isDebugEnabled()) {
            log.debug("Checking pre-condition for Phase \"" + phaseName + "\"");
        }

        int currentIndex = msgctx.getCurrentPhaseIndex();

        if (currentIndex == 0) {
            checkPreconditions(msgctx);
        }

        if (log.isDebugEnabled()) {
            log.debug("Invoking phase \"" + phaseName + "\"");
        }

        while (currentIndex < handlers.size()) {
            Handler handler = (Handler) handlers.get(currentIndex);

            log.debug("Invoking Handler '" + handler.getName() + "' in Phase '" + phaseName + "'");
            handler.invoke(msgctx);

            if (msgctx.isPaused()) {
                return;
            }

            currentIndex++;
            msgctx.setCurrentPhaseIndex(currentIndex);
        }

        if (log.isDebugEnabled()) {
            log.debug("Checking post-conditions for phase \"" + phaseName + "\"");
        }

        msgctx.setCurrentPhaseIndex(0);
        checkPostConditions(msgctx);
    }

    public String toString() {
        return this.getPhaseName();
    }

    /**
     * Method getBeforeAfter.
     *
     * @param handler
     * @return Returns AFTER or ANYWHERE or BOTH_BEFORE_AFTER
     * @throws org.apache.axis2.phaseresolver.PhaseException
     *
     */
    private int getBeforeAfter(Handler handler) throws PhaseException {
        PhaseRule rules = handler.getHandlerDesc().getRules();
        String beforeRules = rules.getBefore();
        String afterRules = rules.getAfter();
        if ((!"".equals(beforeRules))
                && (!"".equals(afterRules))) {
            if (beforeRules.equals(
                    afterRules)) {
                throw new PhaseException(
                        "Both before and after cannot be the same for this handler"
                                + handler.getName());
            }

            return BOTH_BEFORE_AFTER;
        } else if (!"".equals(beforeRules)) {
            return BEFORE;
        } else if (!"".equals(afterRules)) {
            return AFTER;
        } else {
            return ANYWHERE;
        }
    }

    public int getHandlerCount() {
        return handlers.size();
    }

    public HandlerDescription getHandlerDesc() {
        return null;    
    }

    /**
     * Gets all the handlers in the phase.
     *
     * @return Returns an ArrayList of Handlers
     */
    public ArrayList getHandlers() {
        return handlers;
    }

    public QName getName() {
        return new QName(phaseName);
    }

    public Parameter getParameter(String name) {
        return null;    
    }

    /**
     * @return Returns the name.
     */
    public String getPhaseName() {
        return phaseName;
    }

    public void setName(String phaseName) {
        this.phaseName = phaseName;
    }

    /**
     * Method setPhaseFirst.
     *
     * @param phaseFirst
     * @throws PhaseException
     */
    public void setPhaseFirst(Handler phaseFirst) throws PhaseException {
        if (phasefirstset) {
            throw new PhaseException("PhaseFirst has been set already, cannot have two"
                    + " phaseFirst Handler for same phase " + this.getPhaseName());
        } else {
            handlers.add(0, phaseFirst);
            phasefirstset = true;

            // TODO: move this error check to where we read the rules
            if (getBeforeAfter(phaseFirst) != ANYWHERE) {
                throw new PhaseException("Handler with PhaseFirst can not have "
                        + "any before or after proprty error in "
                        + phaseFirst.getName());
            }
        }
    }

    /**
     * Method setPhaseLast.
     *
     * @param phaseLast
     * @throws PhaseException
     */
    public void setPhaseLast(Handler phaseLast) throws PhaseException {
        if (phaselastset) {
            throw new PhaseException("PhaseLast already has been set,"
                    + " cannot have two PhaseLast Handler for same phase "
                    + this.getPhaseName());
        }

        if (handlers.size() == 0) {
            handlers.add(phaseLast);
        } else {
            handlers.add(handlers.size() - 1, phaseLast);
        }

        phaselastset = true;

        // TODO: Move this check to where we read the rules
        if (getBeforeAfter(phaseLast) != ANYWHERE) {
            throw new PhaseException("Handler with PhaseLast property "
                    + "can not have any before or after property error in "
                    + phaseLast.getName());
        }
    }
}
