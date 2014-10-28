/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.smile.liferay;


import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicRequestLine;
import org.esigate.Driver;
import org.esigate.DriverFactory;
import org.esigate.HttpErrorPage;
import org.esigate.Parameters;
import org.esigate.http.IncomingRequest;
import org.esigate.servlet.HttpServletRequestContext;
import org.esigate.servlet.impl.HttpServletSession;
import org.esigate.tags.BlockRenderer;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Layout;
import com.liferay.portal.theme.PortletDisplay;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;

/**
 * Esigate portlet that demonstrate the integration of remote application in a Portlet.
 */
public class EsigatePortlet extends GenericPortlet {


    private static final String URL_PARAMETER = "url";
    private static final String CONTENT_PARAMETER = "content";
    private static final String ACTION_PARAMETER = "remoteaction";


    private static final String NORMAL_VIEW = "/normal.jsp";
    private static final String HELP_VIEW = "/help.jsp";
    private static final String EDIT_VIEW = "/edit.jsp";


    private static final String PREF_BLOCK = "block";
    private static final String PREF_PROVIDER = "provider";
    private static final String URL_INSTANCE_ID = "__instanceID__";
    private static Log LOG = LogFactoryUtil.getLog(EsigatePortlet.class);
    private PortletRequestDispatcher normalView;
    private PortletRequestDispatcher helpView;
    private PortletRequestDispatcher editView;

    @Override
    public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("processAction in portlet mode " + request.getPortletMode());
        }


        if (request.getPortletMode().equals(PortletMode.EDIT)) {
            String provider = request.getParameter("provider");
            String block = request.getParameter("block");
            if (StringUtils.isEmpty(block)) {
                block = null;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("provider " + provider + ", block=" + block);
            }

            request.getPreferences().setValue(PREF_PROVIDER, provider);
            request.getPreferences().setValue(PREF_BLOCK, block);
            request.getPreferences().store();

        } else {
            String targetUrl = request.getParameter(ACTION_PARAMETER);

            if (LOG.isDebugEnabled()) {
                LOG.debug("processAction " + targetUrl);
            }


            if (!StringUtils.isEmpty(targetUrl)) {
                // Decrypt data on other side, by processing encoded data
                byte[] valueDecoded = Base64.decodeBase64(targetUrl);
                targetUrl = new String(valueDecoded);

                CloseableHttpResponse driverResponse = proxy(request, targetUrl, request.getMethod());
                HttpEntity entity = driverResponse.getEntity();

                String content = IOUtils.toString(entity.getContent());
                response.setRenderParameter(CONTENT_PARAMETER, content);
            }
        }
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {

        String resourceID = request.getResourceID();
        byte[] valueDecoded = Base64.decodeBase64(resourceID);
        resourceID = new String(valueDecoded);

        if (LOG.isDebugEnabled()) {
            LOG.debug("serveResource [" + resourceID + "]");
        }


        if (resourceID != null) {

            CloseableHttpResponse driverResponse = this.proxy(request, resourceID, "GET");

            HttpEntity httpEntity = driverResponse.getEntity();
            if (httpEntity != null) {

                Header contentType = httpEntity.getContentType();
                if (contentType != null) {
                    response.setContentType(contentType.getValue());
                }
                HeaderUtils.copyHeader(driverResponse, response, HttpHeaders.LAST_MODIFIED, HttpHeaders.CACHE_CONTROL);

                httpEntity.writeTo(response.getPortletOutputStream());
            } else {
                throw new RuntimeException("" + driverResponse.getStatusLine().getStatusCode() + "  " + driverResponse.getStatusLine().getReasonPhrase());

            }
        }
        super.serveResource(request, response);
    }

    /**
     * @param request
     * @return
     * @throws PortletException
     */
    public Pair<String, String> getBaseUrl(PortletRequest request) throws PortletException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("calculate the portlet resource/action URLs");
        }
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
        PortletDisplay portletDisplay = themeDisplay.getPortletDisplay();
        Layout layout = themeDisplay.getLayout();


        String instanceID = portletDisplay.getInstanceId();

        if (StringUtils.isEmpty(instanceID)) {
            instanceID = URL_INSTANCE_ID;
        }

        String baseUrl = "";

        try {
            baseUrl = new StringBuilder().append(PortalUtil.getLayoutFriendlyURL(layout, themeDisplay)).append("/-/esigate").toString();
        } catch (PortalException e) {
            throw new PortletException(e);
        } catch (SystemException e) {
            throw new PortletException(e);
        }

        String baseActionURL = new StringBuilder().append(baseUrl).append("/action/").append(instanceID).append("/s/").append(request.getWindowState()).append("/l/1/param/").toString();
        String baseResourceURL = new StringBuilder(baseUrl).append("/resource/").append(instanceID).append("?p_p_resource_id=").toString();


        if (LOG.isDebugEnabled()) {
            LOG.debug("baseURL is " + baseUrl);
            LOG.debug("baseActionURL is " + baseActionURL);
            LOG.debug("baseResourceURL is " + baseResourceURL);
        }

        return new ImmutablePair<>(baseResourceURL, baseActionURL);

    }

    public void doView(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("ParameterMap=" + request.getParameterMap());
            LOG.debug("WindowsState=" + request.getWindowState());
            LOG.debug("Action Parameter =" + request.getParameter(EsigatePortlet.ACTION_PARAMETER));
        }

        if (WindowState.MINIMIZED.equals(request.getWindowState())) {
            return;
        }

        if (WindowState.NORMAL.equals(request.getWindowState()) || WindowState.MAXIMIZED.equals(request.getWindowState())) {
            String provider = request.getPreferences().getValue(PREF_PROVIDER, null);

            if (StringUtils.isEmpty(provider)) {
                normalView.include(request, response);
            } else {
                String content = request.getParameter(CONTENT_PARAMETER);
                if (content != null) {
                    ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
                    PortletDisplay portletDisplay = themeDisplay.getPortletDisplay();
                    content = content.replaceAll(URL_INSTANCE_ID, portletDisplay.getInstanceId());
                    IOUtils.write(content, response.getPortletOutputStream());
                } else {

                    String targetUrl = "/";

                    if (!StringUtils.isEmpty(request.getParameter(URL_PARAMETER))) {
                        targetUrl = request.getParameter(URL_PARAMETER);
                    }

                    CloseableHttpResponse driverResponse = proxy(request, targetUrl, "GET");
                    HttpEntity httpEntity = driverResponse.getEntity();

                    Header contentType = httpEntity.getContentType();
                    if (contentType != null) {
                        response.setContentType(contentType.getValue());
                    }

                    httpEntity.writeTo(response.getPortletOutputStream());

                }
            }
        }
    }

    /**
     * Proxy request to provider application
     *
     * @param request
     * @param targetUrl
     * @param method
     * @return
     * @throws IOException
     * @throws PortletException
     */
    private CloseableHttpResponse proxy(PortletRequest request, String targetUrl, String method) throws IOException, PortletException {
        Pair<String, String> baseUrls = getBaseUrl(request);

        String baseActionURL = baseUrls.getRight();
        String baseResourceURL = baseUrls.getLeft();

        if (targetUrl.startsWith(baseActionURL)) {
            targetUrl = targetUrl.replace(baseActionURL, "");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Proxy to " + targetUrl);
        }

        String provider = request.getPreferences().getValue(PREF_PROVIDER, null);
        String block = request.getPreferences().getValue(PREF_BLOCK, null);

        IncomingRequest incomingRequest = this.create(request, method);
        Driver driver = DriverFactory.getInstance(provider);
        String[] baseUrl = Parameters.REMOTE_URL_BASE.getValue(driver.getConfiguration().getProperties());
        LiferayUrlRewriter rewriter = new LiferayUrlRewriter(driver.getConfiguration().getProperties(), baseActionURL, baseResourceURL);
        ResourceFixupRenderer renderer = new ResourceFixupRenderer(baseUrl[0], targetUrl, rewriter);
        BlockRenderer r = new BlockRenderer(block, null);
        try {
            CloseableHttpResponse driverResponse = driver.proxy(targetUrl, incomingRequest, renderer, r);
            HttpEntity httpEntity = driverResponse.getEntity();
            if (httpEntity == null) {
                throw new RuntimeException("" + driverResponse.getStatusLine().getStatusCode() + "  " + driverResponse.getStatusLine().getReasonPhrase());
            }

            return driverResponse;


        } catch (HttpErrorPage e) {
            throw new PortletException(e);

        }
    }

    @Override
    protected void doEdit(RenderRequest request, RenderResponse response) throws PortletException, IOException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("doEdit");
        }
        String block = request.getPreferences().getValue(PREF_BLOCK, "");
        String provider = request.getPreferences().getValue(PREF_PROVIDER, null);


        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading preferences. Provider=" + provider + ", block=" + block);
        }


        Collection<Driver> drivers = DriverFactory.getInstances();

        List<DriverPojo> driverPojos = new ArrayList<>(drivers.size());

        for (Driver driver : drivers) {
            String driverName = driver.getConfiguration().getInstanceName();
            String[] remoteURLs = Parameters.REMOTE_URL_BASE.getValue(driver.getConfiguration().getProperties());
            String remoteURL = Arrays.toString(remoteURLs);
            DriverPojo driverPojo = new DriverPojo();
            driverPojo.setName(driverName);
            driverPojo.setUrl(remoteURL);
            driverPojos.add(driverPojo);
        }

        request.setAttribute("drivers", driverPojos);
        request.setAttribute("block", block);
        request.setAttribute("provider", provider);
        editView.forward(request, response);
    }

    /**
     * DoHelp
     *
     * @param request
     * @param response
     * @throws PortletException
     * @throws IOException
     */
    protected void doHelp(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        helpView.include(request, response);
    }

    /**
     * Init esigate and portlet
     *
     * @param config
     * @throws PortletException
     */
    public void init(PortletConfig config) throws PortletException {
        super.init(config);
        normalView = config.getPortletContext().getRequestDispatcher(NORMAL_VIEW);
        helpView = config.getPortletContext().getRequestDispatcher(HELP_VIEW);
        editView = config.getPortletContext().getRequestDispatcher(EDIT_VIEW);
        DriverFactory.ensureConfigured();

    }

    /**
     * Transform request to IncominqRequest
     *
     * @param request
     * @param method
     * @return an incoming request
     * @throws IOException
     */
    public IncomingRequest create(PortletRequest request, String method)
            throws IOException {

        HttpServletRequest httpServletRequest = PortalUtil.getOriginalServletRequest(PortalUtil.getHttpServletRequest(request));

        StringBuilder uri = new StringBuilder("http://localhost:8080/");

        StringBuilder query = new StringBuilder();
        Enumeration<String> parameters = request.getParameterNames();
        String sep = "";
        while (parameters.hasMoreElements()) {
            String name = parameters.nextElement();
            String[] values = request.getParameterValues(name);
            if (!name.equals(ACTION_PARAMETER)) {
                for (String value : values) {
                    query.append(sep);
                    query.append(name).append("=").append(URLEncoder.encode(value, "UTF-8"));
                    sep = "&";
                }
            }
        }

        ProtocolVersion protocolVersion = HttpVersion.HTTP_1_1.forVersion(1, 0);

        if (method.equals("GET")) {
            if (!query.toString().isEmpty()) {
                if (!uri.toString().contains("?")) {
                    uri.append("?");
                } else {
                    uri.append("&");
                }
                uri.append(query);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating Incoming request with method " + method + ", URI " + uri + ", protocoleVersion " + protocolVersion);
        }
        IncomingRequest.Builder builder = IncomingRequest.builder(new BasicRequestLine(method, uri.toString(), protocolVersion));

        if (method.equals("POST")) {
            // create entity
            InputStream inputStream = IOUtils.toInputStream(query.toString());

            if (inputStream != null) {
                // Copy entity-related headers
                InputStreamEntity entity = new InputStreamEntity(inputStream, query.length());
                String contentTypeHeader = httpServletRequest.getContentType();
                if (contentTypeHeader != null) {
                    entity.setContentType(contentTypeHeader);
                }
                String contentEncodingHeader = httpServletRequest.getCharacterEncoding();
                if (contentEncodingHeader != null) {
                    entity.setContentEncoding(contentEncodingHeader);
                }
                builder.setEntity(entity);
            }
        }


        HttpServletRequestContext context = new HttpServletRequestContext(httpServletRequest, null, null);
        builder.setContext(context);
        builder.setRemoteAddr(httpServletRequest.getRemoteAddr());
        builder.setRemoteUser(request.getRemoteUser());
        HttpSession session = httpServletRequest.getSession(false);
        if (session != null) {
            builder.setSessionId(session.getId());
        }
        builder.setUserPrincipal(request.getUserPrincipal());
        // Copy cookies
        javax.servlet.http.Cookie[] src = request.getCookies();

        if (src != null) {
            LOG.debug("Copying " + src.length + " cookie(s) to response.");
            for (int i = 0; i < src.length; i++) {
                javax.servlet.http.Cookie c = src[i];
                BasicClientCookie dest = new BasicClientCookie(c.getName(), c.getValue());
                dest.setSecure(c.getSecure());
                dest.setDomain(c.getDomain());
                dest.setPath(c.getPath());
                dest.setComment(c.getComment());
                dest.setVersion(c.getVersion());
                builder.addCookie(dest);
            }
        }

        builder.setSession(new HttpServletSession(httpServletRequest));

        IncomingRequest incomingRequest = builder.build();
        return incomingRequest;

    }

    /**
     * Destroy portlet method
     */
    public void destroy() {
        normalView = null;
        helpView = null;
        editView = null;
        super.destroy();

    }

}
