package de.benjaminborbe.bridge;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public final class ProvisionActivator implements BundleActivator {

	private final class RunStartBundle implements Runnable {

		private final Bundle bundle;

		private RunStartBundle(final Bundle bundle) {
			this.bundle = bundle;
		}

		@Override
		public void run() {
			try {
				servletContext.log("Starting bundle [" + bundle.getSymbolicName() + "]");
				bundle.start();
			}
			catch (final Exception e) {
			}
		}
	}

	private static final String BUNDLE_PREFIX = "bb.bundle.";

	private static final boolean THREAD_START = false;

	private static final String CONFIG = "/WEB-INF/activator.properties";

	private final ServletContext servletContext;

	private List<Bundle> installed;

	public ProvisionActivator(final ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void start(final BundleContext context) throws Exception {
		servletContext.setAttribute(BundleContext.class.getName(), context);

		installed = new ArrayList<Bundle>();

		for (final URL url : orderBundles(findBundles(), getProperties())) {
			servletContext.log("Installing bundle [" + url + "]");
			final Bundle bundle = context.installBundle(url.toExternalForm());
			installed.add(bundle);
		}

		servletContext.log("Installing bundles done");

		for (final Bundle bundle : installed) {
			if (THREAD_START) {

				final Thread thread = new Thread(new RunStartBundle(bundle), "start felix-osgi-bundle");
				thread.start();
			}
			else {
				servletContext.log("Starting bundle [" + bundle.getSymbolicName() + "]");
				bundle.start();
			}
		}

		servletContext.log("Starting bundles done");
	}

	/**
	 * Nimmt die Liste der uebergebenen Bundles und versucht sie anhand der Konfig zu
	 * sortieren
	 */
	protected List<URL> orderBundles(final List<URL> bundles, final Properties props) {
		final List<URL> result = new ArrayList<URL>();
		final Set<URL> availableBundles = new HashSet<URL>(bundles);
		// Reinfolge aus Konfig lesen
		final List<String> installBundles = getInstallConfigOrders(BUNDLE_PREFIX, props);

		servletContext.log("install config defined bundles started");
		// Wert aus Konfig durch gehen und einfuegen falls vorhanden
		for (final String i : installBundles) {
			final String installBundle = i.trim();
			servletContext.log("installBundle: \"" + installBundle + "\"");
			for (final URL b : availableBundles) {
				servletContext.log(b.getFile() + " contains " + installBundle);
				if (b.getFile().indexOf(installBundle) != -1) {
					servletContext.log(b.getFile() + " contains " + installBundle + " => true");
					result.add(b);
				}
			}
		}
		servletContext.log("install config defined bundles finished");

		// Alle bereits hinzugefuegten aus availableBundles entfernen
		availableBundles.removeAll(result);

		// Alle die nicht ueber Konfig gefunden wurden noch hinten anfuegen => Reinfolge
		// undefiniert
		result.addAll(availableBundles);
		for (final URL url : result) {
			servletContext.log("orderd bundle [" + url + "]");
		}
		return result;
	}

	protected List<String> getInstallConfigOrders(final String prefix, final Properties props) {
		final List<String> result = new ArrayList<String>();
		int i = 1;
		while (props.containsKey(prefix + i)) {
			result.add(props.getProperty(prefix + i).toString());
			i++;
		}
		return result;
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		servletContext.setAttribute(BundleContext.class.getName(), context);

		for (final Bundle bundle : installed) {
			bundle.stop();
		}
		installed.clear();
	}

	private List<URL> findBundles() throws Exception {
		final ArrayList<URL> list = new ArrayList<URL>();
		for (final Object o : this.servletContext.getResourcePaths("/WEB-INF/bundles/")) {
			final String name = (String) o;
			if (name.endsWith(".jar")) {
				final URL url = servletContext.getResource(name);
				if (url != null) {
					list.add(url);
				}
			}
		}
		for (final URL url : list) {
			servletContext.log("found bundle [" + url + "]");
		}

		return list;
	}

	public Properties getProperties() {
		final Properties props = new Properties();
		try {
			props.load(servletContext.getResourceAsStream(CONFIG));
		}
		catch (final IOException e) {
			servletContext.log("load framework.properties failed", e);
		}
		return props;
	}

}
