package unit.calendarizacion;

import model.Recurso;
import repository.RecursoRepository;

import java.util.ArrayList;
import java.util.List;

public class RecursoRepositoryEnMemoria implements RecursoRepository {

    private final List<Recurso> recursos = new ArrayList<>();

    @Override
    public void guardar(Recurso recurso) {
        recursos.add(recurso);
    }

    @Override
    public List<Recurso> listar() {
        return recursos;
    }

    @Override
    public Recurso buscarPorId(String id) {
        return recursos.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void actualizar(Recurso recurso) {
        recursos.removeIf(r -> r.getId().equals(recurso.getId()));
        recursos.add(recurso);
    }

    @Override
    public void eliminar(Recurso recurso) {
        recursos.removeIf(r -> r.getId().equals(recurso.getId()));
    }

    @Override
    public List<Recurso> buscarPorCategoria(String categoriaId) {
        return recursos.stream()
                .filter(r -> r.getCategoria() != null && r.getCategoria().getId().equals(categoriaId))
                .toList();
    }

    @Override
    public List<Recurso> listarTodos() {
        return recursos;
    }
}
