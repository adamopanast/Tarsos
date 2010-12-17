package be.hogent.tarsos.ui.pitch;

import be.hogent.tarsos.sampled.pitch.Annotation;

/**
 * Elements interested in representing annotations should implement this
 * interface.
 * 
 * The implementor should be able to clear all annotations and reset a subset of
 * annotations rather quickly.
 */
public interface AnnotationListener {
	/**
	 * Add an annotation to the element.
	 * 
	 * @param annotation
	 *            The annotation to add: this method is called a lot. After a
	 *            clearAnnotations() call a very large number of annotations is
	 *            possible. So efficiently adding annotations should be possible
	 *            or threaded.
	 */
	void addAnnotation(Annotation annotation);

	/**
	 * Clears all annotations.
	 */
	void clearAnnotations();

	/**
	 * A hook to react to annotation extraction. This method is called when an
	 * audio file is dropped and extraction of pitch annotations starts.
	 */
	void extractionStarted();

	/**
	 * A hook to react to annotation extraction. This method is called when an
	 * audio file is dropped and extraction of pitch annotations is completed.
	 */
	void extractionFinished();

	/**
	 * Is called after a list of annotations is added.
	 */
	void annotationsAdded();
}