package ee.ttu.java.studenttester.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ee.ttu.java.studenttester.classes.StudentHelperClass;
import ee.ttu.java.studenttester.classes.StudentTesterClass;

/**
 * Tests for StudentTester.
 * Instructions on how to create a new unit test:
 * <br> - Inside "templates" package, write your code in a txt file
 * <br> - The template names should follow the format SomeCode and SomeCodeTest
 * <br> - The unit test testing with these templates should be named testSomeCode
 * <br> - Autogenerated names are provided by variables currentStudentCodeName and currentTestName
 * <br> - First, import the templates (getFileTemplate), perform necessary operations on them to
 * make them valid Java classes. Then, add these strings to a valid category (addCode, addTest).
 * Finally, call getTestResults. Results are returned from StudentTester as a JSON object.
 * @author Andres
 */
public class Tests {

	@Test(description = "Check if a simple test can receive a result from a class.")
	public void testTrivialStudent() {
		JSONObject results = getSimpleTestResults();
		Assert.assertTrue(results.getString("output").contains("Grade: 100"));
		Assert.assertTrue(results.getString("output").contains("Overall grade: 100"));
		Assert.assertEquals(results.getInt("percent"), 100);
	}

	@Test(description = "Check if a simple JUnit test can receive a result from a class.")
	public void testTrivialStudentJUnit() {
		JSONObject results = getSimpleTestResults();
		Assert.assertTrue(results.getString("output").contains("Grade: 100"));
		Assert.assertTrue(results.getString("output").contains("Overall grade: 100"));
		Assert.assertEquals(results.getInt("percent"), 100);
	}

	@Test(description = "Check two exceptions. One should succeed, the other should not.")
	public void testTwoExceptions() {
		JSONObject results = getSimpleTestResults();
		Assert.assertTrue(results.getString("output").contains("Grade: 50"));
		Assert.assertTrue(results.getString("output").contains("Overall grade: 50"));
		Assert.assertEquals(results.getInt("percent"), 50);
	}

	@Test(description = "Create a muted test. DummyTest names and scores should not be visible.")
	public void testMuted() {
		JSONObject results = getSimpleTestResults();
		Assert.assertFalse(results.getString("output").contains("testSanity"));
		Assert.assertFalse(results.getString("output").contains("Overall grade"));
	}
	
	@Test(description = "The test should get an exception as a shell command is invoked")
	public void testRuntimeExec() {
		JSONObject results = getSimpleTestResults();
		Assert.assertEquals(results.getInt("percent"), 100);
	}

	@Test(description = "DummyTest weights with random numbers. Some tests fail, some do not. The grade must be correct.")
	public void testWeights100() throws IOException, URISyntaxException {

		String testCode = getFileTemplate(currentTestName);
		String testCodeInner = getFileTemplate(currentTestName + "Func");

		final int TEST_AMOUNT = 100;
		boolean[] testStatus = new boolean[TEST_AMOUNT];
		int[] testWeights = new int[TEST_AMOUNT];

		double expectedTotal = 0;
		double expectedPassed = 0;
		String tests = "";

		for (int i = 0; i < TEST_AMOUNT; i++) {
			testStatus[i] = rn.nextBoolean();
			testWeights[i] = rn.nextInt(101);
			expectedTotal += testWeights[i];
			if (testStatus[i]) {
				expectedPassed += testWeights[i];
			}
			tests += testCodeInner.replace("$counter", Integer.toString(i))
					.replace("$weight", Integer.toString(testWeights[i]))
					.replace("$falsetrue", Boolean.toString(testStatus[i]));
		}

		if (expectedTotal == 0) {
			expectedTotal = 1;
		}
		double expectedGrade = (expectedPassed / expectedTotal) * 100;

		addTest(testCode.replace("$tests", tests), currentTestName);
		JSONObject results = getTestResults(false, true);
		Assert.assertEquals(results.getDouble("percent"), expectedGrade, 0.1);
	}

	@Test(description = "Check the output contains 'Nothing to run.' when both Checkstyle and TestNG are disabled.")
	public void testDoNothing() {
		JSONObject results = getTestResults(false, false);
		Assert.assertTrue(results.getString("output").contains("Nothing to run."));
	}

	@Test(description = "Check if student's compilation error is displayed correctly.")
	public void testBrokenStudentCode() {
		JSONObject results = getSimpleTestResults();
		Assert.assertTrue(results.getString("output").contains(String.format("';' expected", testCounter)));
	}

	@Test(description = "Check if file contents are included in JSON")
	public void testFileContentsInJSON() {
		JSONObject results = getSimpleTestResults("TrivialStudent"); // let's be lazy and reuse a previous template
		String expectedCode = getFileTemplate(currentStudentCodeName);
		String expectedTest = getFileTemplate(currentTestName);
		Assert.assertEquals(results.getJSONArray("source").length(), 1);
		Assert.assertEquals(results.getJSONArray("testSource").length(), 1);
		Assert.assertEquals(results.getJSONArray("source").getJSONObject(0).getString("content"), expectedCode);
		Assert.assertEquals(results.getJSONArray("testSource").getJSONObject(0).getString("content"), expectedTest);
		Assert.assertEquals(results.getJSONArray("source").getJSONObject(0).getString("type"), "code");
		Assert.assertEquals(results.getJSONArray("testSource").getJSONObject(0).getString("type"), "test");
	}

	@Test(description = "Check if Checkstyle works")
	public void testCheckstyleBare() {
		setCurrentBaseNames("TrivialStudent");
		addCode(getFileTemplate(currentStudentCodeName), currentStudentCodeName);
		addTest(getFileTemplate(currentTestName), currentTestName);
		JSONObject results = getTestResults(true, false);
		Assert.assertFalse(results.has("percent"));
		Assert.assertFalse(results.getString("output").contains("Overall grade"));
		Assert.assertFalse(results.getString("output").contains("Compilation succeeded"));
		Assert.assertFalse(results.getString("output").contains("Compilation failed"));
		Assert.assertEquals(results.getJSONArray("results").length(), 1);
		Assert.assertEquals(results.getJSONArray("results").getJSONObject(0).getInt("errorCount"), 6);
		Assert.assertEquals(results.getJSONArray("results").getJSONObject(0).getInt("percent"), 0);
		Assert.assertTrue(results.getString("output")
				.contains(results.getJSONArray("results").getJSONObject(0).getString("output")));
	}
	// TODO: new functionality

	/**
	 * Automatically constructs a test from current test name and
	 * returns the results in JSON format. There must exist a template
	 * for testable code and the test itself, see existing unit tests.
	 * @return JSON-formatted StudentTester results, null on error
	 */
	private JSONObject getSimpleTestResults() {
		String studentCode = getFileTemplate(currentStudentCodeName);
		String testCode = getFileTemplate(currentTestName);
		addCode(studentCode, currentStudentCodeName);
		addTest(testCode, currentTestName);
		return getTestResults(false, true);
	}

	/**
	 * Automatically constructs a test from current test name and
	 * returns the results in JSON format. There must exist a template
	 * for testable code and the test itself, see existing unit tests.
	 * @return JSON-formatted StudentTester results, null on error
	 */
	private JSONObject getSimpleTestResults(final String basename) {
		setCurrentBaseNames(basename);
		return getSimpleTestResults();
	}

	/**
	 * Adds some test code to the dummy test environment.
	 * @param code - code to be added
	 * @param name - file name
	 * @return success
	 */
	public boolean addTest(final String code, final String name) {
		return addFile(code, name, true);
	}

	/**
	 * Adds some code to the dummy test environment.
	 * @param code - code to be added
	 * @param name - file name
	 * @return success
	 */
	public boolean addCode(final String code, final String name) {
		return addFile(code, name, false);
	}

	/**
	 * Adds some code to the dummy test environment.
	 * @param code - code to be added
	 * @param name - file name
	 * @param isTest - is the code a test?
	 * @return success
	 */
	public boolean addFile(final String code, final String name, final boolean isTest) {
		String arg = String.format(tempDirName + "%s/%s.java", isTest ? "test" : "source", name);
		try (PrintWriter writer = new PrintWriter(arg, "UTF-8")) {
			writer.write(code);
			File f = new File(arg);
			if (f.exists()) {
				if (isTest) {
					testFiles.add(f);
				} else {
					studentFiles.add(f);
				}
				return true;
			}
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Finalises objects, runs the test and returns results.
	 * @return test results in JSON format
	 */
	public JSONObject getTestResults(final boolean checkStyleEnabled, final boolean testNGEnabled) {
		StudentTesterClass c = new StudentTesterClass(tempDirName + "test/", tempDirName + "source/");
		c.enableCheckstyle(checkStyleEnabled);
		c.enableTestNG(testNGEnabled);
		c.outputJSON(true);
		// c.setQuiet(true);
		c.setVerbosity(3);
		c.run();
		JSONObject results = new JSONObject(c.getJson());
		return results;
	}

	/**
	 * Reads and returns a txt file from templates folder.
	 * @param filename - .txt file to open
	 * @return - .txt file contents
	 */
	public String getFileTemplate(final String filename) {
		InputStream is = getClass().getResourceAsStream("/templates/" + filename + ".txt");
		try (Scanner s = new Scanner(is, "UTF-8")) {
			s.useDelimiter("\\A");
			return s.next();
		}
	}

	/**
	 * Set names before unit test.
	 * @param method the name of the test about to be executed
	 */
	@BeforeMethod
	public void beforeMethod(final Method method) {
		setCurrentBaseNames(method.getName().substring(4)); // remove "test" prefix
	}

	/**
	 * Sets the template names for current test.
	 * @param name base name for test class
	 */
	public void setCurrentBaseNames(final String name) {
		currentStudentCodeName = name;
		currentTestName = currentStudentCodeName + "Test";
	}

	/**
	 * Delete files after testing.
	 */
	@AfterMethod
	public void afterMethod() {
		currentStudentCodeName = null;
		currentTestName = null;
		studentFiles.forEach(x -> x.delete());
		studentFiles.clear();
		testFiles.forEach(x -> x.delete());
		testFiles.clear();
		testCounter++;
	}

	/**
	 * Initialize folders (create fixture).
	 */
	@BeforeClass
	public void beforeClass() {
		if (tempDirName == null) {
			throw new NullPointerException("Temp folder not found!");
		}
		testDir = new File(tempDirName + "test");
		testDir.mkdir();
		sourceDir = new File(tempDirName + "source");
		sourceDir.mkdir();
	}

	/**
	 * Delete junk files after tests (destroy fixture).
	 */
	@AfterClass
	public void afterClass() {
		StudentHelperClass.deleteFolder(testDir);
		StudentHelperClass.deleteFolder(sourceDir);
	}

	/**
	 * Holds a temporary list of mockup code files.
	 */
	private List<File> studentFiles = new ArrayList<File>();
	/**
	 * Holds a temporary list of test files.
	 */
	private List<File> testFiles = new ArrayList<File>();
	/**
	 * Get temp folder.
	 */
	private String tempDirName = System.getProperty("java.io.tmpdir");
	/**
	 * Stores pointers to folders.
	 */
	private File testDir, sourceDir;
	/**
	 * Counter for naming tests.
	 */
	private int testCounter = 0;
	/**
	 * Random number gen for tests.
	 */
	private Random rn = new Random();
	/**
	 * Holds the current basename of a test.
	 */
	private String currentStudentCodeName, currentTestName;
}