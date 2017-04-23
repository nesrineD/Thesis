package tu.master.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.LocalEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FileSystem.WriteMode;
import org.apache.flink.graph.Graph;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import edu.stanford.nlp.util.CoreMap;
import tu.master.ConceptDetection.GraphCreation;
import tu.master.ConceptDetection.Helper;
import edu.cmu.lti.ws4j.impl.WuPalmer;

public class SentenceCompare {

	static LocalEnvironment env;
	private static ILexicalDatabase db = new NictWordNet();

	private static RelatednessCalculator rc = new WuPalmer(db);
	static List<Tuple2<String, String>> sentImpMapping = new ArrayList<Tuple2<String, String>>();
	private static BufferedReader train;
	private static BufferedReader br;

	private static double[][] run(String[] words1, String[] string) {
		WS4JConfiguration.getInstance().setMFS(true);
		double[][] similarity = rc.getSimilarityMatrix(words1, string);
		for (double[] row : similarity) {
			System.out.println(java.util.Arrays.toString(row));
		}

		return similarity;
	}

	public static void execution() {
		Configuration conf = new Configuration();
		conf.setFloat(ConfigConstants.TASK_MANAGER_MEMORY_FRACTION_KEY, 0.3f);
		env = LocalEnvironment.createLocalEnvironment(conf);
	}

	public static List<String[]> sentenceTokenizer(String pathToTrainingText) throws Exception {
		List<String[]> sentences = new ArrayList<String[]>();
		Helper help = new Helper();
		train = new BufferedReader(new FileReader(pathToTrainingText));
		String readString = null;

		while ((readString = train.readLine()) != null) {
			help.setSentences(help.performAnnotation(readString));
		}

		// tokenize the training text
		if (help.getSentences().size() > 1) {
			for (int sent = 0; sent < help.getSentences().size() - 1; sent += 2) {
				CoreMap currentsentence = help.getSentences().get(sent);
				List<String> vertexList = new ArrayList<String>();
				vertexList = help.parseISentence(currentsentence);
			//String[] sentence = vertexList.toArray(new String[0]);
				sentences.add(vertexList.toArray(new String[0]));
				//help.getNodesList().addAll(vertexList);
				//help.followEdges(help.getEdges(), help.getListOfvertices());
				//help.childEdges(help.getEdges(), help.semanticGraph(currentsentence));
				List<String> impl = help.parseImplication(help.getSentences().get(sent + 1));
				if (impl != null) {
					help.addImplicationEdges(vertexList, help.parseImplication(help.getSentences().get(sent + 1)));
					// fill the list of tuples sent, imp
					Tuple2<String, String> sentTup = new Tuple2<String, String>(vertexList.toString(),
							impl.toString());
					sentImpMapping.add(sentTup);
				}
			}
			// store the sentence implication mappin in a file
			ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment();
			env.setParallelism(1);
			DataSet<Tuple2<String, String>> mapping = env.fromCollection(sentImpMapping);
			mapping.writeAsCsv("file.csv", "\n", "|", WriteMode.OVERWRITE);
			env.execute();

		}
		// tokenize the testing sentence -- case just one sentence
		/*else {
			CoreMap currentsentence = help.getSentences().get(0);
			List<String> vertexList = new ArrayList<String>();
			vertexList = help.parseISentence(currentsentence);
			sentences.add(vertexList.toArray(new String[0]));
		}*/
		return sentences;
	}

	public static Tuple2<String[], Double> relatednessMeasure(List<String[]> sentences, List<String[]> trainingSentences)
			throws Exception {
		// sentences is a list of just one sentence to match
		
		//List<String[]> trainingSentences = sentenceTokenizer(trainingSentences2);
		double maxR = 0.0;
		String[] mostSimSent = null;
		for (int sent = 0; sent < trainingSentences.size(); sent++) {
			// double[][] similarity = new
			// double[sentences.get(0).length][sentences.get(sent).length];
			double[][] similarity = run(sentences.get(0), trainingSentences.get(sent));
			double sum = 0.0;
			for (int i = 0; i < Math.min(sentences.get(0).length, trainingSentences.get(sent).length); i++) {
				sum += similarity[i][i];
			}
			double relate = 2.0 * sum / (sentences.get(0).length + trainingSentences.get(sent).length);
			System.out.println(" the relatedness with index " + sent + " is " + relate);
			if (relate > maxR) {
				maxR = relate;
				mostSimSent = trainingSentences.get(sent);
			}
		}
		Tuple2<String[], Double> SentRelate = new Tuple2<String[], Double>(mostSimSent, maxR);
		return SentRelate;
	}

	public static void main(String[] args) throws Exception {
		
		
		List<String[]> trainingSentences = new ArrayList<String[]>();
		List<String[]> sentences = new ArrayList<String[]>();
		String pathToTestingSentence = "resources\\sentence.txt";
		String pathToTrainingText = "resources\\sentence2.txt";
	       
		Helper help = new Helper();
		GraphCreation gcreate = new GraphCreation();
		br = new BufferedReader(new FileReader(pathToTestingSentence));
		String read = null;
		while ((read = br.readLine()) != null) {
			help.setTsentences(help.performAnnotation(read));
		}
		// add follow and child edges
		//for (int sent = 0; sent < help.getTsentences().size(); sent += 1) {
			List<String> vertexList = new ArrayList<String>();
			vertexList = help.parseISentence(help.getTsentences().get(0)); //
			sentences.add(vertexList.toArray(new String[0]));
			gcreate.getTestingList().addAll(vertexList); // //
			// parseISentence(getSentences().get(sent));
			help.followEdges(gcreate.getTedges(), help.getListOfvertices());
			help.childEdges(gcreate.getTedges(), help.semanticGraph(help.getTsentences().get(0)));
		//}
			
			// read the sentence implication mapping
			ExecutionEnvironment envi = ExecutionEnvironment.createLocalEnvironment();
			envi.setParallelism(1);
			DataSet<Tuple2<String, String>> map = envi.readCsvFile("file.csv").fieldDelimiter("|").lineDelimiter("\n")
					.types(String.class, String.class);
			// map.
			System.out.println(" reading from Dataset  \n");
			List<Tuple2<String, String>> mapping = map.collect();
			mapping.forEach(System.out::println);	
			for (Tuple2<String, String> tup: mapping){
				String sent = tup.f0;
				
				List<String> psentences =  help.parseImplication(help.performAnnotation(sent).get(0));
				trainingSentences.add(psentences.toArray(new String[0]));
				System.out.println(" List<String> psentences : \n" );
				psentences.forEach(System.out::println);
			}
			System.out.println(" the trainin sentences are \n ");
			trainingSentences.forEach(System.out::println);
		
		long t0 = System.currentTimeMillis();
		Tuple2<String[], Double> relatedness = relatednessMeasure(sentences, trainingSentences);
		long t1 = System.currentTimeMillis();
		System.out.println("Done in " + (t1 - t0) + " msec.");
		System.out.println(" the maximum relatedness is  " + relatedness.f1);
		System.out.println(" the sentence  is \n ");
		String[] sentence = relatedness.f0;
		for (int i = 0; i < sentence.length; i++)
			System.out.println(sentence[i]);
		System.out.println(" the induced implications are \n ");
		// @ToDO extract the implication from the stored file
		
		List<Tuple2<String, String>> implication = mapping.stream().filter(k -> k.f0.equals(Arrays.toString(sentence)))
				.collect(Collectors.toList());

		String inducedImp = implication.get(0).f1;

		List<String> implications = help.parseImplication(help.performAnnotation(inducedImp).get(0));
		System.out.println(" the induced imp is ");
		implications.forEach(System.out::println);

		// create the testing graph

		gcreate.addImplicationEdges(vertexList, implications);
		// call testing graph method
		System.out.println(" the testing sentence is ");
		vertexList.forEach(System.out::println);
		Graph<String, Long, String> graph = gcreate.initialGraph(gcreate.getTedges());
		System.out.println(" testing edges are ");
		graph.getEdges().print();
		gcreate.visualizeGraph("resources\\sentence.graphml",
				gcreate.getTestingList(), graph.getEdges().collect(),null,null);

	}

}