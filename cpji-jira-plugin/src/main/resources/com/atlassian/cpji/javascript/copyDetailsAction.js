(function ($){
    $(function () {
        $(".singleSelect").each(function(){
            new AJS.SingleSelect({
                element: AJS.$(this),
                itemAttrDisplayed: "label",
                width: "250px",
                revertOnInvalid: true
            })
        });

        //JRA-32483 - we do not display hovers on IE 10
        var isIE10 = (AJS.$.browser.msie && ~~(AJS.$.browser.version) >= 10) && ( !JIRA.Version.isGreaterThanOrEqualTo("6") );
        if(!isIE10 && $("#copy-attachments-group .attachments-list").length){

            var marker = $("#copy-attachments-group .attachments-error .aui-lozenge");
            AJS.InlineDialog(marker, "attachments-popup",
                function (content, trigger, showPopup) {
                    content.css("padding", "10px").html( $("#copy-attachments-group .attachments-list").html());
                    showPopup();
                },
                {
                    onHover: true,
                    hideDelay : 10000
                }
            );

        }

    });
})(AJS.$);


