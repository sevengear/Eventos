function add(fichero, imagen){
var listItem = '<li><img src="'+imagen+'" heigh="150px">' + fichero + '</li>';
$("#lista").append(listItem);
$('#lista').listview('refresh');
}
function vaciar(){
$("#lista").empty();
$('#lista').listview('refresh');
}