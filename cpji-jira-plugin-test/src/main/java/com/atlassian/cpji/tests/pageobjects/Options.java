package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.pageobjects.elements.Option;
import com.google.common.base.Function;

/**
 * {@link Option} objects factory.
 *
 */
public final class Options
{
    public static Function<Option,String> getValue()
    {
        return new Function<Option, String>()
        {
            @Override
            public String apply(Option option)
            {
                return option.value();
            }
        };
    }

    public static Function<Option,String> getId()
    {
        return new Function<Option, String>()
        {
            @Override
            public String apply(Option option)
            {
                return option.id();
            }
        };
    }

    public static Function<Option,String> getText()
    {
        return new Function<Option, String>()
        {
            @Override
            public String apply(Option option)
            {
                return option.text();
            }
        };
    }
}
