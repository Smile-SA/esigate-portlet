package fr.smile.liferay;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;

import javax.portlet.ResourceResponse;

/**
 * @author Alexis Thaveau on 11/04/14.
 */
public class HeaderUtils {

    /**
     * Copy headers from driverResponse to portal response
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
