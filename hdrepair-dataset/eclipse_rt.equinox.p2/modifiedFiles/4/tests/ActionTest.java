/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.easymock.EasyMock.*;

import java.io.*;
import java.util.*;
import junit.framework.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

@SuppressWarnings( {"cast", "restriction", "unchecked"})
public abstract class ActionTest extends AbstractProvisioningTest {
	protected static final String COMMA_SEPARATOR = ","; //$NON-NLS-1$
	protected static final String JAR = "jar";//$NON-NLS-1$
	private static final boolean DEBUG = false;

	protected String os = "win32";//$NON-NLS-1$
	protected String ws = "win32";//$NON-NLS-1$
	protected String arch = "x86";//$NON-NLS-1$
	protected String configSpec = AbstractPublisherAction.createConfigSpec(ws, os, arch);//"win32.win32.x86"; // or macosx
	protected String flavorArg = "tooling";//$NON-NLS-1$
	protected String[] topLevel;
	protected AbstractPublisherAction testAction;
	protected IPublisherInfo publisherInfo;
	protected IPublisherResult publisherResult;

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified.
	 */
	public static String[] getArrayFromString(String list, String separator) {
		if (list == null || list.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		List<String> result = new ArrayList<String>();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				result.add(token);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	protected void verifyProvidedCapability(ProvidedCapability[] prov, String namespace, String name, Version version) {
		for (int i = 0; i < prov.length; i++)
			if (prov[i].getName().equalsIgnoreCase(name) && prov[i].getNamespace().equalsIgnoreCase(namespace) && prov[i].getVersion().equals(version))
				return; // pass
		Assert.fail("Missing ProvidedCapability: " + name + version.toString()); //$NON-NLS-1$
	}

	protected void verifyRequiredCapability(RequiredCapability[] required, String namespace, String name, VersionRange range) {
		for (int i = 0; i < required.length; i++)
			if (required[i].getName().equalsIgnoreCase(name) && required[i].getNamespace().equalsIgnoreCase(namespace) && required[i].getRange().equals(range))
				return;
		Assert.fail("Missing RequiredCapability: " + name + range.toString()); //$NON-NLS-1$
	}

	protected IInstallableUnit mockIU(String id, Version version) {
		IInstallableUnit result = createMock(IInstallableUnit.class);
		expect(result.getId()).andReturn(id).anyTimes();
		if (version == null)
			version = Version.emptyVersion;
		expect(result.getVersion()).andReturn(version).anyTimes();
		expect(result.getFilter()).andReturn(null).anyTimes();
		replay(result);
		return result;
	}

	protected Map getFileMap(Map map, File[] files, Path root) {
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory())
				map = getFileMap(map, files[i].listFiles(), root);
			else {
				if (files[i].getPath().endsWith(JAR))
					continue;
				try {
					ByteArrayOutputStream content = new ByteArrayOutputStream();
					File contentBytes = files[i];
					FileUtils.copyStream(new FileInputStream(contentBytes), false, content, true);

					IPath entryPath = new Path(files[i].getAbsolutePath());
					entryPath = entryPath.removeFirstSegments(root.matchingFirstSegments(entryPath));
					entryPath = entryPath.setDevice(null);
					map.put(entryPath.toString(), new Object[] {contentBytes, content.toByteArray()});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return map;
	}

	protected void contains(ProvidedCapability[] capabilities, String namespace, String name, Version version) {
		for (int i = 0; i < capabilities.length; i++) {
			ProvidedCapability capability = capabilities[i];
			if (capability.getNamespace().equals(namespace) && capability.getName().equals(name) && capability.getVersion().equals(version))
				return;
		}
		fail();
	}

	protected void contains(RequiredCapability[] capabilities, String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple) {
		for (int i = 0; i < capabilities.length; i++) {
			RequiredCapability capability = capabilities[i];
			if (filter == null) {
				if (capability.getFilter() != null)
					continue;
			} else if (!filter.equals(capability.getFilter()))
				continue;
			if (multiple != capability.isMultiple())
				continue;
			if (!name.equals(capability.getName()))
				continue;
			if (!namespace.equals(capability.getNamespace()))
				continue;
			if (optional != capability.isOptional())
				continue;
			if (!range.equals(capability.getRange()))
				continue;
			return;
		}
		fail();
	}

	public void setupPublisherResult() {
		publisherResult = new PublisherResult();
	}

	/**
	 * Call this method to setup Publisher Info, not <code>insertPublisherInfoBehavior</code>
	 */
	public void setupPublisherInfo() {
		publisherInfo = createPublisherInfoMock();

		String[] config = getArrayFromString(configSpec, COMMA_SEPARATOR);
		expect(publisherInfo.getConfigurations()).andReturn(config).anyTimes();
		insertPublisherInfoBehavior();
		replay(publisherInfo);
	}

	/**
	 * Creates the mock object for the IPublisherInfo. Subclasses
	 * can override to create a nice or strict mock instead.
	 * @return The publisher info mock
	 * @see org.easymock.EasyMock#createNiceMock(Class)
	 * @see org.easymock.EasyMock#createStrictMock(Class)
	 */
	protected IPublisherInfo createPublisherInfoMock() {
		return createMock(IPublisherInfo.class);
	}

	/**
	 * Do not call this method, it is called by <code>setupPublisherInfo</code>.
	 */
	protected void insertPublisherInfoBehavior() {
		//leave empty, for easy insertion of publisherInfo mock
	}

	public void cleanup() {
		publisherInfo = null;
		publisherResult = null;
	}

	/**
	 * Prints a message used for debugging tests.
	 */
	public void debug(String message) {
		if (DEBUG)
			debug(message);
	}
}
