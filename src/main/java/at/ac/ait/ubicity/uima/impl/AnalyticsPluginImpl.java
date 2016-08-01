package at.ac.ait.ubicity.uima.impl;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import net.xeoh.plugins.base.annotations.events.Shutdown;

import org.apache.log4j.Logger;

import at.ac.ait.ubicity.commons.broker.BrokerConsumer;
import at.ac.ait.ubicity.commons.broker.BrokerProducer;
import at.ac.ait.ubicity.commons.broker.events.EventEntry;
import at.ac.ait.ubicity.commons.dto.rss.RssDTO;
import at.ac.ait.ubicity.commons.exceptions.UbicityBrokerException;
import at.ac.ait.ubicity.commons.util.PropertyLoader;
import at.ac.ait.ubicity.uima.AnalyticsPlugin;
import at.ac.ait.ubicity.uima.impl.pipes.NERPipeline;
import at.ac.ait.ubicity.uima.impl.pipes.NERPipeline.NamedEntities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@PluginImplementation
public class AnalyticsPluginImpl extends BrokerConsumer implements AnalyticsPlugin {

	private String name;
	private static Logger logger = Logger.getLogger(AnalyticsPluginImpl.class);
	private static Gson gson = new GsonBuilder().create();

	private static NERPipeline nerPipe;
	// private SentimentPipeline sentimentPipe;

	private Producer producer;

	class Producer extends BrokerProducer {
		public Producer() throws UbicityBrokerException {
			super.init();
		}
	}

	@Override
	@Init
	public void init() {
		PropertyLoader config = new PropertyLoader(AnalyticsPluginImpl.class.getResource("/analytics.cfg"));
		this.name = config.getString("plugin.analytics.name");
		String modelLoc = config.getString("plugin.analytics.models");

		try {
			super.init();

			setConsumer(this, config.getString("plugin.analytics.broker.consumer"));
			producer = new Producer();

		} catch (Exception e) {
			logger.error("During init caught exc.", e);
		}

		if (nerPipe == null) {
			nerPipe = new NERPipeline();
			nerPipe.init(modelLoc);
		}

		// sentimentPipe = new SentimentPipeline();
		// sentimentPipe.init(modelLoc);

		logger.info(name + " loaded");
	}

	@Override
	@Shutdown
	public void shutdown() {
		super.shutdown();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void onReceived(String destination, EventEntry event) {

		try {
			if (destination.contains(".RSS.")) {
				event = processRSS(destination, event);
			}
			producer.publish(event);
		} catch (UbicityBrokerException e) {
			logger.error("Could not publish to CB: ", e);
		}
	}

	private synchronized EventEntry processRSS(String destination, EventEntry event) {

		RssDTO dto = gson.fromJson(event.getBody(), RssDTO.class);

		// Run NER (Named Entity Recognition Algorithm)
		if (destination.contains("." + nerPipe.getModuleId())) {
			NamedEntities res = nerPipe.process(dto.getText());
			dto.setAnalyticsResult(nerPipe.getModuleId(), res);
			event.setBody(dto.toJson());
		}

		// Run Standard Sentiment Analysis
		/*
		 * if (destination.contains("." + sentimentPipe.getModuleId())) { List<Integer> res = sentimentPipe.process(dto.getText());
		 * dto.setAnalyticsResult(sentimentPipe.getModuleId(), sentimentPipe.aggregate(res)); event.setBody(dto.toJson()); }
		 */
		return event;
	}

	@Override
	protected void onReceivedRaw(String destination, String tmsg) {
		throw new UnsupportedOperationException("Not supported.");
	}
}