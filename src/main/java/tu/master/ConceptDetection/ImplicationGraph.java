package tu.master.ConceptDetection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.LocalEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.Vertex;
import org.apache.flink.types.NullValue;

import edu.stanford.nlp.util.CoreMap;

public class ImplicationGraph {
	private BufferedReader bufferedReader;
	List<Tuple3<String, String, String>> implications = new ArrayList<Tuple3<String, String, String>>();

	public List<Tuple3<String, String, String>> getImplications() {
		return implications;
	}

	public void setImplications(List<Tuple3<String, String, String>> implications) {
		this.implications = implications;
	}
	LocalEnvironment env;
	List<String> impNodeList = new ArrayList<String>();
	

	public List<String> getImpNodeList() {
		return impNodeList;
	}

	public void setImpNodeList(List<String> impNodeList) {
		this.impNodeList = impNodeList;
	}

	List<Edge<String, String>> impEdgelist = new ArrayList<Edge<String, String>>();

	public List<Edge<String, String>> getImpEdgelist() {
		return impEdgelist;
	}

	public void setImpEdgelist(List<Edge<String, String>> impEdgelist) {
		this.impEdgelist = impEdgelist;
	}
	
	public void execution() {
		Configuration conf = new Configuration();
		conf.setFloat(ConfigConstants.TASK_MANAGER_MEMORY_FRACTION_KEY, 0.3f);
		env = LocalEnvironment.createLocalEnvironment(conf);
	}
	public List<Tuple3<CoreMap, Integer, List<String>>> generateImplications(String pathToTrainingText) throws IOException {

		Helper help = new Helper();
		String readString = null;
		bufferedReader = new BufferedReader(new FileReader(pathToTrainingText));
		while ((readString = bufferedReader.readLine()) != null) {
			help.setSentences(help.performAnnotation(readString));
		}

		// list of sentence, index, implication
		List<Tuple3<CoreMap, Integer, List<String>>> sentImp = new ArrayList<Tuple3<CoreMap, Integer, List<String>>>();
		for (int sent = 0; sent < help.getSentences().size() - 1; sent += 2) {
			if (help.parseImplication(help.getSentences().get(sent + 1)) != null) {
				Tuple3<CoreMap, Integer, List<String>> impTup = new Tuple3<CoreMap, Integer, List<String>>(
						help.getSentences().get(sent), sent / 2,
						help.parseImplication(help.getSentences().get(sent + 1)));
				sentImp.add(impTup);
			}
		}
		//System.out.println("sentence            ,  index   ,   Implications");
		//sentImp.forEach(System.out::println);
		// list of implications which corresponding sentences have a distance of
		// max
		//setImplications(createImplicationEdges(sentImp));
		return sentImp;
	}
	
	public List<Tuple3<String, String, String>> createImplicationEdges (List<Tuple3<CoreMap, Integer, List<String>>> sentImp, int distance ){
		List<Tuple2<List<String>, List<String>>> listOfImp = new ArrayList<Tuple2<List<String>, List<String>>>();
		for (int i = 0; i < sentImp.size(); i++) {
			for (int j = i + 1; j < sentImp.size(); j++) {
				if (Math.abs(sentImp.get(i).f1 - ( sentImp.get(j)).f1) < distance) {
					Tuple2<List<String>, List<String>> impTupl = new Tuple2<List<String>, List<String>>(
							sentImp.get(i).f2, sentImp.get(j).f2);
					listOfImp.add(impTupl);

				}

			}
		}
		//System.out.println("listOfImp 1            ,  listOfImp 2");
		//listOfImp.forEach(System.out::println);
		List<Tuple3<String, String, String>> edges = new ArrayList<Tuple3<String, String, String>>();

		// create implication graph
		for (Tuple2<List<String>, List<String>> tupl : listOfImp) {
			// list of follow edges
			for (int i = 0; i < tupl.f0.size() - 1; i++) {
				Tuple3<String, String, String> e = new Tuple3<String, String, String>(tupl.f0.get(i),
						tupl.f0.get(i + 1), "f");
				edges.add(e);
			}
			for (int j = 0; j < tupl.f1.size() - 1; j++) {
				Tuple3<String, String, String> e = new Tuple3<String, String, String>(tupl.f1.get(j),
						tupl.f1.get(j + 1), "f");
				edges.add(e);
			}
			// close implications
			/*for (int i = 0; i < tupl.f0.size() ; i++) {
				for (int j = 0; j < tupl.f1.size() ; j++) {
					
				
			Tuple3<String, String, String> nextImp = new Tuple3<String, String, String>(tupl.f0.get(i),
					tupl.f1.get(j), "f");
			edges.add(nextImp);
				}
			}*/
		}
		List<Tuple3<String, String, String>>  impEdges = edges.stream().distinct().collect(Collectors.toList());
		/*System.out.println(" list of implication edges     ");
		impEdges.forEach(System.out::println);*/
	return impEdges;
	}
	
	public Graph<String, Long, String> implGraph(String pathToTrainingText,int distance) throws Exception {

		execution();
		List<Tuple3<String, String, String>> listOfAllEdges = createImplicationEdges(generateImplications( pathToTrainingText),distance);
		DataSet<Tuple3<String, String, String>> Edges = env
				.fromCollection(listOfAllEdges);
		Graph<String, NullValue, String> graph = Graph.fromTupleDataSet(Edges, env);
		DataSet<Edge<String, String>> edgeSet = graph.getEdges();
		setImpEdgelist(edgeSet.collect());
		DataSet<Vertex<String, NullValue>> vertices = graph.getVertices();
		List<Vertex<String, NullValue>> list = vertices.collect();
		List<Vertex<String, Long>> vlist = new ArrayList<Vertex<String, Long>>();
		for (int i = 0; i < list.size(); i++) {
			String v = list.get(i).f0;
			Vertex<String, Long> vertex = new Vertex<String, Long>(v, 1L);
			vlist.add(vertex);
			getImpNodeList().add(v);
		}
		
		
		
		DataSet<Vertex<String, Long>> vertex = env.fromCollection(vlist);
		Graph<String, Long, String> fgraph = Graph.fromDataSet(vertex, edgeSet, env);
		
		
		return fgraph;
	}

	public Graph<String, Long, String> implicationGraph(String pathToTrainingText, String pathToImpgraphml, int distance) throws Exception {
		GraphCreation gcreate = new GraphCreation();
		Graph<String, Long, String> impGraph = implGraph( pathToTrainingText, distance);
		gcreate.visualizeGraph(pathToImpgraphml, getImpNodeList(), getImpEdgelist(), null, null);
		return impGraph;
	}

}
