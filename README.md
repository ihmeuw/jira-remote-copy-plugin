This is a fork of the jira-remote-copy-plugin by Atlassian Labs. The purpose of this fork is to keep supporting issue remote copy functionality for Jira 9 and Jira 10.

----------------

JIRA to JIRA Issue Copy
============

The JIRA to JIRA Issue Copy was created to make it possible to copy issues between different JIRA servers. It also adds an ability to copy issues between projects which is more flexible than JIRA's Clone action.

Changing JIRA to JIRA Issue Copy
-----------------

The plugin is divided into two modules:

* cpji-jira-plugin which is the core functionality
* cpji-jira-plugin-test which includes functional tests for it

You can use [Atlassian Plugin SDK](http://confluence.atlassian.com/display/DEVNET/Developing+your+Plugin+using+the+Atlassian+Plugin+SDK) to run it locally.

To run pre-configured JIRA instances in debug mode use:

`cd cpji-jira-plugin && mvn amps:debug -DinstanceId=jira1 -Pjira1`

`cd cpji-jira-plugin && mvn amps:debug -DinstanceId=jira2 -Pjira2`

To run integration tests use:

`cd cpji-jira-plugin-test && mvn amps:integration-test -DtestGroups=jira-tests`

To run CLI for each:

`cd cpji-jira-plugin && mvn jira:cli -Pjira1`

`cd cpji-jira-plugin && mvn jira:cli -Pjira2`

To install plugin in JIRA 2:

`atlas-mvn amps:install -Dhttp.port=2991 -Dcontext.path=jira`


Which branch should I target?
-----------------

- Latest - `main`
- Jira 10.x - `jira-10`
- Jira 9.x - `jira-9`
- Jira 8.x - `jira-8`

Latest Release
-----------------

- [cpji-jira-10.0.0](https://github.com/ihmeuw/jira-remote-copy-plugin/releases/tag/cpji-jira-10.0.0)
> This release is compatible with Jira 10.x
- [cpji-jira-9.0.0](https://github.com/ihmeuw/jira-remote-copy-plugin/releases/tag/cpji-jira-9.0.0)
> This release is compatible with Jira 9.x
 
Contributing to JIRA to JIRA Issue Copy
-----------------

Refer to [CONTRIBUTING.md](CONTRIBUTING.md)

Code of Conduct
-----------------

Refer to [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
