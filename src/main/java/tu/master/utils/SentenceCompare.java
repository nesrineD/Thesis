package tu.master.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import edu.stanford.nlp.util.CoreMap;
import tu.master.ConceptDetection.GraphCreation;
import tu.master.ConceptDetection.Helper;
import edu.cmu.lti.ws4j.impl.Lin;
public class SentenceCompare {
	

	private static ILexicalDatabase db = new edu.cmu.lti.lexical_db.NictWordNet();
	private static RelatednessCalculator rc = new Lin(db);
	static List<Tuple2<String[], List<String>>> sentImpMapping = new ArrayList<Tuple2<String[], List<String>>>();
	private static BufferedReader train;

	private static double[][] run(String[] words1, String[] words2) {
		WS4JConfiguration.getInstance().setMFS(true);
		double[][] similarity = rc.getSimilarityMatrix(words1, words2);
		for (double[] row : similarity) {
			System.out.println(java.util.Arrays.toString(row));
		}

		return similarity;
	}

	public static List<String[]> sentenceTokenizer(String pathToTrainingText) throws Exception {
		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
		List<String[]> sentences = new ArrayList<String[]>();
		Helper help = new Helper();
		train = new BufferedReader(new FileReader(pathToTrainingText));
		String readString = null;

		while ((readString = train.readLine()) != null) {
			help.setSentences(help.performAnnotation(readString));
		}
    	
    	if(help.getSentences().size()>1){
		for (int sent = 0; sent < help.getSentences().size() - 1; sent += 2) {
			CoreMap currentsentence = help.getSentences().get(sent);
			List<String> vertexList = new ArrayList<String>();
			vertexList = help.parseISentence(currentsentence);
			String[] sentence = vertexList.toArray(new String[0]);
			sentences.add(vertexList.toArray(new String[0]));
			// sentence = (String[]) vertexList.toArray();
			help.getNodesList().addAll(vertexList);
			help.followEdges(help.getEdges(), help.getListOfvertices());
			help.childEdges(help.getEdges(), help.semanticGraph(currentsentence));
			List<String> impl = help.parseImplication(help.getSentences().get(sent + 1));
			if (impl != null) {
				help.addImplicationEdges(vertexList, help.parseImplication(help.getSentences().get(sent + 1)));
				// fill the list of tuples sent, imp
				Tuple2<String[], List<String>> sentTup = new Tuple2<String[], List<String>>(sentence,
						impl);
				sentImpMapping.add(sentTup);
			}
		}
			// store the sentence and its implications in a CSV TextFile
        
			//sentImpMapping.forEach(System.out::println);
			DataSet<Tuple2<String[], List<String>>> mapping = env.fromCollection(sentImpMapping);
			//mapping.print();
			//mapping.getType();
			mapping.writeAsCsv("resources\\mappings\\sentImpMap.csv");
        }
    	else{
    		CoreMap currentsentence = help.getSentences().get(0);
			List<String> vertexList = new ArrayList<String>();
			vertexList = help.parseISentence(currentsentence);
			sentences.add(vertexList.toArray(new String[0]));
    	}
			return sentences;
	}

	public static Tuple2<String[],Double> relatednessMeasure(String pathToTestingText, String pathToTrainingText) throws Exception {
		// sentences is a list of just one sentence to match 
		List<String[]> sentences = sentenceTokenizer(pathToTestingText);
	    List<String[]> sentences2 = sentenceTokenizer(pathToTrainingText);
		double maxR = 0.0;
		String[] mostSimSent = null;
		for (int sent = 0; sent < sentences2.size(); sent++) {
			//double[][] similarity = new double[sentences.get(0).length][sentences.get(sent).length];
			double[][] similarity = run(sentences.get(0), sentences2.get(sent));
			double sum = 0.0;
			for (int i = 0; i < Math.min(sentences.get(0).length, sentences2.get(sent).length); i++) {
				sum += similarity[i][i];
			}
				double relate = 2.0 * sum / (sentences.get(0).length + sentences2.get(sent).length);
				System.out.println(" the relatedness with index "+ sent + " is " + relate);
				if (relate>maxR){
					maxR = relate;
					mostSimSent = sentences2.get(sent);
				}
			}
		   Tuple2<String[],Double> SentRelate = new Tuple2<String[],Double>(mostSimSent,maxR);
			
			//relatedness += relate;
			//System.out.println("The relatedness of the two sentences is " + relate);
		//}
		return SentRelate;
	}

	public static void main(String[] args) throws Exception {
		//String pathToTrainingText ="resources\\JayDickText\\JayDickLabledT.txt";
		//String pathToTestingSentence = "resources\\JayDickText\\testingSentence.txt";
		String pathToTestingSentence = "resources\\sentence.txt";
	    String pathToTrainingText ="resources\\sentence2.txt";
		long t0 = System.currentTimeMillis();
		Tuple2<String[],Double> relatedness = relatednessMeasure(pathToTestingSentence,pathToTrainingText);
		long t1 = System.currentTimeMillis();
		System.out.println("Done in " + (t1 - t0) + " msec.");
		System.out.println(" the maximum relatedness is  " +  relatedness.f1);
		System.out.println(" the sentence  is \n " );
		String[] sentence = relatedness.f0;
		for(int i =0 ; i< sentence.length; i++)
			System.out.println(sentence[i]);
		System.out.println(" the induced implications are \n " );
		
		List<Tuple2<String[], List<String>>> implication =sentImpMapping.stream().filter(k -> (Arrays.equals(k.f0, sentence))).collect(Collectors.toList());
		//implication.forEach(System.out::println);
		
		List<String> inducedImp = implication.get(0).f1;
		inducedImp.forEach(System.out::print);
		
	}

}