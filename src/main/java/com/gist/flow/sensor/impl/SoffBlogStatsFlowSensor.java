package com.gist.flow.sensor.impl;

import com.gist.flow.actuator.IFlowActuator;
import com.gist.flow.actuator.impl.GistFlowActuator;
import com.gist.flow.configuration.RssFlowSensorConfiguration;
import com.gist.flow.exception.FlowException;
import com.gist.flow.model.entity.FlowResource;
import com.gist.flow.sensor.IFlowSensor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

@Data
@Service
@Slf4j
@Profile("soffblog")
public class SoffBlogStatsFlowSensor implements IFlowSensor<FlowResource> {
	private Set<IFlowActuator<FlowResource>> flowActuators;

	@Value("${wpingester.wordPressWSURL}")
	private String wordPressWSURL;

	@Autowired
	private RssFlowSensorConfiguration rssFlowSensorConfiguration;

	@Autowired
	public SoffBlogStatsFlowSensor(@Autowired Set<IFlowActuator<FlowResource>> flowActuators) {
		this.flowActuators = flowActuators;
		for (IFlowActuator<FlowResource> flowActuator : flowActuators){
			if(flowActuator.getClass() != GistFlowActuator.class){
				getFlowActuators().remove(flowActuator);
			}
		}
	}

	@Scheduled(cron = "0 30 23 ? * SUN")
	public void getStats() throws FlowException {
		try {
			String data = URLEncoder.encode("method", "UTF-8") + "=getStats&index=DAY&value=7";

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

			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new ByteArrayInputStream(sb.toString().getBytes()));
			doc.getDocumentElement().normalize();
			NodeList categories = doc.getElementsByTagName("category");

			Set<FlowResource> resources = new HashSet<>();
			int i = 0, postNumber = 0;
			FlowResource flowResource = new FlowResource();
			StringBuilder builder = new StringBuilder("<br><br>");
			while(i<categories.getLength()) {
				Node nNode = categories.item(i++);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					String name = eElement.getAttributeNode("category_name").getValue();
					String categoryId = eElement.getAttributeNode("category_id").getValue();
					String count = eElement.getAttributeNode("count").getValue();
					builder.append(String.format("<a href='%s/?cat=%s'>%s</a> --- %s offerta(e)<br>",rssFlowSensorConfiguration.getRssLink(),categoryId,name,count));
					postNumber+=Integer.parseInt(count);
				}
			}
			flowResource.setName("Statistiche della settimana");
			builder.insert(0,String.format("Un totale di %d offerte di lavoro pubblicate e così suddivise in categorie<br>Clicca sul nome della categorie per vedere tutte le offerte sul blog :",postNumber));
			flowResource.setDescription(builder.toString());
			resources.add(flowResource);
			log.info("RssFlowSensor - Check completed");
			if (!resources.isEmpty()) {
				try {
					onChange(resources);
				} catch (FlowException e) {
					log.error(e.getMessage());
				}
			} else {
				log.info("RssFlowSensor - No changes founded");
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

	@Override
	public void onChange(Set<FlowResource> resources) throws FlowException {
		log.info(String.format("SoffBlogStatsFlowSensor onChange has been called with [%d] resources", resources.size()));
		for(IFlowActuator flowActuator : getFlowActuators()){
			flowActuator.doAction(resources);
		}
		log.info(String.format("SoffBlogStatsFlowSensor onChange has been successfully terminated with [%d] resources",
				resources.size()));
	}

	@Override
	public Calendar getLastChangeDate() {
		return null;
	}
}
