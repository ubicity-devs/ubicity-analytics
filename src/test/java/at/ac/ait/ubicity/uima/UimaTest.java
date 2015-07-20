package at.ac.ait.ubicity.uima;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.BeforeClass;
import org.junit.Test;

public class UimaTest {

	private static final Logger logger = Logger.getLogger(UimaTest.class);
	private static AnalysisEngine ae;

	private static String testDataLoc = "C:\\temp/data/";
	private static String uimaDescriptorLoc = "C:\\temp/descriptors/";

	@BeforeClass
	public static void setup() throws Exception {
		File taeDescriptor = new File(uimaDescriptorLoc + "example.xml");

		XMLInputSource in = new XMLInputSource(taeDescriptor);

		ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
		ae = UIMAFramework.produceAnalysisEngine(specifier);
	}

	@Test
	public void testAnnotation() {
		try {
			String doc = FileUtils.file2String(new File(testDataLoc + "Apache_UIMA.txt"));

			JCas cas = ae.newJCas();
			cas.setDocumentText(doc);
			ae.process(cas);

			ae.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
