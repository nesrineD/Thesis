package tu.master.ConceptDetection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.Vertex;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

public class SimilarityMeasure {
    private static ILexicalDatabase db = new NictWordNet();

    private Optional<String> findAny;

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

    public double computeSimRank(Graph<String, Long, String> impgraph1,

	    Graph<String, Long, String> impgraph2, List<Vertex<String, Long>> listOfVertices,
	    List<Vertex<String, Long>> listOfbVertices) throws Exception {
	//WS4JConfiguration.getInstance().setMFS(false);

	List<Tuple2<String, List<String>>> listofNeighborsA = NeighborsFinder(impgraph1, listOfVertices);
	// listofNeighborsA.forEach(System.out::println);
	List<Tuple2<String, List<String>>> listofNeighborsB = NeighborsFinder(impgraph2, listOfbVertices);
	// listofNeighborsB.forEach(System.out::println);
	/*System.out.println(" list of vertices of first graph \n");
	listOfVertices.forEach(System.out::println);
	System.out.println(" list of vertices of first graph \n");
	listOfbVertices.forEach(System.out::println);*/
	/*long sumR = 0f;
	// long initialRank = 0f;

	
	for (Vertex<String, Long> a : listOfVertices) {
	    for (Vertex<String, Long> b : listOfbVertices) {
		// Tuple2<String, String> ab = new Tuple2<String, String>(a.f0,
		// b.f0);
		if (a.equals(b)) {
		    commonVertices.add(a.f0);
		    sumR += 1;
		}
	    }
	}*/
	List<String> similar = new ArrayList<String>();
	/*List<Vertex<String, Long>> vertices =*/
	List<String> aVertices = listOfVertices.stream().map(x ->x.f0).collect(Collectors.toList());
	List<String> bVertices = listOfbVertices.stream().map(x ->x.f0).collect(Collectors.toList());
	List<String> similarA =aVertices.stream().filter(x-> bVertices.stream().anyMatch(y->isSimilar(x,y))).collect(Collectors.toList());
	List<String> similarB =bVertices.stream().filter(x-> aVertices.stream().anyMatch(y->isSimilar(x,y))).collect(Collectors.toList());
	similar.addAll(similarA);
	similar.addAll(similarB);
	List<String> similar1 =  similar.stream().distinct().collect(Collectors.toList());
	/*for(Vertex<String, Long> v:vertices){
	    commonVertices.add(v.f0);
	}*/
	//System.out.println(" similar vertices are  \n ");
	//similar.forEach(System.out::println);
	double sum = similar1.stream().count();
	double initialRank = 2 * sum / (listOfVertices.size() + listOfbVertices.size());
	
	System.out.println(" initialRank  is  " +  initialRank  + "sum is " +sum +" \n ");
	double finalRank = 0;

	double verticesrank = 0;
	double iterationrank = 0;

	for (String c : similar1) {
	    // rank of 1 common vertex
	    double rank = 0;
	   // long sum = 0f;
	   // List<String> commonNeighbors = new ArrayList<String>();
	    
	    List<String> ia = new ArrayList<String>();
	    List<String> ib = new ArrayList<String>();
	    List<String> similar2 = new ArrayList<String>();
	 
	   // ia is the list of the neighbors of the node c in the first graph
	    Optional<Tuple2<String, List<String>>> nA = listofNeighborsA.stream()
		    .filter(nodeDeg -> nodeDeg.f0.equals(c)).findAny();
	    if (nA.isPresent()) {
		ia.addAll(nA.get().f1);
	    }
	    
	    Optional<Tuple2<String, List<String>>> nB = listofNeighborsB.stream()
		    .filter(nodeDeg -> nodeDeg.f0.equals(c)).findAny();
	    if (nB.isPresent()) {
		ib.addAll(nB.get().f1);
	    }
           /*
	    System.out.println(" vertex is \n " + c + " neighbors tuples of first graph \n");
	    listofNeighborsA.forEach(System.out::println);
	    System.out.println(" vertex is \n " + c + " neighbors tuples of second graph \n");
	    listofNeighborsB.forEach(System.out::println);
	 */
	 
	    if (!ia.isEmpty() && !ib.isEmpty()){	
		List<String> similar2A =ia.stream().filter(x-> ib.stream().anyMatch(y->isSimilar(x,y))).collect(Collectors.toList());
		List<String> similar2B =ib.stream().filter(x-> ia.stream().anyMatch(y->isSimilar(x,y))).collect(Collectors.toList());
		similar2.addAll(similar2A);
		similar2.addAll(similar2B);
		List<String> sim2 = similar2.stream().distinct().collect(Collectors.toList());
		
		//double som = ia.stream().filter(x-> ib.contains(x)).count();
		//  System.out.println(" nbr of matched neighbors  is \n " + som + "sum is "+ sum +"size ia " +  ia.size() +"size ib " +  ib.size());  
		rank = 2 * sim2.size() / (ia.size() + ib.size());
		
	    }else if (ia.isEmpty() && ib.isEmpty()) rank = 1;
	     
	    else
		rank = 0;

	    verticesrank = verticesrank + rank;
	  //  System.out.println(" Rank  is  " + rank + " \n ");
	    /* Optional<Tuple2<String, List<String>>> nB = listofNeighborsB.stream()
	    .filter(nodeDeg -> nodeDeg.f0.equals(c)).findAny();
    if (nB.isPresent()) {
	ib = nB.get().f1;
    }*/
  /*  List<String> ia = listofNeighborsA.stream()
	    .filter(nodeDeg -> nodeDeg.f0.equals(c)).findAny().get().f1;
    List<String> ib = listofNeighborsB.stream()
	    .filter(nodeDeg -> nodeDeg.f0.equals(c)).findAny().get().f1;*/
    /*System.out.println(" vertex is \n " + c + " neighbors in first graph \n");
    ia.forEach(System.out::println);
    System.out.println(" vertex is \n " + c + " neighbors in second graph \n");
    ib.forEach(System.out::println);*/
		

	}
	if (similar1.size() != 0) {
	    iterationrank = verticesrank / similar1.size();
	} else
	    iterationrank = 0;
	System.out.println(" rank of the  iteration is  " + iterationrank + " \n ");

	finalRank = (initialRank + iterationrank) / 2;
	System.out.println(" the  final rank is  " + finalRank + " \n ");

	return finalRank;
    }
    private boolean isSimilar(final String word1 , final String word2){
	    RelatednessCalculator rc = new WuPalmer(db);
	    return  rc.calcRelatednessOfWords(word1, word2) > 0.75 ;
	}

}
