package it.com.atlassian.cpji

import com.atlassian.jira.testkit.client.Backdoor
import scala.Array

object BackdoorHelpers {

	val removeProjectRolePermission = (testkit: Backdoor, schemeId: Int, permission: Int, roleId: Int) => {
		try {
			testkit.permissionSchemes().removeProjectRolePermission(schemeId, permission, roleId)
		} catch {
			case e: Exception => ""
		}
	}

	def removeProjectRolePermissions(testkit: Backdoor, schemeId: Int, permission: Int, roleIds: Array[Int]): Array[Int] = {
		var result: Array[Int] = new Array(0)
		for (roleId <- roleIds) {
			try {
				testkit.permissionSchemes().removeProjectRolePermission(schemeId, permission, roleId)
				result = result :+ roleId
			} catch {
				case e: Exception => ""
			}
		}
		result
	}

	val addProjectRolePermission = (testkit: Backdoor, schemeId: Int, permission: Int, roleId: Int) => {
		try {
			testkit.permissionSchemes().addProjectRolePermission(schemeId, permission, roleId)
		} catch {
			case e: Exception => ""
		}
	}

	val addProjectRolePermissions = (testkit: Backdoor, schemeId: Int, permission: Int, roleIds: Array[Int]) => {
		for (roleId <- roleIds)
			addProjectRolePermission(testkit, schemeId, permission, roleId)
	}

}
