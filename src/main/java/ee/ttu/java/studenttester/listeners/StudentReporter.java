package ee.ttu.java.studenttester.listeners;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ee.ttu.java.studenttester.annotations.Gradeable;
import ee.ttu.java.studenttester.classes.*;
import ee.ttu.java.studenttester.enums.StudentPolicy;
import ee.ttu.java.studenttester.exceptions.StudentTesterException;
import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;

import ee.ttu.java.studenttester.annotations.TestContextConfiguration;
import ee.ttu.java.studenttester.dataclasses.SingleTest;
import ee.ttu.java.studenttester.dataclasses.TestResults;
import ee.ttu.java.studenttester.enums.ReportMode;
import ee.ttu.java.studenttester.interfaces.IBaseStudentReporter;
/**
 * Custom reporter class.
 * @author Andres
 *
 */
public final class StudentReporter implements IReporter, IBaseStudentReporter {

	/**
	 * Do not yield any results until the reporter has finished.
	 */
	private boolean reporterFinished = false;
	/**
	 * Should be set to true if compilation was partial.
	 */
	private boolean hasIncompleteResults = false;
	/**
	 * Temporary data class for test results.
	 */
	private TestResults results;
	/**
	 * Stores assertion errors etc for including in diagnostic test results.
	 */
	private List<String> unitTestNotes;
	/**
	 * Verbosity settings for test class.
	 */
	private ReportMode reportMode;
	/**
	 * Returns results to tester class.
	 * @return test results in TestResults format.
	 * @throws StudentTesterException if an error has occurred or the reported has not been run.
	 */
	public TestResults getResults() throws StudentTesterException {
		if (reporterFinished) {
			return results;
		}
		throw new StudentTesterException("The reporter has not successfully finished, no data to return.");
	}
	/**
	 * Lets the reporter know that the compilation was partial.
	 * @param hasIncompleteResults true if compilation was partial
	 */
	public void setHasIncompleteResults(final boolean hasIncompleteResults) {
		this.hasIncompleteResults = hasIncompleteResults;
	}
	/**
	 * Defines unit test outcomes.
	 */
	private static final int 	SUCCESS = 0,
								FAILURE = 1,
								SKIP = 2;

	@Override
	public void generateReport(final List<XmlSuite> xmlSuites, final List<ISuite> suites, final String outputDirectory) {
		results = new TestResults(); // prepare new test results object
		/**
		 * test counter for json
		 */
		int index = 1;
		/**
		 * overall total score
		 */
		double overallTotal = 0;
		/**
		 * overall passed score
		 */
		double overallPassed = 0;
		/**
		 * test output string
		 */
		String globalOutput = "";
		/**
		 * remove overall grade if grading is muted anywhere
		 */
		boolean hasMute = false;

		for (ISuite suite : suites) {

			// globalOutput += String.format("DummyTest suite \"%s\"\n", suite.getName());
			Map<String, ISuiteResult> suiteResults = suite.getResults();

			for (ISuiteResult sr : suiteResults.values()) {

				/**
				 * test output string for only this test
				 */
				String localOutput = "";

				ITestContext tc = sr.getTestContext();
				if (tc.getCurrentXmlTest().getClasses().size() > 1) {
					StudentLogger.log(String.format("DummyTest context %s contains %d classes. "
							+ "%s will be pulled from %s and it applies "
							+ "to ALL other classes in this test context.",
							tc.getName(),
							tc.getCurrentXmlTest().getClasses().size(),
							TestContextConfiguration.class.getName(),
							tc.getCurrentXmlTest().getClasses().get(0).getName()));
				}
				TestContextConfiguration conf = getClassMetadata(tc);

				// get info from class annotation
				if (conf != null && conf.mode() != null) {
					reportMode = conf.mode();
				} else {
					reportMode = ReportMode.NORMAL;
				}

				if (conf != null && conf.identifier() > -1) { // if identifier is found, use this instead
					if (results.getResultList() // also check for clashing
							.stream()
							.map(SingleTest::getCode)
							.collect(Collectors.toList())
							.contains(conf.identifier())) {
						StudentLogger.log(tc.getCurrentXmlTest().getClasses().get(0).getName()
								+ " clashes with already existing identifier " + conf.identifier());
						return;
					}
					index = conf.identifier();
				}

				localOutput += "\n ---";
				localOutput += String.format("\n%s\n%s\n", tc.getName(), tc.getEndDate());
				if (conf != null && !conf.welcomeMessage().isEmpty()) {
					localOutput += String.format("%s\n", conf.welcomeMessage());
				}
				localOutput += " ---\n";

				double total = 0;
				double passed = 0;
				unitTestNotes = new ArrayList<String>(); // clear or initialize diagnostic array
				Set<ITestResult> testsFromContext;

				// iterate over three result types
				for (int type = 0; type < 3; type++) {
					switch (type) {
					// reduce code duplication
					case SUCCESS:
						testsFromContext = tc.getPassedTests().getAllResults();
						break;
					case FAILURE:
						testsFromContext = tc.getFailedTests().getAllResults();
						break;
					case SKIP:
						testsFromContext = tc.getSkippedTests().getAllResults();
						break;
					default:
						StudentLogger.log("This should never happen.");
						return;
					}
					for (ITestResult unitTestResult : testsFromContext) {
						Gradeable testMetadata = getTestMetadata(unitTestResult);
						if (testMetadata != null) {
							localOutput += (getTestReportString(unitTestResult, testMetadata));
							if (type == SUCCESS) {
								passed += testMetadata.weight();
							}
							total += testMetadata.weight();
						} else {
							localOutput += (getTestReportString(unitTestResult, getMockAnnotation()));
							if (type == SUCCESS) {
								passed += getMockAnnotation().weight();
							}
							total += getMockAnnotation().weight();
						}
						if (type == FAILURE || type == SKIP) {
							unitTestNotes.add(String.format("FAILURE: %s (%s)",
									unitTestResult.getName(), unitTestResult.getThrowable().toString()));
						}
						if (type == FAILURE) {
							unitTestNotes.add(String.format("\tStack trace of %s:  %s",
									unitTestResult.getName(),
									StudentHelperClass.getStackTraceString(unitTestResult.getThrowable(), unitTestResult.getName().split(" ")[0])));
						}
						if (StudentTesterAPI.hasInstance(unitTestResult.getTestClass().getRealClass())) {
							StudentTesterAPI api = StudentTesterAPI.getInstance(unitTestResult.getTestClass().getRealClass());
							api.getPrivateMessages().forEach((test, msgs) -> {
								unitTestNotes.add(String.format("\tNotes on %s:\n\t - %s\n", test,
										String.join("\n\t - ", msgs)));
							});
						}
					}
				}

				overallTotal += total;
				overallPassed += passed;

				// if no total, avoid dividing by 0
				if (total == 0) {
					total = -1;
				}

				if (reportMode != ReportMode.MUTED) {
					localOutput += String.format("\nPassed unit tests: %d/%d\n"
							+ "Failed unit tests: %d\n"
							+ "Skipped unit tests: %d\n"
							+ "Grade: %.1f%%\n",
							tc.getPassedTests().getAllResults().size(),
							// TODO: find shorter solution for this, getAllTestMethods does not take reused tests into account
							tc.getPassedTests().getAllResults().size()
								+ tc.getFailedTests().getAllResults().size()
								+ tc.getSkippedTests().getAllResults().size(),
							tc.getFailedTests().getAllResults().size(),
							tc.getSkippedTests().getAllResults().size(),
							(passed / total) * 100);
				} else {
					hasMute = true;
					localOutput += "Unit tests were run, but no output will be shown.\n";
				}

				// add results to temp class
				results.addTest(index, tc.getName(), (passed / total) * 100, String.join("\n", unitTestNotes), localOutput);
				index++;
			}
		}

		if (overallTotal == 0) {
			overallTotal = 1;
		}

		Collections.sort(results.getResultList());

		globalOutput += results.getResultList()
				.stream()
				.map(SingleTest::getOutput)
				.collect(Collectors.joining());

		if (!hasMute) {
			globalOutput += String.format("\nOverall grade: %.1f%%%s\n", (overallPassed / overallTotal) * 100,
					hasIncompleteResults ? "*\nFinal result based on only successfully compiled tests" : "");
		}
		// global results to object
		results.setStudentOutput(globalOutput);
		results.setPercent((overallPassed / overallTotal) * 100);
		reporterFinished = true;
	}

	/**
	 * Prints the results of a single unit test.
	 * @param unitTest - the unit test object
	 * @param testMetadata - annotations
	 * @return friendly string
	 */
	private String getTestReportString(final ITestResult unitTest, Gradeable testMetadata) {

		// JUnit tests return the method name in a weird format. Fix it
		String cleanName = unitTest.getName().split(" ")[0];

		if (reportMode == ReportMode.MUTED || reportMode == ReportMode.ANONYMOUS) {
			return "";
		}
		if (testMetadata == null) {
			testMetadata = getMockAnnotation();
		}
		String str = "";
		switch (unitTest.getStatus()) {
		case ITestResult.SUCCESS:
			if (reportMode == ReportMode.VERBOSE || reportMode == ReportMode.MAXVERBOSE) {
				str += String.format("SUCCESS: %s\n\t%d msec%s, weight: %d unit%s\n",
						cleanName,
						unitTest.getEndMillis() - unitTest.getStartMillis(),
						unitTest.getEndMillis() - unitTest.getStartMillis() == 1 ? "" : "s",
						testMetadata.weight(),
						testMetadata.weight() == 1 ? "" : "s");
				str += ((testMetadata.description() == null || testMetadata.description().isEmpty()) ?
						"" : String.format("\tDescription: %s\n", testMetadata.description()));
			}
			break;
		case ITestResult.FAILURE:
			str += String.format("FAILURE: %s\n\t%d msec%s, weight: %d unit%s\n",
					cleanName,
					unitTest.getEndMillis() - unitTest.getStartMillis(),
					unitTest.getEndMillis() - unitTest.getStartMillis() == 1 ? "" : "s",
					testMetadata.weight(),
					testMetadata.weight() == 1 ? "" : "s");
			str += ((testMetadata.description() == null || testMetadata.description().isEmpty()) ?
					"" : String.format("\tDescription: %s\n", testMetadata.description()));
			str += String.format("\tException type: %s\n", unitTest.getThrowable().getClass());
			if ((testMetadata.printExceptionMessage() || reportMode == ReportMode.VERBOSE  || reportMode == ReportMode.MAXVERBOSE)
					&& unitTest.getThrowable().getMessage() != null) {
				str += String.format("\tDetailed information:  %s\n", unitTest.getThrowable().getMessage());
			}
			if (unitTest.getThrowable() instanceof SecurityException
					&& unitTest.getThrowable().getMessage().contains("exit the JVM")) {
				str += "\tWarning: It seems that System.exit() is used in the code. "
						+ "Please remove it to prevent the tester from working abnormally.\n";
			}
			if (testMetadata.printStackTrace() || reportMode == ReportMode.MAXVERBOSE) {
				str += String.format("\tStack trace:  %s", StudentHelperClass.getStackTraceString(unitTest.getThrowable(), cleanName));
			}
			break;
		case ITestResult.SKIP:
			str += String.format("SKIPPED: %s\n\tWeight: %d unit%s\n",
					cleanName,
					testMetadata.weight(),
					testMetadata.weight() == 1 ? "" : "s");
			str += ((testMetadata.description() == null || testMetadata.description().isEmpty()) ?
					"" : String.format("\tDescription: %s\n", testMetadata.description()));
			str += String.format("\tDummyTest skipped because:  %s\n", unitTest.getThrowable().toString());

			if (unitTest.getMethod().getGroupsDependedUpon().length > 0) {
				str += String.format("\tThis unit test depends on groups: %s\n", String.join(", ", unitTest.getMethod().getGroupsDependedUpon()));
			}
			if (unitTest.getMethod().getMethodsDependedUpon().length > 0) {
				str += String.format("\tThis unit test depends on tests: %s\n", String.join(", ", unitTest.getMethod().getMethodsDependedUpon()));
			}
			break;
		default:
			StudentLogger.log("No such test result code: " + unitTest.getStatus());
			return null;
		}
		if (StudentTesterAPI.hasInstance(unitTest.getTestClass().getRealClass())) {
			StudentTesterAPI api = StudentTesterAPI.getInstance(unitTest.getTestClass().getRealClass());
			if (api.getPublicMessages().containsKey(unitTest.getName())) {
				str += String.format("\tNotes on %s:\n\t - %s\n", cleanName,
						String.join("\n\t - ", api.getPublicMessages().get(unitTest.getName())));
			}
		}
		return str;
	}

	/**
	 * Gets the custom annotations from a unit test.
	 * @param test - unit test to get the metadata from
	 * @return annotation data if found
	 */
	private Gradeable getTestMetadata(final ITestResult test) {
		try {
			Method m = test.getMethod().getConstructorOrMethod().getMethod();
			return (Gradeable) m.getAnnotation(Gradeable.class);
		} catch (SecurityException e) {
			StudentLogger.log(e.getMessage());
		}
		return null;
	}

	/**
	 * Gets the custom annotation from the first annotated
	 * class referenced in a test.
	 * @param context - context to get the metadata from
	 * @return annotation data if found
	 */
	private TestContextConfiguration getClassMetadata(final ITestContext context) {
		try {
			//Class<?> c = Class.forName(context.getName());
			for (XmlClass c : context.getCurrentXmlTest().getClasses()) {
				TestContextConfiguration a = ((Class<?>) c.getSupportClass()).getAnnotation(TestContextConfiguration.class);
				if (a != null) {
					return a;
				}
			}
			return null;
		} catch (SecurityException e) {
			StudentLogger.log(e.getMessage());
		}
		return null;
	}

	/**
	 * Mock annotation for unit tests that don't have one.
	 * @return default annotation for tests
	 */
	private Gradeable getMockAnnotation() {
		Gradeable annotation = new Gradeable() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return Gradeable.class;
			}

			@Override
			public int weight() {
				return 1;
			}

			@Override
			public String description() {
				return null;
			}

			@Override
			public boolean printExceptionMessage() {
				return false;
			}

			@Override
			public boolean printStackTrace() {
				return false;
			}
		};

		return annotation;
	}

}