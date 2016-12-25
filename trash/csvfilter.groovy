import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import com.google.common.collect.*;

/**
 * Remove nodes responsible for 80% of the edges
 * 
 * @author sarnobat 2016-12-24
 */
public class CsvFilter {

	public static void main(String[] args) throws IOException {
		double percentageOfRelationshipsToRemove;
		if (args.length > 0) {
			percentageOfRelationshipsToRemove = Double.parseDouble(args[0]);
		} else {
			percentageOfRelationshipsToRemove = 0.8;
		}

		HashMultimap<String, String> outgoingRelationships = HashMultimap.create();
		HashMultimap<String, String> incomingRelationships = HashMultimap.create();
		_1: {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String lineOrig = null;
			while ((lineOrig = reader.readLine()) != null) {
				String line = lineOrig;
				String[] rel = line.split(",");
				String left = rel[0].replace("\"", "");
				String right = rel[1].replace("\"", "");
				outgoingRelationships.put(left, right);
				incomingRelationships.put(right, left);
				if (right.contains("debug") || left.contains("org.apache.commons.logging.Log.debug")) {
					System.err.println("CsvFilter.main() - " + left + "::" + right);
					
					System.err.println("CsvFilter.main() " + outgoingRelationships.get(left).size());
					System.err.println("CsvFilter.main() " + incomingRelationships.get(right).size());
				}
			}
		}
		Map<String, Integer> outgoingCounts = new HashMap<String, Integer>();
		for (String sourceNode : outgoingRelationships.keySet()) {
			int outgoingRelationshipCount = outgoingRelationships.get(sourceNode).size();
			outgoingCounts.put(sourceNode, outgoingRelationshipCount);
		}
		Map<String, Integer> incomingCounts = new HashMap<String, Integer>();
		for (String destinationNode : incomingRelationships.keySet()) {
			int incomingRelationshipCount = incomingRelationships.get(destinationNode).size();
			incomingCounts.put(destinationNode, incomingRelationshipCount);
		}

		for (String sourceNode : outgoingCounts.keySet()) {
			System.out.println(outgoingCounts.get(sourceNode) + "\t" + sourceNode);
		}

		for (String destinationNode : incomingCounts.keySet()) {
			System.out.println(incomingCounts.get(destinationNode) + "\t" + destinationNode);
		}
	}

}
