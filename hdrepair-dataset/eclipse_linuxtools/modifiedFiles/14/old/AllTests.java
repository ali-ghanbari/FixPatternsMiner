package org.eclipse.linuxtools.changelog.core.tests;
import org.eclipse.linuxtools.changelog.core.formatters.tests.GNUFormatTest;
import org.eclipse.linuxtools.changelog.parsers.tests.JavaParserTest;
import org.eclipse.linuxtools.changelog.tests.fixtures.TestChangeLogTestProject;
import org.junit.runners.Suite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	ChangeLogWriterTest.class,
	GNUFormatTest.class,
	JavaParserTest.class,
	// A small test for the fixture
	TestChangeLogTestProject.class,
	}
)

public class AllTests {
	// empty
}
