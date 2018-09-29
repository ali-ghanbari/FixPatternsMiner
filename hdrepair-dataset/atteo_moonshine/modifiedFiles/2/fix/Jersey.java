/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atteo.moonshine.jersey;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

import org.atteo.evo.classindex.ClassIndex;
import org.atteo.moonshine.TopLevelService;
import org.atteo.moonshine.services.ImportService;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.sun.jersey.core.util.FeaturesAndProperties;
import com.sun.jersey.freemarker.FreemarkerViewProcessor;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 * Starts Jersey JAX-RS implementation.
 */
@XmlRootElement(name = "jersey")
public class Jersey extends TopLevelService {
	@XmlElement
	@XmlIDREF
	@ImportService
	private org.atteo.moonshine.webserver.ServletContainer servletContainer;

	/**
	 * Prefix under which JAX-RS resources should be registered.
	 */
	@XmlElement
	private String prefix = "";

	/**
	 * Automatically register in Jersey any class marked with
	 * &#064;{@link Path} or &#064;{@link Provider} annotations.
	 * To manually register them simply {@link Binder#bind(Class) bind} them in Guice.
	 */
	@XmlElement
	private boolean discoverResources = true;

	/**
	 * If true, returned XML documents will be formatted for human readability.
	 */
	@XmlElement
	private boolean formatOutput = false;

	@Override
	public Module configure() {
		return new PrivateModule() {
			@Override
			protected void configure() {
				Map<String, String> params = new HashMap<>();
				params.put(ServletContainer.FEATURE_FILTER_FORWARD_ON_404, "true");
				if (formatOutput) {
					params.put(FeaturesAndProperties.FEATURE_FORMATTED, "true");
				}
				params.put(ServletContainer.PROPERTY_FILTER_CONTEXT_PATH, prefix);
				params.put(FreemarkerViewProcessor.FREEMARKER_TEMPLATES_BASE_PATH, "templates");

				bind(GuiceContainer.class);
				servletContainer.addFilter(getProvider(GuiceContainer.class), params, prefix + "/*");

				if (discoverResources) {
					for (Class<?> klass : ClassIndex.getAnnotated(Path.class)) {
						bind(klass);
					}
					for (Class<?> klass : ClassIndex.getAnnotated(Provider.class)) {
						bind(klass);
					}
				}
			}
		};
	}
}
