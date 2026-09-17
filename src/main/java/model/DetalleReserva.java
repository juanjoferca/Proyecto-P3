package model;

public class DetalleReserva {

    private CategoriaRecurso categoria;
    private Recurso recurso;

    public DetalleReserva(CategoriaRecurso categoria, Recurso recurso) {
        if (categoria == null) {
            throw new IllegalArgumentException("La categoría del detalle no puede ser nula.");
        }
        if (recurso == null) {
            throw new IllegalArgumentException("El recurso del detalle no puede ser nulo.");
        }
        this.categoria = categoria;
        this.recurso = recurso;
    }

    public CategoriaRecurso getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaRecurso categoria) {
        this.categoria = categoria;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public void setRecurso(Recurso recurso) {
        this.recurso = recurso;
    }
}