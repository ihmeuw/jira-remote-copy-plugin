package it.com.atlassian.cpji;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.pageobjects.WebDriverTester;

/**
 * * The second jira instance in the test group, defined in pom.xml. This class is used to define the httpPort.
 *
 * @since v2.1
 */
@com.atlassian.pageobjects.Defaults(instanceId = "jira2", contextPath = "/jira", httpPort = 2991)
public class SecondJiraTestedProduct extends JiraTestedProduct
{
    public SecondJiraTestedProduct(TestedProductFactory.TesterFactory<WebDriverTester> testerFactory, ProductInstance productInstance)
    {
        super(testerFactory, productInstance);
    }
}
