package dna.visualization.graph.ToolTip.button;

import org.graphstream.ui.spriteManager.Sprite;

import dna.visualization.graph.GraphVisualization;
import dna.visualization.graph.ToolTip.ToolTip;

/**
 * Button is a wrapper-class for the GraphStream Sprite class. It is also an
 * abstract class that defines basic methods and operations a Button inside a
 * GraphVis-ToolTip should possess.
 * 
 * @author Rwilmes
 * @date 15.12.2015
 */
public abstract class Button extends ToolTip {

	/**
	 * The constructor.
	 * 
	 * @param s
	 *            The GraphStream Sprite the button will wrap.
	 * @param name
	 *            Name of the button. Will be used for identification.
	 * @param attachementId
	 *            The id of the GraphStream object the Button will be attached
	 *            too. E.g. the node the tooltip will show up at.
	 */
	public Button(Sprite s, String name, String attachementId) {
		this.s = s;
		setName(name);
		setType();
		attachToNode(attachementId);

		// store on sprite
		storeThisOnSprite();
	}

	/** Sets the label of the Button. **/
	public void setLabel(String label) {
		this.s.setAttribute(GraphVisualization.labelKey, label);
	}

	/** Returns the ToolTipType. Override to return a specific type. **/
	public abstract ToolTipType getType();

	/** Returns the default label. **/
	protected abstract String getDefaultLabel();

	/** Returns the label when the button is pressed. **/
	protected abstract String getPressedLabel();

	/** Sets the default style. **/
	public void setDefaultStyle() {
		this.s.setAttribute(GraphVisualization.styleKey, getDefaultStyle());
		setLabel(getDefaultLabel());
	}

	/** Sets the pressed style. **/
	public void setPressedStyle() {
		this.s.setAttribute(GraphVisualization.styleKey, getPressedStyle());
		setLabel(getPressedLabel());
	}

	/**
	 * Called when the button is clicked with the left mouse-button.
	 * 
	 * <p>
	 * 
	 * Use to add actual logic to your button.
	 * **/
	public abstract void onLeftClick();

	/**
	 * Called when the button is clicked with the right mouse-button.
	 * 
	 * <p>
	 * 
	 * Use to add actual logic to your button.
	 * **/
	public abstract void onRightClick();

	/**
	 * Returns the default-style.
	 * 
	 * <p>
	 * 
	 * Override to use other styles.
	 **/
	protected abstract String getDefaultStyle();

	/**
	 * Returns the style when the button is pressed.
	 * 
	 * <p>
	 * 
	 * Override to use other styles.
	 **/
	protected abstract String getPressedStyle();

	/** The default style. **/
	protected static final String defaultStyle = "" + "shape:rounded-box; "
			+ "size:100px,30px; " + "fill-mode:plain; "
			+ "fill-color: rgba(155,155,155, 150); " + "stroke-mode:dots; "
			+ "stroke-color: rgb(40, 40, 40); " + "text-alignment:center;";

	/** The default style when the button is shown pressed. **/
	protected static final String pressedStyle = "" + "shape:rounded-box; "
			+ "size:100px,30px; " + "fill-mode:plain; "
			+ "fill-color: rgba(200,0,0, 150); " + "stroke-mode:dots; "
			+ "stroke-color: rgb(40, 40, 40); " + "text-alignment:center;";

}
