package de.benjaminborbe.website.servlet;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.inject.Injector;

import de.benjaminborbe.tools.guice.GuiceInjectorBuilder;
import de.benjaminborbe.website.guice.WebsiteModulesMock;
import de.benjaminborbe.website.servlet.WebsiteSearchServlet;

public class WebsiteSearchServletTest {

	@Test
	public void singleton() {
		final Injector injector = GuiceInjectorBuilder.getInjector(new WebsiteModulesMock());
		final WebsiteSearchServlet a = injector.getInstance(WebsiteSearchServlet.class);
		final WebsiteSearchServlet b = injector.getInstance(WebsiteSearchServlet.class);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, b);
	}

}
