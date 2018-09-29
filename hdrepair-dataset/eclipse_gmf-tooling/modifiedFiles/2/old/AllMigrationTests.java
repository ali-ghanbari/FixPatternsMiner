/*
 * Copyright (c) 2006, 2008 Borland Software Corporation
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: dvorak - initial API and implementation
 */
package org.eclipse.gmf.tests.migration;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllMigrationTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.eclipse.gmf.tests.migration"); //$NON-NLS-1$
		//$JUnit-BEGIN$
		suite.addTestSuite(GenericMigrationTest.class);
// COMMENTED OUT for M7 BUILD		suite.addTestSuite(MigrationPatchesTest.class);
		suite.addTestSuite(TestCustomCopier.class);
		//$JUnit-END$
		return suite;
	}

}
