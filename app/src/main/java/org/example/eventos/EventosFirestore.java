package org.example.eventos;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Created by Miguel Á. Núñez on 20/03/2018.
 */

public class EventosFirestore {
    public static String EVENTOS = "eventos";
    static String SERVIDOR = "https://pixabay.com/static/uploads/photo/";

    public static void crearEventos() {
        Evento evento;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        evento = new Evento("Carnaval", "Rio de Janeiro", "21/02/2017", SERVIDOR + "2014/08/06/11/51/carnival-411494_960_720.jpg");
        db.collection(EVENTOS).document("carnaval").set(evento);
        evento = new Evento("Fallas", "Valencia", "19/03/2017", SERVIDOR + "2015/03/22/18/12/failures-685055_960_720.jpg");
        db.collection(EVENTOS).document("fallas").set(evento);
        evento = new Evento("Nochevieja", "Nueva York", "31/12/2016", SERVIDOR + "2013/02/10/15/35/fireworks-80187_960_720.jpg");
        db.collection(EVENTOS).document("nochevieja").set(evento);
        evento = new Evento("Noche de San Juan", "Alicante", "23/06/2017", SERVIDOR + "2013/06/30/18/29/midsummer-142484_960_720.jpg");
        db.collection(EVENTOS).document("sanjuan").set(evento);
        evento = new Evento("Semana Santa", "Sevilla", "14/04/2017", SERVIDOR + "2016/03/17/10/08/easter-1262664_960_720.jpg");
        db.collection(EVENTOS).document("semanasanta").set(evento);
    }
}
