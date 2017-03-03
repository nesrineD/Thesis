package tu.master.ConceptDetection;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.flink.api.java.LocalEnvironment;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import tu.master.utils.Stemmer;
import tu.master.utils.StopWords;

public class Helper {
	
	final static Logger logger = Logger.getLogger(Helper.class);
	
	Stemmer s = new Stemmer();
	// pattern to match against words
	private transient Pattern wordPattern;
	
	
	
	
	List<Tuple3<String, String, String>> edges = new ArrayList<Tuple3<String, String, String>>();
	
	public List<Tuple3<String, String, String>> getEdges() {
		return edges;
	}
	
	public void setEdges(List<Tuple3<String, String, String>> edges) {
		this.edges = edges;
	}
	
	List<Tuple3<String, String, String>> tedges = new ArrayList<Tuple3<String, String, String>>();
	static Set<String> impSet = new HashSet<String>();
	StopWords stop = new StopWords();
	List<CoreMap> uSent = new ArrayList<CoreMap>();
	List<Edge<String, String>> edgelist = new ArrayList<Edge<String, String>>();
	List<Edge<String, String>> tedgelist = new ArrayList<Edge<String, String>>();
	CoreMap parsed = null;
	LocalEnvironment env ;
	private List<CoreMap> sentences = new ArrayList<CoreMap>();
	private List<CoreMap> Tsentences = new ArrayList<CoreMap>();
	public void execution(){
		Configuration conf = new Configuration();
		conf.setFloat(ConfigConstants.TASK_MANAGER_MEMORY_FRACTION_KEY, 0.3f);
		env =  LocalEnvironment.createLocalEnvironment(conf);
	}
	public List<CoreMap> getTsentences() {
		return Tsentences;
	}
	
	public void setTsentences(List<CoreMap> tsentences) {
		Tsentences = tsentences;
	}
	
	private List<String> nodesList = new ArrayList<String>();

	private List<IndexedWord> listOfvertices = new ArrayList<IndexedWord>();
	private List<IndexedWord> listOftvertices = new ArrayList<IndexedWord>();
	
	public List<IndexedWord> getListOftvertices() {
		return listOftvertices;
	}
	
	public void setListOftvertices(List<IndexedWord> listOftvertices) {
		this.listOftvertices = listOftvertices;
	}
	
	public List<IndexedWord> getListOfvertices() {
		return listOfvertices;
	}
	
	public void setListOfvertices(List<IndexedWord> listOfvertices) {
		this.listOfvertices = listOfvertices;
	}
	
	public List<String> getNodesList() {
		return nodesList;
	}
	
	public void setNodesList(List<String> nodesList) {
		this.nodesList = nodesList;
	}
	
	public List<CoreMap> getSentences() {
		return sentences;
	}
	
	public void setSentences(List<CoreMap> sentences) {
		this.sentences = sentences;
	}
	
	public static Set<String> getImpSet() {
		return impSet;
	}
	
	public void setImpSet(Set<String> impSet) {
		this.impSet = impSet;
	}
	
	public Helper() {}
	
	/**
	 * This method annotates a sentence using the nlp core framework
	 * 
	 * @param sentence : a sentence from the input file
	 * @return annotated sentence
	 */
	public List<CoreMap> performAnnotation(String sentence) {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,dcoref");
		
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		Annotation annotation = new Annotation(sentence);
		pipeline.annotate(annotation);
		return annotation.get(CoreAnnotations.SentencesAnnotation.class);
		
	}
	
	/**
	 * this method parses a sentence from the input file and returns the list of
	 * the vertices
	 * 
	 * @param sentence a sentence from the input file
	 * @return the vertex list
	 */
	public SemanticGraph semanticGraph(CoreMap sentence) {
		return sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	}
	
	public List<String> parseISentence(CoreMap sentence) {
		List<String> vertexList = new ArrayList<String>();
		stop.stopwordsSet();
	   // wordPattern = Pattern.compile("(\\p{Alpha})+");
		// dependencies =
		// sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		setListOfvertices(semanticGraph(sentence).vertexListSorted());
		for (IndexedWord v : getListOfvertices()) {
			String lemm = v.lemma().toLowerCase();
			String stem = s.stem(lemm);
			//Matcher m = wordPattern.matcher(lemm);
			if (/*m.matches()&&*/!stop.getSet().contains(lemm)) {
				vertexList.add(stem);
			}
			// getNodesList().addAll(vertexList);
			
		}
		return vertexList;
		
	}
	
	/**
	 * @param sentence
	 * @return a list containing the words of the parsed implication
	 */
	public List<String> parseImplication(CoreMap sent) {
		List<String> input = new ArrayList<String>();
		stop.stopwordsSet();
		
		for (CoreLabel token : sent.get(TokensAnnotation.class)) {
			// this is the text of the token
			// String word = token.get(TextAnnotation.class);
			String lemma = token.get(LemmaAnnotation.class);
			if (!stop.getSet().contains(s.stem(lemma))) {
				input.add(s.stem(lemma));
			}
		}
		
		return input;
	}
	
	/**
	 * @param vlist
	 * @param input impSet contains the set of implications
	 * @throws FileNotFoundException 
	 */
	public void addImplicationEdges(List<String> vlist, List<String> input) throws FileNotFoundException {
		/*
		 * if (!impl.getText().equals(null)) { parseImplication(impl.getText());
		 * }
		 */
		for (int i = 0; i < vlist.size(); i++) {
			for (int j = 0; j < input.size(); j++) {
				Tuple3<String, String, String> e = new Tuple3<String, String, String>(vlist.get(i), input.get(j), "i");
				getNodesList().add(input.get(j));
				getImpSet().add(input.get(j));
				// add the implication degree	
			getEdges().add(e);
			
				
			}
		}
}
	
	/**
	 * adds the follow edges
	 * 
	 * @param dependencies
	 */
	public void followEdges(List<Tuple3<String, String, String>> edges, List<IndexedWord> list) {
		List<IndexedWord> filteredList = new ArrayList<IndexedWord>();
		for (IndexedWord v : list) {
			String lemm = v.lemma().toLowerCase();
			if (!stop.getSet().contains(lemm)) {
				filteredList.add(v);
				
			}
			
		}
		// add the follow edges
		for (int i = 0; i < filteredList.size() - 1; i++) {
			Tuple3<String, String, String> e = new Tuple3<String, String, String>(s.stem(filteredList.get(i).lemma()
					.toLowerCase()), s.stem(filteredList.get(i + 1).lemma().toLowerCase()), "f");
			
			edges.add(e);
			
		}
	}
	
	/**
	 * add the child edges
	 * 
	 * @param edges
	 * @param sem
	 */
	public void childEdges(List<Tuple3<String, String, String>> edges, SemanticGraph sem) {
		
		List<SemanticGraphEdge> listOfEdges = sem.edgeListSorted();
		for (SemanticGraphEdge edge : listOfEdges) {
			String gov = edge.getGovernor().lemma().toLowerCase();
			String sgov = s.stem(gov);
			String dep = edge.getDependent().lemma().toLowerCase();
			String sdep = s.stem(dep);
			
			if (stop.getSet().contains(gov) || stop.getSet().contains(dep)) {
				
			}
			
			else {
				// add the word to the edge collection
				Tuple3<String, String, String> e = new Tuple3<String, String, String>(sgov, sdep, "c");
				edges.add(e);
				
			}
		}
		
	}
	
	
}
