/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import java.io.File;
import java.io.IOException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class LauncherConfigLocationTest extends AbstractFwkAdminTest {

	public LauncherConfigLocationTest(String name) {
		super(name);
	}	
	
	public void testCustomLauncherConfig() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		startSimpleConfiguratormManipulator();
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = Activator.getContext().getDataFile(LauncherConfigLocationTest.class.getName());
		if(installFolder.exists())
			delete(installFolder);
		
		File configurationFolder = new File(installFolder, "configuration");
		String launcherName = "foo";

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));
		
		File defaultlaunchConfig = new File(installFolder, launcherName + ".ini");
		assertEquals(defaultlaunchConfig.exists(), false);
		File launchConfig = new File(installFolder, "mylaunch.ini");
		assertEquals(launchConfig.exists(), false);
		launcherData.setLauncherConfigLocation(launchConfig);
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}
		
		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar")).toExternalForm(), 0, true);
		BundleInfo configuratorBi = new BundleInfo("org.eclipse.equinox.simpleconfigurator", "1.0.0", FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar")).toExternalForm(), 1, true);

		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.getConfigData().addBundle(configuratorBi);

		manipulator.save(false);

		assertEquals(launchConfig.exists(), true);
		assertEquals(defaultlaunchConfig.exists(), false);
	}
}
