/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.e4.ui.tests.application;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.core.services.context.EclipseContextFactory;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.MApplicationFactory;
import org.eclipse.e4.ui.model.application.MMenu;
import org.eclipse.e4.ui.model.application.MTestHarness;
import org.eclipse.e4.ui.model.application.MWindow;
import org.eclipse.e4.ui.services.events.IEventBroker;
import org.eclipse.e4.workbench.ui.UIEvents;
import org.eclipse.e4.workbench.ui.UIEvents.ApplicationElement;
import org.eclipse.e4.workbench.ui.UIEvents.Command;
import org.eclipse.e4.workbench.ui.UIEvents.Context;
import org.eclipse.e4.workbench.ui.UIEvents.Contribution;
import org.eclipse.e4.workbench.ui.UIEvents.Dirtyable;
import org.eclipse.e4.workbench.ui.UIEvents.ElementContainer;
import org.eclipse.e4.workbench.ui.UIEvents.EventTags;
import org.eclipse.e4.workbench.ui.UIEvents.Input;
import org.eclipse.e4.workbench.ui.UIEvents.Parameter;
import org.eclipse.e4.workbench.ui.UIEvents.UIElement;
import org.eclipse.e4.workbench.ui.UIEvents.UILabel;
import org.eclipse.e4.workbench.ui.UIEvents.Window;
import org.eclipse.e4.workbench.ui.internal.UIEventPublisher;
import org.eclipse.emf.common.notify.Notifier;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class UIEventsTest extends HeadlessApplicationElementTest {

	class EventTester {
		String testerName;
		IEventBroker eventBroker;
		String topic;
		String[] attIds;
		boolean[] hasFired;

		EventHandler attListener = new EventHandler() {
			public void handleEvent(Event event) {
				assertTrue(event.getTopic().equals(topic),
						"Incorrect Topic: " + event.getTopic()); //$NON-NLS-1$

				String attId = (String) event.getProperty(EventTags.ATTNAME);
				int attIndex = getAttIndex(attId);
				assertTrue(attIndex >= 0, "Unknown Attribite: " + attId); //$NON-NLS-1$
				hasFired[attIndex] = true;
			}
		};

		public EventTester(String name, String topic, String[] attIds,
				IEventBroker eventBroker) {
			this.testerName = name;
			this.topic = UIEvents.buildTopic(topic);
			this.attIds = attIds;
			this.eventBroker = eventBroker;

			hasFired = new boolean[attIds.length];
			reset();

			eventBroker.subscribe(this.topic, attListener);
		}

		/**
		 * @param b
		 * @param string
		 */
		protected void assertTrue(boolean b, String string) {
		}

		/**
		 * @param attId
		 * @return
		 */
		protected int getAttIndex(String attId) {
			for (int i = 0; i < attIds.length; i++) {
				if (attIds[i].equals(attId))
					return i;
			}
			return -1;
		}

		public void dispose() {
			eventBroker.unsubscribe(attListener);
		}

		public void reset() {
			for (int i = 0; i < hasFired.length; i++)
				hasFired[i] = false;
		}

		public String[] getAttIds(boolean fired) {
			List<String> atts = new ArrayList<String>();
			for (int i = 0; i < hasFired.length; i++) {
				if (hasFired[i] == fired)
					atts.add(attIds[i]);
			}

			return (String[]) atts.toArray(new String[atts.size()]);
		}
	}

	public class AppElementTester extends EventTester {
		AppElementTester(IEventBroker eventBroker) {
			super("AppElement", ApplicationElement.TOPIC,
					new String[] { ApplicationElement.ID }, eventBroker);
		}
	}

	public class CommandTester extends EventTester {
		CommandTester(IEventBroker eventBroker) {
			super("Command", Command.TOPIC,
					new String[] { Command.COMMANDNAME }, eventBroker);
		}
	}

	public class ContextTester extends EventTester {
		ContextTester(IEventBroker eventBroker) {
			super("Context", Context.TOPIC, new String[] { Context.CONTEXT,
					Context.VARIABLES }, eventBroker);
		}
	}

	public class ContributionTester extends EventTester {
		ContributionTester(IEventBroker eventBroker) {
			super("Contribution", Contribution.TOPIC, new String[] {
					Contribution.URI, Contribution.PERSISTEDSTATE,
					Contribution.OBJECT }, eventBroker);
		}
	}

	public class ElementContainerTester extends EventTester {
		ElementContainerTester(IEventBroker eventBroker) {
			super("ElementContainer", ElementContainer.TOPIC, new String[] {
					ElementContainer.CHILDREN, ElementContainer.ACTIVECHILD },
					eventBroker);
		}
	}

	public class DirtyableTester extends EventTester {
		DirtyableTester(IEventBroker eventBroker) {
			super("Dirtyable", Dirtyable.TOPIC,
					new String[] { Dirtyable.DIRTY }, eventBroker);
		}
	}

	public class InputTester extends EventTester {
		InputTester(IEventBroker eventBroker) {
			super("Input", Input.TOPIC, new String[] { Input.INPUTURI },
					eventBroker);
		}
	}

	public class ParameterTester extends EventTester {
		ParameterTester(IEventBroker eventBroker) {
			super("Parameter", Parameter.TOPIC, new String[] { Parameter.TAG,
					Parameter.VALUE }, eventBroker);
		}
	}

	public class UIElementTester extends EventTester {
		UIElementTester(IEventBroker eventBroker) {
			super("UIElement", UIElement.TOPIC, new String[] {
					UIElement.RENDERER, UIElement.TOBERENDERED,
					UIElement.PARENT, UIElement.ONTOP, UIElement.VISIBLE,
					UIElement.WIDGET }, eventBroker);
		}
	}

	public class UIItemTester extends EventTester {
		UIItemTester(IEventBroker eventBroker) {
			super("UIItem", UILabel.TOPIC, new String[] { UILabel.LABEL,
					UILabel.ICONURI, UILabel.TOOLTIP }, eventBroker);
		}
	}

	public class WindowTester extends EventTester {
		WindowTester(IEventBroker eventBroker) {
			super("Window", Window.TOPIC, new String[] { Window.MAINMENU,
					Window.X, Window.Y, Window.WIDTH, Window.HEIGHT },
					eventBroker);
		}
	}

	@Override
	protected MApplicationElement createApplicationElement(
			IEclipseContext appContext) throws Exception {
		MApplication application = MApplicationFactory.eINSTANCE
				.createApplication();
		application.getChildren().add(
				MApplicationFactory.eINSTANCE.createWindow());
		return application;
	}

	public void testAllTopics() {
		IEventBroker eventBroker = (IEventBroker) applicationContext
				.get(IEventBroker.class.getName());

		// Create a tester for each topic
		AppElementTester appTester = new AppElementTester(eventBroker);
		CommandTester commandTester = new CommandTester(eventBroker);
		ContextTester contextTester = new ContextTester(eventBroker);
		ContributionTester contributionTester = new ContributionTester(
				eventBroker);
		ElementContainerTester elementContainerTester = new ElementContainerTester(
				eventBroker);
		DirtyableTester dirtyableTester = new DirtyableTester(eventBroker);
		InputTester inputTester = new InputTester(eventBroker);
		ParameterTester parameterTester = new ParameterTester(eventBroker);
		UIElementTester uiElementTester = new UIElementTester(eventBroker);
		UIItemTester uiItemTester = new UIItemTester(eventBroker);
		WindowTester windowTester = new WindowTester(eventBroker);

		// Create an array to check for 'cross talk' (i.e. events being fired
		// on incorrect topics
		EventTester[] allTesters = { appTester, commandTester, contextTester,
				contributionTester, elementContainerTester, inputTester,
				parameterTester, uiElementTester, uiItemTester, windowTester };

		// Create the test harness and hook up the event publisher
		MTestHarness allData = MApplicationFactory.eINSTANCE
				.createTestHarness();
		((Notifier) allData).eAdapters().add(
				new UIEventPublisher(applicationContext));

		// AppElement
		reset(allTesters);
		String newId = "Some New Id";
		allData.setId(newId);
		checkForFailures(allTesters, appTester);

		// Test that no-ops don't throw events
		appTester.reset();
		allData.setId(newId);
		assertTrue("event thrown on No-Op",
				appTester.getAttIds(true).length == 0);

		// Command
		reset(allTesters);
		IEclipseContext newContext = EclipseContextFactory.create();
		allData.setContext(newContext);
		allData.getVariables().add("foo");
		checkForFailures(allTesters, contextTester);

		// Context
		reset(allTesters);
		allData.setContext(EclipseContextFactory.create());
		allData.getVariables().add("A var");
		checkForFailures(allTesters, contextTester);

		// Contribution
		reset(allTesters);
		allData.setURI("Some URI");
		allData.setObject("Some onbject");
		allData.setPersistedState("Some state");
		checkForFailures(allTesters, contributionTester);

		// ElementContainer
		reset(allTesters);
		MMenu menu = MApplicationFactory.eINSTANCE.createMenu();
		allData.getChildren().add(menu);
		allData.setActiveChild(menu);
		checkForFailures(allTesters, elementContainerTester);

		// Input
		reset(allTesters);
		allData.setInputURI("New Input Uri");
		checkForFailures(allTesters, inputTester);

		// Dirtyable
		reset(allTesters);
		allData.setDirty(!allData.isDirty());
		checkForFailures(allTesters, dirtyableTester);

		// Parameter
		reset(allTesters);
		allData.setTag("New Tag");
		allData.setValue("New Value");
		checkForFailures(allTesters, parameterTester);

		// UIElement
		reset(allTesters);
		MTestHarness newParent = MApplicationFactory.eINSTANCE
				.createTestHarness();
		allData.setRenderer("New Renderer");
		allData.setParent(newParent);
		allData.setToBeRendered(!allData.isToBeRendered());
		allData.setVisible(!allData.isVisible());
		allData.setOnTop(!allData.isOnTop());
		allData.setWidget("New Widget");
		checkForFailures(allTesters, uiElementTester);

		// UIItem
		reset(allTesters);
		allData.setLabel("New Name");
		allData.setIconURI("New Icon URI");
		allData.setTooltip("New Tooltip");
		checkForFailures(allTesters, uiItemTester);

		// Window tests
		reset(allTesters);
		MWindow window = ((MApplication) applicationElement).getChildren().get(
				0);
		window.setX(1234);
		window.setY(1234);
		window.setWidth(1234);
		window.setHeight(1234);

		MMenu newMainMenu = MApplicationFactory.eINSTANCE.createMenu();
		window.setMainMenu(newMainMenu);
		checkForFailures(allTesters, windowTester);
	}

	/**
	 * @param allTesters
	 * @param tester
	 */
	private void checkForFailures(EventTester[] allTesters, EventTester tester) {
		ensureAllSet(tester);
		ensureNoCrossTalk(allTesters, tester);
	}

	/**
	 * Ensures that no events were picked up from topics other than the one we
	 * expect to see changes in.
	 * 
	 * @param tester
	 */
	private void ensureNoCrossTalk(EventTester[] allTesters, EventTester skipMe) {
		List<EventTester> badTesters = new ArrayList<EventTester>();
		for (EventTester t : allTesters) {
			if (t.equals(skipMe))
				continue;

			if (t.getAttIds(true).length > 0)
				badTesters.add(t);
		}

		if (badTesters.size() > 0) {
			String msg = "Events were fired in the wrong topic(s): "
					+ badTesters;
			fail(msg);
		}
	}

	/**
	 * @param tester
	 */
	private void ensureAllSet(EventTester tester) {
		String[] unfiredIds = tester.getAttIds(false);
		if (unfiredIds.length > 0) {
			String msg = "No event fired:" + unfiredIds;
			for (int i = 0; i < unfiredIds.length; i++) {
				msg += ' ' + unfiredIds[i];
			}
			fail(msg);
		}
	}

	/**
	 * @param allTesters
	 */
	private void reset(EventTester[] allTesters) {
		for (EventTester t : allTesters) {
			t.reset();
		}
	}
}
