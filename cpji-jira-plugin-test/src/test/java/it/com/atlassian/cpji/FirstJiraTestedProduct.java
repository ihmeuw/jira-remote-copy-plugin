package it.com.atlassian.cpji;

import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import com.atlassian.webdriver.pageobjects.WebDriverTester;

/**
 * The first jira instance in the test group, defined in pom.xml. This class is used to define the httpPort.
 *
 * @since v2.1
 */
@com.atlassian.pageobjects.Defaults(instanceId = "jira1", contextPath = "/jira", httpPort = 2990)
public class FirstJiraTestedProduct extends JiraTestedProduct
{
    public FirstJiraTestedProduct(TestedProductFactory.TesterFactory<WebDriverTester> testerFactory, ProductInstance productInstance)
    {
        super(testerFactory, productInstance);
    }
}
