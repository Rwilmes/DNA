package dna.visualization.graph.rules.nodes;

import org.graphstream.graph.Node;

import dna.graph.weights.Weight;
import dna.util.parameters.Parameter;
import dna.util.parameters.StringParameter;
import dna.visualization.graph.rules.GraphStyleRule;
import dna.visualization.graph.rules.GraphStyleUtils;

/**
 * A simple rule which adds the nodeweight to the label of the node.<br>
 * <br>
 * 
 * <b>Note:</b> When instantiating with a label-string, it should contain the
 * substring $weight$, which will be replaced by the actual weight.<br>
 * For example: String label = "w=$weight$" <br>
 * 
 * @author Rwilmes
 * 
 */
public class NodeWeightLabel extends GraphStyleRule {

	public static final String weightReplacement = "$weight$";

	protected String label;

	public NodeWeightLabel(String name) {
		this(name, new Parameter[0]);
	}

	public NodeWeightLabel(String name, String label) {
		this(name, new Parameter[] { new StringParameter("label", label) });
	}

	public NodeWeightLabel(String name, Parameter[] params) {
		this.name = name;
		this.label = "w=" + weightReplacement;

		for (Parameter p : params) {
			if (p.getName().toLowerCase().equals("label")) {
				this.label = p.getValue();
			}
		}
	}

	@Override
	public void onNodeAddition(Node n, Weight w) {
		if (w != null) {
			if (GraphStyleUtils.getLabel(n) == null)
				GraphStyleUtils.setLabel(n, craftLabel(w.asString()));
			else
				GraphStyleUtils.appendToLabel(n, ", "
						+ craftLabel(w.asString()));
		}
	}

	@Override
	public void onNodeWeightChange(Node n, Weight wNew, Weight wOld) {
		String oldLabel = GraphStyleUtils.getLabel(n);
		String newWeightLabel = craftLabel(wNew.asString());

		if (oldLabel != null && wOld != null
				&& oldLabel.contains(craftLabel(wOld.asString())))
			GraphStyleUtils.setLabel(n, oldLabel.replace(
					craftLabel(wOld.asString()), newWeightLabel));
		else
			GraphStyleUtils.appendToLabel(n, newWeightLabel);
	}

	@Override
	public String toString() {
		return "NodeWeightLabel-Rule: '" + this.label + "'";
	}

	protected String craftLabel(String weight) {
		return this.label.replace(weightReplacement, weight);
	}

}
