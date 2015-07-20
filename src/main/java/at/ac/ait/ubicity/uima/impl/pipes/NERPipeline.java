package at.ac.ait.ubicity.uima.impl.pipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class NERPipeline implements AnalyticsPipeline {

	private static final Logger logger = Logger.getLogger(NERPipeline.class);
	private StanfordCoreNLP pipeline;

	@Override
	public String getModuleId() {
		return "NER";
	}

	@Override
	public void init(String modelLocation) {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
		props.setProperty("ner.applyNumericClassifiers", "false");
		props.setProperty("ner.useSUTime", "false");

		props.put("pos.model", modelLocation + "pos/english-left3words-distsim.tagger");
		props.put("ner.model", modelLocation + "ner/english.all.3class.distsim.crf.ser.gz");
		props.put("ner.useSUTime", "false");

		pipeline = new StanfordCoreNLP(props);
	}

	@Override
	public NamedEntities process(String text) {
		Annotation ann = new Annotation(text);
		pipeline.annotate(ann);

		// logger.info(pipeline.timingInformation());
		return structure(ann);
	}

	private NamedEntities structure(Annotation ann) {
		List<CoreLabel> wordList = ann.get(CoreAnnotations.TokensAnnotation.class);

		String entity = "";
		NamedEntities ne = new NamedEntities();

		for (int i = 0; i < wordList.size(); i++) {
			// Merge multiple successive NE of same type
			if (NamedEntities.PERSON.equalsIgnoreCase(wordList.get(i).ner().toString())) {
				entity += wordList.get(i).originalText() + " ";

				if (i + 1 == wordList.size() || !NamedEntities.PERSON.equalsIgnoreCase(wordList.get(i + 1).ner().toString())) {
					if (!ne.persons.contains(entity.toLowerCase().trim())) {
						ne.persons.add(entity.toLowerCase().trim());
					}
					entity = "";
				}
			}
			// Merge multiple successive NE of same type
			else if (NamedEntities.ORG.equalsIgnoreCase(wordList.get(i).ner().toString())) {
				entity += wordList.get(i).originalText() + " ";

				if (i + 1 == wordList.size() || !NamedEntities.ORG.equalsIgnoreCase(wordList.get(i + 1).ner().toString())) {
					if (!ne.organizations.contains(entity.toLowerCase().trim())) {
						ne.organizations.add(entity.toLowerCase().trim());
					}
					entity = "";
				}
			}
			// Merge multiple successive NE of same type
			else if (NamedEntities.LOC.equalsIgnoreCase(wordList.get(i).ner().toString())) {
				entity += wordList.get(i).originalText() + " ";

				if (i + 1 == wordList.size() || !NamedEntities.LOC.equalsIgnoreCase(wordList.get(i + 1).ner().toString())) {
					if (!ne.locations.contains(entity.toLowerCase().trim())) {
						ne.locations.add(entity.toLowerCase().trim());
					}
					entity = "";
				}
			}
		}

		return ne;
	}

	/**
	 * Object with all extracted Named Entities
	 *
	 */
	public class NamedEntities {
		static final String PERSON = "PERSON";
		static final String LOC = "LOCATION";
		static final String ORG = "ORGANIZATION";

		public List<String> persons = new ArrayList<String>();
		public List<String> organizations = new ArrayList<String>();
		public List<String> locations = new ArrayList<String>();
	}
}
