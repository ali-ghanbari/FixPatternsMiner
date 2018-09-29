/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.net4j.util.tests;

import org.eclipse.net4j.util.io.IOUtil;
import org.eclipse.net4j.util.om.OMBundle;

import java.io.InputStream;
import java.net.URL;

/**
 * @author Eike Stepper
 */
public class OMTest
{
  /**
   * Requires this class to be exported to a jar file in this project's plugins/ folder!<br>
   * Also requires an export of the net4j.util bundle in the same folder!
   */
  public static void main(String[] args) throws Exception
  {
    OMBundle bundle = org.eclipse.net4j.internal.util.bundle.OM.BUNDLE;

    URL baseUrl = bundle.getBaseURL();
    IOUtil.OUT().println(baseUrl);
    IOUtil.OUT().println();

    InputStream stream = bundle.getInputStream("/plugin.xml"); //$NON-NLS-1$

    try
    {
      IOUtil.copy(stream, IOUtil.OUT());
    }
    finally
    {
      IOUtil.close(stream);
    }
  }
}
