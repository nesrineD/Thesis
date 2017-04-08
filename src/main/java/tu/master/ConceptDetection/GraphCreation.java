package tu.master.ConceptDetection;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.LocalEnvironment;
import org.apache.flink.api.java.operators.JoinOperator.DefaultJoin;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.DataSetUtils;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.library.LabelPropagation;
import org.apache.flink.types.LongValue;
import org.apache.flink.types.NullValue;
import org.apache.log4j.Logger;

import edu.stanford.nlp.util.CoreMap;
import tu.master.utils.GraphMLConverter;

public class GraphCreation {

	final static Logger logger = Logger.getLogger(GraphCreation.class);
	LocalEnvironment env ;
	List<Edge<String, String>> edgelist = new ArrayList<Edge<String, String>>();
	public List<Edge<String, String>> getEdgelist() {
		return edgelist;
	}

	public void setEdgelist(List<Edge<String, String>> edgelist) {
		this.edgelist = edgelist;
	}

	List<Edge<String, String>> tedgelist = new ArrayList<Edge<String, String>>();
	public List<Edge<String, String>> getTedgelist() {
		return tedgelist;
	}

	public void setTedgelist(List<Edge<String, String>> tedgelist) {
		this.tedgelist = tedgelist;
	}

	Graph<String, Long, String> trainingGraph = null;
	
	public Graph<String, Long, String> getTrainingGraph() {
		return trainingGraph;
	}
	
	public void setTrainingGraph(Graph<String, Long, String> graph) {
		this.trainingGraph = graph;
	}
	Graph<String, Long, String> implicationGraph = null;
	public Graph<String, Long, String> getImplicationGraph() {
		return implicationGraph;
	}

	public void setImplicationGraph(Graph<String, Long, String> implicationGraph) {
		this.implicationGraph = implicationGraph;
	}

	List<Tuple3<String, String, String>> tedges = new ArrayList<Tuple3<String, String, String>>();
	public List<Tuple3<String, String, String>> getTedges() {
		return tedges;
	}
	
	public void setTedges(List<Tuple3<String, String, String>> tedges) {
		this.tedges = tedges;
	}
	
	private List<String> testingList = new ArrayList<String>();
	
	public List<String> getTestingList() {
		return testingList;
	}
	
	public void setTestingList(List<String> testingList) {
		this.testingList = testingList;
	}
	private PrintStream out;
	private PrintStream degree;
	private static PrintStream clusterMap;
	
	static Map<Long, List<String>> vertMapping = new HashMap<Long, List<String>>();
	public static Map<Long, List<String>> getVertMapping() {
		return vertMapping;
	}
	
	public void setVertMapping(Map<Long, List<String>> vertMapping) {
		GraphCreation.vertMapping = vertMapping;
	}
	
	private List<String> nodesList = new ArrayList<String>();
	public List<String> getNodesList() {
		return nodesList;
	}
	
	public void setNodesList(List<String> nodesList) {
		this.nodesList = nodesList;
	}
	
	public void execution(){
		Configuration conf = new Configuration();
		conf.setFloat(ConfigConstants.TASK_MANAGER_MEMORY_FRACTION_KEY, 0.3f);
		env =  LocalEnvironment.createLocalEnvironment(conf);
	}
	
	/**
	 * @param edges: list of edges
	 * @return the graph
	 * @throws Exception
	 */
	public Graph<String, Long, String> initialGraph(List<Tuple3<String, String, String>> edges)
			throws Exception {
	
	
		execution();
		DataSet<Tuple3<String, String, String>> Edges = env.fromCollection(edges);
		Graph<String, NullValue, String> graph = Graph.fromTupleDataSet(Edges, env);
		DataSet<Edge<String, String>> edgeSet = graph.getEdges();
		setEdgelist(edgeSet.collect());
		DataSet<Vertex<String, NullValue>> vertices = graph.getVertices();
		
		List<Vertex<String, NullValue>> list = vertices.collect();
		List<Vertex<String, Long>> vlist = new ArrayList<Vertex<String, Long>>();
		for (int i = 0; i < list.size(); i++) {
			Vertex<String, Long> v = new Vertex<String, Long>(list.get(i).f0, 1L);
			vlist.add(v);
		}
		DataSet<Vertex<String, Long>> vertex = env.fromCollection(vlist);
		Graph<String, Long, String> fgraph = Graph.fromDataSet(vertex, edgeSet, env);
		
		return fgraph;
	}
	
	
	
	/**
	 * performs clustering on the training graph
	 * 
	 * @throws Exception
	 */
	public void clustering(Graph<String, Long, String> graph)
			
			throws Exception {
		
		execution();
		
		// Initialize each vertex with a unique numeric label and run the label
		//Graph<String, Long, String> graph = initialGraph(edges).getUndirected();
		DataSet<Tuple2<String, Long>> idsWithInitialLabels = DataSetUtils.zipWithUniqueId(graph.getVertexIds()).map(
				new MapFunction<Tuple2<Long, String>, Tuple2<String, Long>>() {
					
					private static final long serialVersionUID = 1L;

					@Override
					
					public Tuple2<String, Long> map(Tuple2<Long, String> tuple2)
							throws Exception {
						return new Tuple2<String, Long>(tuple2.f1, tuple2.f0);
					}
				});
		
		logger.info(" ids with initial label" + idsWithInitialLabels.toString());
		
		DataSet<Vertex<String, Long>> verticesWithCommunity = graph.joinWithVertices(idsWithInitialLabels,
				(Long v1, Long v2) -> v2).run(new LabelPropagation<String, Long, String>(100));
		
		logger.info(" the vertices and their communities   " + verticesWithCommunity.toString());
	
		DataSet<Tuple2<String, LongValue>> deg = graph.getDegrees();
		List<Tuple2<String, LongValue>> degList = deg.collect();
		List<Tuple2<String, LongValue>> degrees = new ArrayList<Tuple2<String, LongValue>>();
		for (Tuple2<String, LongValue> item : degList) {
			Tuple2<String, LongValue> tup = new Tuple2<String, LongValue>(item.f0, item.f1);
			degrees.add(tup);
		}
		
		DataSet<Tuple2<String, LongValue>> degree = env.fromCollection(degrees);
		
		
		// store the vertices and their communities in a dataset to be able to
		// perform the join transformation
		List<Vertex<String, Long>> vList = verticesWithCommunity.collect();
		List<Tuple2<String, Long>> vertices = new ArrayList<Tuple2<String, Long>>();
		for (Vertex<String, Long> item : vList) {
			Tuple2<String, Long> tup = new Tuple2<String, Long>(item.f0, item.f1);
			vertices.add(tup);
		}
		
		DataSet<Tuple2<String, Long>> vert = env.fromCollection(vertices);
		
		// join the (vertex, degree) and the (vertex,community) datasets
		
		DefaultJoin<Tuple2<String, LongValue>, Tuple2<String, Long>> j = degree.join(vert).where(0).equalTo(0);
		List<Tuple2<Tuple2<String, LongValue>, Tuple2<String, Long>>> joinList = j.collect();
		// transform the result in a tuple list (vertex id, degree, community)
		List<Tuple3<String, LongValue, Long>> tuplList = new ArrayList<Tuple3<String, LongValue, Long>>();
		for (Tuple2<Tuple2<String, LongValue>, Tuple2<String, Long>> tpl : joinList) {
			Tuple3<String, LongValue, Long> t = new Tuple3<String, LongValue, Long>(tpl.f0.f0, tpl.f0.f1, tpl.f1.f1);
			tuplList.add(t);
		}
		// order the list according to the node degree
		Comparator<Tuple3<String, LongValue, Long>> compa = new Comparator<Tuple3<String, LongValue, Long>>() {
			
			public int compare(Tuple3<String, LongValue, Long> v1, Tuple3<String, LongValue, Long> v2) {
				// TODO Auto-generated method stub
				return v2.f1.compareTo(v1.f1);
			}
		};
		Collections.sort(tuplList, compa);
		logger.info(" the tuple list (vertex id, degree, clusterID) ordered  according to the degree");
		tuplList.forEach(logger::info);
		logger.info("\n--------------------------------------- the ordered map ---------------------------------------\n");
		Map<Long, List<String>> map2 = NodesMapping(tuplList); //
		setVertMapping(map2);
	}
	
	
	/**
	 * @param tuplList the list (vertex id, degree, clusterID) ordered according
	 *        to the degree
	 * @return a map (clusterID, vertices) where vertices are ordered according
	 *         to their degrees
	 * @throws FileNotFoundException
	 */
	private Map<Long, List<String>> NodesMapping(List<Tuple3<String, LongValue, Long>> tuplList)
			throws FileNotFoundException {
		List<Tuple2<String, Long>> verList = new ArrayList<Tuple2<String, Long>>();
		for (Tuple3<String, LongValue, Long> tpl : tuplList) {
			Tuple2<String, Long> t = new Tuple2<String, Long>(tpl.f0, tpl.f2);
			verList.add(t);
		}
		out = new PrintStream(new FileOutputStream("resources\\NodesMap.txt"));
		verList.forEach(s -> out.println(s));
		Map<Long, List<String>> map = new HashMap<Long, List<String>>();
		for (Tuple2<String, Long> item : verList) {
			
			List<String> list = map.get(item.f1);
			if (list == null) {
				list = new ArrayList<String>();
				map.put(item.f1, list);
			}
			list.add(item.f0);
		}
		return map;
		
	}
	
	/**
	 * @return a list of colors to be used for the clusterColoring
	 */
	public static List<String> colorsSet() {
		List<String> colors = new ArrayList<String>();
		// Set of stop words
		colors.add("#2F4F4F");
		colors.add("#330099");
		colors.add("#D2691E");
		colors.add("#800000");
		colors.add("#D2B48C");
		colors.add("#DEB887");
		colors.add("#708090");
		colors.add("#FF4500");
		colors.add("#3CB371");
		colors.add("#FF6347");
		colors.add("#FF66FF");
		colors.add("#FF3333");
		colors.add("#66FFCC");
		colors.add("#CCFF00");
		colors.add("#660066");
		colors.add("#0099CC");
		colors.add("#006600");
		colors.add("#333366");
		colors.add("#33CC99");
		colors.add("#660066");
		colors.add("#339999");
		colors.add("#999900");
		colors.add("#FFFF00");
		colors.add("#FFDAB9");
		colors.add("#BDB76B");
		colors.add("#DDA0DD");
		colors.add("#EE82EE");
		colors.add("#BA55D3");
		colors.add("#8A2BE2");
		colors.add("#483D8B");
		colors.add("#E0FFFF");
		colors.add("#4682B4");
		colors.add("#A52A2A");
		colors.add("#2F4F4F");
		colors.add("#330099");
		colors.add("#D2691E");
		colors.add("#800000");
		colors.add("#D2B48C");
		colors.add("#DEB887");
		colors.add("#708090");
		colors.add("#FF4500");
		colors.add("#3CB371");
		colors.add("#FF6347");
		colors.add("#FF66FF");
		colors.add("#FF3333");
		colors.add("#66FFCC");
		colors.add("#CCFF00");
		colors.add("#660066");
		colors.add("#0099CC");
		colors.add("#006600");
		colors.add("#333366");
		colors.add("#33CC99");
		colors.add("#660066");
		colors.add("#339999");
		colors.add("#999900");
		colors.add("#FFFF00");
		colors.add("#FFDAB9");
		colors.add("#BDB76B");
		colors.add("#DDA0DD");
		colors.add("#EE82EE");
		colors.add("#BA55D3");
		colors.add("#8A2BE2");
		colors.add("#483D8B");
		colors.add("#E0FFFF");
		colors.add("#4682B4");
		colors.add("#A52A2A");
		colors.add("#B0C4DE");
		colors.add("#B0C4DE");
		
		return colors;
		
	}
	
	/**
	 * @return a map containing a mapping between the cluster ID and the color
	 * @throws IOException
	 */
	public static Map<Long, String> clusterColoring()

			throws IOException {
		Map<Long, String> map = new HashMap<Long, String>();
		List<String> colors = colorsSet();
		Map <Long, List<String>> vertMappingList =  getVertMapping();
		clusterMap = new PrintStream(new FileOutputStream("resources\\ClusterMap.txt"));
		getVertMapping().forEach((k,v)-> clusterMap.println(("cluster ID " + k+ " list of vertices  " +v)));
		
		Iterator<Map.Entry<Long, List<String>>> p = vertMappingList.entrySet().iterator();
		while (p.hasNext()) {
			Long key = p.next().getKey();
			// clusterId, color
			map.put(key, colors.get(0));
			colors.remove(0);
		}
		return map;
	}

	public Graph<String, Long, String> testingGraph(List<Tuple3<String, String, String>> edges)
			throws Exception {
		
		execution();
		addImplications();
		DataSet<Tuple3<String, String, String>> Edges = env.fromCollection(edges);
		Graph<String, NullValue, String> graph = Graph.fromTupleDataSet(Edges, env);
		DataSet<Edge<String, String>> edgeSet = graph.getEdges();
		setTedgelist( edgeSet.collect());
		DataSet<Vertex<String, NullValue>> vertices = graph.getVertices();
		
		List<Vertex<String, NullValue>> list = vertices.collect();
		List<Vertex<String, Long>> vlist = new ArrayList<Vertex<String, Long>>();
		for (int i = 0; i < list.size(); i++) {
			Vertex<String, Long> v = new Vertex<String, Long>(list.get(i).f0, 1L);
			vlist.add(v);
		}
		DataSet<Vertex<String, Long>> vertex = env.fromCollection(vlist);
		Graph<String, Long, String> fgraph = Graph.fromDataSet(vertex, edgeSet, env);
		
		return fgraph;
	}
	
	/**
	 * adds the implication edges to the list of testing edges
	 * 
	 * @return list of implication edges
	 * @throws Exception
	 */
	public List<String> addImplications()
			throws Exception {
		
		HashMap <String, Integer> impDegree = new HashMap <String, Integer>();
		Graph<String, Long, String> impGraph = getTrainingGraph().filterOnEdges((Edge<String, String> edge) -> edge.getValue()
				.equals("i"));	
		List<Edge<String, String>> impEdges = impGraph.getEdges().collect();
		List<String> implications = new ArrayList<String>();
		for (int i = 0; i < testingList.size(); i++) {
			for (Edge<String, String> e : impEdges) {
				if (testingList.get(i).equals(e.getSource())) {
					Tuple3<String, String, String> tupl = new Tuple3<String, String, String>(getTestingList().get(i),
							e.getTarget(), "impl");
					implications.add(e.getTarget());
					if(impDegree.get (e.getTarget()) != null)
					   impDegree.put (e.getTarget(),impDegree.get (e.getTarget())+1);
					else
						impDegree.put (e.getTarget(),1);
					tedges.add(tupl);
					
				}
			}
		}
		
		    degree = new PrintStream(new FileOutputStream("resources\\ImpDegrees.txt"));
			impDegree.forEach((k,v)-> degree.println(("implication is " + k+ "has degree " +v)));
			 
	
		
		return implications;
		
	}
	/**
	 * creates the graphml file to be used by yED
	 * 
	 * @throws Exception
	 */
	public void visualizeGraph(String path, List<String> nodes , List<Edge<String, String>>  edges,List<String> tnodes , List<Edge<String, String>>  tedges )
			throws Exception {
		GraphMLConverter gm = new GraphMLConverter();
		Helper helper = new Helper();
		//gm.convert(path, helper.getNodesList(), edgelist , getTestingList(), tedgelist);
		gm.convert(path, nodes, edges, tnodes, tedges);
		
	}
}
