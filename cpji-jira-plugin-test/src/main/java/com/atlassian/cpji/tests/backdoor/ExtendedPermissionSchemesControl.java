package com.atlassian.cpji.tests.backdoor;

import com.atlassian.jira.testkit.client.JIRAEnvironmentData;
import com.atlassian.jira.testkit.client.PermissionSchemesControl;

/**
 * @since v3.0
 */
public class ExtendedPermissionSchemesControl extends PermissionSchemesControl {

    public ExtendedPermissionSchemesControl(JIRAEnvironmentData environmentData) {
        super(environmentData);
    }

    public void addProjectRolePermission(Long schemeId, int permission, long projectRoleId) {
        addPermission(schemeId, permission, "projectrole", Long.toString(projectRoleId));
    }


    public void removeProjectRolePermission(long schemeId, int permission, long projectRoleId) {
        removePermission(schemeId, permission, "projectrole", Long.toString(projectRoleId));
    }


    private void addPermission(long schemeId, int permission, String type, String parameter) {
        get(createResource().path("permissionSchemes/entity/add")
                .queryParam("schemeId", "" + schemeId)
                .queryParam("permission", "" + permission)
                .queryParam("type", type)
                .queryParam("parameter", parameter));
    }


    private void removePermission(long schemeId, int permission, String type, String parameter) {
        get(createResource().path("permissionSchemes/entity/remove")
                .queryParam("schemeId", "" + schemeId)
                .queryParam("permission", "" + permission)
                .queryParam("type", type)
                .queryParam("parameter", parameter));
    }


}
