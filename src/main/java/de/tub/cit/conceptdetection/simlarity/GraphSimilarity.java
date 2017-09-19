package tu.master.ConceptDetection;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
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
import org.apache.flink.types.NullValue;

public class SimilarityMeasure {

	public List<Tuple2<String, List<String>>> NeighborsFinder(Graph<String, Long, String> impgraph1,
			List<Vertex<String, Long>> listOfVertices) throws Exception {

		List<Tuple2<String, List<String>>> listOfAllNeighbors = new ArrayList<Tuple2<String, List<String>>>();
		List<Edge<String, String>> listOfEdges = impgraph1.getEdges().collect();
		for (Vertex<String, Long> v : listOfVertices) {
			List<String> neighborList = new ArrayList<String>();

			for (Edge<String, String> edge : listOfEdges) {
				if (v.f0.equals(edge.f1)) {
					neighborList.add(edge.f0);
				} else if (v.f0.equals(edge.f0)) {
					neighborList.add(edge.f1);

				}

			}
			Tuple2<String, List<String>> nTupl = new Tuple2<String, List<String>>(v.f0, neighborList);
			listOfAllNeighbors.add(nTupl);
		}
		return listOfAllNeighbors;
	}

	public float computeSimRank(Graph<String, Long, String> impgraph1,

			Graph<String, Long, String> impgraph2, List<Vertex<String, Long>> listOfVertices,
			List<Vertex<String, Long>> listOfbVertices) throws Exception {
		List<Tuple2<String, List<String>>> listofNeighborsA = NeighborsFinder(impgraph1, listOfVertices);
		//listofNeighborsA.forEach(System.out::println);
		List<Tuple2<String, List<String>>> listofNeighborsB = NeighborsFinder(impgraph2, listOfbVertices);
		//listofNeighborsB.forEach(System.out::println);

		
		float sumR = 0f;
		// float initialRank = 0f;

		List<String> commonVertices = new ArrayList<String>();
		for (Vertex<String, Long> a : listOfVertices) {
			for (Vertex<String, Long> b : listOfbVertices) {
				//Tuple2<String, String> ab = new Tuple2<String, String>(a.f0, b.f0);
				if (a.equals(b)) {
					commonVertices.add(a.f0);
					sumR += 1;
				}
			}
		}

		float initialRank = sumR / listOfVertices.size();
		System.out.println(" initial simRank is  " + initialRank);

		float finalRank = 0f;
			
			
			float verticesrank = 0f;
			float iterationrank = 0f;
			
			for (String c : commonVertices) {
				// rank of 1 common vertex
				float rank = 0f;
				float sum = 0f;
				List<String> commonNeighbors = new ArrayList<String>();
				// ia is the list of the neighbors of the node c in the first graph
				List<String> ia = listofNeighborsA.stream().filter(nodeDeg -> nodeDeg.f0.equals(c)).findAny()
						.orElse(null).f1;
				// ib is the list of the neighbors of the node c in the second graph
				List<String> ib = listofNeighborsB.stream().filter(nodeDeg -> nodeDeg.f0.equals(c)).findAny()
						.orElse(null).f1;

				for (String iia : ia) {
					for (String iib : ib) {
						if (iia.equals(iib)) {
							commonNeighbors.add(iia);
							sum += 1;
						}
					}
				}
				if (ia.size() != 0 && ib.size()!=0)
					rank = sum / Math.min(ia.size(),ib.size());
				else if (ia.size() == 0 && ib.size()==0)
					    rank = 1;
					else 
						rank =0;
				
				verticesrank = verticesrank + rank;

			}
			if(commonVertices.size()!=0){
			iterationrank =  verticesrank/commonVertices.size();
			}
			else 
				iterationrank = 0f;
			System.out.println(" rank of the first iteration is  " + iterationrank + " \n ");
			
			finalRank = (initialRank + iterationrank)/2;
			System.out.println(" the  final rank is  " + finalRank + " \n ");
			
			return finalRank;
	}

}