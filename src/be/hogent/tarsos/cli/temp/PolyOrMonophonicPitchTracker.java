package be.hogent.tarsos.cli.temp;

import java.util.List;

import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.TarsosPitchDetection;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

/**
 * @author Joren Six
 */
public final class PolyOrMonophonicPitchTracker {

    /**
     */
    private PolyOrMonophonicPitchTracker() {
    }

    /**
     * @param args
     *            Arguments.
     */
    public static void main(final String[] args) {
        final List<AudioFile> files = AudioFile.audioFiles("channels");
        for (final AudioFile file : files) {
            final PitchDetector detector = new TarsosPitchDetection(file, PitchDetectionMode.TARSOS_YIN);
            detector.executePitchDetection();
            final List<Annotation> samples = detector.getAnnotations();
            final AmbitusHistogram ambitusHistogram = Annotation.ambitusHistogram(samples);
            final ToneScaleHistogram toneScaleHistogram = ambitusHistogram.toneScaleHistogram();
            final String title = detector.getName() + "_" + file.basename();
            toneScaleHistogram.plot("data/tests/" + title + ".png", file.basename());
        }
    }

}