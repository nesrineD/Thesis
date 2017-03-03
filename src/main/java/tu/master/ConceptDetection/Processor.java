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
import java.util.Map;

import javax.swing.JFrame;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.GraphAnalytic;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.library.similarity.JaccardIndex;
import org.apache.flink.types.LongValue;
import org.apache.log4j.Logger;

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
	boolean training = true;

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

		if (training == true) {

			String pathToTrainingText1 = "resources\\JayDickText\\Text.txt";
			String pathTographml1 = "resources\\JayDickText\\Text.graphml";

			String pathToTrainingText2 = "resources\\DigitalP\\digitalPlabled.txt";
			String pathTographml2 = "resources\\DigitalP\\digitalP.graphml";

			Graph<String, Long, String> graph1 = trainingGraph(pathToTrainingText1, pathTographml1);
			 Graph<String, Long, String> graph2 = trainingGraph(pathToTrainingText1, pathTographml1);
			 SimilarityMeasure sim = new SimilarityMeasure();
			 sim.computeSimRank(graph1, graph2);
			
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
			gcreate.setTrainingGraph(gcreate.initialGraph(help.getEdges()));

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
		BufferedReader br = new BufferedReader(new FileReader(pathToTrainingText));
		String readString = null;
		while ((readString = br.readLine()) != null) {
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

	public List<Tuple2<String, List<String>>> neighborsFinder(Graph<String, Long, String> graph1) throws Exception {

		List<Vertex<String, Long>> listOfVertices = graph1.getVertices().collect();
		List<Tuple2<String, List<String>>> listOfAllNeighbors = new ArrayList<Tuple2<String, List<String>>>();
		List<Edge<String, String>> listOfEdges = graph1.getEdges().collect();
		for (Vertex<String, Long> v : listOfVertices) {
			List<String> neighborList = new ArrayList<String>();

			for (Edge<String, String> edge : listOfEdges) {
				if (v.f0.equals(edge.f1)) {
					neighborList.add(edge.f0);
				}
				if (v.f0.equals(edge.f0)) {
					neighborList.add(edge.f1);

				}
			}
			Tuple2<String, List<String>> nTupl = new Tuple2<String, List<String>>(v.f0, neighborList);
			listOfAllNeighbors.add(nTupl);
		}
		return listOfAllNeighbors;
	}

}

// }
