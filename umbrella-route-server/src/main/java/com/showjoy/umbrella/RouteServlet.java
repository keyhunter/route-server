package com.showjoy.umbrella;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RouteServlet extends HttpServlet {

    /** @author jiujie 2016年5月3日 下午4:43:41 */
    private static final long serialVersionUID = -1571539729250487705L;

    private Logger            log              = Logger.getLogger(RouteServlet.class.getName());

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        doPost(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) {

        BufferedInputStream webToProxyBuf = null;
        BufferedOutputStream proxyToClientBuf = null;
        HttpURLConnection con;

        try {
            int statusCode;
            int oneByte;
            String methodName;
            String headerText;

            String urlString = request.getRequestURL().toString();
            String queryString = request.getQueryString();

            urlString += queryString == null ? "" : "?" + queryString;
            URL url = new URL(urlString);

            log.info("Fetching >" + url.toString());

            con = (HttpURLConnection) url.openConnection();

            methodName = request.getMethod();
            con.setRequestMethod(methodName);
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setFollowRedirects(false);
            con.setUseCaches(true);

            for (Enumeration e = request.getHeaderNames(); e.hasMoreElements();) {
                String headerName = e.nextElement().toString();
                con.setRequestProperty(headerName, request.getHeader(headerName));
            }

            con.connect();

            if (methodName.equals("POST")) {
                BufferedInputStream clientToProxyBuf = new BufferedInputStream(
                    request.getInputStream());
                BufferedOutputStream proxyToWebBuf = new BufferedOutputStream(
                    con.getOutputStream());

                while ((oneByte = clientToProxyBuf.read()) != -1)
                    proxyToWebBuf.write(oneByte);

                proxyToWebBuf.flush();
                proxyToWebBuf.close();
                clientToProxyBuf.close();
            }

            statusCode = con.getResponseCode();
            response.setStatus(statusCode);

            for (Iterator i = con.getHeaderFields().entrySet().iterator(); i.hasNext();) {
                Map.Entry mapEntry = (Map.Entry) i.next();
                if (mapEntry.getKey() != null)
                    response.setHeader(mapEntry.getKey().toString(),
                        ((List) mapEntry.getValue()).get(0).toString());
            }

            webToProxyBuf = new BufferedInputStream(con.getInputStream());
            proxyToClientBuf = new BufferedOutputStream(response.getOutputStream());

            while ((oneByte = webToProxyBuf.read()) != -1)
                proxyToClientBuf.write(oneByte);

            proxyToClientBuf.flush();
            proxyToClientBuf.close();

            webToProxyBuf.close();
            con.disconnect();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
        }
    }

}
