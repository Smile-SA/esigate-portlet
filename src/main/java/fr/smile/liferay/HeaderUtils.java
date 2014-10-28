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


import javax.portlet.ResourceResponse;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;

/**
 * @author Alexis Thaveau on 11/04/14.
 */
public class HeaderUtils {

    /**
     * Copy headers from driverResponse to portal response
     *
     * @param driverResponse
     * @param response
     * @param headers
     */
    public static void copyHeader(CloseableHttpResponse driverResponse, ResourceResponse response, String... headers) {
        for (String header : headers) {

            Header[] values = driverResponse.getHeaders(header);
            if (values != null) {
                for (Header value : values) {
                    response.setProperty(header, value.getValue());
                }
            }
        }


    }
}
