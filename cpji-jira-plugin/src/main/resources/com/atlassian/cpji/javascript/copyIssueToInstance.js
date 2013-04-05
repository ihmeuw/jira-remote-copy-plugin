(function ($) {
    $(function () {
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
                        {onHover: true}
                    );
                }
            }
        });
        console.log(isWarningAdded);
        if(isWarningAdded){
            $(".hover-over-info").removeClass("hidden");
        }

    });
})(AJS.$);