package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.layout.GroupLayout;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileDrop;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.ScalaFile;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

public final class AnalysisPanel extends JPanel implements ScaleChangedListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4447342085724047798L;

	private static final Logger LOG = Logger.getLogger(AnalysisPanel.class.getName());

	private AudioFile audioFile;
	private double[] scale;

	public AnalysisPanel() {
		super(new BorderLayout());

		final JPopupMenu popup = new JPopupMenu();
		setComponentPopupMenu(popup);

		scaleChangedListeners = new ArrayList<ScaleChangedListener>();
		audioFileChangedListeners = new ArrayList<AudioFileChangedListener>();

		addFileDropListener();

		final ToneScalePane toneScalePane = new ToneScalePane(new ToneScaleHistogram(), this);
		addAudioFileChangedListener(toneScalePane);
		addScaleChangedListener(toneScalePane);

		final ToneScalePanel ambitusPanel = new ToneScalePanel(new AmbitusHistogram(), this);
		addAudioFileChangedListener(ambitusPanel);
		addScaleChangedListener(ambitusPanel);

		final PitchContour pitchContourPanel = new PitchContour();
		addAudioFileChangedListener(pitchContourPanel);
		addScaleChangedListener(pitchContourPanel);

		final IntervalTable intervalTable = new IntervalTable();
		addScaleChangedListener(intervalTable);

		final ControlPanel controlPanel = new ControlPanel();
		addAudioFileChangedListener(controlPanel);
		controlPanel.addHandler(toneScalePane);
		controlPanel.addHandler(ambitusPanel);
		controlPanel.addHandler(pitchContourPanel);

		final KeyboardPanel keyboardPanel = new KeyboardPanel();
		addScaleChangedListener(keyboardPanel);

		final JPanel dynamicPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(dynamicPanel, BoxLayout.Y_AXIS);
		dynamicPanel.setLayout(boxLayout);
		// toneScalePane.setAlignmentX(Component.CENTER_ALIGNMENT);
		dynamicPanel.add(toneScalePane);

		JPanel layersJPanel = new JPanel(new BorderLayout());
		layersJPanel.setInheritsPopupMenu(true);

		Component histogramComponent = new JLabel();

		AudioFileBrowserPanel browser = new AudioFileBrowserPanel(new GridLayout(0, 2));
		addAudioFileChangedListener(browser);
		browser.setBorder(new EmptyBorder(0, 0, 0, 0));
		browser.setBackground(Color.WHITE);

		final JCheckBox toneScaleCheckBox = new JCheckBox("Tone scale");
		toneScaleCheckBox.getModel().setSelected(true);
		toneScaleCheckBox.addActionListener(new ShowPanelActionListener(dynamicPanel, toneScalePane, this));

		final JCheckBox ambitusCheckBox = new JCheckBox("Ambitus");
		ambitusCheckBox.addActionListener(new ShowPanelActionListener(dynamicPanel, ambitusPanel, this));

		final JCheckBox keyboardCheckBox = new JCheckBox("Keyboard");
		keyboardCheckBox.addActionListener(new ShowPanelActionListener(dynamicPanel, keyboardPanel, this));

		final JCheckBox intervalTableCheckBox = new JCheckBox("Interval table");
		intervalTableCheckBox
				.addActionListener(new ShowPanelActionListener(dynamicPanel, intervalTable, this));

		final JCheckBox pitchContourCheckBox = new JCheckBox("Annotations");
		pitchContourCheckBox.addActionListener(new ShowPanelActionListener(dynamicPanel, pitchContourPanel,
				this));

		final JCheckBox controlsCheckBox = new JCheckBox("Controls");
		controlsCheckBox.addActionListener(new ShowPanelActionListener(dynamicPanel, controlPanel, this));

		final JPanel checkBoxPanel = new JPanel();
		GroupLayout layout = new GroupLayout(checkBoxPanel);
		checkBoxPanel.setLayout(layout);
		checkBoxPanel.setBorder(new TitledBorder("View"));

		layout.setHorizontalGroup(layout
				.createSequentialGroup()
				.add(layout.createParallelGroup(GroupLayout.LEADING).add(ambitusCheckBox)
						.add(keyboardCheckBox).add(controlsCheckBox))
				.add(layout.createParallelGroup(GroupLayout.LEADING).add(intervalTableCheckBox)
						.add(pitchContourCheckBox).add(toneScaleCheckBox)));

		layout.setVerticalGroup(layout
				.createSequentialGroup()
				.add(layout.createParallelGroup(GroupLayout.BASELINE).add(ambitusCheckBox)
						.add(pitchContourCheckBox))
				.add(layout.createParallelGroup(GroupLayout.BASELINE).add(intervalTableCheckBox)
						.add(keyboardCheckBox))
				.add(layout.createParallelGroup(GroupLayout.BASELINE).add(controlsCheckBox)
						.add(toneScaleCheckBox)));

		JScrollPane browserScollPane = new JScrollPane(browser);
		browserScollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		browserScollPane.getViewport().setBackground(Color.WHITE);

		layersJPanel.add(histogramComponent, BorderLayout.NORTH);
		layersJPanel.add(browserScollPane, BorderLayout.CENTER);
		layersJPanel.add(checkBoxPanel, BorderLayout.SOUTH);
		layersJPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, layersJPanel,
				dynamicPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setBorder(new EmptyBorder(0, 0, 0, 0));

		add(splitPane, BorderLayout.CENTER);
	}

	private class ShowPanelActionListener implements ActionListener {
		private final JComponent parent;
		private final JComponent panel;
		private final JComponent toValidate;

		public ShowPanelActionListener(final JComponent parentComponent, final JComponent panelToShow,
				final JComponent componentToValidate) {
			this.parent = parentComponent;
			this.panel = panelToShow;
			this.toValidate = componentToValidate;
		}

		public void actionPerformed(final ActionEvent e) {
			final JCheckBox checkBox = (JCheckBox) e.getSource();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (checkBox.getModel().isSelected()) {
						panel.setAlignmentX(Component.CENTER_ALIGNMENT);
						parent.add(panel);
					} else {
						parent.remove(panel);
					}
					toValidate.validate();
				}
			});
		}
	}

	private void addFileDropListener() {
		new FileDrop(this, new FileDrop.Listener() {
			public void filesDropped(final java.io.File[] files) {
				if (files.length != 1) {
					LOG.log(Level.WARNING, String.format(
							"Dropped %s files. For the moment only ONE file should be dropped", files.length));
				}
				final File droppedFile = files[0];
				if (droppedFile.getName().endsWith(".scl")) {
					ScalaFile scalaFile = new ScalaFile(droppedFile.getAbsolutePath());
					scaleChanged(scalaFile.getPitches(), false);
				} else if (FileUtils.isAudioFile(droppedFile)) {
					final AudioFile newAudioFile = new AudioFile(droppedFile.getAbsolutePath());
					setAudioFile(newAudioFile);
				}
				LOG.fine(String.format("Dropped %s .", droppedFile.getAbsolutePath()));
			}
		});
	}

	/* -- Audio file publish subscribe -- */
	private final List<AudioFileChangedListener> audioFileChangedListeners;

	private void setAudioFile(final AudioFile newAudioFile) {
		this.audioFile = newAudioFile;
		notifyAudioFileChangedListeners();
	}

	private AudioFile getAudioFile() {
		return audioFile;
	}

	private void notifyAudioFileChangedListeners() {
		LOG.log(Level.FINE,
				String.format("Notify listeners of audio file change: %s .", getAudioFile().basename()));
		for (AudioFileChangedListener listener : audioFileChangedListeners) {
			listener.audioFileChanged(getAudioFile());
		}
	}

	public synchronized void addAudioFileChangedListener(AudioFileChangedListener listener) {
		audioFileChangedListeners.add(listener);
	}

	/* -- Scale publish subscribe -- */
	private final List<ScaleChangedListener> scaleChangedListeners;

	private void notifyScaleChangedListeners(boolean isChanging) {
		LOG.log(Level.FINE,
				String.format("Notify listeners of scale change %s \t %s.", isChanging,
						Arrays.toString(getScale())));

		for (ScaleChangedListener listener : scaleChangedListeners) {
			listener.scaleChanged(getScale(), isChanging);
		}
	}

	public synchronized void addScaleChangedListener(ScaleChangedListener listener) {
		scaleChangedListeners.add(listener);
	}

	public void scaleChanged(double[] newScale, boolean isChanging) {
		scale = newScale;
		notifyScaleChangedListeners(isChanging);
	}

	private double[] getScale() {
		return scale;
	}

}
