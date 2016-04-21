package dna.visualization.config.graph;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.jar.JarFile;

import dna.util.Config;
import dna.util.IOUtils;
import dna.util.Log;
import dna.visualization.config.JSON.JSONObject;
import dna.visualization.config.JSON.JSONTokener;
import dna.visualization.config.graph.rules.RulesConfig;

/**
 * Configuration object holding all values used to configure a GraphPanel.
 * 
 * @author Rwilmes
 * 
 */
public class GraphPanelConfig {

	public static final GraphPanelConfig defaultGraphPanelConfig = GraphPanelConfig
			.getDefaultConfig();

	protected RulesConfig rules;

	protected int width;
	protected int height;
	protected boolean fullscreen;

	protected String timestampFormat;
	protected boolean renderHQ;
	protected boolean renderAA;

	protected double zoomSpeed;
	protected double scrollSpeed;
	protected boolean toolTipsEnabled;

	protected boolean directedEdgeArrowsEnabled;

	protected double nodeSize;
	protected String nodeColor;
	protected double edgeSize;

	public GraphPanelConfig(int width, int height, boolean fullscreen,
			String timestampFormat, boolean renderHQ, boolean renderAA,
			double zoomSpeed, double scrollSpeed, boolean toolTipsEnabled,
			boolean directedEdgeArrowsEnabled, double nodeSize,
			String nodeColor, double edgeSize, RulesConfig rules) {
		this.width = width;
		this.height = height;
		this.fullscreen = fullscreen;
		this.timestampFormat = timestampFormat;
		this.renderHQ = renderHQ;
		this.renderAA = renderAA;
		this.zoomSpeed = zoomSpeed;
		this.scrollSpeed = scrollSpeed;
		this.toolTipsEnabled = toolTipsEnabled;
		this.directedEdgeArrowsEnabled = directedEdgeArrowsEnabled;
		this.nodeSize = nodeSize;
		this.nodeColor = nodeColor;
		this.edgeSize = edgeSize;
		this.rules = rules;
	}

	public void read(String dir, String filename) {
		// TODO!
	}

	public void write(String dir, String filename) {
		// TODO!
	}

	/** Creates a main display config object from a given json object. **/
	public static GraphPanelConfig getFromJSONObject(JSONObject o) {
		GraphPanelConfig def = GraphPanelConfig.defaultGraphPanelConfig;


		// init values
		RulesConfig rules = null;
		int width = 1024;
		int height = 768;
		boolean fullscreen = false;
		String timestampFormat = null;
		boolean renderHQ = false;
		boolean renderAA = false;
		double zoomSpeed = 0.03;
		double scrollSpeed = 0.02;
		boolean toolTipsEnabled = true;
		boolean directedEdgeArrowsEnabled = true;

		double nodeSize = 10.0;
		String nodeColor = null;
		double edgeSize = 0.2;

		if (def != null) {
			// read default values
			rules = def.getRules();
			width = def.getWidth();
			height = def.getHeight();
			fullscreen = def.isFullscreen();
			timestampFormat = def.getTimestampFormat();
			renderHQ = def.isRenderHQ();
			renderAA = def.isRenderAA();
			zoomSpeed = def.getZoomSpeed();
			scrollSpeed = def.getScrollSpeed();
			toolTipsEnabled = def.isToolTipsEnabled();
			directedEdgeArrowsEnabled = def.isDirectedEdgeArrowsEnabled();
			nodeSize = def.getNodeSize();
			nodeColor = def.getNodeColor();
			edgeSize = def.getEdgeSize();
		}

		if (JSONObject.getNames(o) != null) {
			for (String s : JSONObject.getNames(o)) {
				switch (s) {
				case "DirectedEdgeArrowsEnabled":
					directedEdgeArrowsEnabled = o.getBoolean(s);
					break;
				case "EdgeSize":
					edgeSize = o.getDouble(s);
					break;
				case "Fullscreen":
					fullscreen = o.getBoolean(s);
					break;
				case "Height":
					height = o.getInt(s);
					break;
				case "NodeColor":
					nodeColor = o.getString(s);
					break;
				case "NodeSize":
					nodeSize = o.getDouble(s);
					break;
				case "RenderAA":
					renderAA = o.getBoolean(s);
					break;
				case "RenderHQ":
					renderHQ = o.getBoolean(s);
					break;
				case "RulesConfig":
					rules = RulesConfig.getFromJSONObject(o
							.getJSONObject("RulesConfig"));
					break;
				case "ScrollSpeed":
					scrollSpeed = o.getDouble(s);
					break;
				case "TimestampFormat":
					timestampFormat = o.getString(s);
					break;
				case "ToolTipsEnabled":
					toolTipsEnabled = o.getBoolean(s);
					break;
				case "Width":
					width = o.getInt(s);
					break;
				case "ZoomSpeed":
					zoomSpeed = o.getDouble(s);
					break;
				}
			}
		}

		return new GraphPanelConfig(width, height, fullscreen, timestampFormat,
				renderHQ, renderAA, zoomSpeed, scrollSpeed, toolTipsEnabled,
				directedEdgeArrowsEnabled, nodeSize, nodeColor, edgeSize, rules);
	}

	public static GraphPanelConfig getDefaultConfig() {
		return readConfig(Config.get("GRAPH_VIS_CONFIG_DEFAULT_PATH"));
	}

	public static GraphPanelConfig readConfig(String path) {
		Log.info("Reading GraphPanel-config: '" + path + "'");
		GraphPanelConfig config = null;
		InputStream is = null;
		JarFile jar = null;

		File file = new File(path);

		if (file.exists()) {
			try {
				is = new FileInputStream(path);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			if (IOUtils.isRunFromJar()) {
				Log.info("'" + path
						+ "-> ' not found. Attempting to read from .jar");
				try {
					jar = IOUtils.getExecutionJarFile();
					is = IOUtils.getInputStreamFromJar(jar, path, true);
				} catch (URISyntaxException | IOException e) {
					e.printStackTrace();
				}
			} else {
				Log.info("\t-> '" + path + "' not found!");
			}
		}

		if (is != null) {
			JSONTokener tk = new JSONTokener(is);
			JSONObject jsonConfig = new JSONObject(tk);
			config = GraphPanelConfig.getFromJSONObject(jsonConfig
					.getJSONObject("GraphPanelConfig"));

			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			is = null;
		}

		if (jar != null) {
			try {
				jar.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			jar = null;
		}

		return config;
	}

	public static GraphPanelConfig getDefaultgraphpanelconfig() {
		return defaultGraphPanelConfig;
	}

	public double getNodeSize() {
		return nodeSize;
	}

	public String getNodeColor() {
		return nodeColor;
	}

	public double getEdgeSize() {
		return edgeSize;
	}

	public RulesConfig getRules() {
		return rules;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public boolean isFullscreen() {
		return fullscreen;
	}

	public String getTimestampFormat() {
		return timestampFormat;
	}

	public boolean isRenderHQ() {
		return renderHQ;
	}

	public boolean isRenderAA() {
		return renderAA;
	}

	public double getZoomSpeed() {
		return zoomSpeed;
	}

	public double getScrollSpeed() {
		return scrollSpeed;
	}

	public boolean isToolTipsEnabled() {
		return toolTipsEnabled;
	}

	public boolean isDirectedEdgeArrowsEnabled() {
		return directedEdgeArrowsEnabled;
	}
}
