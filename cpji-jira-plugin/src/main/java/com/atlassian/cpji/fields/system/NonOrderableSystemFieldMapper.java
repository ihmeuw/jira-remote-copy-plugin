package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.SystemFieldMapper;

/**
 * @since v2.0
 */
public interface NonOrderableSystemFieldMapper extends SystemFieldMapper
{
    boolean isVisible();
}
