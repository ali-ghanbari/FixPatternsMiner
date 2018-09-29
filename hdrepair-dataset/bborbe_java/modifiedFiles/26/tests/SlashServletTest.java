package de.benjaminborbe.slash.servlet;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Injector;

import de.benjaminborbe.slash.guice.SlashModulesMock;
import de.benjaminborbe.tools.guice.GuiceInjectorBuilder;

public class SlashServletTest {

	@Test
	public void singleton() {
		final Injector injector = GuiceInjectorBuilder.getInjector(new SlashModulesMock());
		final SlashServlet a = injector.getInstance(SlashServlet.class);
		final SlashServlet b = injector.getInstance(SlashServlet.class);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, b);
	}

	@Test
	public void buildRedirectTargetPath() {
		final SlashServlet slashServlet = new SlashServlet(null);
		final HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
		EasyMock.expect(request.getContextPath()).andReturn("/bb");
		EasyMock.replay(request);
		assertEquals("/bb/website/dashboard", slashServlet.buildRedirectTargetPath(request));
	}
}
