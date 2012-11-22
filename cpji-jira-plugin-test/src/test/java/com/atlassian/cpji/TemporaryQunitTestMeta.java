package com.atlassian.cpji;

import com.atlassian.aui.test.runner.QunitTestMeta;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This file is copied from qui-qunit-helper due to slightly different uri regex pattern - "webcontext" vs "webContext"
 */
public class TemporaryQunitTestMeta extends QunitTestMeta
{
    public TemporaryQunitTestMeta(String url, String qunitTestFilePath)
    {
        super(url, qunitTestFilePath);
    }

    public static QunitTestMeta createFromUrl(String url)
    {
        // remove the context path and the html part, and the trailing .js if it exists
        final Matcher m = Pattern.compile("^(.*)(/qunit/run/(?:(?:webcontext|plugin)/)(.+?)/run.html$)").matcher(url);
        if (!m.matches()) {
            throw new IllegalArgumentException("could not parse url " + url);
        }

        return new QunitTestMeta(m.group(2), m.group(3));
    }

}
