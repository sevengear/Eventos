package org.example.eventos;

/**
 * Created by Miguel Á. Núñez on 20/03/2018.
 */

public class Evento {
    private String evento;
    private String ciudad;
    private String fecha;
    private String imagen;

    public Evento() {
    }

    public Evento(String evento, String ciudad, String fecha, String imagen) {
        this.evento = evento;
        this.ciudad = ciudad;
        this.fecha = fecha;
        this.imagen = imagen;
    }

    public String getEvento() {
        return evento;
    }

    public void setEvento(String evento) {
        this.evento = evento;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }
}
