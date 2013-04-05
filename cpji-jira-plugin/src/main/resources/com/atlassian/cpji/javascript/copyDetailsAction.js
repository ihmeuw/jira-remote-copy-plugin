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

        if($("#copy-attachments-group .attachments-list").length){
            var marker = $(RIC.Templates.warningMarker());
            $("#copy-attachments-group .attachments-error").append(marker);
            AJS.InlineDialog(marker, "attachments-popup",
                function (content, trigger, showPopup) {
                    content.css("padding", "10px").html( $("#copy-attachments-group .attachments-list").html());
                    showPopup();
                },
                {
                    onHover: true
                }
            );

        }

    });
})(AJS.$);


