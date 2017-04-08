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
import org.apache.flink.api.java.LocalEnvironment;
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

	private PrintStream impDeg;
	private PrintStream clusterColoring;
	private PrintStream vetClusterMap;
	private String pathToText;
	private BufferedReader text;
	private PrintStream feq;
	private PrintStream rank;
	private PrintStream similarities;
	boolean training = true;
	private BufferedReader br;
	

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

		if (training == true) {

			String pathToTrainingText1 = "resources\\PsyOfSinging\\PsyLabeld.txt";
			String pathToRankFile = "resources\\DGraphSimilarity2.txt";
			String pathToImpgraphml = "resources\\PsyOfSinging\\singImp.graphml";
			//String pathToTrainingText1 = "resources\\DigitalP\\text.txt";
			
			String pathToTrainingText2 = "resources\\Dreams\\DreamPsyLab.txt";
			String pathToImpgraphml2 = "resources\\Dreams\\DreamPsyLabImp.graphml";
			//String pathTographml1 = "resources\\DigitalP\\digitalPlabled.graphml";*/
			
			//String pathToTrainingText2 = "resources\\ResonnanceinSingingandSpeaking\\ResLabeled.txt";
			/*"resources\\DigitalP\\digitalPlabled.txt"
			 * "resources\\JayDickText\\JayDickLabled.txt"
			 * "resources\\PSEthics\\PSethicsLabeled.txt"
			 * "resources\\PSGuide\\PSGuideLabeled.txt"
			 * "resources\\PsyOfDreams\\PsyDreamsLabled.txt"
			 * 
			 */
			// Graph<String, Long, String> graph1 =
			// trainingGraph(pathToTrainingText1, pathTographml1);
			
            ImplicationGraph impGr1 = new ImplicationGraph();
			Graph<String, NullValue, String> impgraph1 = impGr1.implicationGraph(pathToTrainingText1,pathToImpgraphml, distance);
			ImplicationGraph impGr2 = new ImplicationGraph();
			Graph<String, NullValue, String> impgraph2 = impGr2.implicationGraph(pathToTrainingText2,pathToImpgraphml2, distance);
			List<Vertex<String, NullValue>> listOfVertices = impgraph1.getVertices().collect();
		 	List<Vertex<String, NullValue>> listOfbVertices = impgraph2.getVertices().collect();

			SimilarityMeasure sim = new SimilarityMeasure();
			ranking = sim.computeSimRank(impgraph1, impgraph2, listOfVertices, listOfbVertices);
			rank = new PrintStream(new FileOutputStream(pathToRankFile));
			rank.println(("the similarity between "+ pathToTrainingText1 + " and "+ pathToTrainingText2 + "distance  " + (distance-1) + " is " + ranking ));
				

		} else {

			// Testing graph
			String pathToTestgraphml = "resources\\Testing\\JayDickTest.graphml";

			String pathToTrainingText = "resources\\JayDickText\\JayDickLabled.txt";
			String pathToTestingText = "resources\\DigitalP\\digitalP.txt";
			Helper help = new Helper();
			GraphCreation gcreate = new GraphCreation();
			train = new BufferedReader(new FileReader(pathToTrainingText));
			String readString = null;
			while ((readString = train.readLine()) != null) {
				help.setSentences(help.performAnnotation(readString));
			}

			for (int sent = 0; sent < help.getSentences().size() - 1; sent += 2) {
				List<String> vertexList = new ArrayList<String>();
				vertexList = help.parseISentence(help.getSentences().get(sent));
				// out = new PrintStream(new
				// FileOutputStream("resources\\vertices.txt"));
				// vertexList.forEach(s -> out.println(s));
				help.getNodesList().addAll(vertexList);
				help.followEdges(help.getEdges(), help.getListOfvertices());
				help.childEdges(help.getEdges(), help.semanticGraph(help.getSentences().get(sent)));
				if (help.parseImplication(help.getSentences().get(sent + 1)) != null) {
					help.addImplicationEdges(vertexList, help.parseImplication(help.getSentences().get(sent + 1)));
				}

			}
			// initial graph = graph before clustering
			// gcreate.setTrainingGraph(gcreate.initialGraph(help.getEdges()));

			BufferedReader br = new BufferedReader(new FileReader(pathToTestingText));
			String read = null;
			while ((read = br.readLine()) != null) {
				help.setTsentences(help.performAnnotation(read));
			}

			for (int sent = 0; sent < help.getTsentences().size(); sent += 1) {
				List<String> vertexList = new ArrayList<String>();
				vertexList = help.parseISentence(help.getTsentences().get(sent)); //
				logger.info(" the new list is " + vertexList);
				gcreate.getTestingList().addAll(vertexList); //
				// parseISentence(getSentences().get(sent));
				help.followEdges(gcreate.getTedges(), help.getListOfvertices());
				help.childEdges(gcreate.getTedges(), help.semanticGraph(help.getTsentences().get(sent)));
			}

			Graph<String, Long, String> graph = gcreate.testingGraph(gcreate.getTedges());
			System.out.println(" testing edges are ");
			graph.getEdges().print();
			gcreate.visualizeGraph(pathToTestgraphml, help.getNodesList(), gcreate.getEdgelist(),
					gcreate.getTestingList(), gcreate.getTedgelist());

			// }

		}
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

	public Graph<String, Long, String> trainingGraph(String pathToTrainingText, String pathTographm1l)
			throws Exception {
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
			// help.childEdges(help.getEdges(),
			// help.semanticGraph(help.getSentences().get(sent)));
			if (help.parseImplication(help.getSentences().get(sent + 1)) != null) {
				help.addImplicationEdges(vertexList, help.parseImplication(help.getSentences().get(sent + 1)));

			}
		}
		gcreate.setTrainingGraph(gcreate.initialGraph(help.getEdges()));
		Graph<String, Long, String> graph = gcreate.getTrainingGraph();
		gcreate.clustering(graph.getUndirected());
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

	public List<Tuple2<String, LongValue>> getInDegree(Graph<String, Long, String> graph1) throws Exception {
		DataSet<Tuple2<String, LongValue>> inDegrees = graph1.inDegrees();
		List<Tuple2<String, LongValue>> inDegTupList = inDegrees.collect();
		return inDegTupList;
	}

}

// }
