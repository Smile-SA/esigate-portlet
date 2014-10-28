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

import static org.apache.commons.lang3.StringUtils.stripEnd;

import java.io.IOException;
import java.io.Writer;

import org.esigate.Renderer;
import org.esigate.impl.DriverRequest;

/**
 * This renderer "fixes" links to resources, images and pages in pages retrieved by esigate :
 * <ul>
 * <li>Current-path-relative urls are converted to full path relative urls ( img/test.img ->
 * /myapp/curentpath/img/test.img)</li>
 * <li>All relative urls can be converted to absolute urls (including server name)</li>
 * </ul>
 * 
 * This enables use of esigate without any special modifications of the generated urls on the provider side.
 * 
 * All href and src attributes are processed, except javascript links.
 * 
 * @author Nicolas Richeton
 * 
 */
public class ResourceFixupRenderer implements Renderer {
    private final String baseUrl;
    private final String requestUrl;
    private final LiferayUrlRewriter urlRewriter;

    /**
     * Creates a renderer which fixes urls. The domain name and the url path are computed from the full url made of
     * baseUrl + pageFullPath.
     * 
     * If mode is ABSOLUTE, all relative urls will be replaced by the full urls :
     * <ul>
     * <li>images/image.png is replaced by http://server/context/images/image.png</li>
     * <li>/context/images/image.png is replaced by http://server/context/images/image.png</li>
     * </ul>
     * 
     * If mode is RELATIVE, context will be added to relative urls :
     * <ul>
     * <li>images/image.png is replaced by /context/images/image.png</li>
     * </ul>
     * 
     * @param baseUrl
     *            Base url (same as configured in provider).
     * @param requestUrl
     *            Page as used in tag lib or using API
     * @param urlRewriter
     *            The url rewriter for this provider
     */
    public ResourceFixupRenderer(String baseUrl, String requestUrl, LiferayUrlRewriter urlRewriter) {
        this.baseUrl = stripEnd(baseUrl, "/");
        this.requestUrl = requestUrl;
        this.urlRewriter = urlRewriter;
    }

    @Override
    public void render(DriverRequest httpRequest, String src, Writer out) throws IOException {
        String content = src;
        content = urlRewriter.rewriteResource(content, requestUrl, baseUrl).toString();
        content = urlRewriter.rewriteAction(content, requestUrl, baseUrl).toString();
        out.write(content);
    }

}
