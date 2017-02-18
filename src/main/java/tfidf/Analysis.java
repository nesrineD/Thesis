package tfidf;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple6;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

public class Analysis {
	
	public Analysis()
			throws Exception {
		extract();
	}
	
	// TODO Auto-generated constructor stub
	public final static String[] STOP_WORDS = { "the", "i", "a", "an", "at", "are", "am", "for", "and", "or", "is",
			"there", "it", "this", "that", "on", "was", "by", "of", "to", "in", "to", "message", "not", "be", "with",
			"you", "have", "as", "can" };
	
	public void extract()
			throws Exception {
		// get an ExecutionEnvironment
		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
		
		
		// read sender and body fields
		DataSet<Tuple2<String, String>> text = env.readCsvFile("resources\\tfidfDocs.csv")
				.lineDelimiter("//").fieldDelimiter(";").types(String.class, String.class);
		// text.print();
		//DataSet<String> text = env.readTextFile("file:///path/to/file");
		
		long nbrDoc = text.count();
		// compute for each word the number mails it is contained in (DF)
		DataSet<Tuple2<String, Integer>> docFrequency = text
		// extract unique words from mails
				.flatMap(new UniqueWordExtractor(STOP_WORDS)).groupBy(0).sum(1);
		
		// compute the frequency of words within each mail (TF)
		DataSet<Tuple3<String, String, Integer>> termFrequency = text.flatMap(new TFComputer(STOP_WORDS));
		
		// compute the TF-IDF score for each word-mail pair
		DataSet<Tuple3<String, String, Double>> tfIdf = docFrequency
		// join TF and DF on word fields
				.join(termFrequency).where(0).equalTo(1)
				// compute TF-IDF
				.with(new TfIdfComputer(nbrDoc));
		
		// print result
		tfIdf.print();
		
	}
	
	/**
	 * Extracts the all unique words from the mail body. Words consist only of
	 * alphabetical characters. Frequent words (stop words) are filtered out.
	 */
	public static class UniqueWordExtractor
			extends RichFlatMapFunction<Tuple2<String, String>, Tuple2<String, Integer>> {
		
		// set of stop words
		private Set<String> stopWords;
		// set of emitted words
		private transient Set<String> emittedWords;
		// pattern to match against words
		private transient Pattern wordPattern;
		
		public UniqueWordExtractor() {
			this.stopWords = new HashSet<>();
		}
		
		public UniqueWordExtractor(String[] stopWords) {
			// setup stop words set
			this.stopWords = new HashSet<>();
			Collections.addAll(this.stopWords, stopWords);
		}
		
		@Override
		public void open(Configuration config) {
			// init set and word pattern
			this.emittedWords = new HashSet<>();
			this.wordPattern = Pattern.compile("(\\p{Alpha})+");
		}
		
		@Override
		public void flatMap(Tuple2<String, String> mail, Collector<Tuple2<String, Integer>> out)
				throws Exception {
			
			// clear set of emitted words
			this.emittedWords.clear();
			// split body along whitespaces into tokens
			StringTokenizer st = new StringTokenizer(mail.f1);
			
			// for each word candidate
			while (st.hasMoreTokens()) {
				// normalize to lower case
				String word = st.nextToken().toLowerCase();
				Matcher m = this.wordPattern.matcher(word);
				if (m.matches() && !this.stopWords.contains(word) && !this.emittedWords.contains(word)) {
					// candidate matches word pattern, is not a stop word, and
					// was not emitted before
					out.collect(new Tuple2<>(word, 1));
					this.emittedWords.add(word);
				}
			}
		}
	}
	
	/**
	 * Computes the frequency of words in a mails body. Words consist only of
	 * alphabetical characters. Frequent words (stop words) are filtered out.
	 */
	public static class TFComputer
			extends RichFlatMapFunction<Tuple2<String, String>, Tuple3<String, String, Integer>> {
		
		// set of stop words
		private Set<String> stopWords;
		// map to count the frequency of words
		private transient Map<String, Integer> wordCounts;
		// pattern to match against words
		private transient Pattern wordPattern;
		
		public TFComputer() {
			this.stopWords = new HashSet<>();
		}
		
		public TFComputer(String[] stopWords) {
			// initialize stop words
			this.stopWords = new HashSet<>();
			Collections.addAll(this.stopWords, stopWords);
		}
		
		@Override
		public void open(Configuration config) {
			// initialized map and pattern
			this.wordPattern = Pattern.compile("(\\p{Alpha})+");
			this.wordCounts = new HashMap<>();
		}
		
		@Override
		public void flatMap(Tuple2<String, String> mail, Collector<Tuple3<String, String, Integer>> out)
				throws Exception {
			
			// clear count map
			this.wordCounts.clear();
			
			// tokenize mail body along whitespaces
			StringTokenizer st = new StringTokenizer(mail.f1);
			// for each candidate word
			while (st.hasMoreTokens()) {
				// normalize to lower case
				String word = st.nextToken().toLowerCase();
				Matcher m = this.wordPattern.matcher(word);
				if (m.matches() && !this.stopWords.contains(word)) {
					// word matches pattern and is not a stop word -> increase
					// word count
					int count = 0;
					if (wordCounts.containsKey(word)) {
						count = wordCounts.get(word);
					}
					wordCounts.put(word, count + 1);
				}
			}
			
			// emit all counted words
			for (String word : this.wordCounts.keySet()) {
				out.collect(new Tuple3<>(mail.f0, word, this.wordCounts.get(word)));
			}
		}
	}
	
	/**
	 * Compute the TF-IDF score for a word in a mail by combining TF, DF, and
	 * total mail count.
	 */
	public static class TfIdfComputer
			implements
			JoinFunction<Tuple2<String, Integer>, Tuple3<String, String, Integer>, Tuple3<String, String, Double>> {
		
		private double mailCount;
		
		public TfIdfComputer() {}
		
		public TfIdfComputer(long mailCount) {
			this.mailCount = (double) mailCount;
		}
		
		@Override
		public Tuple3<String, String, Double> join(Tuple2<String, Integer> docFreq,
				Tuple3<String, String, Integer> termFreq)
				throws Exception {
			// compute TF-IDF
			return new Tuple3<>(termFreq.f0, // messageID
					termFreq.f1, // word
					termFreq.f2 * (mailCount / docFreq.f1) // TF-IDF
			);
		}
	}
	
}
