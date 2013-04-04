(function ($) {
    $(function () {
        $(".fields-list label").each(function(){
            var labelTarget = $(this).attr("for");
            var unmapped = $("#unmapped-for-"+labelTarget);
            if(unmapped.length == 1){
                var unmappedMarker = $(RIC.Templates.unmappedMarker());
                $(this).prepend(unmappedMarker);
                AJS.InlineDialog(unmappedMarker, "unmapped-dialog-"+labelTarget,
                    function (content, trigger, showPopup) {
                        content.html(RIC.Templates.unmappedValues({
                            unmappedValues : JSON.parse(unmapped.text())
                        }));
                        showPopup();
                    },
                    {onHover: true}
                );
            }
        });

    });
})(AJS.$);