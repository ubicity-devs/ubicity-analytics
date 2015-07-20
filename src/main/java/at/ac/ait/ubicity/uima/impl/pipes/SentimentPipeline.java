package at.ac.ait.ubicity.uima.impl.pipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree;
import edu.stanford.nlp.util.CoreMap;

public class SentimentPipeline implements AnalyticsPipeline {

	private static final Logger logger = Logger.getLogger(SentimentPipeline.class);
	private StanfordCoreNLP pipeline;

	@Override
	public String getModuleId() {
		return "SENTIMENT";
	}

	@Override
	public void init(String modelLocation) {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, sentiment");
		props.setProperty("ner.applyNumericClassifiers", "false");
		props.setProperty("ner.useSUTime", "false");

		props.put("parse.model", modelLocation + "parse/englishPCFG.ser.gz");
		props.put("pos.model", modelLocation + "pos/english-left3words-distsim.tagger");
		props.put("ner.model", modelLocation + "ner/english.all.3class.distsim.crf.ser.gz");
		props.put("ner.useSUTime", "false");

		props.put("sentiment.model", modelLocation + "sentiment/sentiment.ser.gz");

		pipeline = new StanfordCoreNLP(props);
	}

	/**
	 * @param text
	 * @return List of sentiments per sentence: (-2) very negative, (0) neutral, (+2) very positive.
	 */
	@Override
	public List<Integer> process(String text) {
		Annotation ann = new Annotation(text);
		pipeline.annotate(ann);

		// logger.info(pipeline.timingInformation());
		return structure(ann);
	}

	private List<Integer> structure(Annotation ann) {
		List<Integer> sentList = new ArrayList<Integer>();

		for (CoreMap sentence : ann.get(CoreAnnotations.SentencesAnnotation.class)) {
			int score = RNNCoreAnnotations.getPredictedClass(sentence.get(SentimentAnnotatedTree.class));
			sentList.add(score - 3);
		}

		return sentList;
	}

	public Double aggregate(List<Integer> sentimentList) {

		Double agg = 0.0;

		for (Integer sent : sentimentList) {
			agg += sent;
		}
		return (agg / sentimentList.size());
	}
}
