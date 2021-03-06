package dna.util.fromArgs;

import dna.graph.generators.GraphGenerator;
import dna.graph.generators.reading.KonectGraph;
import dna.graph.generators.reading.TimestampedGraph;
import dna.graph.generators.reading.TimestampedReader;
import dna.graph.generators.reading.KonectReader.KonectBatchType;
import dna.graph.weights.Weight.WeightSelection;
import dna.updates.generators.BatchGenerator;
import dna.updates.generators.evolving.BarabasiAlbertBatch;
import dna.updates.generators.evolving.PositiveFeedbackPreferenceBatch;
import dna.updates.generators.evolving.RandomGrowth;
import dna.updates.generators.random.GrowingRandomEdgeExchange;
import dna.updates.generators.random.RandomBatch;
import dna.updates.generators.random.RandomEdgeExchange;
import dna.updates.generators.random.RandomScalingBatch;
import dna.updates.generators.reading.KonectBatch;
import dna.updates.generators.reading.TimestampedBatch;
import dna.updates.generators.reading.TimestampedBatch.TimestampedBatchType;

public class BatchGeneratorFromArgs {
	public static enum BatchType {
		BarabasiAlbert, PositiveFeedbackPreference, RandomGrowth, RandomScaling, Random, RandomW, RandomEdgeExchange, Timestamped, Konect, GrowingRandomEdgeExchange
	}

	public static BatchGenerator parse(GraphGenerator gg, BatchType batchType,
			String... args) {
		switch (batchType) {
		case BarabasiAlbert:
			return new BarabasiAlbertBatch(Integer.parseInt(args[0]),
					Integer.parseInt(args[1]));
		case PositiveFeedbackPreference:
			if (args.length == 1) {
				return new PositiveFeedbackPreferenceBatch(
						Integer.parseInt(args[0]));
			} else {
				return new PositiveFeedbackPreferenceBatch(
						Integer.parseInt(args[0]), Double.parseDouble(args[1]),
						Double.parseDouble(args[2]),
						Double.parseDouble(args[3]));
			}
		case RandomGrowth:
			return new RandomGrowth(Integer.parseInt(args[0]),
					Integer.parseInt(args[1]));
		case RandomScaling:
			if (args.length == 2) {
				return new RandomScalingBatch(Double.parseDouble(args[0]),
						Double.parseDouble(args[1]));
			} else {
				return new RandomScalingBatch(Double.parseDouble(args[0]),
						Double.parseDouble(args[1]),
						Double.parseDouble(args[2]),
						Double.parseDouble(args[3]));
			}
		case Random:
			return new RandomBatch(Integer.parseInt(args[0]),
					Integer.parseInt(args[1]), Integer.parseInt(args[2]),
					Integer.parseInt(args[3]));
		case RandomW:
			return new RandomBatch(Integer.parseInt(args[0]),
					Integer.parseInt(args[1]), Integer.parseInt(args[2]),
					WeightSelection.valueOf(args[3]),
					Integer.parseInt(args[4]), Integer.parseInt(args[5]),
					Integer.parseInt(args[6]), WeightSelection.valueOf(args[7]));
		case RandomEdgeExchange:
			return new RandomEdgeExchange(Integer.parseInt(args[0]),
					Integer.parseInt(args[1]));
		case Timestamped:
			TimestampedReader reader = ((TimestampedGraph) gg).getReader();
			if (args.length == 2) {
				return new TimestampedBatch(reader,
						TimestampedBatchType.valueOf(args[0]),
						Long.parseLong(args[1]));
			} else {
				return new TimestampedBatch(reader,
						TimestampedBatchType.valueOf(args[0]),
						Long.parseLong(args[1]), Long.parseLong(args[2]));
			}
		case Konect:
			return new KonectBatch(((KonectGraph) gg).getReader(),
					KonectBatchType.valueOf(args[0]), args[1]);
		case GrowingRandomEdgeExchange:
			return new GrowingRandomEdgeExchange(Integer.valueOf(args[0]),
					Integer.valueOf(args[1]));
		default:
			throw new IllegalArgumentException("unknown batch type: "
					+ batchType);
		}
	}
}
