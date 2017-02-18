package tu.master.ConceptDetection;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
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
	private BufferedReader br;
	private BufferedReader train;
	Helper count = new Helper();
	WordCount frequency = new WordCount();
	Helper help = new Helper();
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

		/*
		 * String pathToTrainingText = "resources\\Dreams\\DreamPsyLab.txt";
		 * String pathTographml = "resources\\Dreams\\Dreams.graphml"; // String
		 * String
		 * pathToTermFreqFile = "resources\\Dreams\\DreamPsyFrequency.txt";
		 * String pathToNodesDegFile =
		 * "resources\\Dreams\\DreamPsyNodesDegree.txt"; String pathToImpDegFile
		 * = "resources\\Dreams\\DreamPsyImpDegrees.txt";
		 */
		if (training == true) {
			String pathToTrainingText = "resources\\JayDickText\\JayDickLabled.txt";
			String pathTographml = "resources\\JayDickText\\JayDick.graphml";
			// String pathToTestgraphml = "resources\\onlytestsentence.graphml";
			String pathToText = "resources\\JayDickText\\JayDickText.txt";
			String pathToTermFreqFile = "resources\\JayDickText\\JayDickFrequency.txt";
			String pathToNodesDegFile = "resources\\JayDickText\\JayDickNodesDegree.txt";
			String pathToImpDegFile = "resources\\JayDickText\\JayDickImpDegrees.txt";
		/*	 String pathToTrainingText = "resources\\sentence.txt";
			 String pathTographml =  "resources\\sentencegraph.graphml";
			 String pathToTermFreqFile = "resources\\Frequency.txt"; 	*/
			// term frequency calculation
			Map<String, Integer> wordCount = new HashMap<String, Integer>();
			wordCount = frequency.termFrequency(pathToText);

			Map<String, Integer> orderFrequencies = new LinkedHashMap<>();
			// sort by key, a,b,c..., and put it into the "result" map
			wordCount.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
					.forEachOrdered(x -> orderFrequencies.put(x.getKey(), x.getValue()));

			feq = new PrintStream(new FileOutputStream(pathToTermFreqFile));
			feq.println(("term  ,  frequency"));
			orderFrequencies.forEach((k, v) -> feq.println((k + " , " + v)));

			// training graph
			br = new BufferedReader(new FileReader(pathToTrainingText));
			String readString = null;
			while ((readString = br.readLine()) != null) {
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
			help.setTrainingGraph(help.initialGraph(help.getEdges()));
			Graph<String, Long, String> graph = help.getTrainingGraph();
			help.clustering(graph.getUndirected());
			// nodes degree
			help.NodesDegrees(pathToNodesDegFile);
			
			// nodes degree
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
			// clusterColoring = new PrintStream(new
			// FileOutputStream("resources\\ClusterColoring.txt"));
			// Helper.clusterColoring().forEach((k,v)->
			// clusterColoring.println(("cluster ID " + k+ "has color " +v)));
			// vetClusterMap = new PrintStream(new
			// FileOutputStream("resources\\VertClusterMap.txt"));
			// Helper.getVertMapping().forEach((k,v)->
			// vetClusterMap.println(("cluster ID " + k+ " list of vertices "
			// +v)));

			help.visualizeGraph(pathTographml);
		} else {

			// Testing graph
			 String pathToTestgraphml = "resources\\testText41.graphml"; 
			// String pathToTestgraphml = "resources\\ethicsTesting.graphml"; 
			 String pathToTrainingText = "resources\\sentence.txt";
			// String pathToTestingText = "resources\\PSEthics\\PSEthics.txt";
			// String pathToTrainingText = "resources\\PSGuide\\PSGuideLabeled.txt";
			 String pathToTestingText = "resources\\testsentence.txt";
			
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
			help.setTrainingGraph(help.initialGraph(help.getEdges()));

			br = new BufferedReader(new FileReader(pathToTestingText));
			String read = null;
			while ((read = br.readLine()) != null) {
				help.setTsentences(help.performAnnotation(read));
			}

			for (int sent = 0; sent < help.getTsentences().size(); sent += 1) {
				List<String> vertexList = new ArrayList<String>();
				vertexList = help.parseISentence(help.getTsentences().get(sent)); //
				logger.info(" the new list is " + vertexList);
				help.getTestingList().addAll(vertexList); //
				//parseISentence(getSentences().get(sent));
				help.followEdges(help.getTedges(), help.getListOfvertices());
				help.childEdges(help.getTedges(), help.semanticGraph(help.getTsentences().get(sent)));
			}
			

			Graph<String, Long, String> graph = help.testingGraph(help.getTedges());
			System.out.println(" testing edges are ");
			graph.getEdges().print();
			help.visualizeGraph(pathToTestgraphml);

			// }

		}

	}
}

// }
