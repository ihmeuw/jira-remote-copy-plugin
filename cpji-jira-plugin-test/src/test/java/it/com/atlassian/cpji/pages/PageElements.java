package it.com.atlassian.cpji.pages;

import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.base.Function;

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
}
