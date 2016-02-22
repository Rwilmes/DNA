package dna.plot;

/**
 * Wrapper-class for the labels one can set in gnuplot via set label.<br>
 * <br>
 * 
 * The getLine()-method will return the line to be added to the gnuplot script.
 * 
 * @author Rwilmes
 * 
 */
public class PlotLabel {

	public enum Orientation {
		left, center, middle
	}

	private int tag;
	private String text;

	private double posX;
	private double posY;
	private double offset;

	private Orientation orientation;

	private boolean rotate;
	private double rotationDegree;

	private String fontName;
	private double fontSize;

	private boolean noenhanced;
	private boolean foreground;

	private String colorSpec;
	private String pointStyle;

	public PlotLabel(String text, double posX) {
		this(text, posX, 0);
	}

	public PlotLabel(String text, double posX, double posY) {
		this(text, posX, posY, null, false, 0, 0);
	}

	public PlotLabel(String text, double posX, double posY,
			Orientation orientation, boolean rotate, double rotation,
			double offset) {
		this(-1, text, posX, posY, orientation, rotate, rotation, null, 0,
				false, false, null, null, 0);
	}

	public PlotLabel(Integer tag, String text, double posX, double posY,
			Orientation orientation, boolean rotate, double rotationDegree,
			String fontName, double fontSize, boolean noenhanced,
			boolean foreground, String textColor, String pointStyle,
			double offset) {
		this.tag = tag;
		this.text = text;
		this.posX = posX;
		this.posY = posY;
		this.orientation = orientation;
		this.rotationDegree = rotationDegree;
		this.offset = offset;

		this.fontName = fontName;
		this.fontSize = fontSize;

		this.noenhanced = noenhanced;
		this.foreground = foreground;

		this.colorSpec = textColor;
		this.pointStyle = pointStyle;
	}

	public String getLine() {
		String buff = "set label";
		if (tag >= 0)
			buff += " " + tag;

		buff += " " + '"' + text + '"';
		buff += " " + "at" + " " + posX + "," + posY;

		if (orientation != null)
			buff += " " + orientation.toString();

		if (rotate)
			buff += " " + "rotate by" + " " + rotationDegree;

		if (fontName != null) {
			buff += " " + fontName;
			if (fontSize > 0)
				buff += "," + fontSize;
		}

		if (noenhanced)
			buff += " " + "noenhanced";

		if (foreground)
			buff += " " + "front";

		if (colorSpec != null)
			buff += " " + "textcolor" + " " + colorSpec;

		if (pointStyle != null)
			buff += " " + "point" + " " + pointStyle;

		if (offset > 0)
			buff += " " + "offset" + " " + offset;
		return buff;
	}
}