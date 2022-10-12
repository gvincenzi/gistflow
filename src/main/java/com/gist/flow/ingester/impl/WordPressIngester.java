package com.gist.flow.ingester.impl;

import com.gist.flow.exception.FlowException;
import com.gist.flow.model.entity.FlowResource;
import lombok.Data;
import lombok.extern.java.Log;
import com.gist.flow.ingester.IFlowIngester;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

@Slf4j
@Data
@Service
@Profile("soffblog")
@ConfigurationProperties("wpingester")
public class WordPressIngester implements IFlowIngester<FlowResource> {
	private static final int DEFAULT_CATEGORY = 1;

	/**
	 * Date formatter
	 */
	protected static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private String postStatus;
	private String commentStatus;
	private String postType;
	private String wordPressWSURL;
	private Map<Integer, String> categories;

	@SuppressWarnings("unused")
	@Override
	public void ingest(Set<FlowResource> contents) throws FlowException {
		log.info(String.format("WordPressIngester ingest has been called for [%d] contents",contents.size()));
		for (FlowResource wordPressResource : contents) {
			log.info(String.format("Ingestion of content [%s]",wordPressResource.getName()));
			try {
				String data = URLEncoder.encode("method", "UTF-8") + "=" + URLEncoder.encode("insertPost", "UTF-8");
				data += "&" + URLEncoder.encode("post_title", "UTF-8") + "="
						+ URLEncoder.encode(wordPressResource.getName(), "UTF-8");
				data += "&" + URLEncoder.encode("post_content", "UTF-8") + "="
						+ URLEncoder.encode(wordPressResource.getDescription().toString(), "UTF-8");
				data += "&" + URLEncoder.encode("post_status", "UTF-8") + "="
						+ URLEncoder.encode(postStatus, "UTF-8");
				data += "&" + URLEncoder.encode("comment_status", "UTF-8") + "="
						+ URLEncoder.encode(commentStatus, "UTF-8");
				data += "&" + URLEncoder.encode("post_type", "UTF-8") + "="
						+ URLEncoder.encode(postType, "UTF-8");
				data += "&" + URLEncoder.encode("post_category", "UTF-8") + "="
						+ URLEncoder.encode(String.valueOf(findCategory(wordPressResource)), "UTF-8");

				//formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
				data += "&" + URLEncoder.encode("post_date_gmt", "UTF-8") + "=" + URLEncoder
						.encode(formatter.format(wordPressResource.getStartDateOfValidity().getTime()), "UTF-8");

				URL wp_ws = new URL(wordPressWSURL);
				URLConnection conn = wp_ws.openConnection();
				conn.setDoInput(true);
				conn.setDoOutput(true);
				conn.setUseCaches(false);
				conn.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
				conn.connect();
				DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
				wr.writeBytes(data);
				wr.flush();

				BufferedReader r = new BufferedReader(
						new InputStreamReader(conn.getInputStream(), Charset.forName("UTF-8")));

				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					sb.append(line);
				}

				NodeList listOfPosts = null;
				// Attr postTitle = null
				Attr postUrl = null, remoteId = null;
				DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
				Document doc = docBuilder.parse(new ByteArrayInputStream(sb.toString().getBytes()));
				doc.getDocumentElement().normalize();
				listOfPosts = doc.getElementsByTagName("post");
				Node nNode = null;
				nNode = listOfPosts.item(0);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					postUrl = eElement.getAttributeNode("url");
					remoteId = eElement.getAttributeNode("remoteid");
				}
			} catch (UnsupportedEncodingException e) {
				throw new FlowException(e);
			} catch (MalformedURLException e) {
				throw new FlowException(e);
			} catch (IOException e) {
				throw new FlowException(e);
			} catch (ParserConfigurationException e) {
				throw new FlowException(e);
			} catch (SAXException e) {
				throw new FlowException(e);
			}
		}
		log.info(String.format("WordPressIngester ingest successfully terminated for [%d] contents",contents.size()));
	}

	private Integer findCategory(FlowResource media) throws FlowException {
		for (Integer categoryIndex : categories.keySet()) {
			String keywords = categories.get(categoryIndex);
			for (String keyword : keywords.split(",")) {
				String name = media.getName().toUpperCase();
				if (name.contains(keyword.toUpperCase())) {
					return categoryIndex;
				}
			}
		}

		return DEFAULT_CATEGORY;
	}

	public Calendar getLastPostDate() throws FlowException {
		Calendar returned = getLastPostDate("getLastPostScheduledDate");
		if (returned != null) {
			return returned;
		} else {
			return getLastPostDate("getLastPostDate");
		}
	}

	private Calendar getLastPostDate(String method) throws FlowException {
		Calendar lastPostDate = Calendar.getInstance();
		try {
			String data = URLEncoder.encode("method", "UTF-8") + "=" + URLEncoder.encode(method, "UTF-8");

			URL wp_ws = new URL(wordPressWSURL);
			URLConnection conn = wp_ws.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestProperty("User-Agent",
					"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
			conn.connect();
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.writeBytes(data);
			wr.flush();

			BufferedReader r = new BufferedReader(
					new InputStreamReader(conn.getInputStream(), Charset.forName("UTF-8")));

			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				sb.append(line);
			}

			NodeList listOfPosts = null;
			Attr date = null;
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new ByteArrayInputStream(sb.toString().getBytes()));
			doc.getDocumentElement().normalize();
			listOfPosts = doc.getElementsByTagName("post");
			Node nNode = null;

			if (listOfPosts.getLength() <= 0) {
				return null;
			}

			nNode = listOfPosts.item(0);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				date = eElement.getAttributeNode("date");
				lastPostDate.setTime(formatter.parse(date.getValue()));
			}
			return lastPostDate;
		} catch (SAXParseException e) {
			return null;
		} catch (UnsupportedEncodingException e) {
			throw new FlowException(e);
		} catch (MalformedURLException e) {
			throw new FlowException(e);
		} catch (IOException e) {
			throw new FlowException(e);
		} catch (ParserConfigurationException e) {
			throw new FlowException(e);
		} catch (SAXException e) {
			throw new FlowException(e);
		} catch (ParseException e) {
			throw new FlowException(e);
		}
	}

}
