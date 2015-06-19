package it.com.atlassian.cpji;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.tests.pageobjects.DefaultProductInstance;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.qunit.test.runner.QUnitPageObjectsHelper;
import org.apache.axis.utils.StringUtils;
import org.junit.Test;

import java.io.File;

public class TestQunit {

    static JiraTestedProduct jira1 = TestedProductFactory.create(JiraTestedProduct.class, new DefaultProductInstance("http://localhost:2990/jira", "jira1", 2990, "/jira"), null);

    private final File outdir;

    public TestQunit() {
        String location = System.getProperty("jira.qunit.testoutput.location");
        if (StringUtils.isEmpty(location)) {
            System.err.println("Writing result XML to tmp, jira.qunit.testoutput.location not defined");
            location = System.getProperty("java.io.tmpdir");
        }
        outdir = new File(location);
    }

    @Test
    public void runJustOurTest() throws Exception {
        QUnitPageObjectsHelper helper = new QUnitPageObjectsHelper(outdir, jira1.getPageBinder());
        helper.runTests(QUnitPageObjectsHelper.suiteNameContains("cpji-jira-plugin"));
    }
}
