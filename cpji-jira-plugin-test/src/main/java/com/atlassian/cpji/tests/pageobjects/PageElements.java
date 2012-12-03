package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import javax.annotation.Nullable;

/**
 * TODO: Document this class / interface here
 *
 * @since v5.2
 */
public class PageElements {

	public static Function<PageElement, String> getText() {
		return new Function<PageElement, String>() {
			@Override
			public String apply(@Nullable PageElement input) {
				return input.getText();
			}
		};
	}

	public static Predicate<PageElement> isVisible()
	{
		return new Predicate<PageElement>()
		{
			@Override
			public boolean apply(PageElement input)
			{
				return input.isVisible();
			}
		};
	}

	public static Function<WebDriver, Boolean> isEnabled(final By location) {
		return new Function<WebDriver, Boolean>() {
			@Override
			public Boolean apply(WebDriver input) {
				return input.findElement(location).isEnabled();
			}
		};
	}
}
