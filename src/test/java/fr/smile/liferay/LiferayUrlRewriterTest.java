package fr.smile.liferay;

import java.util.Properties;

import com.liferay.portal.kernel.util.Base64;

import static org.junit.Assert.assertEquals;

public class LiferayUrlRewriterTest {

    @org.junit.Test
    public void testRewriteUrl1() throws Exception {

        Properties prop = new Properties();
        prop.setProperty("fixMode", "relative");
        LiferayUrlRewriter rewriter = new LiferayUrlRewriter("", "");


        String url = "/logo.jpg";
        String requestUrl = "/";
        String baseUrl = "http://localhost:4567";
        String strVisibleBaseUrl = "http://localhost:8080/web/guest/mapage/-/esigate/resource/opMnOV3rK1WG?p_p_resource_id=";

        String result = rewriter.rewriteUrl(url, requestUrl, baseUrl, strVisibleBaseUrl);

        String resultUrl = new String(Base64.decode(result.substring(strVisibleBaseUrl.length())));
        assertEquals(url, resultUrl);

    }

    @org.junit.Test
    public void testRewriteUrl2() throws Exception {

        Properties prop = new Properties();
        prop.setProperty("fixMode", "relative");
        LiferayUrlRewriter rewriter = new LiferayUrlRewriter("", "");


        String url = "/smartcloudportal/plugins/jquery-1.4.4.1/js/jquery/jquery-1.4.4.js";
        String requestUrl = "/";
        String baseUrl = "http://5.39.26.188:8080/smartcloudportal/login/auth";
        String strVisibleBaseUrl = "http://localhost:8080/web/guest/welcome/-/esigate/resource/5hrUXR0KUtEa?p_p_resource_id=";


        String result = rewriter.rewriteUrl(url, requestUrl, baseUrl, strVisibleBaseUrl);

        String resultUrl = new String(Base64.decode(result.substring(strVisibleBaseUrl.length())));
        assertEquals(url, resultUrl);


    }
}