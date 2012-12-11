package com.atlassian.cpji.tests.backdoor;

import com.atlassian.jira.testkit.client.Backdoor;
import com.atlassian.jira.testkit.client.JIRAEnvironmentData;
import com.atlassian.jira.testkit.client.PermissionSchemesControl;

/**
 * @since v3.0
 */
public class ExtendedBackdoor extends Backdoor {

    private final ExtendedPermissionSchemesControl extendedPermissionSchemesControl;

    public ExtendedBackdoor(JIRAEnvironmentData environmentData) {
        super(environmentData);
        extendedPermissionSchemesControl = new ExtendedPermissionSchemesControl(environmentData);
    }

    @Override
    public PermissionSchemesControl permissionSchemes() {
        return extendedPermissionSchemesControl;
    }

    public ExtendedPermissionSchemesControl extendedPermissionSchemesControl() {
        return extendedPermissionSchemesControl;
    }


}
