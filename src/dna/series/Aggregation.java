package dna.series;

import java.io.IOException;
import java.util.ArrayList;

import dna.io.filesystem.Dir;
import dna.series.aggdata.AggregatedBatch;
import dna.series.aggdata.AggregatedBinnedDistribution;
import dna.series.aggdata.AggregatedDistribution;
import dna.series.aggdata.AggregatedDistributionList;
import dna.series.aggdata.AggregatedMetric;
import dna.series.aggdata.AggregatedMetricList;
import dna.series.aggdata.AggregatedNodeValueList;
import dna.series.aggdata.AggregatedNodeValueListList;
import dna.series.aggdata.AggregatedRunTimeList;
import dna.series.aggdata.AggregatedSeries;
import dna.series.aggdata.AggregatedValue;
import dna.series.aggdata.AggregatedValueList;
import dna.series.data.BatchData;
import dna.series.data.MetricData;
import dna.series.data.RunData;
import dna.series.data.RunTime;
import dna.series.data.SeriesData;
import dna.series.data.Value;
import dna.series.data.distr2.BinnedDistr;
import dna.series.data.distr2.BinnedDoubleDistr;
import dna.series.data.distr2.BinnedIntDistr;
import dna.series.data.distr2.BinnedLongDistr;
import dna.series.data.distr2.Distr;
import dna.series.data.distr2.QualityDistr;
import dna.series.data.distr2.QualityDoubleDistr;
import dna.series.data.distr2.QualityIntDistr;
import dna.series.data.distr2.QualityLongDistr;
import dna.series.data.nodevaluelists.NodeValueList;
import dna.util.ArrayUtils;
import dna.util.Config;
import dna.util.Log;
import dna.util.Memory;

/**
 * 
 * This class provides methods to aggregate data from different sources, e.g.,
 * multiple runs of the same configuration, int a unified datastructure.
 * 
 * @author benni
 * 
 */
public class Aggregation {
	/**
	 * Method is used to aggregate runs of a given range.
	 * 
	 * @param seriesData
	 *            SeriesData object that is about to be aggregated
	 * @param from
	 *            Index of the first run
	 * @param to
	 *            Index of the last run
	 * @return AggregatedSeries object containing the aggregated runs
	 * @throws AggregationException
	 * @throws IOException
	 */
	public static AggregatedSeries aggregate(SeriesData seriesData, int from,
			int to) throws AggregationException, IOException {
		ArrayList<RunData> rdList = new ArrayList<RunData>();

		// check all RunData-Objects for compatibility
		// for (int i = 0; i < rdList.size() - 1; i++) {
		// if (!RunData.isSameType(seriesData.getRun(i),
		// seriesData.getRun(i + 1)))
		// throw new AggregationException("RunDatas not of the same type!");
		// }

		for (int i = from; i < to + 1; i++) {
			try {
				rdList.add(seriesData.getRun(i));
			} catch (IndexOutOfBoundsException e) {
				throw new AggregationException("Trying to aggregate over run "
						+ i + " from series " + seriesData.getName()
						+ " which is not available.");
			}
		}
		return new AggregatedSeries(aggregateRuns(seriesData.getDir(), rdList));
	}

	/**
	 * Aggregates over a whole series.
	 * 
	 * @param series
	 *            Series to be aggregated.
	 * @return Returns an aggregated series.
	 */
	public static AggregatedSeries aggregateSeries(SeriesData series)
			throws IOException {
		return new AggregatedSeries(aggregateRuns(series.getDir(),
				series.getRuns()));
	}

	/**
	 * Aggregates over several runs.
	 * 
	 * @param dir
	 *            Directory in which the runs are located.
	 * @param runs
	 *            ArrayList containing the runs.
	 * @return Array of AggregatedBatch-objects.
	 * @throws IOException
	 */
	public static AggregatedBatch[] aggregateRuns(String dir,
			ArrayList<RunData> runs) throws IOException {
		String runInfo = "";
		for (int i = 0; i < runs.size(); i++) {
			if (i == runs.size() - 1)
				runInfo += "run " + runs.get(i).getRun();
			else
				runInfo += "run " + runs.get(i).getRun() + ", ";
		}
		Log.info("aggregating data for " + runInfo);

		// treat single run as special case
		if (runs.size() == 1)
			return aggregateRun(dir, runs.get(0));

		long maxTimestamp = 0;
		int maxAmountBatches = 0;
		int runId = 0;
		for (int i = 0; i < runs.size(); i++) {
			RunData run = runs.get(i);
			if (run.getBatches().size() > maxAmountBatches) {
				maxAmountBatches = run.getBatches().size();
				runId = i;
			}
			for (BatchData batch : run.getBatches().getList()) {
				if (batch.getTimestamp() > maxTimestamp)
					maxTimestamp = batch.getTimestamp();
			}
		}
		boolean nmode = Config.getBoolean("AGGREGATION_IGNORE_MISSING_VALUES");
		if (nmode)
			Log.info("aggregation mode /n, ignoring missing values");
		else
			Log.info("aggregation mode: /n+1, treating missing values as 0");

		int gcCounter = 1;

		RunData maxRun = runs.get(runId);
		AggregatedBatch[] aBatches = new AggregatedBatch[maxAmountBatches];
		AggregatedBatch tempBatch;

		// iterate over batches
		for (int batchId = 0; batchId < runs.get(runId).getBatches().size(); batchId++) {
			ArrayList<BatchData> batches = new ArrayList<BatchData>(runs.size());
			BatchData structure = maxRun.getBatches().get(batchId);
			long timestamp = structure.getTimestamp();

			// record memory usage
			double mem = (new Memory()).getUsed();
			Log.info("\tBatch: " + timestamp + " (memory: " + mem + ")");

			// iterate over runs and read batches
			for (int i = 0; i < runs.size(); i++) {
				try {
					// read batch and add it
					batches.add(BatchData.readBatchValuesIntelligent(
							Dir.getBatchDataDir(dir, i, timestamp), timestamp,
							structure));
				} catch (Exception e) {
					e.printStackTrace();
					if (nmode)
						batches.add(new BatchData(-1));
				}
			}

			// aggregate
			AggregatedValueList aStats = aggregateBatchStatistics(batches);
			AggregatedRunTimeList aGeneralRuntimes = aggregateGeneralRuntimes(batches);
			AggregatedRunTimeList aMetricRuntimes = aggregateMetricRuntimes(batches);
			AggregatedMetricList aMetrics = aggregateMetrics(batches);

			// craft aggregated batch
			tempBatch = new AggregatedBatch(timestamp, aStats,
					aGeneralRuntimes, aMetricRuntimes, aMetrics);

			// write batch
			tempBatch.writeIntelligent(Dir.getBatchDataDir(
					Dir.getAggregationDataDir(dir), timestamp));

			// overwrite tempbatch
			tempBatch = null;
			aBatches[batchId] = new AggregatedBatch(timestamp);

			// call garbage collection
			if (Config.getBoolean("AGGREGATION_CALL_GC")
					&& batchId == Config.getInt("AGGREGATION_GC_OCCURENCE")
							* gcCounter) {
				System.gc();
				gcCounter++;
			}
		}

		// return
		return aBatches;
	}

	/**
	 * Aggregates the statistics of several batches.
	 * 
	 * @param batches
	 *            Batches to be aggregated.
	 * @return List of aggregated statistics.
	 */
	private static AggregatedValueList aggregateBatchStatistics(
			ArrayList<BatchData> batches) {
		if (batches.size() == 1)
			return aggregateBatchStatistics(batches.get(0));

		AggregatedValueList aStats = new AggregatedValueList(batches.get(0)
				.getValues().size());
		for (String stat : batches.get(0).getValues().getNames()) {
			double[] values = new double[batches.size()];

			for (int i = 0; i < batches.size(); i++) {
				if (batches.get(i).getTimestamp() == -1)
					values[i] = 0;
				else
					values[i] = batches.get(i).getValues().get(stat).getValue();
			}
			aStats.add(new AggregatedValue(stat, aggregate(values)));
		}
		return aStats;
	}

	/**
	 * Aggregates the statistics of a single batch.
	 * 
	 * @param b
	 *            BatchData object to be aggregated.
	 * @return List of aggregated statistics.
	 */
	private static AggregatedValueList aggregateBatchStatistics(BatchData b) {
		AggregatedValueList aStats = new AggregatedValueList();

		for (Value stat : b.getValues().getList()) {
			double value = stat.getValue();
			double[] aggregatedValue = new double[] { value, value, value,
					value, 0.0, 0.0, 0.0, value, value };
			aStats.add(new AggregatedValue(stat.getName(), aggregatedValue));
		}
		return aStats;
	}

	/**
	 * Aggregates the general runtimes of several batches.
	 * 
	 * @param batches
	 *            Batches to be aggregated.
	 * @return Aggregated general runtimes.
	 */
	private static AggregatedRunTimeList aggregateGeneralRuntimes(
			ArrayList<BatchData> batches) {
		if (batches.size() == 1)
			return aggregateGeneralRuntimes(batches.get(0));

		AggregatedRunTimeList aGeneralRuntimes = new AggregatedRunTimeList(
				Config.get("BATCH_GENERAL_RUNTIMES"));
		for (RunTime gRuntime : batches.get(0).getGeneralRuntimes().getList()) {
			double[] values = new double[batches.size()];
			for (int i = 0; i < batches.size(); i++) {
				if (batches.get(i).getTimestamp() == -1)
					values[i] = 0;
				else
					values[i] = batches.get(i).getGeneralRuntimes()
							.get(gRuntime.getName()).getRuntime();
			}
			aGeneralRuntimes.add(new AggregatedValue(gRuntime.getName(),
					Aggregation.aggregate(values)));
		}
		return aGeneralRuntimes;
	}

	/**
	 * Aggregates the general runtimes of a single batch.
	 * 
	 * @param b
	 *            BatchData object to be aggregated.
	 * @return Aggregated general runtimes.
	 */
	private static AggregatedRunTimeList aggregateGeneralRuntimes(BatchData b) {
		AggregatedRunTimeList aGeneralRuntimes = new AggregatedRunTimeList(
				Config.get("BATCH_GENERAL_RUNTIMES"));

		for (RunTime gRuntime : b.getGeneralRuntimes().getList()) {
			double value = gRuntime.getRuntime();
			double[] aggregatedValue = new double[] { value, value, value,
					value, 0.0, 0.0, 0.0, value, value };
			aGeneralRuntimes.add(new AggregatedValue(gRuntime.getName(),
					aggregatedValue));
		}
		return aGeneralRuntimes;
	}

	/**
	 * Aggregates the metric runtimes of several batches.
	 * 
	 * @param batches
	 *            Batches to be aggregated.
	 * @return Aggregated metric runtimes.
	 */
	private static AggregatedRunTimeList aggregateMetricRuntimes(
			ArrayList<BatchData> batches) {
		if (batches.size() == 1)
			return aggregateMetricRuntimes(batches.get(0));

		AggregatedRunTimeList aMetricRuntimes = new AggregatedRunTimeList(
				Config.get("BATCH_METRIC_RUNTIMES"));
		for (RunTime mRuntime : batches.get(0).getMetricRuntimes().getList()) {
			double[] values = new double[batches.size()];
			for (int i = 0; i < batches.size(); i++) {
				if (batches.get(i).getTimestamp() == -1)
					values[i] = 0;
				else
					values[i] = batches.get(i).getMetricRuntimes()
							.get(mRuntime.getName()).getRuntime();
			}
			aMetricRuntimes.add(new AggregatedValue(mRuntime.getName(),
					Aggregation.aggregate(values)));
		}
		return aMetricRuntimes;
	}

	/**
	 * Aggregates the metric runtimes of a single batch.
	 * 
	 * @param b
	 *            BatchData object to be aggregated.
	 * @return Aggregated metric runtimes.
	 */
	private static AggregatedRunTimeList aggregateMetricRuntimes(BatchData b) {
		AggregatedRunTimeList aMetricRuntimes = new AggregatedRunTimeList(
				Config.get("BATCH_METRIC_RUNTIMES"));

		for (RunTime mRuntime : b.getMetricRuntimes().getList()) {
			double value = mRuntime.getRuntime();
			double[] aggregatedValue = new double[] { value, value, value,
					value, 0.0, 0.0, 0.0, value, value };
			aMetricRuntimes.add(new AggregatedValue(mRuntime.getName(),
					aggregatedValue));
		}
		return aMetricRuntimes;
	}

	/**
	 * Aggregates a metric over several batches.
	 * 
	 * @param batches
	 *            Batches containing the metric
	 * @param metric
	 *            MetricData object to be aggregated
	 * @return AggregatedMetric object
	 */
	private static AggregatedMetricList aggregateMetrics(
			ArrayList<BatchData> batches) {
		if (batches.size() == 1)
			return aggregateMetrics(batches.get(0));

		// get reference batch which contains data
		BatchData refBatch = null;
		AggregatedMetricList aMetrics = null;
		for (BatchData b : batches) {
			if (b.getTimestamp() != -1) {
				refBatch = b;
				aMetrics = new AggregatedMetricList(b.getMetrics().size());
				break;
			}
		}

		// iterate over metrics
		for (String metric : refBatch.getMetrics().getNames()) {
			MetricData refMetric = refBatch.getMetrics().get(metric);

			// init aggregated lists
			AggregatedValueList aValues = new AggregatedValueList(refMetric
					.getValues().size());
			AggregatedDistributionList aDistributions = new AggregatedDistributionList(
					refMetric.getDistributions().size());
			AggregatedNodeValueListList aNodeValues = new AggregatedNodeValueListList(
					refMetric.getNodeValues().size());

			// iterate over values
			for (Value v : refMetric.getValues().getList()) {
				// fill values array (keep null if run doesnt support
				// batch -> timestamp == -1)
				double[] values = new double[batches.size()];
				for (int i = 0; i < batches.size(); i++) {
					if (batches.get(i).getTimestamp() != -1)
						values[i] = batches.get(i).getMetrics().get(metric)
								.getValues().get(v.getName()).getValue();
				}
				aValues.add(new AggregatedValue(v.getName(), aggregate(values)));
			}

			// iterate over distributions
			for (Distr<?, ?> d : refMetric.getDistributions().getList()) {
				// fill distributions array (keep null if run doesnt support
				// batch -> timestamp == -1)
				Distr<?, ?>[] distributions = new Distr<?, ?>[batches.size()];
				for (int i = 0; i < batches.size(); i++) {
					if (batches.get(i).getTimestamp() != -1)
						distributions[i] = batches.get(i).getMetrics()
								.get(metric).getDistributions()
								.get(d.getName());
				}
				aDistributions.add(aggregateDistributions(distributions));
			}

			// iterate over nodevaluelists
			for (NodeValueList n : refMetric.getNodeValues().getList()) {
				// fill nodevaluelists array (keep null if run doesnt support
				// batch -> timestamp == -1)
				NodeValueList[] nodevalues = new NodeValueList[batches.size()];
				for (int i = 0; i < batches.size(); i++) {
					if (batches.get(i).getTimestamp() != -1)
						nodevalues[i] = batches.get(i).getMetrics().get(metric)
								.getNodeValues().get(n.getName());
				}
				aNodeValues.add(aggregateNodeValueLists(nodevalues));
			}

			// TODO: nodenodevaluelists

			// craft and add aggregated metric
			aMetrics.add(new AggregatedMetric(refMetric.getName(), aValues,
					aDistributions, aNodeValues));
		}

		// return
		return aMetrics;
	}

	/**
	 * Aggregates over given distributions array.
	 * 
	 * @param distributions
	 *            Distributions to be aggregated
	 * @return Aggregated distribution
	 */
	private static AggregatedDistribution aggregateDistributions(
			Distr<?, ?>[] distributions) {
		// get reference distribution which contains data
		Distr<?, ?> refDist = null;
		for (Distr<?, ?> d : distributions) {
			if (d != null) {
				refDist = d;
				break;
			}
		}

		// switch on different types
		switch (refDist.getDistrType()) {
		case BINNED_DOUBLE:
			return aggregateBinnedDistributions(distributions);
		case BINNED_INT:
			return aggregateBinnedDistributions(distributions);
		case BINNED_LONG:
			return aggregateBinnedDistributions(distributions);
		case QUALITY_DOUBLE:
			return aggregateQualityDistributions(distributions);
		case QUALITY_INT:
			return aggregateQualityDistributions(distributions);
		case QUALITY_LONG:
			return aggregateQualityDistributions(distributions);
		default:
			Log.warn("Wrong distribution type in aggregation! Returning null.");
			return null;
		}
	}

	/** Aggregates over binned distributions. **/
	private static AggregatedBinnedDistribution aggregateBinnedDistributions(
			Distr<?, ?>[] dists) {
		BinnedDistr<?>[] distributions = new BinnedDistr<?>[dists.length];
		for (int i = 0; i < dists.length; i++) {
			distributions[i] = (BinnedDistr<?>) dists[i];
		}
		return aggregateBinnedDistributions(distributions);
	}

	/** Aggregates over binned distributions. **/
	public static AggregatedBinnedDistribution aggregateBinnedDistributions(
			BinnedDistr<?>[] dists) {
		// get ref-distribution from array
		BinnedDistr<?> refDist = null;
		for (BinnedDistr<?> d : dists) {
			if (d != null) {
				refDist = d;
				break;
			}
		}

		// calc 'longest' distribution
		int amountValues = 0;
		for (BinnedDistr<?> d : dists) {
			if (d != null) {
				if (d.getValues().length > amountValues)
					amountValues = d.getValues().length;
			}
		}

		// array that will be filled with aggregated values
		AggregatedValue[] aValues = new AggregatedValue[amountValues];

		// iterate over values
		for (int i = 0; i < amountValues; i++) {
			double[] values = new double[dists.length];
			for (int j = 0; j < dists.length; j++) {
				try {
					values[j] = dists[j].getValues()[i] * 1.0
							/ dists[j].getDenominator();
				} catch (IndexOutOfBoundsException | NullPointerException e) {
					values[j] = 0;
				}
			}
			double[] aggregatedValues = aggregate(values);
			double[] temp = new double[aggregatedValues.length + 1];
			temp[0] = i;
			for (int j = 0; j < aggregatedValues.length; j++) {
				temp[j + 1] = aggregatedValues[j];
			}
			aValues[i] = new AggregatedValue(refDist.getName(), temp);
		}

		// return proper distribution
		switch (refDist.getDistrType()) {
		case BINNED_DOUBLE:
			return new AggregatedBinnedDistribution(refDist.getName(), aValues,
					((BinnedDoubleDistr) refDist).getBinSize());
		case BINNED_INT:
			return new AggregatedBinnedDistribution(refDist.getName(), aValues,
					((BinnedIntDistr) refDist).getBinSize());
		case BINNED_LONG:
			return new AggregatedBinnedDistribution(refDist.getName(), aValues,
					((BinnedLongDistr) refDist).getBinSize());
		default:
			Log.warn("Wrong distribution type in aggregation! Returning null.");
			return null;
		}
	}

	/** Aggregates over quality distributions. **/
	private static AggregatedDistribution aggregateQualityDistributions(
			Distr<?, ?>[] dists) {
		// cast to quality-distributions
		QualityDistr<?>[] distributions = new QualityDistr<?>[dists.length];
		for (int i = 0; i < dists.length; i++) {
			distributions[i] = (QualityDistr<?>) dists[i];
		}
		return aggregateQualityDistributions(distributions);
	}

	/** Aggregates over quality distributions. **/
	public static AggregatedDistribution aggregateQualityDistributions(
			QualityDistr<?>[] dists) {
		// get ref-distribution from array
		QualityDistr<?> refDist = null;
		for (QualityDistr<?> d : dists) {
			if (d != null) {
				refDist = d;
				break;
			}
		}

		// calc 'longest' distribution
		int amountValues = 0;
		for (QualityDistr<?> d : dists) {
			if (d != null) {
				if (d.getValues().length > amountValues)
					amountValues = d.getValues().length;
			}
		}

		// array that will be filled with aggregated values
		AggregatedValue[] aValues = new AggregatedValue[amountValues];

		// iterate over values
		for (int i = 0; i < amountValues; i++) {
			double[] values = new double[dists.length];
			for (int j = 0; j < dists.length; j++) {
				try {
					values[j] = dists[j].getValues()[i];
				} catch (IndexOutOfBoundsException | NullPointerException e) {
					values[j] = 0;
				}
			}
			double[] aggregatedValues = aggregate(values);
			double[] temp = new double[aggregatedValues.length + 1];
			temp[0] = i;
			for (int j = 0; j < aggregatedValues.length; j++) {
				temp[j + 1] = aggregatedValues[j];
			}
			aValues[i] = new AggregatedValue(refDist.getName(), temp);
		}

		// return
		return new AggregatedDistribution(refDist.getName(), aValues);
	}

	/**
	 * Aggregates over given node value lists array.
	 * 
	 * @param nodevalues
	 *            NodeValueLists to be aggregated.
	 * @return Aggregated node value lists.
	 */
	private static AggregatedNodeValueList aggregateNodeValueLists(
			NodeValueList[] nodevalues) {
		// get reference nodevaluelist which contains data
		NodeValueList refNvl = null;
		for (NodeValueList n : nodevalues) {
			if (n != null) {
				refNvl = n;
				break;
			}
		}
		// calc 'longest' nodevaluelist
		int amountValues = 0;
		for (NodeValueList n : nodevalues) {
			if (n != null) {
				if (n.getValues().length > amountValues)
					amountValues = n.getValues().length;
			}
		}

		AggregatedValue[] aValues = new AggregatedValue[amountValues];

		for (int i = 0; i < amountValues; i++) {
			double[] values = new double[nodevalues.length];
			for (int j = 0; j < nodevalues.length; j++) {
				if (nodevalues[j] == null)
					values[j] = 0;
				else {
					try {
						values[j] = nodevalues[j].getValues()[i];
					} catch (IndexOutOfBoundsException | NullPointerException e) {
						values[j] = 0;
					}
				}
			}
			double[] aggregatedValues = aggregate(values);
			double[] temp = new double[aggregatedValues.length + 1];
			temp[0] = i;
			for (int j = 0; j < aggregatedValues.length; j++) {
				temp[j + 1] = aggregatedValues[j];
			}
			aValues[i] = new AggregatedValue(refNvl.getName(), temp);
		}
		return new AggregatedNodeValueList(refNvl.getName(), aValues);
	}

	/**
	 * Aggregates all metrics of a single batch.
	 * 
	 * @param b
	 *            BatchData object to be aggregated.
	 * @return List of aggregated metrics.
	 */
	private static AggregatedMetricList aggregateMetrics(BatchData b) {
		AggregatedMetricList aMetrics = new AggregatedMetricList(b.getMetrics()
				.size());
		for (MetricData m : b.getMetrics().getList()) {
			aMetrics.add(aggregateMetric(m));
		}
		return aMetrics;
	}

	/**
	 * Aggregates a single metricdata object.
	 * 
	 * @param m
	 *            MetricData object to be aggregated
	 * @return AggregatedMetric object
	 */
	private static AggregatedMetric aggregateMetric(MetricData m) {
		// VALUES
		AggregatedValueList aValuesList = new AggregatedValueList(m.getValues()
				.size());

		for (Value v : m.getValues().getList()) {
			double value = v.getValue();
			double[] aggregatedValue = new double[] { value, value, value,
					value, 0.0, 0.0, 0.0, value, value };
			aValuesList.add(new AggregatedValue(v.getName(), aggregatedValue));
		}

		// DISTRIBUTIONS
		AggregatedDistributionList aDistributions = new AggregatedDistributionList(
				m.getDistributions().size());

		// aggregate
		for (Distr<?, ?> d : m.getDistributions().getList())
			aDistributions.add(aggregateDistribution(d));

		// NODEVALUELISTS
		AggregatedNodeValueListList aNodeValueLists = new AggregatedNodeValueListList(
				m.getNodeValues().size());

		// aggregate
		for (NodeValueList n : m.getNodeValues().getList())
			aNodeValueLists.add(aggregateNodeValueList(n));

		// TODO: AGGREGATE NODENODEVALUELISTS

		return new AggregatedMetric(m.getName(), aValuesList, aDistributions,
				aNodeValueLists);
	}

	/** Aggregates over a single node value list **/
	public static AggregatedNodeValueList aggregateNodeValueList(NodeValueList n) {
		double[] values = n.getValues();
		AggregatedValue[] aggregatedValues = new AggregatedValue[values.length];
		for (int i = 0; i < values.length; i++) {
			double value = values[i];
			double[] aValues = new double[] { i, value, value, value, value,
					0.0, 0.0, 0.0, value, value };
			aggregatedValues[i] = new AggregatedValue(n.getName() + i, aValues);
		}

		return new AggregatedNodeValueList(n.getName(), aggregatedValues);
	}

	/** Aggregates over a single distribution. **/
	public static AggregatedDistribution aggregateDistribution(Distr<?, ?> d) {
		switch (d.getDistrType()) {
		case BINNED_DOUBLE:
			return aggregateBinnedDistr((BinnedDoubleDistr) d);
		case BINNED_INT:
			return aggregateBinnedDistr((BinnedIntDistr) d);
		case BINNED_LONG:
			return aggregateBinnedDistr((BinnedLongDistr) d);
		case QUALITY_DOUBLE:
			return aggregatedQualityDistr((QualityDoubleDistr) d);
		case QUALITY_INT:
			return aggregatedQualityDistr((QualityIntDistr) d);
		case QUALITY_LONG:
			return aggregatedQualityDistr((QualityLongDistr) d);
		default:
			Log.warn("Wrong distribution type in aggregation! Returning null.");
			return null;
		}
	}

	/** Aggregates over a single binned-distribution. **/
	private static AggregatedBinnedDistribution aggregateBinnedDistr(
			BinnedDistr<?> d) {
		long[] values = d.getValues();
		AggregatedValue[] aggregatedValues = new AggregatedValue[values.length];
		for (int i = 0; i < values.length; i++) {
			double value = values[i] * 1.0 / d.getDenominator();
			double[] aValues = new double[] { i, value, value, value, value,
					0.0, 0.0, 0.0, value, value };
			aggregatedValues[i] = new AggregatedValue(d.getName() + i, aValues);
		}

		switch (d.getDistrType()) {
		case BINNED_DOUBLE:
			return new AggregatedBinnedDistribution(d.getName(),
					aggregatedValues, ((BinnedDoubleDistr) d).getBinSize());
		case BINNED_INT:
			return new AggregatedBinnedDistribution(d.getName(),
					aggregatedValues, ((BinnedIntDistr) d).getBinSize());
		case BINNED_LONG:
			return new AggregatedBinnedDistribution(d.getName(),
					aggregatedValues, ((BinnedLongDistr) d).getBinSize());
		default:
			Log.warn("Wrong distribution type in aggregation! Returning null.");
			return null;
		}
	}

	/** Aggregates over a single quality-distribution. **/
	private static AggregatedDistribution aggregatedQualityDistr(
			QualityDistr<?> d) {
		double[] values = d.getValues();
		AggregatedValue[] aggregatedValues = new AggregatedValue[values.length];
		for (int i = 0; i < values.length; i++) {
			double value = values[i];
			double[] aValues = new double[] { i, value, value, value, value,
					0.0, 0.0, 0.0, value, value };
			aggregatedValues[i] = new AggregatedValue(d.getName() + i, aValues);
		}
		return new AggregatedDistribution(d.getName(), aggregatedValues);
	}

	/**
	 * Aggregates a single run.
	 * 
	 * @param dir
	 *            Directory in which the run is located.
	 * @param run
	 *            Run that is to be aggregated.
	 * @return Array of AggregatedBatch-objects.
	 * 
	 * @throws Throwable
	 */
	private static AggregatedBatch[] aggregateRun(String dir, RunData run)
			throws IOException {
		// init
		int batchesAmount = run.getBatches().size();
		AggregatedBatch[] aBatches = new AggregatedBatch[batchesAmount];
		AggregatedBatch tempBatch;

		// gc counter
		int gcCounter = 1;

		// iterate over batches
		for (int batchId = 0; batchId < run.getBatches().size(); batchId++) {
			BatchData structure = run.getBatches().get(batchId);
			BatchData b;
			long timestamp = structure.getTimestamp();

			// record memory usage
			double mem = (new Memory()).getUsed();
			Log.info("\tBatch: " + timestamp + " (memory: " + mem + ")");

			// read batch
			b = BatchData.readBatchValuesIntelligent(Dir.getBatchDataDir(
					Dir.getRunDataDir(dir, run.getRun()), timestamp),
					timestamp, structure);

			// aggregate
			AggregatedRunTimeList aGeneralRuntimes = aggregateGeneralRuntimes(b);
			AggregatedRunTimeList aMetricRuntimes = aggregateMetricRuntimes(b);
			AggregatedValueList aStats = aggregateBatchStatistics(b);
			AggregatedMetricList aMetrics = aggregateMetrics(b);

			// craft aggregated batch
			tempBatch = new AggregatedBatch(timestamp, aStats,
					aGeneralRuntimes, aMetricRuntimes, aMetrics);

			// write
			tempBatch.writeIntelligent(Dir.getBatchDataDir(
					Dir.getAggregationDataDir(dir), timestamp));

			// overwrite tempbatch
			tempBatch = null;
			aBatches[batchId] = new AggregatedBatch(timestamp);

			// call garbage collection
			if (Config.getBoolean("AGGREGATION_CALL_GC")
					&& batchId == Config.getInt("AGGREGATION_GC_OCCURENCE")
							* gcCounter) {
				System.gc();
				gcCounter++;
			}
		}

		// return
		return aBatches;
	}

	/**
	 * Aggregates over the given inputData.
	 * 
	 * @param inputData
	 * @return double array containing the aggregated data
	 */
	private static double[] aggregate(double[] inputData) {
		// aggregated array structure: { avg, min, max, median, variance,
		// variance-low, variance-up, confidence-low, confidence-up }
		double avg = ArrayUtils.avg(inputData);
		double[] varLowUp = ArrayUtils.varLowUp(inputData, avg);
		double[] conf = ArrayUtils.conf(inputData);
		double[] temp = { avg, ArrayUtils.min(inputData),
				ArrayUtils.max(inputData), ArrayUtils.med(inputData),
				varLowUp[0], varLowUp[1], varLowUp[2], conf[0], conf[1] };

		return temp;
	}

	/**
	 * Aggregates over the given inputData.
	 * 
	 * @param inputData
	 * @return double array containing the aggregated data
	 */
	public static double[] aggregate(long[] inputData) {
		// aggregated array structure: { avg, min, max, median, variance,
		// variance-low, variance-up, confidence-low, confidence-up }
		double avg = ArrayUtils.avg(inputData);
		double[] varLowUp = ArrayUtils.varLowUp(inputData, avg);
		double[] conf = ArrayUtils.conf(inputData);
		double[] temp = { avg, ArrayUtils.min(inputData),
				ArrayUtils.max(inputData), ArrayUtils.med(inputData),
				varLowUp[0], varLowUp[1], varLowUp[2], conf[0], conf[1] };

		return temp;
	}

	/**
	 * Aggregates over the given inputData.
	 * 
	 * @param inputData
	 * @return double array containing the aggregated data
	 */
	private static double[] aggregate(int[] inputData) {
		// aggregated array structure: { avg, min, max, median, variance,
		// variance-low, variance-up, confidence-low, confidence-up }
		double avg = ArrayUtils.avg(inputData);
		double[] varLowUp = ArrayUtils.varLowUp(inputData, avg);
		double[] conf = ArrayUtils.conf(inputData);
		double[] temp = { avg, ArrayUtils.min(inputData),
				ArrayUtils.max(inputData), ArrayUtils.med(inputData),
				varLowUp[0], varLowUp[1], varLowUp[2], conf[0], conf[1] };

		return temp;
	}
}
