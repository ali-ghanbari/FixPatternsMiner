package de.benjaminborbe.website.link;

import de.benjaminborbe.html.api.HttpContext;
import de.benjaminborbe.html.api.Widget;
import de.benjaminborbe.website.util.CompositeWidget;
import de.benjaminborbe.website.util.StringWidget;
import de.benjaminborbe.website.util.TagWidget;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LinkMailtoWidget extends CompositeWidget implements Widget {

	private final String email;

	private final Widget content;

	public LinkMailtoWidget(final String email) {
		this(email, new StringWidget(email));
	}

	public LinkMailtoWidget(final String email, final String content) {
		this(email, new StringWidget(content));
	}

	public LinkMailtoWidget(final String email, final Widget content) {
		this.email = email;
		this.content = content;
	}

	@Override
	protected Widget createWidget(final HttpServletRequest request, final HttpServletResponse response, final HttpContext context) throws Exception {
		final TagWidget a = new TagWidget("a", content);
		a.addAttribute("href", "mailto:" + email);
		a.render(request, response, context);
		return a;
	}
}
