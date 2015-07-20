package at.ac.ait.ubicity.uima;

import java.io.File;
import java.util.List;

import org.apache.uima.util.FileUtils;
import org.junit.Test;

import at.ac.ait.ubicity.uima.impl.pipes.NERPipeline;
import at.ac.ait.ubicity.uima.impl.pipes.NERPipeline.NamedEntities;
import at.ac.ait.ubicity.uima.impl.pipes.SentimentPipeline;

public class StanfordNLPTest {

	private static String testDataLoc = "C:\\temp/data/";
	private static String modelLoc = "C:\\temp/stanford/";

	@Test
	public void testNER() throws Exception {
		String doc = FileUtils.file2String(new File(testDataLoc + "doc.txt"));

		NERPipeline pipe = new NERPipeline();
		pipe.init(modelLoc);
		NamedEntities res = pipe.process(doc);

		res.persons.forEach((pers) -> {
			System.out.println(pers);
		});

		res.organizations.forEach((org) -> {
			System.out.println(org);
		});

		res.locations.forEach((loc) -> {
			System.out.println(loc);
		});
	}

	@Test
	public void testSentiment() throws Exception {
		String doc = FileUtils.file2String(new File(testDataLoc + "doc.txt"));

		SentimentPipeline pipe = new SentimentPipeline();
		pipe.init(modelLoc);
		List<Integer> res = pipe.process(doc);

		res.forEach((sent) -> {
			System.out.println(sent);
		});

		System.out.println(pipe.aggregate(res));
	}
}