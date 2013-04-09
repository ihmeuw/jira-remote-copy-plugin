(function ($) {
    $(function () {

        //JRA-32483 - we do not display hovers on IE 10
        var isIE10 = (AJS.$.browser.msie && ~~(AJS.$.browser.version) >= 10) && ( !JIRA.Version.isGreaterThanOrEqualTo("6") );
        if(isIE10){
            return;
        }

        var isWarningAdded = false;
        $(".fields-list label").each(function(){
            var labelTarget = $(this).attr("for");
            var unmapped = $("#unmapped-for-"+labelTarget);
            if(unmapped.length == 1){
                isWarningAdded = true;
                var unmappedJson = JSON.parse(unmapped.text());
                if(unmappedJson.length > 0){
                    var unmappedMarker = $(RIC.Templates.warningMarker());
                    $(this).prepend(unmappedMarker);
                    AJS.InlineDialog(unmappedMarker, "unmapped-dialog-"+labelTarget,
                        function (content, trigger, showPopup) {
                            content.html(RIC.Templates.unmappedValues({
                                unmappedValues : unmappedJson
                            }));
                            showPopup();
                        },
                        {
                            onHover: true,
                            hideDelay : 10000
                        }
                    );
                }
            }
        });

        if(isWarningAdded){
            $(".hover-over-info").removeClass("hidden");
        }

    });
})(AJS.$);