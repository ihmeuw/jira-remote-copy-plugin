package com.atlassian.cpji;

import com.atlassian.aui.test.runner.QunitIndexPage;
import com.atlassian.aui.test.runner.QunitTestMeta;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

import java.util.LinkedList;
import java.util.List;

/**
 * This file is copied from qui-qunit-helper due to slightly different uri regex pattern - "webcontext" vs "webContext"
 */
public class TemporaryQUnitIndexPage extends QunitIndexPage
{
    public List<QunitTestMeta> getAllTests() {
        List<PageElement> links = elementFinder.findAll(By.cssSelector("a.testlink"));
        List<QunitTestMeta> results = new LinkedList<QunitTestMeta>();
        for (PageElement link : links) {
            String href = link.getAttribute("href");
            results.add(TemporaryQunitTestMeta.createFromUrl(href));
        }
        return results;
    }

}
