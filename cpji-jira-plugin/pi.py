#!/usr/bin/python
from subprocess import Popen, call

Popen(["atlas-mvn", "-o", 
	"com.atlassian.maven.plugins:maven-jira-plugin:copy-bundled-dependencies",
	"com.atlassian.maven.plugins:maven-jira-plugin:compress-resources",
	"org.apache.maven.plugins:maven-resources-plugin:resources", 
	"com.atlassian.maven.plugins:maven-jira-plugin:filter-plugin-descriptor",
	"compile", 
	"com.atlassian.maven.plugins:maven-jira-plugin:generate-manifest",
	"com.atlassian.maven.plugins:maven-jira-plugin:validate-manifest",
	"com.atlassian.maven.plugins:maven-jira-plugin:jar",
	"org.apache.maven.plugins:maven-install-plugin:install"]).wait()

for p in range(1, 4):
	call(["atlas-mvn", "-o", "-q",
		"com.atlassian.maven.plugins:maven-jira-plugin:install",
		"-Pjira" + str(p)])
