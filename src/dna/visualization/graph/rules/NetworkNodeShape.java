package dna.visualization.graph.rules;

import java.awt.Color;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

import dna.graph.generators.network.weights.NetworkWeight;
import dna.graph.weights.Weight;
import dna.util.Config;
import dna.visualization.graph.GraphVisualization;
import dna.visualization.graph.rules.GraphStyleUtils.ElementShape;

/**
 * This rule shape and colors nodes according to their NetworkNodeWeight.
 * 
 * @author Rwilmes
 * 
 */
public class NetworkNodeShape extends GraphStyleRule {

	protected String name;

	protected ElementShape defaultShape;
	protected ElementShape hostShape;
	protected ElementShape portShape;

	protected int portBlue;

	public NetworkNodeShape(String name) {
		this(name, ElementShape.circle, ElementShape.circle, ElementShape.box,
				Config.getInt("GRAPH_VIS_NETWORK_PORT_NODE_BLUE"));
	}

	public NetworkNodeShape(String name, ElementShape defaultShape,
			ElementShape hostShape, ElementShape portShape, int portBlue) {
		this.name = name;
		this.defaultShape = defaultShape;
		this.hostShape = hostShape;
		this.portShape = portShape;
		this.portBlue = portBlue;
	}

	@Override
	public void onNodeAddition(Node n) {
		Weight w = n.getAttribute(GraphVisualization.weightKey);
		if (w instanceof NetworkWeight) {
			switch (((NetworkWeight) w).getWeight()) {
			case HOST:
				GraphStyleUtils.setShape(n, hostShape);
				break;
			case PORT:
				GraphStyleUtils.setShape(n, portShape);
				colorPortNode(n);
				break;
			default:
				GraphStyleUtils.setShape(n, defaultShape);
			}
		}
	}

	@Override
	public void onNodeWeightChange(Node n, Weight wNew, Weight wOld) {
		if (wNew instanceof NetworkWeight) {
			switch (((NetworkWeight) wNew).getWeight()) {
			case HOST:
				GraphStyleUtils.setShape(n, hostShape);
				break;
			case PORT:
				GraphStyleUtils.setShape(n, portShape);
				colorPortNode(n);
				break;
			default:
				GraphStyleUtils.setShape(n, defaultShape);
			}
		}
	}

	/** Sets the blue-part of the node color. **/
	protected void colorPortNode(Node n) {
		Color c = GraphStyleUtils.getColor(n);
		if (c != null)
			GraphStyleUtils.setColor(n, new Color(c.getRed(), c.getGreen(),
					portBlue));
	}

	@Override
	public String toString() {
		return "NetworkNodeShape-Rule: '" + this.name + "'";
	}

	/*
	 * UN-IMPORTANT
	 */

	@Override
	public void onNodeRemoval(Node n) {
		// DO NOTHING
	}

	@Override
	public void onEdgeAddition(Edge e, Node n1, Node n2) {
		// DO NOTHING
	}

	@Override
	public void onEdgeRemoval(Edge e, Node n1, Node n2) {
		// DO NOTHING
	}

	@Override
	public void onEdgeWeightChange(Edge e, Weight wNew, Weight wOld) {
		// DO NOTHING
	}

}
