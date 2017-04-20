package tu.master.ConceptDetection;

import java.awt.Container;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JFrame;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.LocalEnvironment;
import org.apache.flink.api.java.io.CsvReader;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.GraphAnalytic;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.library.similarity.JaccardIndex;
import org.apache.flink.types.LongValue;
import org.apache.flink.types.NullValue;
import org.apache.log4j.Logger;

import edu.stanford.nlp.util.CoreMap;
import tu.master.termfrequency.WordCount;

/**
 * @author Nesrine
 *
 */
@SuppressWarnings("serial")
public class Processor extends JFrame {

	final static Logger logger = Logger.getLogger(Processor.class);
	private PrintStream out;

	private BufferedReader train;
	Helper count = new Helper();
	LocalEnvironment env;
	private PrintStream impDeg;
	private PrintStream clusterColoring;
	private PrintStream vetClusterMap;
	private String pathToText;
	private BufferedReader text;
	private PrintStream feq;
	private PrintStream rank;
	private PrintStream similarities;
	boolean training = false;
	private BufferedReader br;
	private String[] sentence;

	private BufferedReader bufferedReader;

	/**
	 * @throws Exception
	 */
	public Processor() throws Exception {
		this.initWindow();
	}

	/**
	 * creates the window and calls the methods corresponding to the actions to
	 * be performed when the buttons are clicked
	 * 
	 * @throws Exception
	 */
	protected void initWindow() throws Exception {
		int distance = 5;
		float ranking;
		float tranking;
		if (training == true) {
			String pathToTrainingText1 = "resources\\DigitalP\\digitalPlabled.txt";
			String PathToNodesMap1 = "resources\\DigitalP\\ClusterMap.txt";
			String pathToTrainingText2 = "resources\\PsyOfDreams\\PsyDreamsLabled.txt";
			String PathToNodesMap2 = "resources\\PsyOfDreams\\ClusterMap.txt";
			String pathToTrainingText3 = "resources\\Dreams\\DreamPsyLab.txt";
			String PathToNodesMap3 = "resources\\Dreams\\ClusterMap.txt";
			String pathToTrainingText4 = "resources\\PsyOfSinging\\PsyLabeld.txt";
			String PathToNodesMap4 = "resources\\PsyOfSinging\\ClusterMap.txt";
			String pathToTrainingText5 = "resources\\PSEthics\\PSethicsLabeled.txt";
			String PathToNodesMap5 = "resources\\PSEthics\\ClusterMap.txt";
			// String pathToImpgraphml1 = "resources\\f.graphml";
			String pathTographml = "resources\\f.graphml";
			// String pathToText = "resources\\PSEthics\\PSEthics.txt";
			// String pathToTermFreqFile = "resources\\PSEthics\\TermFreq.txt";
			// String pathToImpDegFile =
			// "resources\\PSEthics\\ImplicationsDegrees.txt";

			String pathToTrainingText6 = "resources\\JayDickText\\JayDickLabled.txt";
			String PathToNodesMap6 = "resources\\JayDickText\\ClusterMap.txt";
			String pathToTrainingText7 = "resources\\ResonnanceinSingingandSpeaking\\ResLabeled.txt";
			String PathToNodesMap7 = "resources\\ResonnanceinSingingandSpeaking\\ClusterMap.txt";
			String pathToTrainingText8 = "resources\\PSGuide\\PSGuideLabeled.txt";
			String PathToNodesMap8 = "resources\\PSGuide\\ClusterMap.txt";
			// String pathToImpgraphml2 =
			// "resources\\PSGuide\\ImpPSGuide.graphml";
			// String pathToImpgraphml2 = "resources\\f.graphml";
			// String PathToNodesMap2 = "resources\\f.txt";
			// String pathToText2 = "resources\\PSGuide\\PSGuide.txt";
			// String pathToTermFreqFile2 = "resources\\PSGuide\\TermFreq.txt";
			// String pathToImpDegFile2
			// ="resources\\PSGuide\\ImplicationsDegrees.txt";
			// String pathToRankFile
			// "resources\\ImpGraphSimilarity-Res-PSG.txt";

			// termFrequency(pathToText, pathToTermFreqFile);
			// termFrequency(pathToText2, pathToTermFreqFile2);
			// trainingGraph(pathToTrainingText1, pathTographml,
			// PathToNodesMap1);
			// trainingGraph(pathToTrainingText2, pathTographml,
			// PathToNodesMap2);
			trainingGraph(pathToTrainingText3, pathTographml, PathToNodesMap3);
			trainingGraph(pathToTrainingText4, pathTographml, PathToNodesMap4);
			trainingGraph(pathToTrainingText5, pathTographml, PathToNodesMap5);
			trainingGraph(pathToTrainingText6, pathTographml, PathToNodesMap6);
			trainingGraph(pathToTrainingText7, pathTographml, PathToNodesMap7);
			trainingGraph(pathToTrainingText8, pathTographml, PathToNodesMap8);
			// impDegrees(pathToImpDegFile, graph1);

			// impDegrees(pathToImpDegFile2, graph2);
			// List<Vertex<String, Long>> tlistOfVertices =
			// graph1.getVertices().collect();
			// List<Vertex<String, Long>> tlistOfbVertices =
			// graph2.getVertices().collect();
			/*
			 * ImplicationGraph impGr1 = new ImplicationGraph(); Graph<String,
			 * Long, String> impgraph1 =
			 * impGr1.implicationGraph(pathToTrainingText1,pathToImpgraphml1,
			 * distance); ImplicationGraph impGr2 = new ImplicationGraph();
			 * Graph<String, Long, String> impgraph2 =
			 * impGr2.implicationGraph(pathToTrainingText2, pathToImpgraphml2,
			 * distance); List<Vertex<String, Long>> listOfVertices =
			 * impgraph1.getVertices().collect(); List<Vertex<String, Long>>
			 * listOfbVertices = impgraph2.getVertices().collect();
			 * 
			 * SimilarityMeasure sim = new SimilarityMeasure(); ranking =
			 * sim.computeSimRank(impgraph1, impgraph2, listOfVertices,
			 * listOfbVertices); // tranking = sim.computeSimRank(graph1,
			 * graph2, tlistOfVertices, tlistOfbVertices); rank = new
			 * PrintStream(new FileOutputStream(pathToRankFile));
			 * rank.println(("the implication graph similarity between " +
			 * pathToTrainingText1 + " and " + pathToTrainingText2 +
			 * "distance  " + (distance - 1) + " is \n" + ranking)); //
			 * rank.println(("the training graph similarity between " +
			 * pathToTrainingText1 + " and " + pathToTrainingText2 // +
			 * "distance  " + (distance - 1) + " is \n" + tranking));
			 */

		} else {
			execution();
			// Testing graph
			String pathToTestgraphml = "resources\\Testing\\JayDickTest.graphml";
			String pathToTrainingText = "resources\\JayDickText\\JayDickLabledT.txt";
			String pathToTestingSentence = "resources\\JayDickText\\testingSentence.txt";
			// String pathToTestingText = "resources\\DigitalP\\digitalP.txt";
			Helper help = new Helper();
			GraphCreation gcreate = new GraphCreation();

			List<Tuple2<List<String>, List<String>>> sentImpMapping = new ArrayList<Tuple2<List<String>, List<String>>>();

			// create the training graph
			train = new BufferedReader(new FileReader(pathToTrainingText));
			String readString = null;
			while ((readString = train.readLine()) != null) {
				help.setSentences(help.performAnnotation(readString));
			}

			for (int sent = 0; sent < help.getSentences().size() - 1; sent += 2) {
				CoreMap currentsentence = help.getSentences().get(sent);
				List<String> vertexList = new ArrayList<String>();
				vertexList = help.parseISentence(currentsentence);

				// sentence = (String[]) vertexList.toArray();
				help.getNodesList().addAll(vertexList);
				help.followEdges(help.getEdges(), help.getListOfvertices());
				help.childEdges(help.getEdges(), help.semanticGraph(currentsentence));
				List<String> impl = help.parseImplication(help.getSentences().get(sent + 1));
				if (impl != null) {
					help.addImplicationEdges(vertexList, help.parseImplication(help.getSentences().get(sent + 1)));
					// fill the list of tuples sent, imp
					Tuple2<List<String>, List<String>> sentTup = new Tuple2<List<String>, List<String>>(vertexList,
							impl);
					sentImpMapping.add(sentTup);
				}

				// store the sentence and its implications in a CSV TextFile

				sentImpMapping.forEach(System.out::println);
				DataSet<Tuple2<List<String>, List<String>>> mapping = env.fromCollection(sentImpMapping);
				mapping.print();
				mapping.getType();
				mapping.writeAsCsv("resources\\mappings\\sentImpMap.csv");
				// mapping.writeAsText("resources\\mappings\\sentImplicationMap.txt");
			}
		//	ExecutionEnvironment envi = ExecutionEnvironment.getExecutionEnvironment();
		//	DataSet<Tuple2<List<String>, List<String>>> map = envi
		//			.readCsvFile("resources\\mappings\\sentImplicationMap.csv").types(String.class, String.class);
		//	map.
		//	System.out.println(" reading from Dataset  \n");
		//	map.print();
			// initial graph = graph before clustering
			// gcreate.setTrainingGraph(gcreate.initialGraph(help.getEdges()));
			// create the testing graph
			/*
			 * BufferedReader br = new BufferedReader(new
			 * FileReader(pathToTestingText)); String read = null; while ((read
			 * = br.readLine()) != null) {
			 * help.setTsentences(help.performAnnotation(read)); } // add follow
			 * and child edges for (int sent = 0; sent <
			 * help.getTsentences().size(); sent += 1) { List<String> vertexList
			 * = new ArrayList<String>(); vertexList =
			 * help.parseISentence(help.getTsentences().get(sent)); //
			 * logger.info(" the new list is " + vertexList);
			 * gcreate.getTestingList().addAll(vertexList); // //
			 * parseISentence(getSentences().get(sent));
			 * help.followEdges(gcreate.getTedges(), help.getListOfvertices());
			 * help.childEdges(gcreate.getTedges(),
			 * help.semanticGraph(help.getTsentences().get(sent))); } // call
			 * testing graph method Graph<String, Long, String> graph =
			 * gcreate.testingGraph(gcreate.getTedges());
			 * System.out.println(" testing edges are ");
			 * graph.getEdges().print();
			 * gcreate.visualizeGraph(pathToTestgraphml, help.getNodesList(),
			 * gcreate.getEdgelist(), gcreate.getTestingList(),
			 * gcreate.getTedgelist());
			 */
			// }

		}
	}

	public void execution() {
		Configuration conf = new Configuration();
		conf.setFloat(ConfigConstants.TASK_MANAGER_MEMORY_FRACTION_KEY, 0.3f);
		env = LocalEnvironment.createLocalEnvironment(conf);
	}

	public void termFrequency(String pathToText, String pathToTermFreqFile) throws FileNotFoundException {
		WordCount frequency = new WordCount();
		Map<String, Integer> wordCount = new HashMap<String, Integer>();
		wordCount = frequency.termFrequency(pathToText);
		Map<String, Integer> orderFrequencies = new LinkedHashMap<>();
		// sort by key, a,b,c..., and put it into the "result" map
		wordCount.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.forEachOrdered(x -> orderFrequencies.put(x.getKey(), x.getValue()));
		feq = new PrintStream(new FileOutputStream(pathToTermFreqFile));
		feq.println(("term  ,  frequency"));
		orderFrequencies.forEach((k, v) -> feq.println((k + " , " + v)));
	}

	public Graph<String, Long, String> trainingGraph(String pathToTrainingText, String pathTographm1l,
			String PathToNodesMap) throws Exception {
		Helper help = new Helper();
		GraphCreation gcreate = new GraphCreation();

		String readString = null;
		bufferedReader = new BufferedReader(new FileReader(pathToTrainingText));
		while ((readString = bufferedReader.readLine()) != null) {
			help.setSentences(help.performAnnotation(readString));
		}

		for (int sent = 0; sent < help.getSentences().size() - 1; sent += 2) {

			List<String> vertexList = new ArrayList<String>();
			vertexList = help.parseISentence(help.getSentences().get(sent));
			help.getNodesList().addAll(vertexList);
			help.followEdges(help.getEdges(), help.getListOfvertices());
			help.childEdges(help.getEdges(), help.semanticGraph(help.getSentences().get(sent)));
			if (help.parseImplication(help.getSentences().get(sent + 1)) != null) {
				help.addImplicationEdges(vertexList, help.parseImplication(help.getSentences().get(sent + 1)));

			}
		}
		gcreate.setTrainingGraph(gcreate.initialGraph(help.getEdges()));
		Graph<String, Long, String> graph = gcreate.getTrainingGraph();
		gcreate.clustering(graph.getUndirected(), PathToNodesMap);
		gcreate.visualizeGraph(pathTographm1l, help.getNodesList(), gcreate.getEdgelist(), null, null);
		return graph;
	}

	public void impDegrees(String pathToImpDegFile, Graph<String, Long, String> graph) throws Exception {
		Map<String, Integer> impDegree = new HashMap<String, Integer>();
		Graph<String, Long, String> impGraph = graph.filterOnEdges((edge) -> edge.getValue().equals("i"));
		List<Edge<String, String>> impEdges = impGraph.getEdges().collect();

		for (Edge<String, String> e : impEdges) {

			if (impDegree.get(e.getTarget()) != null)
				impDegree.put(e.getTarget(), impDegree.get(e.getTarget()) + 1);
			else
				impDegree.put(e.getTarget(), 1);
		}
		// order the list according to the node degree

		Map<String, Integer> orderImpByDegree = new LinkedHashMap<>();

		// sort by key, a,b,c..., and put it into the "result" map
		impDegree.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.forEachOrdered(x -> orderImpByDegree.put(x.getKey(), x.getValue()));

		impDeg = new PrintStream(new FileOutputStream(pathToImpDegFile));
		impDeg.println(("implication  ,  degree "));
		orderImpByDegree.forEach((k, v) -> impDeg.println((k + " , " + v)));
	}

}
