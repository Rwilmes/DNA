package dna.visualization.components.visualizer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import dna.labels.Label;
import dna.labels.LabelList;
import dna.series.data.BatchData;
import dna.visualization.MainDisplay;
import dna.visualization.components.visualizer.traces.LabelTrace;
import dna.visualization.config.VisualizerListConfig;
import dna.visualization.config.components.LabelVisualizerConfig;
import info.monitorenter.gui.chart.IAxis.AxisTitle;
import info.monitorenter.gui.chart.ITrace2D;
import info.monitorenter.gui.chart.ITracePoint2D;
import info.monitorenter.gui.chart.axis.scalepolicy.AxisScalePolicyAutomaticBestFit;
import info.monitorenter.gui.chart.axis.scalepolicy.AxisScalePolicyManualTicks;
import info.monitorenter.gui.chart.labelformatters.LabelFormatterDate;
import info.monitorenter.gui.chart.rangepolicies.RangePolicyFixedViewport;
import info.monitorenter.util.Range;

public class LabelVisualizer extends Visualizer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected HashMap<String, LabelTrace> labelTraces;

	protected HashMap<String, Integer> mapping;
	protected HashMap<String, Color> colorMap;
	protected int mappingCounter;

	protected ArrayList<String> availableValues;
	private LinkedList<BatchData> batchBuffer;
	private BatchData initBatch;
	private int bufferSize;
	private boolean reload;

	private boolean xAxisTypeTimestamp;
	private long currentTimestamp;
	private double xAxisOffset;

	// config
	protected VisualizerListConfig listConfig;
	protected MainDisplay mainDisplay;
	protected LabelVisualizerConfig config;

	public LabelVisualizer(MainDisplay mainDisplay, LabelVisualizerConfig config) {
		// super(config.getChartSize(), config.getLegendSize());
		super(new Dimension(450, 320), new Dimension(190, 330));

		this.mainDisplay = mainDisplay;
		this.labelTraces = new HashMap<String, LabelTrace>();
		this.mapping = new HashMap<String, Integer>();
		this.colorMap = new HashMap<String, Color>();
		this.mappingCounter = 0;
		this.availableValues = new ArrayList<String>();
		// this.listConfig = config.getListConfig();
		// this.bufferSize = config.getTraceLength();
		// this.TRACE_LENGTH = config.getTraceLength();
		this.listConfig = new VisualizerListConfig();
		this.bufferSize = 1000;
		this.TRACE_LENGTH = 1000;
		this.config = config;

		// batch buffer
		this.batchBuffer = new LinkedList<BatchData>();
		this.reload = false;

		// set title and border of the metric visualizer
		TitledBorder title = BorderFactory.createTitledBorder(config.getName());
		title.setBorder(BorderFactory.createEtchedBorder((EtchedBorder.LOWERED)));
		title.setTitleFont(new Font(this.mainDisplay.getDefaultFont().getName(), Font.BOLD,
				this.mainDisplay.getDefaultFont().getSize()));
		title.setTitleColor(this.mainDisplay.getDefaultFontColor());
		this.setBorder(title);

		// if x axis type is date
		this.xAxisTypeTimestamp = true;
		if (config.getxAxisType().equals("date")) {
			this.xAxisTypeTimestamp = false;
			this.xAxis1.setFormatter(new LabelFormatterDate(new SimpleDateFormat(config.getxAxisFormat())));
			this.xAxis1.setMajorTickSpacing(5);
			this.xAxis1.setMinorTickSpacing(1);
			this.xAxis1.setAxisScalePolicy(new AxisScalePolicyAutomaticBestFit());
		}

		this.yAxis1.setAxisScalePolicy(new AxisScalePolicyManualTicks());
		this.yAxis1.setRangePolicy(new RangePolicyFixedViewport(new Range(0, 1)));

		// add menu bar
		super.addMenuBar(config.getMenuBarConfig());

		// add coordinate parsing to mouseover on chart
		this.chart.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (chart.getPointFinder().getNearestPoint(e, chart) != null) {
					ITracePoint2D tempPointFinder = chart.getPointFinder().getNearestPoint(e, chart);
					menuBar.updateCoordsPanel((int) Math.floor(tempPointFinder.getX()), tempPointFinder.getY());
				}
			}

			public void mouseDragged(MouseEvent e) {
			}
		});

		// apply config
		// this.chart.setPreferredSize(config.getChartSize());
		// this.legend.setLegendSize(config.getLegendSize());
		// this.xAxisOffset = config.getxAxisOffset();

		this.chart.setPreferredSize(new Dimension(450, 320));
		this.legend.setLegendSize(new Dimension(190, 330));
		this.xAxisOffset = 0.2;

		this.yAxis1.setAxisTitle(new AxisTitle(config.getY1AxisTitle()));
		this.yAxis2.setAxisTitle(new AxisTitle(config.getY2AxisTitle()));

	}

	/** initializes the data with the first batch **/
	public void initData(BatchData b) {
		this.initBatch = b;
		this.minTimestamp = b.getTimestamp();
		this.maxTimestamp = b.getTimestamp();
		this.minShownTimestamp = b.getTimestamp();
		this.maxShownTimestamp = this.minShownTimestamp;

		this.currentTimestamp = b.getTimestamp();

		// clear chart
		for (ITrace2D t : this.chart.getTraces()) {
			t.removeAllPoints();
		}

		for (String name : this.labelTraces.keySet()) {
			this.labelTraces.get(name).setLastTimestamp(b.getTimestamp() - 1);
		}

		// gather all available values
		for (Label label : b.getLabels().getList()) {
			this.availableValues.add(getLabelKey(label.getName(), label.getType()));
		}

		// init addbox
		String[] tempValues = this.gatherValues(b);
		this.legend.updateAddBox(tempValues);

		// load config
		// if (this.listConfig != null && !super.locked)
		// this.loadConfig(this.listConfig);

		// toggle visibility and validate
		this.toggleYAxisVisibility();
		this.validate();
	}

	/** gathers all plottable values from the batch **/
	public String[] gatherValues(BatchData b) {
		ArrayList<String> tempList = new ArrayList<String>();
		tempList.add("labels");

		ArrayList<String> labelers = new ArrayList<String>();
		ArrayList<ArrayList<String>> types = new ArrayList<ArrayList<String>>();

		for (Label l : b.getLabels().getList()) {
			String name = l.getName();
			String type = l.getType();

			if (!labelers.contains(name)) {
				labelers.add(name);
				types.add(new ArrayList<String>());
			}

			int index = labelers.indexOf(name);
			ArrayList<String> typesList = types.get(index);

			if (!typesList.contains(type))
				typesList.add(type);
		}

		// store original indices before sorting
		HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
		for (int i = 0; i < labelers.size(); i++)
			indexMap.put(labelers.get(i), i);

		// sort labelers alphabetically
		Collections.sort(labelers);

		// fill templist
		for (int i = 0; i < labelers.size(); i++) {
			String name = labelers.get(i);
			tempList.add("--- " + name);

			ArrayList<String> ts = types.get(indexMap.get(name));
			Collections.sort(ts);

			for (String type : ts)
				tempList.add("----- " + name + "." + type);
		}

		String[] tempValues = tempList.toArray(new String[tempList.size()]);
		return tempValues;
	}

	/** adds trace to the visualizer with default trace length **/
	public void addTrace(String name, Color color) {
		if (!this.labelTraces.containsKey(name)) {
			int yMapping = this.getLabelerTypeKeyMapping(name);
			
			this.colorMap.put(name, color);

			LabelTrace labelTrace = new LabelTrace(this, this.chart, name, yMapping, 10, color,
					this.currentTimestamp);
			this.labelTraces.put(name, labelTrace);
		}
		this.yAxis1.setRangePolicy(new RangePolicyFixedViewport(getYAxisRange()));
	}

	/**
	 * If the name is already being mapped it returns the mapping. Else a new
	 * one is created and returned.
	 **/
	protected int getLabelerTypeKeyMapping(String labelerTypeKey) {
		if (this.mapping.containsKey(labelerTypeKey))
			return this.mapping.get(labelerTypeKey);

		int newMapping = this.mappingCounter;
		this.mapping.put(labelerTypeKey, newMapping);
		this.mappingCounter--;

		return newMapping;
	}

	/**
	 * removes a trace from the chart and the traces-list and the current traces
	 **/
	public void removeTrace(String name) {
		if (this.labelTraces.containsKey(name)) {
			LabelTrace trace = this.labelTraces.get(name);
			trace.clear();
			this.labelTraces.remove(name);
		}

		this.toggleXAxisVisibility();
		this.toggleYAxisVisibility();

		this.yAxis1.setRange(getYAxisRange());
	}

	/** Returns the y-axis range based on the number of traces. **/
	public Range getYAxisRange() {
		int max = 0;
		int min = 0;
		for (String name : this.labelTraces.keySet()) {
			int y = this.labelTraces.get(name).getYMapping();
			max = Math.max(max, y);
			min = Math.min(min, y);
		}
		return new Range(min - 1, max + 1);
	}

	/**
	 * Updates the chart and the legend with a new batch.
	 * 
	 * @param b
	 *            New batch
	 */
	public void updateData(BatchData b) {
		long timestamp = b.getTimestamp();
		double timestampDouble = timestamp;

		// check if new batch is before last one which means time slided
		// backwards
		if (timestamp < this.currentTimestamp) {
			while (this.batchBuffer.size() > 0) {
				if (this.batchBuffer.getLast().getTimestamp() <= b.getTimestamp()) {
					break;
				} else {
					this.batchBuffer.removeLast();
				}
			}
			// reload data
			this.reloadData();
			// this.updateData(b);
		} else {
			// if reload flag is set, dont add batch to buffer
			if (!this.reload) {
				// if buffer sizer is reached, remove batches until its
				if (this.batchBuffer.size() >= this.bufferSize)
					while (!(this.batchBuffer.size() < this.bufferSize))
						this.batchBuffer.removeFirst();
				// add new batch as last batch to list
				this.batchBuffer.addLast(b);
			}

			this.currentTimestamp = timestamp;

			if (timestamp < this.minTimestamp)
				this.minTimestamp = timestamp;
			if (timestamp > this.maxTimestamp)
				this.maxTimestamp = timestamp;

			// double offsetX = 0;

			// update values

			// iterate over all added traces
			LabelList labels = b.getLabels();

			for (String name : this.labelTraces.keySet()) {
				LabelTrace labelTrace = this.labelTraces.get(name);
				boolean updated = false;
				for (Label l : labels.getList()) {
					String key = getLabelKey(l.getName(), l.getType());
					if (key.equals(name)) {
						labelTrace.update(timestampDouble, l);
						updated = true;
						break;
					}
				}

				if (!updated)
					labelTrace.update(timestampDouble, null);

			}

			// update chart axis ticks
			this.updateTicks();

			// check if new label contained in the batch
			boolean newLabelFound = false;

			for (Label l : labels.getList()) {
				String name = l.getName();
				String type = l.getType();

				String key = getLabelKey(name, type);

				if (!this.availableValues.contains(key)) {
					newLabelFound = true;
					this.availableValues.add(key);
				}
			}

			if (newLabelFound)
				this.legend.updateAddBox(this.gatherAddBoxChoicesFromAvailableValues());
		}

		this.validate();
	}

	/** Gathers all currently available addbox-choices. **/
	protected String[] gatherAddBoxChoicesFromAvailableValues() {
		ArrayList<String> addBoxList = new ArrayList<String>();
		addBoxList.add("labels");

		HashMap<String, ArrayList<String>> labelers = new HashMap<String, ArrayList<String>>();

		// split available value-keys by .
		for (String keys : this.availableValues) {
			String[] splits = keys.split("\\.");
			String name = splits[0];
			String type = splits[1];

			for (int i = 2; i < splits.length; i++)
				type += "." + splits[i];

			ArrayList<String> tempList;
			if (labelers.containsKey(name)) {
				tempList = labelers.get(name);
			} else {
				tempList = new ArrayList<String>();
				labelers.put(name, tempList);
			}

			tempList.add(type);
		}

		for (String l : labelers.keySet()) {
			addBoxList.add("--- " + l);
			for (String t : labelers.get(l))
				addBoxList.add("----- " + getLabelKey(l, t));
		}

		String[] tempValues = addBoxList.toArray(new String[addBoxList.size()]);
		return tempValues;
	}

	/** Returns the key of a label based on its name and type. **/
	protected String getLabelKey(String name, String type) {
		return name + "." + type;
	}

	/** reloads all data included in the batchBuffer **/
	private void reloadData() {
		// set reload flag
		this.reload = true;

		// reset current timestamp
		this.currentTimestamp = 0;

		// clear chart
		for (ITrace2D t : this.chart.getTraces()) {
			t.removeAllPoints();
		}

		// update with old batches
		Iterator<BatchData> iterator = this.batchBuffer.iterator();
		while (iterator.hasNext()) {
			this.updateData(iterator.next());
		}

		// unset reload flag
		this.reload = false;
	}

	/** resets the metric visualizer **/
	public void reset() {
		this.minShownTimestamp = 0;
		this.maxShownTimestamp = 10;

		// clear labels
		for (String name : this.labelTraces.keySet()) {
			LabelTrace trace = this.labelTraces.get(name);
			trace.clear();
		}

		this.batchBuffer.clear();
		this.availableValues.clear();
		this.chart.updateUI();
	}

}
