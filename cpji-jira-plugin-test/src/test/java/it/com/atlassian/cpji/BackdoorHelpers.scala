package it.com.atlassian.cpji

import com.atlassian.jira.testkit.client.Backdoor

object BackdoorHelpers {

	val removeProjectRolePermission = (testkit: Backdoor, schemeId: Int, permission: Int, roleId: Int) => {
		try {
			testkit.permissionSchemes().removeProjectRolePermission(schemeId, permission, roleId)
		} catch {
			case e: Exception => ""
		}
	}

	val addProjectRolePermission = (testkit: Backdoor, schemeId: Int, permission: Int, roleId: Int) => {
		try {
			testkit.permissionSchemes().addProjectRolePermission(schemeId, permission, roleId)
		} catch {
			case e: Exception => ""
		}
	}

}
