/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.noos.xing.mydoggy.Content;
import org.noos.xing.mydoggy.ContentManager;
import org.noos.xing.mydoggy.MultiSplitConstraint;
import org.noos.xing.mydoggy.MultiSplitContentManagerUI;
import org.noos.xing.mydoggy.TabbedContentManagerUI;
import org.noos.xing.mydoggy.TabbedContentUI;
import org.noos.xing.mydoggy.ToolWindow;
import org.noos.xing.mydoggy.ToolWindowManager;
import org.noos.xing.mydoggy.plaf.MyDoggyToolWindowManager;
import org.noos.xing.mydoggy.plaf.ui.content.MyDoggyMultiSplitContentManagerUI;

import be.hogent.tarsos.sampled.Player;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.AnnotationListener;
import be.hogent.tarsos.sampled.pitch.AnnotationPublisher;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;
import be.hogent.tarsos.ui.BackgroundTask;
import be.hogent.tarsos.ui.BackgroundTask.TaskHandler;
import be.hogent.tarsos.ui.ProgressDialog;
import be.hogent.tarsos.ui.WaveForm;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.Configuration.ConfigChangeListener;
import be.hogent.tarsos.util.FileDrop;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.JLabelHandler;
import be.hogent.tarsos.util.ScalaFile;
import be.hogent.tarsos.util.TextAreaHandler;

/**
 * @author Joren Six
 */
public final class Frame extends JFrame implements ScaleChangedListener, AnnotationListener {
	/**
	 * Default height.
	 */
	private static final int INITIAL_HEIGHT = 600;
	/**
	 * Default width.
	 */
	private static final int INITIAL_WIDTH = 800;
	/**
     */
	private static final long serialVersionUID = -8095965296377515567L;

	/**
	 * Logs messages.
	 */
	private static final Logger LOG = Logger.getLogger(Frame.class.getName());

	private static Frame instance;
	
	private final ToolWindowManager toolWindowManager;

	private AudioFile audioFile;

	private double[] scale;

	public static Frame getInstance() {
		if (instance == null) {
			instance = new Frame();
		}
		return instance;
	}

	/**
	 * Is there processing going on?
	 */
	private synchronized void setWaitState(final boolean isWaiting) {
		Component glassPane = this.getGlassPane();
		if (isWaiting) {
			glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			glassPane.setVisible(true);
		} else {
			glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			glassPane.setVisible(false);
		}
	}

	private Frame() {
		super("Tarsos");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		
		// center
		setLocationRelativeTo(null);
		Frame.setTarsosProgramIcon(this);

		// initialize listener lists
		scaleChangedListeners = new ArrayList<ScaleChangedListener>();
		audioFileChangedListeners = new ArrayList<AudioFileChangedListener>();

		// react to drag and drop 
		addFileDropListener();
		

		// all the components to add to the frame
		JComponent configurationPanel = makeConfigurationPanel();
		JComponent logPanel = makeLogPanel();
		JComponent helpPanel = makeHelpanel();
		JComponent headerPanel = new HeaderPanel();
		JComponent statusBar = makeStatusBar();
		PlayerControlPanel player = new PlayerControlPanel();

		//final PitchClassHistogramPanel pitchClassHistogramPanel = new PitchClassHistogramPanel(new PitchClassHistogram(), this);
		//final PitchClassHistogramPanel ambitusPanel = new PitchClassHistogramPanel(new PitchHistogram(), this);
		final PitchContour pitchContourPanel = new PitchContour();
		final PitchContour regression = new PitchContour();
		final IntervalTable intervalTable = new IntervalTable();
		final WaveForm waveForm = new WaveForm();
		final KeyboardPanel keyboardPanel = new KeyboardPanel();
		final Menu menu = new Menu(false);


		// The annotation publisher is not a ui element.
		final AnnotationPublisher annotationPublisher = AnnotationPublisher.getInstance();

		AudioFileBrowserPanel browser = new AudioFileBrowserPanel(new GridLayout(0, 2));
		browser.setBackground(Color.WHITE);
		
		//KDEData.getPitchClassHistogramInstance().setComponentToRepaint(pitchClassHistogramPanel);
		//KDEData.getPitchHistogramInstance().setComponentToRepaint(ambitusPanel);

		// patch the scale changed listeners
		//addScaleChangedListener(pitchClassHistogramPanel);
		//addScaleChangedListener(ambitusPanel);
		addScaleChangedListener(pitchContourPanel);
		addScaleChangedListener(regression);
		addScaleChangedListener(intervalTable);
		addScaleChangedListener(keyboardPanel);
		addScaleChangedListener(menu);


		// Patch the audio file changed listeners.
		//addAudioFileChangedListener(KDEData.getPitchClassHistogramInstance());
		//addAudioFileChangedListener(KDEData.getPitchHistogramInstance());
		
		//addAudioFileChangedListener(pitchClassHistogramPanel);
		//addAudioFileChangedListener(ambitusPanel);
		addAudioFileChangedListener(pitchContourPanel);
		addAudioFileChangedListener(regression);
		addAudioFileChangedListener(waveForm);
		addAudioFileChangedListener(browser);
		addAudioFileChangedListener(menu);
		addAudioFileChangedListener(player);


		// Patch the annotation listeners
		//annotationPublisher.addListener(KDEData.getPitchClassHistogramInstance());
		//annotationPublisher.addListener(KDEData.getPitchHistogramInstance());
		//annotationPublisher.addListener(pitchClassHistogramPanel);
		//annotationPublisher.addListener(ambitusPanel);
		annotationPublisher.addListener(pitchContourPanel);
		annotationPublisher.addListener(player);
		

		annotationPublisher.addListener(this);


		// initialize content and tool window manager of the 'mydoggy'
		// framework.
		// Create a new instance of MyDoggyToolWindowManager passing the frame.
		MyDoggyToolWindowManager windowManager = new MyDoggyToolWindowManager();
		toolWindowManager = windowManager;
		ContentManager contentManager = toolWindowManager.getContentManager();
		MultiSplitContentManagerUI contentManagerUI = new MyDoggyMultiSplitContentManagerUI();
		contentManager.setContentManagerUI(contentManagerUI);
		contentManagerUI.setShowAlwaysTab(true);
		contentManagerUI.setTabPlacement(TabbedContentManagerUI.TabPlacement.TOP);
		
		JPanel lowerPanel = new JPanel(new BorderLayout());
		lowerPanel.add(player,BorderLayout.NORTH);
		lowerPanel.add(new JSeparator() ,BorderLayout.CENTER);
		lowerPanel.add(statusBar,BorderLayout.SOUTH);
		lowerPanel.setBorder(new EmptyBorder(3, 0, 0, 0));

		// add the components to the frame
		add(headerPanel, BorderLayout.NORTH);
		add(windowManager, BorderLayout.CENTER);
		add(lowerPanel, BorderLayout.SOUTH);

		// add components to the window manager.

		Content content;
		/*
		 = contentManager.addContent("Pitch Class Histogram", "Pitch Class Histogram", null, pitchClassHistogramPanel);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(false);
		*/

		content = contentManager.addContent("Configuration", "Configuration", null, configurationPanel);
		MultiSplitConstraint constraint = new MultiSplitConstraint(content, 1);
		
		setDefaultTabbedContentOptions(content);
		content.setMinimized(true);
		
		/*
		constraint = new MultiSplitConstraint(content, 2);
		content = contentManager.addContent("Pitch Histogram", "Pitch Histogram", null, ambitusPanel, null, constraint);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(false);
		*/
		
		constraint = new MultiSplitConstraint(content, 3);
		content = contentManager.addContent("Interval table", "Interval table", null, intervalTable,null,constraint);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(true);

		
		content = contentManager.addContent("Waveform", "Waveform", null, waveForm);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(false);
		
		constraint = new MultiSplitConstraint(content, 1);
		content = contentManager.addContent("Annotations", "Annotations", null, pitchContourPanel, null,
				constraint);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(true);

		content = contentManager.addContent("Keyboard", "Keyboard", null, keyboardPanel, null);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(true);
		
		content = contentManager.addContent("Logging", "Logging", null, logPanel, null);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(true);
		
		
		//Disable the browser for now.
		//toolWindowManager.registerToolWindow("Player", "Player", null, player, ToolWindowAnchor.BOTTOM);
		//toolWindowManager.registerToolWindow("Help", "Help", null, helpPanel, ToolWindowAnchor.BOTTOM);
		;

		// Make all tools available
		for (ToolWindow window : toolWindowManager.getToolWindows()) {
			window.setAvailable(true);
		}
		
		//toolWindowManager.getToolWindow(1).setVisible(true);
		
		setJMenuBar(menu);
		
		setupChangeInPitchDetectors();
			
		//set initial scale:
		scaleChanged(ScalaFile.westernTuning().getPitches(), false, false);
		
		//restoreWorkspace();
		
		addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent arg0) {
				if(arg0.isControlDown() && arg0.getKeyChar() == '+'){
					Player.getInstance().increaseGain(0.05);
				} else if(arg0.isControlDown() && arg0.getKeyChar() == '-'){
					Player.getInstance().increaseGain(-0.05);
				}
			}
		});
	}
	
	/*
	private void storeWorkspace(){
		try {
		    FileOutputStream output = new FileOutputStream("workspace.xml");
		    toolWindowManager.getPersistenceDelegate().save(output);
		    output.close();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}
	
	private void restoreWorkspace(){
		PersistenceDelegate pstDelegate = toolWindowManager.getPersistenceDelegate();
		try {
		    FileInputStream inputStream = new FileInputStream("workspace.xml");
		    pstDelegate.apply(inputStream);
		    inputStream.close();
		} catch (Exception e1) {
		    e1.printStackTrace();
		}
	}
	*/

	private void setupChangeInPitchDetectors() {
		//react to change in pitch detectors
		Configuration.addListener(new ConfigChangeListener() {
			public void configurationChanged(ConfKey key) {
				if(key==ConfKey.pitch_tracker_list && audioFile != null){
					setNewAudioFile(new File(audioFile.originalPath()));
				}
			}
		});
	}

	private JComponent makeStatusBar() {
		JLabel statusBarLabel = new JLabel();
		statusBarLabel.setForeground(Color.GRAY);
		JLabelHandler.setupLoggerHandler(statusBarLabel);
		statusBarLabel.setBorder(new EmptyBorder(3, 3, 0, 3));
		return statusBarLabel;
	}

	private void setDefaultTabbedContentOptions(final Content content) {
		TabbedContentUI tabbedContent = (TabbedContentUI) content.getContentUI();
		tabbedContent.setCloseable(false);
		tabbedContent.setDetachable(true);
		tabbedContent.setTransparentMode(false);
		tabbedContent.setMinimizable(true);		
	}

	public static void setTarsosProgramIcon(JFrame frame) {
		try {
			final BufferedImage image;
			final String iconPath = "/be/hogent/tarsos/ui/resources/tarsos_logo_small.png";
			image = ImageIO.read(frame.getClass().getResource(iconPath));
			frame.setIconImage(image);
		} catch (IOException e) {
			// fail silently, a lacking icon is not that bad
			LOG.warning("Failed to set program icon");
		}
	}

	private JComponent makeLogPanel() {
		JTextArea output = new JTextArea();
		output.setFont(new Font("Monospaced", Font.PLAIN, 12));
		output.setAutoscrolls(true);
		output.setEditable(false);
		output.setLineWrap(true);
		TextAreaHandler.setupLoggerHandler(output);
		output.setBorder(new EmptyBorder(5, 2, 5, 2));
		JScrollPane scrollPane = new JScrollPane(output);
		scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		return scrollPane;
	}

	private JComponent makeHelpanel() {
		String contents = FileUtils.readFileFromJar("/be/hogent/tarsos/ui/resources/help.html");
		JEditorPane helpLabel = new JEditorPane();
		helpLabel.setEditable(false);
		helpLabel.setContentType("text/html");
		helpLabel.setPreferredSize(new Dimension(500, 300));
		helpLabel.setText(contents);
		helpLabel.setCaretPosition(0);
		return new JScrollPane(helpLabel);
	}

	public static ImageIcon createImageIcon(String path) {
		URL imgURL = Frame.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			LOG.warning(String.format("Unable to find icon: %s", imgURL));
			return null;
		}
	}

	protected JComponent makeConfigurationPanel() {
		JScrollPane configurationPane = new JScrollPane(new ConfigurationPanel());
		configurationPane.getVerticalScrollBar().setUnitIncrement(16);
		return configurationPane;
	}

	private void addFileDropListener() {
		new FileDrop(this, new FileDrop.Listener() {
			public void filesDropped(final java.io.File[] files) {
				for(final File file : files){
					setNewFile(file);
					LOG.fine(String.format("Dropped %s .", file.getAbsolutePath()));
				}
			}
		});
	}

	/* -- Audio file publish subscribe -- */
	private final List<AudioFileChangedListener> audioFileChangedListeners;

	private void setAudioFile(final AudioFile newAudioFile) {
		this.audioFile = newAudioFile;
	}
	
	private void notifyAudioFileChange(){
		// set a title
		this.setTitle("Tarsos " + audioFile.originalBasename());
		notifyAudioFileChangedListeners();
	}
	
	/**
	 * Set a new audio or scala file.
	 * @param newFile The new audio or scala file.
	 */
	public void setNewFile(final File newFile){
		LOG.info("Opening: " + newFile.getName());
		
		//Keep a list of recent files for the recent file menu.
		List<String> recentFiles = Configuration.getList(ConfKey.file_recent);
		String path = newFile.getAbsolutePath();
		int numberOfRecentFiles = 9;
		if(recentFiles.contains(path)){
			int index = recentFiles.indexOf(path);
			recentFiles.remove(index);
			recentFiles.add(0,path);
		} else {
			recentFiles.add(0,path);
			while(recentFiles.size() > numberOfRecentFiles){
				recentFiles.remove(numberOfRecentFiles);
			}
		}		
		Configuration.set(ConfKey.file_recent,recentFiles);
		
		//add one scala file
		if (newFile.getName().toLowerCase().endsWith(".scl")) {
			ScalaFile scalaFile = new ScalaFile(newFile.getAbsolutePath());
			scaleChanged(scalaFile.getPitches(), false, false);
		} else if (FileUtils.isAudioFile(newFile)) {//add one audio file
			setNewAudioFile(newFile);
		//find closest in scala directory	
		} else if(newFile.isDirectory() && FileUtils.glob(newFile.getAbsolutePath(),".*\\.scl",false).size()!=0 ) {
			findClosestScalaFile(newFile);
		//add audio files in directory recursively
		} else if(newFile.isDirectory()){
			String pattern = Configuration.get(ConfKey.audio_file_name_pattern);
			List<String> audioFiles = FileUtils.glob(newFile.getAbsolutePath(), pattern, true);
			for(String file:audioFiles){
				setNewAudioFile(new File(file));
			}
		}else{	
			LOG.warning("Unrecognized file: " + newFile.getAbsolutePath());
		}	
	}
	
	/**
	 * Finds the scala file closest to the currently detected scale in a directory with scala files.
	 * @param directory
	 */
	private void findClosestScalaFile(File directory){
		List<ScalaFile> haystack = new ArrayList<ScalaFile>();
		for(String scalaFileName : FileUtils.glob(directory.getAbsolutePath(),".*\\.scl",false)){
			haystack.add(new ScalaFile(scalaFileName));
		}
		ScalaFile needle = new ScalaFile("",scale);
		ScalaFile closest = needle.findClosest(haystack);
		scale = closest.getPitches();
		LOG.info("Closest scala file: " + closest.getDescription());
		for(ScaleChangedListener listeners : scaleChangedListeners){
			listeners.scaleChanged(scale, false, true);
		}
	}
	
	private void setNewAudioFile(final File newFile){
		AnnotationPublisher.getInstance().clearTree();
		TranscodingTask transcodingTask = new TranscodingTask(newFile);
		List<BackgroundTask> detectorTasks = createTasks(newFile,transcodingTask);
		transcodingTask.addHandler(new TaskHandler() {

			public void taskInterrupted(BackgroundTask backgroundTask, Exception e) {
				
			}
			

			public void taskDone(BackgroundTask backgroundTask) {
				if(backgroundTask instanceof TranscodingTask){
					setAudioFile(((TranscodingTask) backgroundTask).getAudioFile());
				}
			}
		});
		String title = "Progress: " + FileUtils.basename(newFile.getAbsolutePath());
		
		AnnotationPublisher.getInstance().clear();
		AnnotationPublisher.getInstance().extractionStarted();
		final ProgressDialog dialog = new ProgressDialog(this,title,transcodingTask,detectorTasks);
		dialog.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if(evt.getPropertyName().equals("allTasksFinished")){
					notifyAudioFileChange();
					AnnotationPublisher.getInstance().extractionFinished();
				}
			}
		});
		dialog.pack();
		dialog.setVisible(true);			
	}
	
	private List<BackgroundTask> createTasks(final File audioFile,final TranscodingTask transcodingTask){
		final List<BackgroundTask> detectorTasks = new ArrayList<BackgroundTask>();
		for (final String name : Configuration
				.getList(ConfKey.pitch_tracker_list)) {
			final PitchDetectionMode mode = PitchDetectionMode
					.valueOf(name);
			final boolean determinatedLength = (mode == PitchDetectionMode.TARSOS_MPM || PitchDetectionMode.TARSOS_YIN == mode || PitchDetectionMode.TARSOS_DYNAMIC_WAVELET == mode);
			DetectorTask task = new DetectorTask(mode.getDetectionModeName() + " " + FileUtils.basename(audioFile.getAbsolutePath()), determinatedLength, mode);
			transcodingTask.addHandler(task);
			detectorTasks.add(task);
		}
		return detectorTasks;
	}
	
	private class TranscodingTask extends BackgroundTask {

		private final File newFile;
		AudioFile transcodedAudioFile;
		
		protected TranscodingTask(final File file) {
			super("Transcoding " + FileUtils.basename(file.getAbsolutePath()), false);
			newFile = file;
		}

		@Override
		public Void doInBackground() {
			Runnable runTranscoder = new Runnable(){
				public void run() {
					try {
						transcodedAudioFile = new AudioFile(newFile.getAbsolutePath());
					} catch (EncoderException e) {
						interrupt(TranscodingTask.this, e);
					}
				}
			};
			//Do the actual detection in the background
			Thread t = new Thread(runTranscoder, getName());
			t.start();
			setProgress(0);
			while (t.isAlive()) {
				try {
					setProgress(50);
					Thread.sleep(30);
				} catch (InterruptedException e) {
				}
			}
			setProgress(100);
			return null;
		}

		public AudioFile getAudioFile() {
			return transcodedAudioFile;
		}
	}
	
	private class DetectorTask extends BackgroundTask implements TaskHandler{
		private AudioFile file;
		private final PitchDetectionMode mode;
		
		protected DetectorTask(String name, boolean lengthDetermined, PitchDetectionMode detectionMode) {
			super(name, lengthDetermined);
			mode = detectionMode;
		}

		@Override
		public Void doInBackground() {
			final PitchDetector pitchDetector = mode
					.getPitchDetector(file);
			Runnable r = new Runnable() {
				public void run() {
					//Do pitch extraction
					AnnotationPublisher publisher = AnnotationPublisher.getInstance();
					List<Annotation> annotations = pitchDetector.executePitchDetection();
					publisher.addAnnotations(annotations);
				}
			};
			//Do the actual detection in the background
			Thread t = new Thread(r, getName());
			t.start();
			
			setProgress(0);
			// So progress can be updated in this thread, every 30ms.
			while (t.isAlive() && !isCancelled()) {
				try {
					if(lengthIsDetermined()){
						setProgress((int) (pitchDetector.progress() * 100));
					}else{
						setProgress(50);
					}
					Thread.sleep(30);
				} catch (InterruptedException e) {
				}
			}
			if(isCancelled()){
				t.interrupt();
			} else {
				setProgress(100);
			}
			return null;
		}

		public void taskDone(BackgroundTask backgroundTask) {
			if(backgroundTask instanceof TranscodingTask){
				file = ((TranscodingTask) backgroundTask).getAudioFile();
			}
		}

		public void taskInterrupted(BackgroundTask backgroundTask, Exception e) {
			//transcoding interrupted!
		}
	}
	

	private AudioFile getAudioFile() {
		return audioFile;
	}

	private void notifyAudioFileChangedListeners() {
		LOG.log(Level.FINE,
				String.format("Notify listeners of audio file change: %s .", getAudioFile().originalBasename()));
		for (AudioFileChangedListener listener : audioFileChangedListeners) {
			listener.audioFileChanged(getAudioFile());
		}
	}

	public synchronized void addAudioFileChangedListener(AudioFileChangedListener listener) {
		audioFileChangedListeners.add(listener);
	}
	
	public synchronized void removeAudioFileChangedListener(AudioFileChangedListener listener) {
		audioFileChangedListeners.remove(listener);
	}

	/* -- Scale publish subscribe -- */
	private final List<ScaleChangedListener> scaleChangedListeners;

	private void notifyScaleChangedListeners(boolean isChanging) {
		LOG.log(Level.FINE,
				String.format("Notify listeners of scale change %s \t %s.", isChanging,
						Arrays.toString(getScale())));

		for (ScaleChangedListener listener : scaleChangedListeners) {
			listener.scaleChanged(getScale(), isChanging, false);
		}
	}

	public synchronized void addScaleChangedListener(ScaleChangedListener listener) {
		scaleChangedListeners.add(listener);
	}

	public void scaleChanged(double[] newScale, boolean isChanging, boolean shiftHisto) {
		scale = newScale;
		notifyScaleChangedListeners(isChanging);
	}

	private double[] getScale() {
		return scale;
	}

	public void addAnnotations(List<Annotation> annotations) {
		// NO OP
	}

	public void clearAnnotations() {
		// NO OP
	}
	
	public void extractionStarted() {
		// Show waiting cursor.
		setWaitState(true);
	}

	public void extractionFinished() {
		// Show normal cursor again.
		
		setWaitState(false);
		LOG.fine("Wait state disabled.");
	}

	public void addAnnotation(Annotation annotation) {
		// NO OP

	}

	public void annotationsAdded() {
		// TODO Auto-generated method stub

	}
}
