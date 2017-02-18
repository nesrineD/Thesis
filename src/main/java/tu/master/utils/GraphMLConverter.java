package tu.master.utils;

/*
 * GraphMLConverter.java
 */

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.flink.graph.Edge;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import tu.master.ConceptDetection.Helper;

/**
 * Util to convert nodes and edges into grapml document
 * 
 * @author rbolze, nesrine
 */
public class GraphMLConverter {
	
	final static Logger logger = Logger.getLogger(GraphMLConverter.class);
	
	public GraphMLConverter() {
		
	}
	
	/**
	 * method which write in the xml file the graphML structure of the list of
	 * nodes and edges
	 * 
	 * @param fileName the xml file name
	 * @param nodes the list of training nodes
	 * @param edges the list of training edges
	 * @param tnodes the list of testing nodes
	 * @param tedges the list of testing edges
	 * @throws IOException
	 */
	public void convert(String fileName, List<String> nodes, List<Edge<String, String>> edges, List<String> tnodes,
			List<Edge<String, String>> tedges)
			throws IOException {
		// graphml document header
		Element graphml = new Element("graphml", "http://graphml.graphdrawing.org/xmlns");
		Document document = new Document(graphml);
		Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		Namespace schemLocation = Namespace
				.getNamespace("schemLocation",
						"http://graphml.graphdrawing.org/xmlns \n \t http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd");
		Namespace y = Namespace.getNamespace("y", "http://www.yworks.com/xml/graphml");
		
		// add Namespace
		graphml.addNamespaceDeclaration(xsi);
		graphml.addNamespaceDeclaration(y);
		graphml.addNamespaceDeclaration(schemLocation);
		
		// keys for graphic representation
		Element key_d0 = new Element("key", graphml.getNamespace());
		key_d0.setAttribute("id", "d0");
		key_d0.setAttribute("for", "node");
		key_d0.setAttribute("yfiles.type", "nodegraphics");
		graphml.addContent(key_d0);
		Element key_d4 = new Element("key", graphml.getNamespace());
		
		key_d4.setAttribute("id", "d4");
		key_d4.setAttribute("for", "edge");
		key_d4.setAttribute("yfiles.type", "edgegraphics");
		graphml.addContent(key_d4);
		
		Element graph = new Element("graph", graphml.getNamespace());
		graph.setAttribute("id", "G");
		graph.setAttribute("edgedefault", "directed");
		graphml.addContent(graph);
		
	boolean training = true;
	if(training  == true){	
		
		for (int i = 0; i < nodes.size(); i++) {
			int id = nodes.indexOf(nodes.get(i));
			String node = nodes.get(i);
			// same is used to test if the node is an overlapping node, in this
			// case use pink as a color
			//Optional<String> same = tnodes.stream().filter(p -> p.equals(node)).findAny();
			addNode(id, node, graph, graphml);
		}
		
		for (int i = 0; i < edges.size(); i++) {
			Edge<String, String> edge = edges.get(i);
			int id = edges.indexOf(edge);
			// System.out.println(" id of the edge "+edges.get(i) + " index " +i
			// + " is " + id);
			String source = edge.getSource();
			// URL urlsrc = new URL ("http://"+source) ;
			String target = edge.getTarget();
			// URL urltarget = new URL ("http://"+target) ;
			String value = edge.getValue();
			int idSource = nodes.indexOf(source);
			int idTarget = nodes.indexOf(target);
			if (idSource < 0 || idTarget < 0) {
				System.err.println("bad edge: " + edge);
			}
			if (idSource < 0) {
				logger.error("bad source :" + source);
			}
			if (idTarget < 0) {
				logger.error("bad target :" + target);
			}
			addEdge(id, idSource, idTarget, source, target, value, graph, graphml);
			
		}
	}
	else{	
		// testing graph
				for (int i = 0; i < tnodes.size(); i++) {
					int id = tnodes.indexOf(tnodes.get(i));// + 8000;
					String tnode = tnodes.get(i);
					// add the testing node only if it doesn't already exist
					//Optional<String> same = nodes.stream().filter(p -> p.equals(tnode)).findAny();
					//if (!same.isPresent()) {
						addTNode(id, tnode, graph, graphml);
					//}
				}
				
				for (int i = 0; i < tedges.size(); i++) {
					Edge<String, String> tedge = tedges.get(i);
					int id = tedges.indexOf(tedge); //
					System.out.println(" id of the edge " + edges.get(i) + " index " + i 
							+ " is " + id);
					String source = tedge.getSource();
					// URL urlsrc = new URL ("http://"+source) ;
					String target = tedge.getTarget();
					// URL urltarget = new URL ("http://"+target) ;
					String value = tedge.getValue();
					int idSource = 0;
					
					/*if (!same.isPresent())
						idSource = tnodes.indexOf(source) + 8000;
					else*/
						idSource = tnodes.indexOf(source);
					int idTarget = 0;
					//Optional<String> tsame = nodes.stream().filter(p -> p.equals(target)).findAny();
					
						
					if (value.equals("impl")){	
						System.out.println ( "implication edge ");
						int idn = nodes.indexOf(target);
						String node = nodes.get(idn);
						System.out.println ( "implication node "+ node ); 
						Optional<String> same = tnodes.stream().filter(p -> p.equals(node)).findAny();
						// + 8000;
						
						
						System.out.println (" not present  "+!same.isPresent() );
						if (!same.isPresent()){
							
							addNode(idn, node, graph, graphml); 
							System.out.println ( "implication node "+ node +"id n "+ idn); 
							idTarget = idn;
							System.out.println ( "implication node "+ node +" doesn't exist and has for id " +idTarget); 
									
						}
						
						
						else {
						idTarget = tnodes.indexOf(node) ; 
						System.out.println ( "implication node "+ node +" already exists and has for id " +idTarget); 
						
						}
					}
					else 
						idTarget = tnodes.indexOf(target);
					if (idSource < 0 || idTarget < 0) {
						System.err.println("bad edge: " + tedge);
					}
					if (idSource < 0) {
						logger.error("bad source :" + source);
					}
					if (idTarget < 0) {
						logger.error("bad target :" + target);
					}
					addEdge(id, idSource, idTarget, source, target, value, graph, graphml);
				}
	}
		// printAll(document);
		save(fileName, document);
	}
	
	/**
	 * add a edge to the graphML document
	 * 
	 * @param id the id of the edge
	 * @param idSource the id of the node source
	 * @param idTarget the id of the node target
	 * @param source the URL of the source
	 * @param target the URL of the target
	 * @param graph the graph element of the graphML document
	 * @param graphml the graphml element of the graphML document
	 */
	public static void addEdge(int id, int idSource, int idTarget, String source, String target, String value,
			Element graph, Element graphml) {
		Element edge = new Element("edge", graphml.getNamespace());
		edge.setAttribute("id", "e" + id);
		edge.setAttribute("source", "n" + idSource);
		edge.setAttribute("target", "n" + idTarget);
		Element data4 = new Element("data", graphml.getNamespace());
		data4.setAttribute("key", "d4");
		edge.addContent(data4);
		Namespace yns = graphml.getNamespace("y");
		Element shapeNode = new Element("PolyLineEdge", yns);
		Element arrow = new Element("Arrows", yns);
		arrow.setAttribute("source", "none");
		arrow.setAttribute("target", "standard");
		shapeNode.addContent(arrow);
		data4.addContent(shapeNode);
		Element edgeLabel = new Element("EdgeLabel", yns);
		edgeLabel.setAttribute("visible", "true");
		edgeLabel.setAttribute("autoSizePolicy", "content");
		edgeLabel.setText(value);
		Element lineStyle = new Element("LineStyle", yns);
		if (value.equals("f")) {
			lineStyle.setAttribute("color", "#000000");
		} else if ((value.equals("c"))) {
			lineStyle.setAttribute("color", "#0000FF");
		} else {
			lineStyle.setAttribute("color", "#696969");
		}
		shapeNode.addContent(lineStyle);
		shapeNode.addContent(edgeLabel);
		graph.addContent(edge);
	}
	
	/**
	 * add a node to the graphML document
	 * 
	 * @param id the id of the node
	 * @param node2 the URL of the node
	 * @param graph the graph element of the graphML document
	 * @param graphml the graphml element of the graphML document
	 * @throws IOException
	 */
	public static void addNode(int id, String node2, Element graph, Element graphml)
			throws IOException {
		Element node = new Element("node", graphml.getNamespace());
		node.setAttribute("id", "n" + id);
		Element data0 = new Element("data", graphml.getNamespace());
		data0.setAttribute("key", "d0");
		node.addContent(data0);
		Namespace yns = graphml.getNamespace("y");
		Element shapeNode = new Element("ShapeNode", yns);
		data0.addContent(shapeNode);
		Element nodeLabel = new Element("NodeLabel", yns);
		nodeLabel.setAttribute("visible", "true");
		nodeLabel.setAttribute("autoSizePolicy", "content");
		nodeLabel.setText(node2);
		shapeNode.addContent(nodeLabel);
		// map clusterID, list of nodes
		Map<Long, List<String>> map = Helper.getVertMapping();
	    System.out.println(" the map size " + map.size());
		Iterator<Map.Entry<Long, List<String>>> p = map.entrySet().iterator();
		//k = clusterID
		Long k = 0L;
		while (p.hasNext()) {
			Long key = p.next().getKey();
			List<String> list = map.get(key);
			for (String value : list) {
				if (value.equals(node2)) {
					k = key;
					System.out.println(" node"+ value+" in the list and the key is"+ k + "should have color " + Helper.clusterColoring().get(k));
				}
				
			}
		}
		// code for color change for overlapping nodes (if the node is present)
		//map clusterID, color
		//Helper helper = new Helper();
		Map<Long, String> color = Helper.clusterColoring();
		// System.out.println(" the );
		// if (!same.isPresent()) {
		Element Fill = new Element("Fill", yns);
		if (k!=0L){
		Fill.setAttribute("color", Helper.clusterColoring().get(k));
		System.out.println("  key is "+ k + " color is "+ Helper.clusterColoring().get(k)+ " node " +node2 );
		shapeNode.addContent(Fill);
		}
		
	   else {
			System.out.println("  key is null  "+ k + " color is "+ Helper.clusterColoring().get(k)+ "node" +node2 );		
		}
		/*
		 * Element Fill = new Element("Fill", yns); Fill.setAttribute("color",
		 * "#FF00FF"); shapeNode.addContent(Fill); * }
		 */
		
		graph.addContent(node);
	}
	/**
	 * add a node to the graphML document
	 * 
	 * @param id the id of the node
	 * @param node2 the URL of the node
	 * @param graph the graph element of the graphML document
	 * @param graphml the graphml element of the graphML document
	 * @throws IOException
	 */
	public static void addTNode(int id, String node2, Element graph, Element graphml)
			throws IOException {
		Element node = new Element("node", graphml.getNamespace());
		node.setAttribute("id", "n" + id);
		Element data0 = new Element("data", graphml.getNamespace());
		data0.setAttribute("key", "d0");
		node.addContent(data0);
		Namespace yns = graphml.getNamespace("y");
		Element shapeNode = new Element("ShapeNode", yns);
		data0.addContent(shapeNode);
		Element nodeLabel = new Element("NodeLabel", yns);
		nodeLabel.setAttribute("visible", "true");
		nodeLabel.setAttribute("autoSizePolicy", "content");
		nodeLabel.setText(node2);
		shapeNode.addContent(nodeLabel);
		// System.out.println(" the );
		Element Fill = new Element("Fill", yns);
		Fill.setAttribute("color", "#FF0000");
		shapeNode.addContent(Fill);
		graph.addContent(node);
	}
	
	/**
	 * print the content of the document
	 * 
	 * @param doc xml document
	 */
	public static void printAll(Document doc) {
		try {
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			outputter.output(doc, System.out);
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * write the xml document into file
	 * 
	 * @param file the file name
	 * @param doc xml document
	 */
	public static void save(String file, Document doc) {
		System.out.println("### document saved in : " + file);
		try {
			XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
			sortie.output(doc, new java.io.FileOutputStream(file));
		} catch (java.io.IOException e) {}
	}
	
}
