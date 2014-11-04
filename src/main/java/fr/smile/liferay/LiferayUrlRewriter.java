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
 *
 */

package fr.smile.liferay;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.esigate.Parameters;
import org.esigate.util.UriUtils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Base64;

import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.apache.commons.lang3.StringUtils.stripStart;

/**
 * "fixes" links to resources, images and pages in pages retrieved by esigate :
 * <ul>
 * <li>Current-path-relative urls are converted to full path relative urls ( img/test.img ->
 * /myapp/curentpath/img/test.img)</li>
 * <li>All relative urls can be converted to absolute urls (including server name)</li>
 * </ul>
 * <p/>
 * This enables use of esigate without any special modifications of the generated urls on the provider side.
 * <p/>
 * All href and src attributes are processed, except javascript links.
 *
 * @author Nicolas Richeton
 */
public final class LiferayUrlRewriter {
    public static final int ABSOLUTE = 0;
    public static final int RELATIVE = 1;
    private static Log LOG = LogFactoryUtil.getLog(EsigatePortlet.class);
    private static final Pattern URL_PATTERN_RESOURCES = Pattern.compile(
            "<([^\\!][^>]+)(src|background)\\s*=\\s*('[^<']*'|\"[^<\"]*\")([^>]*)>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern URL_PATTERN_ACTION = Pattern.compile(
            "<([^\\!][^>]+)(href|action)\\s*=\\s*('[^<']*'|\"[^<\"]*\")([^>]*)>",
            Pattern.CASE_INSENSITIVE);

    private String visibleBaseUrlResource;
    private String visibleBaseUrlAction;

    private int mode;

    /**
     * Creates a renderer which fixes urls. The domain name and the url path are computed from the full url made of
     * baseUrl + pageFullPath.
     * <p/>
     * If mode is ABSOLUTE, all relative urls will be replaced by the full urls :
     * <ul>
     * <li>images/image.png is replaced by http://server/context/images/image.png</li>
     * <li>/context/images/image.png is replaced by http://server/context/images/image.png</li>
     * </ul>
     * <p/>
     * If mode is RELATIVE, context will be added to relative urls :
     * <ul>
     * <li>images/image.png is replaced by /context/images/image.png</li>
     * </ul>
     *
     * @param properties Configuration properties
     */
    public LiferayUrlRewriter(Properties properties, String strVisibleBaseUrlAction, String strVisibleBaseUrlResource) {
        if ("absolute".equalsIgnoreCase(Parameters.FIX_MODE.getValue(properties))) {
            mode = ABSOLUTE;
        } else {
            mode = RELATIVE;
        }
        this.visibleBaseUrlResource = stripEnd(strVisibleBaseUrlResource, "/");
        this.visibleBaseUrlAction = strVisibleBaseUrlAction;

    }

    private String concatUrl(String begin, String end) {
        return stripEnd(begin, "/") + "/" + stripStart(end, "/");
    }

    /**
     * Fix an url according to the chosen mode.
     *
     * @param url        the url to fix.
     * @param requestUrl The request URL.
     * @param baseUrl    The base URL selected for this request.
     * @return the fixed url.
     */
    public String rewriteUrl(String url, String requestUrl, String baseUrl, String strVisibleBaseUrl) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("rewriteUrl (url=" + url + ",requestUrl=" + requestUrl + ", baseUrl=" + baseUrl + ",strVisibleBaseUrl=" + strVisibleBaseUrl + ")");
        }
        if (url.isEmpty()) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("skip empty url");
            }
            return url;
        }

        // Store the filename, if specified
        String fileName = null;
        if (!requestUrl.isEmpty() && !requestUrl.endsWith("/")) {
            fileName = requestUrl.substring(requestUrl.lastIndexOf('/') + 1);
        }

        // Build clean URI for further processing
        String cleanBaseUrl = stripEnd(baseUrl, "/");
        String visibleBaseUrl = strVisibleBaseUrl;
        if (visibleBaseUrl == null) {
            visibleBaseUrl = cleanBaseUrl;
        }
        String visibleBaseUrlPath = visibleBaseUrl;
        String pagePath = concatUrl(visibleBaseUrlPath, requestUrl);
        if (pagePath != null) {
            int indexSlash = pagePath.lastIndexOf('/');
            if (indexSlash >= 0) {
                pagePath = pagePath.substring(0, indexSlash);
            }
        }

        String result = url;
        if (visibleBaseUrl != null && result.startsWith(cleanBaseUrl)) {
            result = visibleBaseUrl + result.substring(cleanBaseUrl.length());
            if(LOG.isDebugEnabled()) {
                LOG.debug("fix absolute url: " + url + "->" + result);
            }
            return result;
        }

        // Keep absolute, protocol-absolute and javascript urls untouched.
        if (result.startsWith("http://") || result.startsWith("https://") || result.startsWith("//")
                || result.startsWith("#") || result.startsWith("javascript:")) {
            LOG.debug("keeping absolute url:"+ result);
            return result;
        }

        //HttpHost httpHost = UriUtils.extractHost(visibleBaseUrl);
        //String server = httpHost.toURI();

        // Add domain to context absolute urls
        if (result.startsWith("/")) {

            // Check if we are going to replace context
            if (cleanBaseUrl != null && !cleanBaseUrl.equals(visibleBaseUrl)) {
                String baseUrlPath = UriUtils.getPath(cleanBaseUrl);
                if (result.startsWith(baseUrlPath)) {
                    result = result.substring(baseUrlPath.length());
                    result = Base64.encode(result.getBytes());
                    result = visibleBaseUrlPath + result;
                }
            }

        } else {

            if (result.charAt(0) == '?' && fileName != null) {
                result = fileName + result;
            }

            // Process relative urls

            result = pagePath + "/" + result;

        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("url fixed: " + url + "->" + result);
        }
        return result;
    }

    public CharSequence rewriteResource(CharSequence input, String requestUrl, String baseUrlParam) {
        return this.rewriteHtml(input, requestUrl, URL_PATTERN_RESOURCES, baseUrlParam, visibleBaseUrlResource);
    }

    public CharSequence rewriteAction(CharSequence input, String requestUrl, String baseUrlParam) {

        return this.rewriteHtml(input, requestUrl, URL_PATTERN_ACTION, baseUrlParam, visibleBaseUrlAction);
    }


    /**
     * Fix all resources urls and return the result.
     *
     * @param input        The original charSequence to be processed.
     * @param requestUrl   The request URL.
     * @param baseUrlParam The base URL selected for this request.
     * @return the result of this renderer.
     */
    public CharSequence rewriteHtml(CharSequence input, String requestUrl, Pattern pattern, String baseUrlParam, String visibleBaseUrl) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("input=" + input);
            LOG.debug("rewriteHtml (requestUrl=" + requestUrl + ", pattern=" + pattern + ",baseUrlParam)"+ baseUrlParam +",strVisibleBaseUrl=" + visibleBaseUrl + ")");
        }

        StringBuffer result = new StringBuffer(input.length());
        Matcher m = pattern.matcher(input);
        while (m.find()) {
            if(LOG.isTraceEnabled()) {
                LOG.trace("found match: " + m);
            }
            String url = input.subSequence(m.start(3) + 1, m.end(3) - 1).toString();
            url = rewriteUrl(url, requestUrl, baseUrlParam, visibleBaseUrl);
            url = url.replaceAll("\\$", "\\\\\\$"); // replace '$' -> '\$' as it
            // denotes group
            StringBuffer tagReplacement = new StringBuffer("<$1$2=\"").append(url).append("\"");
            if (m.groupCount() > 3) {
                tagReplacement.append("$4");
            }
            tagReplacement.append('>');
            if(LOG.isTraceEnabled()) {
                LOG.trace("replacement: " + tagReplacement);
            }
            m.appendReplacement(result, tagReplacement.toString());
        }
        m.appendTail(result);

        return result;
    }

}
