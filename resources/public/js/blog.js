$(document).ready( function() {

    $("a.hider").click( function(event) {
        $(this).next().toggle("slow");
        return false;
    });

    $("a.submit").click( function(event) {

        $("form").submit();
        return false;

     });

    $("a.delete").click( function(event) {
     
        if( $(this).hasClass("confirm") ) {
            //goto link
            return true;
        }

        $(this).addClass("confirm");
        $(this).text("Click to confirm");
        return false;

     });

});

