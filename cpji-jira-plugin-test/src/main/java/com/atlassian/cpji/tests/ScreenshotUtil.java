package com.atlassian.cpji.tests;

import com.atlassian.jira.pageobjects.config.TestEnvironment;
import com.atlassian.jira.tests.rules.WebDriverScreenshot;
import org.apache.commons.io.FileUtils;
import org.hamcrest.StringDescription;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 */
public class ScreenshotUtil {
	private static final Logger logger = LoggerFactory.getLogger(WebDriverScreenshot.class);

	public static void attemptScreenshot(WebDriver driver, String name)
	{
		if (!isScreenshotCapable(driver))
		{
			logger.warn(new StringDescription().appendText("Unable to take screenshot: WebDriver ")
					.appendValue(driver).appendText(" is not instance of TakesScreenshot").toString());
			return;
		}
		takeScreenshot(driver, name);
	}

	private static void takeScreenshot(WebDriver driver, String name)
	{
		final TestEnvironment testEnvironment = new TestEnvironment();

		try
		{
			TakesScreenshot takingScreenshot = (TakesScreenshot) driver;
			File screenshot = takingScreenshot.getScreenshotAs(OutputType.FILE);
			File target = new File(testEnvironment.artifactDirectory(), name);
			FileUtils.copyFile(screenshot, target);
			logger.info("A screenshot of the page has been stored under " + target.getAbsolutePath());
		}
		catch(Exception e)
		{
			logger.error(new StringDescription().appendText("Unable to take screenshot for failed test ")
					.appendValue(name).toString(), e);
		}
	}

	private static boolean isScreenshotCapable(WebDriver driver)
	{
		return driver instanceof TakesScreenshot;
	}
}
