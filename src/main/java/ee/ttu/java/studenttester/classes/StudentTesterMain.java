package ee.ttu.java.studenttester.classes;
import java.io.File;
import java.util.Arrays;

/**
 *
 * @author Andres Antonen
 *
 */
public final class StudentTesterMain {

	/**
	 * Main entry point.
	 * @param args - see usage javadoc
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println(getUsage());
			System.exit(1);
		}
		StudentTesterClass c = new StudentTesterClass();
		try {
			for (int i = 0; i < args.length; i++) {
				// parse arguments and build tester
				switch (args[i].toLowerCase()) {
				case "-testroot":
					if (quickFileCheck(args[i + 1], true)) {
						c.setTestRootName(args[i + 1]);
						i++;
					} else {
						System.err.println("Could not find test root folder: " + args[i + 1]);
						System.exit(1);
					}
					break;
				case "-contentroot":
					if (quickFileCheck(args[i + 1], true)) {
						c.setContentRootName(args[i + 1]);
						i++;
					} else {
						System.err.println("Could not find content root folder: " + args[i + 1]);
						System.exit(1);
					}
					break;
				case "-temproot":
					c.setTempDirectoryName(args[i + 1]);
					i++;
					break;
				case "-jsonfile":
					c.setOutputFile(args[i + 1]);
					i++;
					c.outputJSON(true);
					break;
				case "-verbosity":
					try {
						int v = Integer.parseInt(args[i + 1]);
						c.setVerbosity(v);
						i++;
					} catch (Exception e) {
						System.err.println("Could not set verbosity level: " + e.getMessage());
					}
					break;
				case "-nocheckstyle":
					c.enableCheckstyle(false);
					break;
				case "-notestng":
					c.enableTestNG(false);
					break;
				case "-jsonoutput":
					c.outputJSON(true);
					break;
				case "-nomute":
					c.muteCodeOutput(false);
					break;
				case "-checkstylexml":
					if (quickFileCheck(args[i + 1], false)) {
						c.setCheckstyleXml(args[i + 1]);
						i++;
					} else {
						System.err.println("Could not find checkstyle xml: " + args[i + 1]);
					}
					break;
				case "-testngxml":
					if (quickFileCheck(args[i + 1], false)) {
						c.setTestNGXml(args[i + 1]);
						i++;
					} else {
						System.err.println("Could not find TestNG xml: " + args[i + 1]);
					}
					break;
				case "-javacoptions":
					c.setCompilerOptions(args[i + 1]);
					i++;
					break;
				default:
					System.err.println("Unknown argument: " + args[i]);
					System.err.println(getUsage());
					System.exit(1);
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println(getUsage());
			System.exit(1);
		}
		c.run();
	}

	/**
	 * Prints the instruction manual for this tool.
	 * @return text
	 */
	private static String getUsage() {
		return "Usage:\n"
				+ "StudentTester -testRoot [path] -contentRoot [path] [options]\n"
				+ "\nOptions:\n"
				+ "-tempRoot [path]\tSets the path to the temporary directory, default is system's temp folder\n"
				+ "-verbosity [level]\tSets the verbosity level, 10 is max, default is 0\n"
				+ "-nocheckstyle\t\tdisables checkstyle, default is false\n"
				+ "-notestng\t\tdisables TestNG, default is false\n"
				+ "-jsonoutput\t\tWrites results to JSON, default is false\n"
				+ "-jsonfile [path]\tWrites results to JSON file\n"
				+ "-nomute\t\t\tWrites code output to stderr instead of discarding, default is false\n"
				+ "-checkstylexml [path]\tSets the path to checkstyle XML file\n"
				+ "-testngxml [path]\tSets the path to TestNG test configuration\n"
				+ "-javacoptions [options]\tPasses additional flags to the compiler; multiple flags should be\n"
				+ "separated with spaces and quoted, e.g -javacoptions \"-Xlint:cast -Xlint:deprecation\""
				+ "\nNotes:\n"
				+ "By default XML files are used from testRoot directory.\n"
				+ "For now, the paths must be absolute.\n";
	}

	/**
	 * Checks if the path is a valid file.
	 * @param path - pathname to the file
	 * @param shouldBeFolder - whether the path should be folder
	 * @return true if the path is ok
	 */
	private static boolean quickFileCheck(final String path, final boolean shouldBeFolder) {
		File f = new File(path);
		if (f.exists() && f.isDirectory()) {
			if (shouldBeFolder) {
				return true;
			}
			return false;
		}
		if (f.exists() && f.isFile()) {
			if (shouldBeFolder) {
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Private dummy constructor.
	 */
	private StudentTesterMain() {
	}
}