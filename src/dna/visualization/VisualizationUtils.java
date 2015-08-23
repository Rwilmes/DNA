package dna.visualization;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.monte.media.Buffer;
import org.monte.media.Format;
import org.monte.media.FormatKeys.MediaType;
import org.monte.media.avi.AVIWriter;
import org.monte.media.math.Rational;

import dna.util.Config;
import dna.util.Log;
import dna.visualization.graph.GraphPanel;

public class VisualizationUtils {

	/** Captures a screenshot from the given JFrame. **/
	public static BufferedImage captureScreenshot(JFrame frame) {
		try {
			Robot robot = new Robot();

			// get bounds from parentFrame
			Rectangle captureRect = frame.getBounds();
			BufferedImage screenFullImage = robot
					.createScreenCapture(captureRect);

			return screenFullImage;
		} catch (AWTException ex) {
			System.err.println(ex);
		}

		return null;
	}

	public static VideoRecorder captureVideoInNewThread(JFrame srcFrame) {
		VideoRecorder rec = new VideoRecorder(null, srcFrame);
		rec.start();
		return rec;
	}

	/** Captures a video from the given JFrame. **/
	public static void captureVideo(JFrame srcFrame)
			throws InterruptedException, IOException {
		VisualizationUtils.captureVideo(srcFrame,
				VisualizationUtils.getVideoPath(srcFrame.getTitle()));
	}

	/** Captures a video from the given JFrame to the destination-path. **/
	public static void captureVideo(JFrame srcFrame, String dstPath)
			throws InterruptedException, IOException {
		VisualizationUtils.captureVideo(srcFrame, dstPath,
				Config.getInt("GRAPH_VIS_VIDEO_MAXIMUM_LENGTH_IN_SECONDS"),
				Config.getInt("GRAPH_VIS_VIDEO_DEFAULT_FPS"));
	}

	/** Captures a video from the given JFrame to the destination-path. **/
	public static void captureVideo(JFrame srcFrame, String dstPath,
			int timeInSeconds, int fps) throws InterruptedException,
			IOException {
		Log.info("capturing " + timeInSeconds + "s video from '"
				+ srcFrame.getTitle() + "'");
		long screenshotInterval = (long) Math.floor(1000 / fps);

		int amount = timeInSeconds * fps;

		BufferedImage[] images = new BufferedImage[amount];

		for (int i = 0; i < amount; i++) {
			long start = System.currentTimeMillis();
			images[i] = VisualizationUtils.captureScreenshot(srcFrame);
			long diff = System.currentTimeMillis() - start;
			if (diff < screenshotInterval)
				Thread.sleep(screenshotInterval - diff);
		}

		File f = new File(dstPath);
		Log.info("rendering video to " + dstPath);
		VisualizationUtils.renderVideo(f, images);
		Log.info("video rendering done");
		images = null;
		System.gc();
	}

	/** Renders a video from the given jpeg frames. **/
	public static void renderVideo(File file, BufferedImage[] frames)
			throws IOException {
		VisualizationUtils.renderVideo(file, frames,
				Config.getInt("GRAPH_VIS_VIDEO_DEFAULT_FPS"),
				Config.getBoolean("GRAPH_VIS_VIDEO_USE_TECHSMITH"));
	}

	/** Renders a video from the given jpeg frames. **/
	public static void renderVideo(File file, BufferedImage[] frames, int fps)
			throws IOException {
		VisualizationUtils.renderVideo(file, frames, fps,
				Config.getBoolean("GRAPH_VIS_VIDEO_USE_TECHSMITH"));
	}

	/** Renders a video from the given jpeg frames. **/
	public static void renderVideo(File file, BufferedImage[] frames,
			boolean useTecHSmithCodec) throws IOException {
		VisualizationUtils
				.renderVideo(file, frames,
						Config.getInt("GRAPH_VIS_VIDEO_DEFAULT_FPS"),
						useTecHSmithCodec);
	}

	/** Renders a video from the given jpeg frames. **/
	public static void renderVideo(File file, BufferedImage[] frames, int fps,
			boolean useTechSmithCodec) throws IOException {
		// MovieWriter out = Registry.getInstance().getWriter(file);
		AVIWriter out = new AVIWriter(file);

		Object encodingFormat;

		if (useTechSmithCodec)
			encodingFormat = org.monte.media.VideoFormatKeys.ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE;
		else
			encodingFormat = org.monte.media.VideoFormatKeys.ENCODING_AVI_MJPG;

		Format format = new Format(
				org.monte.media.VideoFormatKeys.MediaTypeKey, MediaType.VIDEO,
				org.monte.media.VideoFormatKeys.EncodingKey, encodingFormat,
				org.monte.media.VideoFormatKeys.FrameRateKey, new Rational(fps,
						1), org.monte.media.VideoFormatKeys.WidthKey,
				frames[0].getWidth(),
				org.monte.media.VideoFormatKeys.HeightKey,
				frames[0].getHeight(),
				org.monte.media.VideoFormatKeys.DepthKey, 24);
		int track = out.addTrack(format);

		try {
			out.addTrack(format);

			Buffer buf = new Buffer();
			buf.format = new Format(
					org.monte.media.VideoFormatKeys.DataClassKey,
					BufferedImage.class);
			buf.sampleDuration = format.get(
					org.monte.media.VideoFormatKeys.FrameRateKey).inverse();
			for (int i = 0; i < frames.length; i++) {
				buf.data = frames[i];
				out.write(track, buf);
			}
		} finally {
			out.close();
		}
	}

	/** Returns the video path for the name. **/
	public static String getVideoPath(String name) {
		// craft filename
		String dir = Config.get("GRAPH_VIS_VIDEO_DIR");
		String suffix = Config.get("GRAPH_VIS_VIDEO_SUFFIX");
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
		String filename = name + "-" + df.format(new Date());

		// get name
		File file = new File(dir + filename + suffix);
		int id = 0;
		while (file.exists()) {
			id++;
			file = new File(dir + filename + "_" + id + suffix);
		}

		return file.getPath();
	}

	/**
	 * VideoRecorder class is used to capture a video from a JFrame in a
	 * separate thread.
	 **/
	public static class VideoRecorder implements Runnable {

		protected Thread t;
		protected boolean running;

		protected JPanel callingPanel;

		protected JFrame srcFrame;
		protected String dstPath;
		protected int timeInSeconds;
		protected int fps;

		/** Run method. **/
		public void run() {
			try {
				captureVideo(srcFrame, dstPath, timeInSeconds, fps);

				if (this.callingPanel != null) {
					if (this.callingPanel instanceof GraphPanel)
						((GraphPanel) this.callingPanel).recordingStopped();

					// add other panels here for proper termination handling
				}
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}

			// end of run
			this.running = false;
			this.t = null;
			return;
		}

		/** Starts a new recording. **/
		public void start() {
			if (this.t == null) {
				Random random = new Random();
				this.running = true;
				this.t = new Thread(this, "VideoRecorder-Thread"
						+ random.nextFloat());
				this.t.start();
			}
		}

		/** Stops the current recording. **/
		public void stop() {
			this.running = false;
		}

		/** Updates the destination path. **/
		public void updateDestinationPath() {
			this.dstPath = VisualizationUtils.getVideoPath(this.srcFrame
					.getTitle());
		}

		public VideoRecorder(JPanel callingPanel, JFrame srcFrame) {
			this(callingPanel, srcFrame, VisualizationUtils
					.getVideoPath(srcFrame.getTitle()));
		}

		public VideoRecorder(JPanel callingPanel, JFrame srcFrame,
				String dstPath) {
			this(callingPanel, srcFrame, dstPath, Config
					.getInt("GRAPH_VIS_VIDEO_MAXIMUM_LENGTH_IN_SECONDS"),
					Config.getInt("GRAPH_VIS_VIDEO_DEFAULT_FPS"));
		}

		public VideoRecorder(JPanel callingPanel, JFrame srcFrame,
				String dstPath, int timeInSeconds, int fps) {
			this.callingPanel = callingPanel;
			this.srcFrame = srcFrame;
			this.dstPath = dstPath;
			this.timeInSeconds = timeInSeconds;
			this.fps = fps;
		}

		/** Update the progress at the calling panel. **/
		protected void updateVideoProgress(double percent) {
			if (this.callingPanel != null) {
				if (this.callingPanel instanceof GraphPanel)
					((GraphPanel) this.callingPanel)
							.updateVideoProgress(percent);

				// add other panels here for progress update broadcasting
			}
		}

		/** Update the elapsed time of the video recording. **/
		protected void updateElapsedVideoTime(int seconds) {
			if (this.callingPanel != null) {
				if (this.callingPanel instanceof GraphPanel)
					((GraphPanel) this.callingPanel)
							.updateElapsedVideoTime(seconds);

				// add other panels here for progress update broadcasting
			}
		}

		/** Captures a video from the given JFrame to the destination-path. **/
		protected void captureVideo(JFrame srcFrame, String dstPath,
				int timeInSeconds, int fps) throws InterruptedException,
				IOException {
			Log.info("GraphVis - capturing video to '" + dstPath + "'");
			long screenshotInterval = (long) Math.floor(1000 / fps);

			int amount = timeInSeconds * fps;

			BufferedImage[] images = new BufferedImage[amount];
			int counter = 0;
			int seconds = 0;
			for (int i = 0; i < amount; i++) {
				long start = System.currentTimeMillis();
				images[i] = VisualizationUtils.captureScreenshot(srcFrame);

				// update progress approx. each second
				counter++;
				if (counter == fps) {
					this.updateElapsedVideoTime(seconds);
					counter = 0;
					seconds++;
				}

				long diff = System.currentTimeMillis() - start;
				if (diff < screenshotInterval)
					Thread.sleep(screenshotInterval - diff);

				if (!this.running)
					break;
			}

			File f = new File(dstPath);
			Log.info("rendering video to " + dstPath);
			VisualizationUtils.renderVideo(f, images);
			Log.info("video rendering done");

			images = null;
			System.gc();
		}
	}

}