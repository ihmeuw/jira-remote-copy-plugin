package com.atlassian.cpji;

import com.atlassian.aui.test.runner.QUnitPageObjectsHelper;
import com.atlassian.aui.test.runner.QUnitTestResult;
import com.atlassian.aui.test.runner.QunitIndexPage;
import com.atlassian.aui.test.runner.QunitTestMeta;
import com.atlassian.aui.test.runner.QunitTestPage;
import com.atlassian.pageobjects.PageBinder;
import org.hamcrest.Matcher;

import java.io.File;

/**
 * This file is copied from qui-qunit-helper due to slightly different uri regex pattern - "webcontext" vs "webContext"
 */
public class TemporaryQunitPageObjectsHelper extends QUnitPageObjectsHelper
{

    private final File outdir;
    private final PageBinder pageBinder;

    public TemporaryQunitPageObjectsHelper(File outdir, PageBinder pageBinder)
    {
        super(outdir, pageBinder);
        this.outdir = outdir;
        this.pageBinder = pageBinder;
    }


    public void runTests(Matcher<QunitTestMeta> apply) throws Exception {
        QunitIndexPage indexPage = pageBinder.navigateToAndBind(TemporaryQUnitIndexPage.class);
        for (QunitTestMeta test : indexPage.getAllTests()) {
            if (!apply.matches(test)) {
                continue; // only run ours
            }
            QunitTestPage testPage = pageBinder.navigateToAndBind(QunitTestPage.class, test);
            QUnitTestResult result = testPage.getResult();
            result.write(outdir);
        }

    }

}
