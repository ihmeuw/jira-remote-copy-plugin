JIRA to JIRA Issue Copy
============

The JIRA to JIRA Issue Copy was created to make it possible to copy issues between different JIRA servers. It also adds an ability to copy issues between projects which is more flexible than JIRA's Clone action.

Visit [JIRA to JIRA Issue Copy](https://marketplace.atlassian.com/plugins/com.atlassian.cpji.cpji-jira-plugin) on Atlassian Marketplace to learn more. 

Raising bugs or feature requests
-----------------
This plugin is not officially supported. If you would like to raise a bug or feature request, please comment on this issue: https://jira.atlassian.com/browse/JRA-62759. We will periodically re-evaluate the plugin's supported status.

Changing JIRA to JIRA Issue Copy
-----------------

The plugin is divided into two modules:

* cpji-jira-plugin which is the core functionality
* cpji-jira-plugin-test which includes functional tests for it

You can use [Atlassian Plugin SDK](http://confluence.atlassian.com/display/DEVNET/Developing+your+Plugin+using+the+Atlassian+Plugin+SDK) to run it locally.

To run pre-configured JIRA instances in debug mode use:

`cd cpji-jira-plugin && atlas-mvn amps:debug -DinstanceId=jira1 -Pjira1`

`cd cpji-jira-plugin && atlas-mvn amps:debug -DinstanceId=jira2 -Pjira2`

To run integration tests use:

`cd cpji-jira-plugin-test && atlas-mvn amps:integration-test -DtestGroups=jira-tests`

To run CLI for each:

`cd cpji-jira-plugin && atlas-mvn jira:cli -Pjira1`

`cd cpji-jira-plugin && atlas-mvn jira:cli -Pjira2`

To install plugin in JIRA 2:

`atlas-mvn amps:install -Dhttp.port=2991 -Dcontext.path=jira`

Contributing to JIRA to JIRA Issue Copy
-----------------

You are free to modify the plugin under [Apache 2 license](LICENSE) terms.

To create a successful pull request:

* raise an issue with a description of the change you want to make
* fork the repository
* change the plugin
* add new or modify tests so they will cover your changes
* make sure all integration tests pass
* raise a pull request

We'll be happy to incorporate your changes.
