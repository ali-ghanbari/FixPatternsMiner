//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * Asynchronous ProxyServlet.
 * <p/>
 * Forwards requests to another server either as a standard web reverse proxy
 * (as defined by RFC2616) or as a transparent reverse proxy.
 * <p/>
 * To facilitate JMX monitoring, the {@link HttpClient} instance is set as context attribute,
 * prefixed with the servlet's name and exposed by the mechanism provided by
 * {@link ContextHandler#MANAGED_ATTRIBUTES}.
 * <p/>
 * The following init parameters may be used to configure the servlet:
 * <ul>
 * <li>hostHeader - forces the host header to a particular value</li>
 * <li>viaHost - the name to use in the Via header: Via: http/1.1 &lt;viaHost&gt;</li>
 * <li>whiteList - comma-separated list of allowed proxy hosts</li>
 * <li>blackList - comma-separated list of forbidden proxy hosts</li>
 * </ul>
 * <p/>
 * In addition, see {@link #createHttpClient()} for init parameters used to configure
 * the {@link HttpClient} instance.
 *
 * @see ConnectHandler
 */
public class ProxyServlet extends HttpServlet
{
    protected static final String ASYNC_CONTEXT = ProxyServlet.class.getName() + ".asyncContext";
    private static final Set<String> HOP_HEADERS = new HashSet<>();
    static
    {
        HOP_HEADERS.add("proxy-connection");
        HOP_HEADERS.add("connection");
        HOP_HEADERS.add("keep-alive");
        HOP_HEADERS.add("transfer-encoding");
        HOP_HEADERS.add("te");
        HOP_HEADERS.add("trailer");
        HOP_HEADERS.add("proxy-authorization");
        HOP_HEADERS.add("proxy-authenticate");
        HOP_HEADERS.add("upgrade");
    }

    private final Set<String> _whiteList = new HashSet<>();
    private final Set<String> _blackList = new HashSet<>();

    protected Logger _log;
    private String _hostHeader;
    private String _viaHost;
    private HttpClient _client;
    private long _timeout;

    @Override
    public void init() throws ServletException
    {
        _log = createLogger();

        ServletConfig config = getServletConfig();

        _hostHeader = config.getInitParameter("hostHeader");

        _viaHost = config.getInitParameter("viaHost");
        if (_viaHost == null)
            _viaHost = viaHost();

        try
        {
            _client = createHttpClient();

            // Put the HttpClient in the context to leverage ContextHandler.MANAGED_ATTRIBUTES
            getServletContext().setAttribute(config.getServletName() + ".HttpClient", _client);

            String whiteList = config.getInitParameter("whiteList");
            if (whiteList != null)
                getWhiteListHosts().addAll(parseList(whiteList));

            String blackList = config.getInitParameter("blackList");
            if (blackList != null)
                getBlackListHosts().addAll(parseList(blackList));
        }
        catch (Exception e)
        {
            throw new ServletException(e);
        }
    }

    public long getTimeout()
    {
        return _timeout;
    }

    public void setTimeout(long timeout)
    {
        this._timeout = timeout;
    }

    public Set<String> getWhiteListHosts()
    {
        return _whiteList;
    }

    public Set<String> getBlackListHosts()
    {
        return _blackList;
    }

    protected static String viaHost()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException x)
        {
            return "localhost";
        }
    }

    /**
     * @return a logger instance with a name derived from this servlet's name.
     */
    protected Logger createLogger()
    {
        String name = getServletConfig().getServletName();
        name = name.replace('-', '.');
        return Log.getLogger(name);
    }

    public void destroy()
    {
        try
        {
            _client.stop();
        }
        catch (Exception x)
        {
            _log.debug(x);
        }
    }

    /**
     * Creates a {@link HttpClient} instance, configured with init parameters of this servlet.
     * <p/>
     * The init parameters used to configure the {@link HttpClient} instance are:
     * <table>
     * <thead>
     * <tr>
     * <th>init-param</th>
     * <th>default</th>
     * <th>description</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <td>maxThreads</td>
     * <td>256</td>
     * <td>The max number of threads of HttpClient's Executor.  If not set, or set to the value of "-", then the
     * Jetty server thread pool will be used.</td>
     * </tr>
     * <tr>
     * <td>maxConnections</td>
     * <td>32768</td>
     * <td>The max number of connections per destination, see {@link HttpClient#setMaxConnectionsPerDestination(int)}</td>
     * </tr>
     * <tr>
     * <td>idleTimeout</td>
     * <td>30000</td>
     * <td>The idle timeout in milliseconds, see {@link HttpClient#setIdleTimeout(long)}</td>
     * </tr>
     * <tr>
     * <td>timeout</td>
     * <td>60000</td>
     * <td>The total timeout in milliseconds, see {@link Request#timeout(long, TimeUnit)}</td>
     * </tr>
     * <tr>
     * <td>requestBufferSize</td>
     * <td>HttpClient's default</td>
     * <td>The request buffer size, see {@link HttpClient#setRequestBufferSize(int)}</td>
     * </tr>
     * <tr>
     * <td>responseBufferSize</td>
     * <td>HttpClient's default</td>
     * <td>The response buffer size, see {@link HttpClient#setResponseBufferSize(int)}</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @return a {@link HttpClient} configured from the {@link #getServletConfig() servlet configuration}
     * @throws ServletException if the {@link HttpClient} cannot be created
     */
    protected HttpClient createHttpClient() throws ServletException
    {
        ServletConfig config = getServletConfig();

        HttpClient client = newHttpClient();
        
        // Redirects must be proxied as is, not followed
        client.setFollowRedirects(false);

        // Must not store cookies, otherwise cookies of different clients will mix
        client.setCookieStore(new HttpCookieStore.Empty());

        Executor executor;
        String value = config.getInitParameter("maxThreads");
        if (value == null || "-".equals(value))
        {
            executor = (Executor)getServletContext().getAttribute("org.eclipse.jetty.server.Executor");
        }
        else
        {
            QueuedThreadPool qtp= new QueuedThreadPool(Integer.parseInt(value));
            String servletName = config.getServletName();
            int dot = servletName.lastIndexOf('.');
            if (dot >= 0)
                servletName = servletName.substring(dot + 1);
            qtp.setName(servletName);
            executor=qtp;
        }
        
        client.setExecutor(executor);

        value = config.getInitParameter("maxConnections");
        if (value == null)
            value = "32768";
        client.setMaxConnectionsPerDestination(Integer.parseInt(value));

        value = config.getInitParameter("idleTimeout");
        if (value == null)
            value = "30000";
        client.setIdleTimeout(Long.parseLong(value));

        value = config.getInitParameter("timeout");
        if (value == null)
            value = "60000";
        _timeout = Long.parseLong(value);

        value = config.getInitParameter("requestBufferSize");
        if (value != null)
            client.setRequestBufferSize(Integer.parseInt(value));

        value = config.getInitParameter("responseBufferSize");
        if (value != null)
            client.setResponseBufferSize(Integer.parseInt(value));

        try
        {
            client.start();

            // Content must not be decoded, otherwise the client gets confused
            client.getContentDecoderFactories().clear();

            return client;
        }
        catch (Exception x)
        {
            throw new ServletException(x);
        }
    }

    /**
     * @return a new HttpClient instance
     */
    protected HttpClient newHttpClient()
    {
        return new HttpClient();
    }

    private Set<String> parseList(String list)
    {
        Set<String> result = new HashSet<>();
        String[] hosts = list.split(",");
        for (String host : hosts)
        {
            host = host.trim();
            if (host.length() == 0)
                continue;
            result.add(host);
        }
        return result;
    }

    /**
     * Checks the given {@code host} and {@code port} against whitelist and blacklist.
     *
     * @param host the host to check
     * @param port the port to check
     * @return true if it is allowed to be proxy to the given host and port
     */
    public boolean validateDestination(String host, int port)
    {
        String hostPort = host + ":" + port;
        if (!_whiteList.isEmpty())
        {
            if (!_whiteList.contains(hostPort))
            {
                _log.debug("Host {}:{} not whitelisted", host, port);
                return false;
            }
        }
        if (!_blackList.isEmpty())
        {
            if (_blackList.contains(hostPort))
            {
                _log.debug("Host {}:{} blacklisted", host, port);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        final int requestId = getRequestId(request);

        URI rewrittenURI = rewriteURI(request);

        if (_log.isDebugEnabled())
        {
            StringBuffer uri = request.getRequestURL();
            if (request.getQueryString() != null)
                uri.append("?").append(request.getQueryString());
            _log.debug("{} rewriting: {} -> {}", requestId, uri, rewrittenURI);
        }

        if (rewrittenURI == null)
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        final Request proxyRequest = _client.newRequest(rewrittenURI)
                .method(HttpMethod.fromString(request.getMethod()))
                .version(HttpVersion.fromString(request.getProtocol()));

        // Copy headers
        for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements();)
        {
            String headerName = headerNames.nextElement();
            String lowerHeaderName = headerName.toLowerCase(Locale.ENGLISH);

            // Remove hop-by-hop headers
            if (HOP_HEADERS.contains(lowerHeaderName))
                continue;

            if (_hostHeader!=null && lowerHeaderName.equals("host"))
                continue;

            for (Enumeration<String> headerValues = request.getHeaders(headerName); headerValues.hasMoreElements();)
            {
                String headerValue = headerValues.nextElement();
                if (headerValue != null)
                    proxyRequest.header(headerName, headerValue);
            }
        }

        // Force the Host header if configured
        if (_hostHeader != null)
            proxyRequest.header(HttpHeader.HOST, _hostHeader);

        // Add proxy headers
        proxyRequest.header(HttpHeader.VIA, "http/1.1 " + _viaHost);
        proxyRequest.header(HttpHeader.X_FORWARDED_FOR, request.getRemoteAddr());
        proxyRequest.header(HttpHeader.X_FORWARDED_PROTO, request.getScheme());
        proxyRequest.header(HttpHeader.X_FORWARDED_HOST, request.getHeader(HttpHeader.HOST.asString()));
        proxyRequest.header(HttpHeader.X_FORWARDED_SERVER, request.getLocalName());

        proxyRequest.content(new InputStreamContentProvider(request.getInputStream())
        {
            @Override
            public long getLength()
            {
                return request.getContentLength();
            }

            @Override
            protected ByteBuffer onRead(byte[] buffer, int offset, int length)
            {
                _log.debug("{} proxying content to upstream: {} bytes", requestId, length);
                return super.onRead(buffer, offset, length);
            }
        });

        final AsyncContext asyncContext = request.startAsync();
        // We do not timeout the continuation, but the proxy request
        asyncContext.setTimeout(0);
        request.setAttribute(ASYNC_CONTEXT, asyncContext);

        customizeProxyRequest(proxyRequest, request);

        if (_log.isDebugEnabled())
        {
            StringBuilder builder = new StringBuilder(request.getMethod());
            builder.append(" ").append(request.getRequestURI());
            String query = request.getQueryString();
            if (query != null)
                builder.append("?").append(query);
            builder.append(" ").append(request.getProtocol()).append("\r\n");
            for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements();)
            {
                String headerName = headerNames.nextElement();
                builder.append(headerName).append(": ");
                for (Enumeration<String> headerValues = request.getHeaders(headerName); headerValues.hasMoreElements();)
                {
                    String headerValue = headerValues.nextElement();
                    if (headerValue != null)
                        builder.append(headerValue);
                    if (headerValues.hasMoreElements())
                        builder.append(",");
                }
                builder.append("\r\n");
            }
            builder.append("\r\n");

            _log.debug("{} proxying to upstream:{}{}{}{}",
                    requestId,
                    System.lineSeparator(),
                    builder,
                    proxyRequest,
                    System.lineSeparator(),
                    proxyRequest.getHeaders().toString().trim());
        }

        proxyRequest.timeout(getTimeout(), TimeUnit.MILLISECONDS);
        proxyRequest.send(new ProxyResponseListener(request, response));
    }

    protected void onResponseHeaders(HttpServletRequest request, HttpServletResponse response, Response proxyResponse)
    {
        for (HttpField field : proxyResponse.getHeaders())
        {
            String headerName = field.getName();
            String lowerHeaderName = headerName.toLowerCase(Locale.ENGLISH);
            if (HOP_HEADERS.contains(lowerHeaderName))
                continue;

            String newHeaderValue = filterResponseHeader(request, headerName, field.getValue());
            if (newHeaderValue == null || newHeaderValue.trim().length() == 0)
                continue;

            response.addHeader(headerName, newHeaderValue);
        }
    }

    protected void onResponseContent(HttpServletRequest request, HttpServletResponse response, Response proxyResponse, byte[] buffer, int offset, int length) throws IOException
    {
        response.getOutputStream().write(buffer, offset, length);
        _log.debug("{} proxying content to downstream: {} bytes", getRequestId(request), length);
    }

    protected void onResponseSuccess(HttpServletRequest request, HttpServletResponse response, Response proxyResponse)
    {
        AsyncContext asyncContext = (AsyncContext)request.getAttribute(ASYNC_CONTEXT);
        asyncContext.complete();
        _log.debug("{} proxying successful", getRequestId(request));
    }

    protected void onResponseFailure(HttpServletRequest request, HttpServletResponse response, Response proxyResponse, Throwable failure)
    {
        _log.debug(getRequestId(request) + " proxying failed", failure);
        if (!response.isCommitted())
        {
            if (failure instanceof TimeoutException)
                response.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            else
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        }
        AsyncContext asyncContext = (AsyncContext)request.getAttribute(ASYNC_CONTEXT);
        asyncContext.complete();
    }

    protected int getRequestId(HttpServletRequest request)
    {
        return System.identityHashCode(request);
    }

    protected URI rewriteURI(HttpServletRequest request)
    {
        if (!validateDestination(request.getServerName(), request.getServerPort()))
            return null;

        StringBuffer uri = request.getRequestURL();
        String query = request.getQueryString();
        if (query != null)
            uri.append("?").append(query);

        return URI.create(uri.toString());
    }

    /**
     * Extension point for subclasses to customize the proxy request.
     * The default implementation does nothing.
     *
     * @param proxyRequest the proxy request to customize
     * @param request the request to be proxied
     */
    protected void customizeProxyRequest(Request proxyRequest, HttpServletRequest request)
    {
    }

    /**
     * Extension point for remote server response header filtering.
     * The default implementation returns the header value as is.
     * If null is returned, this header won't be forwarded back to the client.
     *
     * @param headerName the header name
     * @param headerValue the header value
     * @param request the request to proxy
     * @return filteredHeaderValue the new header value
     */
    protected String filterResponseHeader(HttpServletRequest request, String headerName, String headerValue)
    {
        return headerValue;
    }

    /**
     * Transparent Proxy.
     * <p/>
     * This convenience extension to ProxyServlet configures the servlet as a transparent proxy.
     * The servlet is configured with init parameters:
     * <ul>
     * <li>proxyTo - a URI like http://host:80/context to which the request is proxied.
     * <li>prefix - a URI prefix that is striped from the start of the forwarded URI.
     * </ul>
     * For example, if a request is received at /foo/bar and the 'proxyTo' parameter is "http://host:80/context"
     * and the 'prefix' parameter is "/foo", then the request would be proxied to "http://host:80/context/bar".
     */
    public static class Transparent extends ProxyServlet
    {
        private String _proxyTo;
        private String _prefix;

        public Transparent()
        {
        }

        public Transparent(String proxyTo, String prefix)
        {
            _proxyTo = URI.create(proxyTo).normalize().toString();
            _prefix = URI.create(prefix).normalize().toString();
        }

        @Override
        public void init() throws ServletException
        {
            super.init();

            ServletConfig config = getServletConfig();

            String prefix = config.getInitParameter("prefix");
            _prefix = prefix == null ? _prefix : prefix;

            // Adjust prefix value to account for context path
            String contextPath = getServletContext().getContextPath();
            _prefix = _prefix == null ? contextPath : (contextPath + _prefix);

            String proxyTo = config.getInitParameter("proxyTo");
            _proxyTo = proxyTo == null ? _proxyTo : proxyTo;

            if (_proxyTo == null)
                throw new UnavailableException("Init parameter 'proxyTo' is required.");

            if (!_prefix.startsWith("/"))
                throw new UnavailableException("Init parameter 'prefix' parameter must start with a '/'.");

            _log.debug(config.getServletName() + " @ " + _prefix + " to " + _proxyTo);
        }

        @Override
        protected URI rewriteURI(HttpServletRequest request)
        {
            String path = request.getRequestURI();
            if (!path.startsWith(_prefix))
                return null;

            StringBuilder uri = new StringBuilder(_proxyTo);
            uri.append(path.substring(_prefix.length()));
            String query = request.getQueryString();
            if (query != null)
                uri.append("?").append(query);
            URI rewrittenURI = URI.create(uri.toString()).normalize();

            if (!validateDestination(rewrittenURI.getHost(), rewrittenURI.getPort()))
                return null;

            return rewrittenURI;
        }
    }

    private class ProxyResponseListener extends Response.Listener.Adapter
    {
        private final HttpServletRequest request;
        private final HttpServletResponse response;

        public ProxyResponseListener(HttpServletRequest request, HttpServletResponse response)
        {
            this.request = request;
            this.response = response;
        }

        @Override
        public void onBegin(Response proxyResponse)
        {
            response.setStatus(proxyResponse.getStatus());
        }

        @Override
        public void onHeaders(Response proxyResponse)
        {
            onResponseHeaders(request, response, proxyResponse);

            if (_log.isDebugEnabled())
            {
                StringBuilder builder = new StringBuilder("\r\n");
                builder.append(request.getProtocol()).append(" ").append(response.getStatus()).append(" ").append(proxyResponse.getReason()).append("\r\n");
                for (String headerName : response.getHeaderNames())
                {
                    builder.append(headerName).append(": ");
                    for (Iterator<String> headerValues = response.getHeaders(headerName).iterator(); headerValues.hasNext();)
                    {
                        String headerValue = headerValues.next();
                        if (headerValue != null)
                            builder.append(headerValue);
                        if (headerValues.hasNext())
                            builder.append(",");
                    }
                    builder.append("\r\n");
                }
                _log.debug("{} proxying to downstream:{}{}{}{}{}",
                        getRequestId(request),
                        System.lineSeparator(),
                        proxyResponse,
                        System.lineSeparator(),
                        proxyResponse.getHeaders().toString().trim(),
                        System.lineSeparator(),
                        builder);
            }
        }

        @Override
        public void onContent(Response proxyResponse, ByteBuffer content)
        {
            byte[] buffer;
            int offset;
            int length = content.remaining();
            if (content.hasArray())
            {
                buffer = content.array();
                offset = content.arrayOffset();
            }
            else
            {
                buffer = new byte[length];
                content.get(buffer);
                offset = 0;
            }

            try
            {
                onResponseContent(request, response, proxyResponse, buffer, offset, length);
            }
            catch (IOException x)
            {
                proxyResponse.abort(x);
            }
        }

        @Override
        public void onSuccess(Response proxyResponse)
        {
            onResponseSuccess(request, response, proxyResponse);
        }

        @Override
        public void onFailure(Response proxyResponse, Throwable failure)
        {
            onResponseFailure(request, response, proxyResponse, failure);
        }

        @Override
        public void onComplete(Result result)
        {
            _log.debug("{} proxying complete", getRequestId(request));
        }
    }
}
