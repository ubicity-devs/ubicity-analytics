package at.ac.ait.ubicity.uima;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;

import org.apache.log4j.Logger;
import org.apache.uima.util.FileUtils;
import org.junit.Test;

public class OpenNLPTest {

	private static final Logger logger = Logger.getLogger(OpenNLPTest.class);
	private static String testDataLoc = "C:\\temp/data/";
	private static String modelLoc = "C:\\temp/stanford/";

	@Test
	public void testTrainSentiment() throws Exception {

		// load training set
		InputStream dataIn = new FileInputStream(testDataLoc + "train_pos.txt");
		ObjectStream<String> lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
		ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

		// Train
		DoccatModel model = DocumentCategorizerME.train("en", sampleStream);

		// Write model
		OutputStream modelOut = null;
		modelOut = new BufferedOutputStream(new FileOutputStream(modelLoc + "en-tweets-pos.bin"));
		model.serialize(modelOut);

		dataIn.close();
		modelOut.close();

		// load training set
		dataIn = new FileInputStream(testDataLoc + "train_neg.txt");
		lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
		sampleStream = new DocumentSampleStream(lineStream);

		// Train
		model = DocumentCategorizerME.train("en", sampleStream);

		// Write model
		modelOut = null;
		modelOut = new BufferedOutputStream(new FileOutputStream(modelLoc + "en-tweets-neg.bin"));
		model.serialize(modelOut);

		dataIn.close();
		modelOut.close();
	}

	@Test
	public void testSentiment() throws Exception {

		InputStream modelIn = new FileInputStream(modelLoc + "en-tweets-pos.bin");
		DoccatModel m = new DoccatModel(modelIn);
		DocumentCategorizerME myCategorizer = new DocumentCategorizerME(m);

		String doc = FileUtils.file2String(new File(testDataLoc + "tweets_doc.txt"));

		double[] outcomesP = myCategorizer.categorize(doc);
		String bestPos = myCategorizer.getBestCategory(outcomesP);

		modelIn = new FileInputStream(modelLoc + "en-tweets-neg.bin");
		m = new DoccatModel(modelIn);
		myCategorizer = new DocumentCategorizerME(m);

		double[] outcomesN = myCategorizer.categorize(doc);
		String bestNeg = myCategorizer.getBestCategory(outcomesN);

		System.out.println(bestPos + " " + bestNeg);
	}

	/**
	 * http://erwinkomen.ruhosting.nl/eng/2014_Longdale-Labels.htm
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPos() throws Exception {

		InputStream modelIn = new FileInputStream(modelLoc + "en-pos-maxent.bin");
		POSModel model = new POSModel(modelIn);
		POSTaggerME tagger = new POSTaggerME(model);

		ObjectStream<String> untokenizedLineStream = new PlainTextByLineStream(new FileReader(new File(testDataLoc + "doc.txt")));

		String line;
		while ((line = untokenizedLineStream.read()) != null) {
			String whitespaceTokenizerLine[] = WhitespaceTokenizer.INSTANCE.tokenize(line);

			for (int i = 0; i < whitespaceTokenizerLine.length; i++) {
				whitespaceTokenizerLine[i] = whitespaceTokenizerLine[i].replaceAll("[\\(,.\\)]", "");
			}

			String tags[] = tagger.tag(whitespaceTokenizerLine);
			double probs[] = tagger.probs();

			for (int i = 0; i < whitespaceTokenizerLine.length; i++) {
				System.out.println(whitespaceTokenizerLine[i] + " [" + tags[i] + "(" + ((int) (probs[i] * 100)) + ")] ");
			}
		}
	}

	@Test
	public void testFinder() throws Exception {

		List<NameFinderME> finders = new ArrayList<NameFinderME>();

		finders.add(loadFinder(modelLoc + "en-ner-person.bin"));
		finders.add(loadFinder(modelLoc + "en-ner-organization.bin"));
		finders.add(loadFinder(modelLoc + "en-ner-date.bin"));
		finders.add(loadFinder(modelLoc + "en-ner-location.bin"));
		finders.add(loadFinder(modelLoc + "en-ner-time.bin"));
		finders.add(loadFinder(modelLoc + "en-ner-money.bin"));
		finders.add(loadFinder(modelLoc + "en-ner-percentage.bin"));

		long ts = System.currentTimeMillis();

		ObjectStream<String> untokenizedLineStream = new PlainTextByLineStream(new FileReader(new File(testDataLoc + "doc.txt")));

		String line;
		while ((line = untokenizedLineStream.read()) != null) {
			String whitespaceTokenizerLine[] = WhitespaceTokenizer.INSTANCE.tokenize(line);

			if (whitespaceTokenizerLine.length == 0) {
				finders.forEach((finder) -> {
					finder.clearAdaptiveData();
				});
			}

			List<Span> names = new ArrayList<Span>();

			finders.forEach((finder) -> {
				Collections.addAll(names, finder.find(whitespaceTokenizerLine));
			});

			Span reducedNames[] = NameFinderME.dropOverlappingSpans(names.toArray(new Span[names.size()]));

			Arrays.asList(reducedNames).forEach((entry) -> {
				String name = "";
				for (int j = entry.getStart(); j < entry.getEnd(); j++) {
					name += whitespaceTokenizerLine[j] + " ";
				}

				logger.info(entry.toString() + " --> " + name);
			});
		}

		System.out.println(System.currentTimeMillis() - ts);
	}

	private NameFinderME loadFinder(String file) throws Exception {
		InputStream modelOrgIn = new FileInputStream(file);
		TokenNameFinderModel modelOrgPerson = new TokenNameFinderModel(modelOrgIn);
		return new NameFinderME(modelOrgPerson);
	}
}