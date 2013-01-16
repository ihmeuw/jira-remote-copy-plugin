package com.atlassian.cpji.conditions;

import com.atlassian.jira.util.BuildUtilsInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @since v3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class TestHasRICBundledCondition {

    @Mock
    private BuildUtilsInfo buildUtilsInfo;

    private HasRICBundledCondition hasRICBundledCondition;

    private void setCurrentVersion(int ... version){
        when(buildUtilsInfo.getVersionNumbers()).thenReturn(version);
    }

    @Before
    public void init(){
        hasRICBundledCondition = new HasRICBundledCondition(buildUtilsInfo);
    }

    @Test
    public void isTrueForJiras6x(){
        setCurrentVersion(6,0,0);
        assertTrue(hasRICBundledCondition.shouldDisplay(null,null));
        setCurrentVersion(6,2,0);
        assertTrue(hasRICBundledCondition.shouldDisplay(null,null));
    }


    @Test
    public void isFalseForJiras5x(){
        setCurrentVersion(5,2,0);
        assertFalse(hasRICBundledCondition.shouldDisplay(null, null));
        setCurrentVersion(5,2,1);
        assertFalse(hasRICBundledCondition.shouldDisplay(null, null));
        setCurrentVersion(5,1,5);
        assertFalse(hasRICBundledCondition.shouldDisplay(null, null));
    }

}
