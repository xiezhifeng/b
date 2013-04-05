(function($){

AJS.DataTable = function(obj, columns, options){
    
    var tbl = null;
    if (obj && obj.jquery) { // if $
        tbl = obj;
    }else if (typeof obj == "string") { // if $ selector
        tbl = $(obj);
    }
    tbl.addClass('data-table');
    if (columns && columns.length){
        this.columns = columns;
        var tr = $('<tr></tr>').appendTo(tbl);
        tr.addClass('data-table-header');
        for (var i = 0; i < columns.length; i++){
            var column = columns[i];
            tr.append('<th class="' + column.className + '">' + column.title + '</th>');
        }
    }
    this.tbl = tbl;
    this.rowIdx = 0;
    
};
AJS.DataTable.prototype.addRow = function(data){
    var row = $('<tr></tr>').appendTo(this.tbl);
    var columns = this.columns;
    for (var i = 0; i < this.columns.length; i++){
        var column = columns[i];
        var td = $('<td></td>').appendTo(row);
        td.addClass(column.className);
        column.renderCell(td, data);
    }
    row.data('row-data', data);
    this._bindRowJs(row, this.rowIdx, 'selected', 'hover');
    this.rowIdx += 1;
    row.attr('tabindex', '-1');
    
};

AJS.DataTable.prototype.selectRow = function(index){
    var row = $('tbody tr', this.tbl)[index + 1];
    $(row).focus();
};

AJS.DataTable.prototype._bindRowJs = function(row, rowIndex, selectedClassName, hoverClassName){
    var dataTable = this;

    row.click(function (e) {
        
        if (selectedClassName) {
            $(dataTable.tbl).find("." + selectedClassName).removeClass(selectedClassName);
            $(this).addClass(selectedClassName);
        }                      
        var data = row.data('row-data');
        dataTable.tbl.trigger('row-select', [data]);
    });
    
    var rowKeyHandler = function (e) {                
        switch(e.keyCode){
            case 13:{
                var keyupHandler;
                var data = row.data('row-data');
                row.keyup(keyupHandler = function(e){
                    dataTable.tbl.trigger('row-action', [data]);
                    row.unbind('keyup', keyupHandler);
                    return AJS.stopEvent(e);
                });
                return AJS.stopEvent(e);            
                break;
            }
            case 38:{
                if (rowIndex > 0){
                    row.prev().focus();     
                }                    
                return AJS.stopEvent(e);
            }
            case 40:{
                var numRows = $('tbody tr', dataTable.tbl).length;
                if (rowIndex < numRows - 1){
                    row.next().focus();      
                }                    
                return AJS.stopEvent(e);
            }
        }
    };
    
    // firefox 3.5 has a quark with keydown. Everything but FF has a quark with arrow keys and keypress.
    if ($.browser.mozilla){ 
        row.keypress(rowKeyHandler);
    }
    else{
        row.keydown(rowKeyHandler); 
    }
    
    row.focus(function(e) {
        row.click();
    });
    if (hoverClassName) {
        row.hover(function () {
            $(this).addClass(hoverClassName);
        }, function () {
            $(this).removeClass(hoverClassName);
        });
    }
};
})(AJS.$);