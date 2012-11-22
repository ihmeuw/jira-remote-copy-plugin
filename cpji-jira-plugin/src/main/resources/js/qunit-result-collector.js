(function ($) {

    // Only set up test collection if there isn't already a AJS.test.results object in existence
    // e.g. Confluence uses a qunit-result-collector to collect results in a particular format
    if (AJS.test && AJS.test.results != undefined) {
        AJS.log("AJS.test.results already exists so not configuring aui-qunit-plugin specific test collection.");
        return;
    }

    AJS.test.results = [];
    AJS.test.allDone = false;
    var currentLog = [];

    QUnit.testDone = function(argsHash) {
        AJS.test.results.push($.extend(argsHash, {
            log: currentLog
        }));
        currentLog = [];
    };

    QUnit.log = function (argsHash) {
        currentLog.push(argsHash);
    };

    QUnit.done = function(failures, total) {
        AJS.test.allDone = true;
    };
})(jQuery);