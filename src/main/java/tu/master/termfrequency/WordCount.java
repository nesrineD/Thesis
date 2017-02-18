package tu.master.termfrequency;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tu.master.utils.Stemmer;
import tu.master.utils.StopWords;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;


public class WordCount {
	Stemmer s = new Stemmer();
	StopWords stop = new StopWords();
	// pattern to match against words
	private transient Pattern wordPattern;
	
		// map to count the frequency of words
		private transient Map<String, Integer> wordCounts=new HashMap<>();
	
	public Map<String, Integer> termFrequency(String file) throws FileNotFoundException{
		wordPattern = Pattern.compile("(\\p{Alpha})+");
		stop.stopwordsSet();
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(new FileReader(file),
	              new CoreLabelTokenFactory(), "");
	      while (ptbt.hasNext()) {
	        CoreLabel label = ptbt.next();
	        //System.out.println(label);
	       // String lemm = label.lemma().toLowerCase();
	        //System.out.println(lemm);
	        //String stem = s.stem(lemm);
	      
	        Matcher m = wordPattern.matcher(label.originalText());
			if (m.matches()&&!stop.getSet().contains(label.originalText().toLowerCase())) {
				int count = 0;
				if (wordCounts.containsKey(s.stem(label.originalText().toLowerCase()))) {
					count = wordCounts.get(s.stem(label.originalText().toLowerCase()));
				}
				wordCounts.put(s.stem(label.originalText().toLowerCase()), count + 1);
			}
	      }
		
		return wordCounts;
		
	}
	
}
