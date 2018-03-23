var evento="";
var wiki="";
function muestraEvento(mEvento){
switch (mEvento){
case "carnaval":
evento ="Carnaval";
wiki="El carnaval es una celebración que tiene lugar inmediatamente antes del inicio de la cuaresma cristiana, que se inicia a su vez con el Miércoles de Ceniza, que tiene fecha variable (entre febrero y marzo según el año). El carnaval combina algunos elementos como disfraces, desfiles, y fiestas en la calle.";
break;
case "fallas":
evento="Fallas";
wiki = "Las Fallas (Falles en valenciano) son unas fiestas que van del 15 al 19 de marzo con una tradición arraigada en la ciudad de Valencia y diferentes poblaciones de la Comunidad Valenciana. Oficialmente empiezan el último domingo de febrero con el acto de la Crida.";
break;
case "nochevieja":
evento="Nochevieja";
wiki = "La Nochevieja, víspera de Año Nuevo, Año Viejo o fin de año, es la última noche del año en el calendario gregoriano, comprendiendo desde 31 de diciembre hasta el 1 de enero (Año Nuevo). Desde que se cambió al calendario gregoriano en el año 1582, se suele celebrar esta festividad, aunque ha ido evolucionando en sus costumbres y supersticiones.";
break;
case "sanjuan":
evento="Noche de San Juan";
wiki ="La víspera de San Juan o noche de San Juan es una festividad cristiana, de origen pagano (Litha) celebrada el 23 de junio,1 en la que se suelen encender hogueras o fuegos y ligada con las celebraciones en las que se festejaba la llegada del solsticio de verano, el 21 de junio en el hemisferio norte, cuyo rito principal consiste en encender una hoguera.";
break;
case "semanasanta":
evento="Semana Santa";
wiki="La Semana Santa es la conmemoración anual cristiana de la Pasión, Muerte y Resurrección de Jesús de Nazaret. Por eso, es un período de intensa actividad litúrgica dentro de las diversas confesiones cristianas. Da comienzo el Domingo de Ramos y finaliza el Domingo de Resurrección,1 aunque su celebración suele iniciarse en varios lugares el viernes anterior (Viernes de Dolores) y se considera parte de la misma el Domingo de Resurrección.";
break;
default:
wiki ="No se encuentra el evento";
}
muestra(evento,wiki);
}
function muestra(mEvento, mWiki){
document.getElementById("evento").innerHTML=mEvento;
document.getElementById("wiki").innerHTML=mWiki;
}
function volver(){
jsInterfazNativa.volver();
}