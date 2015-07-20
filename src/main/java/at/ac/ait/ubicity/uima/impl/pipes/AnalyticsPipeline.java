package at.ac.ait.ubicity.uima.impl.pipes;

public interface AnalyticsPipeline {

	String getModuleId();

	void init(String modelLocation);

	Object process(String text);
}
