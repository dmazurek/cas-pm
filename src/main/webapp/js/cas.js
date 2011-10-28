// $Id: cas.js 21773 2010-09-26 03:50:56Z battags $
var editInnerHTML = "";
var deleteInnerHTML = "";
var currentRow = null;

function swapButtonsForConfirm(rowId, serviceId) {

    resetOldValue();
    var editCell = $("#edit"+rowId);
    var deleteCell = $("#delete"+rowId);

    var row = $("#row" + rowId);
    row.removeClass("over");
    row.addClass("highlightBottom");

    editInnerHTML = editCell.html();
    deleteInnerHTML = deleteCell.html();
    currentRow = rowId;
    
    editCell.html("Really?");
    deleteCell.html("<a id=\"yes\" href=\"deleteRegisteredService.html?id=" + serviceId + "\">Yes</a> <a id=\"no\" href=\"#\" onclick=\"resetOldValue();return false;\">No</a>");
}

function resetOldValue() {
    if (currentRow != null) {
        var curRow = $("#row"+currentRow);
        curRow.removeClass("over");
        curRow.removeClass("highlightBottom");
        var editCell = $("#edit"+currentRow);
        var deleteCell = $("#delete"+currentRow);

        editCell.html(editInnerHTML);
        deleteCell.html(deleteInnerHTML);
       
        editInnerHTML = null;
        deleteInnerHTML = null;
        currentRow = null;
    }
}

$(document).ready(function(){
    //focus username field
    $("input:visible:enabled:first").focus();
    //flash error box
    $('#status').animate({ backgroundColor: 'rgb(187,0,0)' }, 30).animate({ backgroundColor: 'rgb(255,238,221)' }, 500);

    //flash success box
    $('#msg').animate({ backgroundColor: 'rgb(51,204,0)' }, 30).animate({ backgroundColor: 'rgb(221,255,170)' }, 500);
	
	//flash user administration box
	if($('#admin.expire').length) {
		$('#admin.expire').animate({ backgroundColor: '#ffbf80' }, 30).animate({ backgroundColor: '#fed' }, 500);
	} else {
		$('#admin').animate({ backgroundColor: 'rgb(109,187,252)' }, 30).animate({ backgroundColor: 'rgb(210,233,252)' }, 500);
	}
	
	
});

