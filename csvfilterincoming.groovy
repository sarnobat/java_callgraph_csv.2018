import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;

/**
 * Remove nodes responsible for 80% of the edges
 * 
 * @author sarnobat 2016-12-24
 */
public class CsvFilterIncoming {

	public static void main(String[] args) throws IOException {
		double percentageOfRelationshipsToRetain;
		if (args.length > 0) {
			percentageOfRelationshipsToRetain = Double.parseDouble(args[0]);
		} else {
			percentageOfRelationshipsToRetain = 0.8;
		}

		HashMultimap<String, String> incomingRelationships = HashMultimap.create();
		_1: {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String lineOrig = null;
			while ((lineOrig = reader.readLine()) != null) {
				String line = lineOrig;
				if (line.trim().length() == 0) {
					continue;
				}
				String[] rel = line.split(",");
				String left = rel[0].replace("\"", "");
				System.err.println("READING: " + line);
				String right = rel[1].replace("\"", "");
				incomingRelationships.put(right, left);
			}
		}
		Map<String, Integer> incomingCounts = new HashMap<String, Integer>();
		for (String destinationNode : incomingRelationships.keySet()) {
			int incomingRelationshipCount = incomingRelationships.get(destinationNode).size();
			incomingCounts.put(destinationNode, incomingRelationshipCount);
		}

		for (String destinationNode : incomingCounts.keySet()) {
			System.err.println("ARITY: " + incomingCounts.get(destinationNode) + "\t"
					+ destinationNode);
		}
		int incomingCountAccumulator = count(incomingCounts.values());
		int totalCount = incomingCountAccumulator;

		Set<String> keySetIncoming = incomingRelationships.keySet();
		Set<String> allNodesMutable = new HashSet<String>();
		allNodesMutable.addAll(keySetIncoming);

		// come to think of it, I want to keep ones with high outgoing
		// counts
		// Make sure it's sorted in ascending size
		List<Contribution> contributions = populateContributions(incomingCounts);
		Preconditions.checkState(contributions.size() > 0);

		double maximumDesired = totalCount * (percentageOfRelationshipsToRetain);
		double amountCovered = 0;

		Map<String, String> toPrint = new HashMap<String, String>();
		Map<String, String> notPrinting = new HashMap<String, String>();
		for (Contribution node : contributions) {
			int edgesCovered = node.getCount();
			String nodeName = node.getName();

			amountCovered += edgesCovered;
			if (amountCovered < maximumDesired) {
				Set<String> incomingEdges = incomingRelationships.get(nodeName);
				for (String incoming : incomingEdges) {
					toPrint.put(incoming, nodeName);
				}
			} else {
				Set<String> incomingEdges = incomingRelationships.get(nodeName);
				for (String incoming : incomingEdges) {
					notPrinting.put(incoming, nodeName);
				}
			}
		}
		System.err.println("CsvFilterIncoming.main() toPrint = " + toPrint.size());
		System.err.println("CsvFilterIncoming.main() notPrinting = " + notPrinting.size());

		for (String key : notPrinting.keySet()) {
			String value = notPrinting.get(key);
			System.err.println("NOT PRINTING \"" + key + "\",\"" + value + "\"");
		}

		// Finally print everything desired
		for (String key : toPrint.keySet()) {
			String value = toPrint.get(key);
			System.out.println("\"" + key + "\",\"" + value + "\"");
		}
	}

	private static List<Contribution> populateContributions(Map<String, Integer> counts) {
		Preconditions.checkArgument(counts.size() > 0);
		List<Contribution> contributions = new LinkedList<Contribution>();
		for (String node : counts.keySet()) {
			contributions.add(new Contribution(node, counts.get(node)));
		}
		Collections.sort(contributions);
		return ImmutableList.copyOf(contributions);
	}

	private static class Contribution implements Comparable<Contribution> {
		private final String node;
		private final int count;

		Contribution(String node, Integer count) {
			this.count = count;
			this.node = node;
		}

		public String getName() {
			return node;
		}

		public int getCount() {
			return count;
		}

		@Override
		public boolean equals(Object o) {
			Contribution that = (Contribution) o;
			if (that.node.equals(this.node)) {
				return true;
			} else {
				if (count != that.count) {
					throw new RuntimeException("Developer error");
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			return node.hashCode();
		}

		@Override
		public int compareTo(Contribution that) {
			return this.count - that.count;
		}
	}

	private static int count(Collection<Integer> values) {
		int outgoingCountAccumulator = 0;
		for (Integer outgoingCountsIter : values) {
			outgoingCountAccumulator += outgoingCountsIter;
		}
		return outgoingCountAccumulator;
	}

}
