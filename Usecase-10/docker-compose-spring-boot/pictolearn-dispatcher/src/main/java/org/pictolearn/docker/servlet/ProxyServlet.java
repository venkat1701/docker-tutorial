package org.pictolearn.docker.servlet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(urlPatterns = "/proxyServlet/*", loadOnStartup = 1)
public class ProxyServlet extends HttpServlet {

	private static final long serialVersionUID = 2787920473586060865L;

	private static final Logger logger = LoggerFactory.getLogger(ProxyServlet.class);

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String path = request.getRequestURI().substring(request.getContextPath().length());
		path = path.substring(path.indexOf("/proxyServlet/")+"/proxyServlet/".length(),path.length());
		logger.debug("Path to query for GET Request: {}  ", path);
		if(StringUtils.isEmpty(path)){
			  response.setContentType("text/html");
			    PrintWriter out = response.getWriter();
			    out.println("Invalid GET CALL");
			    out.close();
			    return;
		}
		String ipAddress = getRandomIpAddress(response);
		
		String url = "http://" + ipAddress + ":8080/" + path;
		logger.debug("GET url :{} ", url);

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		response.addHeader("WEB-HOST", ipAddress);
		sendResponse(response, con);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String path = request.getRequestURI().substring(request.getContextPath().length());
		path = path.substring(path.indexOf("/proxyServlet/")+"/proxyServlet/".length(),path.length());
		logger.debug("Path to query for POST Request: {}  ", path);
		if(StringUtils.isEmpty(path)){
			  response.setContentType("text/html");
			    PrintWriter out = response.getWriter();
			    out.println("Invalid POST CALL empty URI");
			    out.close();
			    return;
		}
		String ipAddress = getRandomIpAddress(response);
		
		response.addHeader("WEB-HOST", ipAddress);
		String url = "http://" + ipAddress + ":8080/" + path;

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		logger.debug("POST url :{} ", url);

		StringBuilder sb = new StringBuilder();
		for (Entry<String, String[]> e : request.getParameterMap().entrySet()) {
			if (sb.length() > 0) {
				sb.append('&');
			}
			String[] temp = e.getValue();
			for (String s : temp) {
				sb.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(s, "UTF-8"));
			}
		}

		String urlParameters = sb.toString();
		logger.debug("POST url paramters :{} ", urlParameters);
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
		sendResponse(response, con);

	}

	/**
	 * return random IP Address
	 * 
	 * @param response
	 * @return
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private String getRandomIpAddress(HttpServletResponse response) throws UnknownHostException, IOException {
		List<String> ipAddr = new ArrayList<>();
		for (InetAddress addr : InetAddress.getAllByName("web")) {
			logger.debug("Hostnames {}", addr.getHostAddress());
			ipAddr.add(addr.getHostAddress());
		}
		int size = ipAddr.size();
		if (size < 1) {
			logger.error("Size less than 1");
			response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
			response.getWriter().println("Server unavailable");
		}

		logger.debug("Total hosts : {} ", size);

		int random = 0;
		if (size > 1) {
			random = ThreadLocalRandom.current().nextInt(0, ipAddr.size() - 1);
		}

		String ipAddrStr = ipAddr.get(random);
		logger.debug("Returned IP addr : {} ", ipAddrStr);
		return ipAddrStr;
	}

	/**
	 * send response to client.
	 * 
	 * @param response
	 * @param con
	 * @throws ProtocolException
	 * @throws IOException
	 */
	private void sendResponse(HttpServletResponse response, HttpURLConnection con)
			throws ProtocolException, IOException {

		int responseCode = con.getResponseCode();

		if (responseCode == 200) {
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

			String inputLine;
			StringBuffer stringOutput = new StringBuffer();
			ServletOutputStream servletOutputStream = response.getOutputStream();

			while ((inputLine = in.readLine()) != null) {
				stringOutput.append(inputLine);
				servletOutputStream.write(inputLine.getBytes());
			}
			in.close();
			servletOutputStream.flush();
		} else {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Internal Server Error");
		}

	}
}