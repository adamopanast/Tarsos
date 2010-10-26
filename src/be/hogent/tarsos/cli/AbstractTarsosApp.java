package be.hogent.tarsos.cli;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;

/**
 * @author Joren Six
 */
public abstract class AbstractTarsosApp {

    /**
     * Log messages.
     */
    private static final Logger LOG = Logger.getLogger(AbstractTarsosApp.class.getName());

    /**
     * @param args
     *            The arguments to start the program.
     */
    public abstract void run(final String... args);

    /**
     * @return The name of the parameter used to start the application.
     */
    public abstract String name();

    /**
     * @return The short description of the application. What purpose it serves.
     */
    public abstract String description();

    /**
     * Parses arguments, adds and checks for help option an prints command line
     * help for an application.
     * @param args
     *            The command line arguments (options).
     * @param parser
     *            The argument parser.
     * @param application
     *            The application that needs the parameters.
     * @return null if the arguments could not be parsed by parser. An OptionSet
     *         otherwise.
     */
    protected final OptionSet parse(final String[] args, final OptionParser parser,
            final AbstractTarsosApp application) {
        parser.acceptsAll(Arrays.asList("h", "?", "help"), "Show help");
        OptionSet options = null;
        try {
            options = parser.parse(args);
        } catch (final OptionException e) {
            final String message = e.getMessage();
            Tarsos.println(message);
            Tarsos.println("");
            printHelp(parser);
        }
        return options;
    }

    /**
     * Checks if the OptionSet contains the help argument.
     * @param options
     *            The options to check.
     * @return True if options is null or options contain help.
     */
    protected final boolean isHelpOptionSet(final OptionSet options) {
        return options == null || options.has("help");
    }

    /**
     * Creates an optionspec for a pitch detector.
     * @param parser
     *            The parser to add an option to.
     * @return An OptionSpec with a correct name and a clear description.
     */
    protected final OptionSpec<PitchDetectionMode> createDetectionModeSpec(final OptionParser parser) {
        final StringBuilder names = new StringBuilder();
        for (final PitchDetectionMode modes : PitchDetectionMode.values()) {
            names.append(modes.name()).append(" | ");
        }
        final String descr = "The detector to use [" + names.toString() + "]";
        return parser.accepts("detector", descr
        ).withRequiredArg().ofType(PitchDetectionMode.class).defaultsTo(
                PitchDetectionMode.TARSOS_YIN);

    }

    /**
     * Prints command line help for an application.
     * @param parser
     *            The command line argument parser.
     */
    protected final void printHelp(final OptionParser parser) {
        Tarsos.println("Application description");
        Tarsos.println("-----------------------");
        Tarsos.println(description());
        Tarsos.println("");
        try {
            parser.printHelpOn(System.out);
        } catch (final IOException e1) {
            LOG.log(Level.SEVERE, "Could not print to STD OUT. How quaint.", e1);
        }
    }
}