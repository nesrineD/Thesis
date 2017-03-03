package tu.master.ConceptDetection;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.EdgeDirection;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.NeighborsFunctionWithVertexValue;
import org.apache.flink.graph.ReduceNeighborsFunction;
import org.apache.flink.graph.Vertex;
import org.apache.flink.types.LongValue;

public class SimilarityMeasure {
	private PrintStream similarities;

	
	public List<Tuple2<String, LongValue>> getInDegree(Graph<String, Long, String> graph) throws Exception {
		DataSet<Tuple2<String, LongValue>> inDegrees = graph.inDegrees();
		List<Tuple2<String, LongValue>> inDegTupList = inDegrees.collect();
		return inDegTupList;
	}

	public List<Tuple2<String, LongValue>> getOutDegree(Graph<String, Long, String> graph) throws Exception {
		DataSet<Tuple2<String, LongValue>> outDegrees = graph.outDegrees();
		List<Tuple2<String, LongValue>> outDegTupList = outDegrees.collect();
		graph.numberOfVertices();
		return outDegTupList;
	}

	public void initializeSimilarity(Graph<String, Long, String> graphA, Graph<String, Long, String> graphB)
			throws Exception {

		List<Tuple2<String, LongValue>> inNodeListA = getInDegree(graphA);
		List<Tuple2<String, LongValue>> inNodeListB = getInDegree(graphB);
		List<Tuple2<String, LongValue>> outNodeListA = getOutDegree(graphA);
		List<Tuple2<String, LongValue>> outNodeListB = getOutDegree(graphB);
		Double[][] inNodeSim = new Double[(int) graphA.numberOfVertices()][(int) graphB.numberOfVertices()];
		Double[][] outNodeSim = new Double[(int) graphA.numberOfVertices()][(int) graphB.numberOfVertices()];
		Double[][] nodeSim = new Double[(int) graphA.numberOfVertices()][(int) graphB.numberOfVertices()];
		System.out.println(" inNodeListA: \n ");
		inNodeListA.forEach(System.out::println);
		System.out.println(" inNodeListB: \n ");
		inNodeListB.forEach(System.out::println);
		System.out.println(" outNodeListA: \n ");
		outNodeListA.forEach(System.out::println);
		System.out.println(" outNodeListB: \n ");
		outNodeListB.forEach(System.out::println);

		for (Tuple2<String, LongValue> tupl : inNodeListA) {
			for (Tuple2<String, LongValue> tupl1 : inNodeListB) {

				Double maxDegree = Double.valueOf(Math.max(tupl.f1.getValue(), tupl1.f1.getValue()));
				if (maxDegree != 0) {
					inNodeSim[inNodeListA.indexOf(tupl)][inNodeListB
							.indexOf(tupl1)] = ((Math.min(tupl.f1.getValue(), tupl1.f1.getValue()) / (maxDegree)));
					// inNodeSim.add(inSim);
				} else {
					inNodeSim[inNodeListA.indexOf(tupl)][inNodeListB.indexOf(tupl1)] = Double.valueOf(1);
					// inNodeSim.add(inSim);
				}
			}

		}

		for (Tuple2<String, LongValue> tupl : outNodeListA) {
			for (Tuple2<String, LongValue> tupl1 : outNodeListB) {

				Double maxDegree = Double.valueOf(Math.max(tupl.f1.getValue(), tupl1.f1.getValue()));
				if (maxDegree != 0) {
					outNodeSim[outNodeListA.indexOf(tupl)][outNodeListB
							.indexOf(tupl1)] = ((Math.min(tupl.f1.getValue(), tupl1.f1.getValue()) / (maxDegree)));
					// outNodeSim.add(outSim);
				} else {
					outNodeSim[outNodeListA.indexOf(tupl)][outNodeListB.indexOf(tupl1)] = Double.valueOf(1);
					// outNodeSim.add(outSim);
				}
			}

		}

		for (int i = 0; i < inNodeSim.length; i++) {
			for (int j = 0; j < outNodeSim.length; j++) {
				System.out.print(" in node sim " + i + " and " + j + " is  " + inNodeSim[i][j] + "  ");
				System.out.print(" out node sim  " + i + " and " + j + " is  " + outNodeSim[i][j] + "  ");
				nodeSim[i][j] = (inNodeSim[i][j] + outNodeSim[i][j]) / 2;
				System.out.print(" node sim of " + i + " and " + j + " is  " + nodeSim[i][j] + "  ");
			}
			System.out.println();
		}

	}

	public void SimRank(Graph<String, Long, String> graphA, Graph<String, Long, String> graphB, double threshold,
			int maxIter, String simrankOutputPath) throws Exception {

		long numNodesA = graphA.getVertices().count();
		long numNodesB = graphA.getVertices().count();

	}

	public Map<Tuple2<String, String>, Float> computeInitialSimRank(Graph<String, Long, String> graphA,
			Graph<String, Long, String> graphB) throws Exception {
		Map<Tuple2<String, String>, Float> R0 = new LinkedHashMap<Tuple2<String, String>, Float>();
		for (Vertex<String, Long> a : graphA.getVertices().collect()) {
			for (Vertex<String, Long> b : graphB.getVertices().collect()) {
				Tuple2<String, String> ab = new Tuple2<String, String>(a.f0, b.f0);
				if (a.equals(b)) {
					R0.put(ab, 1.0f);
				} else {
					R0.put(ab, 0.0f);
				}
			}
		}
		return R0;
	}

	/**
	 * // * Compute the SimRank for the vertices of the given graph. // * //
	 * * @param <V> // * The vertex type // * @param g // * The graph //
	 * * @return The SimRank, as a map from pairs of vertices to their
	 * similarity //
	 */
	public Map<Tuple2<String, String>, Float> computeSimRank(Graph<String, Long, String> graphA,
			Graph<String, Long, String> graphB) throws Exception {
		String path = "resources\\similarities.txt";
		final int kMax = 5;
		final float C = 0.8f;
		List<Tuple2<String, LongValue>> inNodeListA = getInDegree(graphA);
		List<Tuple2<String, LongValue>> inNodeListB = getInDegree(graphB);
		Map<Tuple2<String, String>, Float> currentR = computeInitialSimRank(graphA, graphB);
		Map<Tuple2<String, String>, Float> nextR = new LinkedHashMap<Tuple2<String, String>, Float>();
		for (int k = 0; k < kMax; k++) {
			for (Vertex<String, Long> a : graphA.getVertices().collect()) {
				for (Vertex<String, Long> b : graphB.getVertices().collect()) {
					float sum = computeSum(graphA, graphB, a.f0, b.f0, currentR);
					Tuple2<String, String> ab = new Tuple2<String, String>(a.f0, b.f0);
					Tuple2<String, LongValue> anodeDegree = inNodeListA.stream()
							.filter(nodeDeg -> nodeDeg.f0.equals(a.f0)).findAny().orElse(null);

					long degreeA = anodeDegree.f1.getValue();
					//System.out
					//		.println("node anodegree " + anodeDegree.f0 + " node a.f0  " + a.f0 + "degree " + degreeA);
					Tuple2<String, LongValue> bnodeDegree = inNodeListB.stream()
							.filter(nodeDeg -> nodeDeg.f0.equals(b.f0)).findAny().orElse(null);
					long degreeB = bnodeDegree.f1.getValue();
					//System.out.println("node   " + b.f0 + " degree " + degreeB);

					if (degreeA == 0 || degreeB == 0) {
						nextR.put(ab, 0.0f);
					} else {
						nextR.put(ab, C / (degreeA * degreeB) * sum);
					}
				}
			}
			//System.out.println(" -------- inNodeListA -------------- ");
			//inNodeListA.forEach(System.out::println);

			// }


			similarities = new PrintStream(new FileOutputStream(path));
			similarities.println("After iteration " + k);
			//nextR contains all the similarity measures
			nextR.forEach((key, v) -> similarities.println((key + " , " + v)));
		
			
			Map<Tuple2<String, String>, Float> temp = nextR;
			nextR = currentR;
			currentR = temp;
		}
		return currentR;
	}

	/**
	 * Compute the sum of all SimRank values of all incoming neighbors of the
	 * given vertices
	 *
	 * @param <V>
	 *            The vertex type
	 * @param g
	 *            The graph
	 * @param a
	 *            The first vertex
	 * @param b
	 *            The second vertex
	 * @param simRank
	 *            The current SimRank
	 * @return The sum of the SimRank values of the incoming neighbors of the
	 *         given vertices
	 * @throws Exception
	 */
	public float computeSum(Graph<String, Long, String> graphA, Graph<String, Long, String> graphB, String a, String b,
			Map<Tuple2<String, String>, Float> simRank) throws Exception {
		List<Tuple2<String, List<String>>> listofInNeighborsA = neighborsFinder(graphA);
		List<Tuple2<String, List<String>>> listofInNeighborsB = neighborsFinder(graphB);
		List<String> ia = listofInNeighborsA.stream().filter(nodeDeg -> nodeDeg.f0.equals(a)).findAny().orElse(null).f1;
		List<String> ib = listofInNeighborsB.stream().filter(nodeDeg -> nodeDeg.f0.equals(b)).findAny().orElse(null).f1;
		float sum = 0;
		for (String iia : ia) {
			for (String ijb : ib) {
				Tuple2<String, String> key = new Tuple2<String, String>(iia, ijb);
				Float r = simRank.get(key);
				sum += r;
			}
		}
		return sum;
	}

	/**
	 * Print a table with the SimRank values
	 *
	 * @param <V>
	 *            The vertex type
	 * @param g
	 *            The graph
	 * @param simRank
	 *            The SimRank
	 * @throws Exception 
	 */
	public  void print(Graph<String, Long, String> graphA, Graph<String, Long, String> graphB,Map<Tuple2<String, String>, Float> simRank) throws Exception {
		final int columnWidth = 8;
		final String format = "%4.3f";
		System.out.printf("%" + columnWidth + "s", "");
 
		
		for (Vertex<String, Long> a : graphA.getVertices().collect()) {
			String s = a.f0;
			System.out.printf("%" + columnWidth + "s", s);
			for (Vertex<String, Long> b : graphB.getVertices().collect()) {
				String u = b.f0;
				System.out.printf("%" + columnWidth + "s", u);
				Tuple2<String, String> ab = new Tuple2<String, String>(a.f0, b.f0);
				Float value = simRank.get(ab);
				String vs = String.format(Locale.ENGLISH, format, value);
				System.out.printf("%" + columnWidth + "s", vs);
			}
		}
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
	/*
	 * public void measureSimilarity(Graph<String, Long, String> graphA,
	 * Graph<String, Long, String> graphB) throws Exception { double
	 * maxDifference = 0.0; boolean terminate = false; List<Tuple2<String,
	 * LongValue>> inNodeListA = getInDegree(graphA); List<Tuple2<String,
	 * LongValue>> inNodeListB = getInDegree(graphB); List<Tuple2<String,
	 * LongValue>> outNodeListA = getOutDegree(graphA); List<Tuple2<String,
	 * LongValue>> outNodeListB = getOutDegree(graphB); Double [][] inNodeSim =
	 * new Double[ (int) graphA.numberOfVertices()][(int)
	 * graphB.numberOfVertices()]; Double [][] outNodeSim = new Double[ (int)
	 * graphA.numberOfVertices()][(int) graphB.numberOfVertices()]; Double [][]
	 * nodeSim = new Double[ (int) graphA.numberOfVertices()][(int)
	 * graphB.numberOfVertices()];
	 * 
	 * 
	 * graphA.groupReduceOnNeighbors(new NeighborsList(), EdgeDirection.IN);
	 * 
	 * 
	 * 
	 * while (!terminate) { maxDifference = 0.0; for (Tuple2<String, LongValue>
	 * tupl : inNodeListA) { for (Tuple2<String, LongValue> tupl1 : inNodeListB)
	 * { //calculate in-degree similarities double similaritySum = 0.0; double
	 * maxDegree = Double.valueOf(Math.max(tupl.f1.getValue(),
	 * tupl1.f1.getValue())); double minDegree =
	 * Double.valueOf(Math.min(tupl.f1.getValue(), tupl1.f1.getValue())); if
	 * (minDegree == Double.valueOf(tupl.f1.getValue())) { similaritySum =
	 * enumerationFunction(tupl.f0, tupl1.f0, 0); } else { similaritySum =
	 * enumerationFunction(tupl1.f0, tupl.f0, 1); } if (maxDegree == 0.0 &&
	 * similaritySum == 0.0) {
	 * inNodeSim[inNodeListA.indexOf(tupl)][inNodeListB.indexOf(tupl1)] = 1.0; }
	 * else if (maxDegree == 0.0) {
	 * inNodeSim[inNodeListA.indexOf(tupl)][inNodeListB.indexOf(tupl1)] = 0.0; }
	 * else { inNodeSim[inNodeListA.indexOf(tupl)][inNodeListB.indexOf(tupl1)] =
	 * similaritySum / maxDegree; } } } }
	 * 
	 * } public double enumerationFunction(String neighborListMin, String
	 * neighborListMax, int graph) { double similaritySum = 0.0; Map<Integer,
	 * Double> valueMap = new HashMap<Integer, Double>(); if (graph == 0) { for
	 * (int i = 0; i < neighborListMin.size(); i++) { int node =
	 * neighborListMin.get(i); double max = 0.0; int maxIndex = -1; for (int j =
	 * 0; j < neighborListMax.size(); j++) { int key = neighborListMax.get(j);
	 * if (!valueMap.containsKey(key)) { if (max < nodeSimilarity[node][key]) {
	 * max = nodeSimilarity[node][key]; maxIndex = key; } } }
	 * valueMap.put(maxIndex, max); } return graph;
	 * 
	 * }
	 */
}
