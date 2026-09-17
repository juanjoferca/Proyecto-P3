package service;

import model.EstadoReserva;
import model.Recurso;
import model.CategoriaRecurso;
import repository.CategoriaRepository;
import repository.RecursoRepository;
import repository.ReservaRepository;

import java.time.LocalDate;
import java.util.List;

public class RecursoService {
    private final RecursoRepository repository;
    private final CategoriaRepository categoriaRepository;
    private final ReservaRepository reservaRepository;

    public RecursoService() {
        this.repository = null;
        this.categoriaRepository = null;
        this.reservaRepository = null;
    }

    public RecursoService(RecursoRepository repository) {
        this(repository, null, null);
    }

    public RecursoService(RecursoRepository repository, CategoriaRepository categoriaRepository,
                          ReservaRepository reservaRepository) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<Recurso> listarTodos() {
        return repository.listar();
    }

    public Recurso buscarPorId(String id) {
        validarId(id);
        return repository.buscarPorId(id.trim());
    }

    public void registrar(Recurso recurso) {
        validarRecurso(recurso);
        if (repository.buscarPorId(recurso.getId().trim()) != null) {
            throw new IllegalArgumentException("El ID del recurso ya existe.");
        }
        repository.guardar(recurso);
    }

    public void actualizar(Recurso recurso) {
        validarRecurso(recurso);
        if (repository.buscarPorId(recurso.getId().trim()) == null) {
            throw new IllegalArgumentException("No existe un recurso con ese ID.");
        }
        repository.actualizar(recurso);
    }

    public void eliminar(Recurso recurso) {
        if (recurso == null) {
            throw new IllegalArgumentException("El recurso no puede ser nulo.");
        }
        validarId(recurso.getId());
        if (repository.buscarPorId(recurso.getId().trim()) == null) {
            throw new IllegalArgumentException("No existe un recurso con ese ID.");
        }

        if (reservaRepository != null) {
            boolean tieneReservasFuturas = reservaRepository.listar().stream()
                    .anyMatch(r -> r.getEstado() == EstadoReserva.ACTIVA
                            && !r.getFecha().isBefore(LocalDate.now())
                            && r.getDetalles().stream().anyMatch(d -> d.getRecurso() != null
                                    && recurso.getId().equals(d.getRecurso().getId())));
            if (tieneReservasFuturas) {
                throw new IllegalArgumentException(
                        "No se puede eliminar el recurso porque tiene reservas futuras activas.");
            }
        }

        repository.eliminar(recurso);
    }

    private void validarRecurso(Recurso recurso) {
        if (recurso == null) {
            throw new IllegalArgumentException("El recurso no puede ser nulo.");
        }
        validarId(recurso.getId());

        CategoriaRecurso categoria = recurso.getCategoria();
        if (categoria == null || categoria.isEmpty()) {
            throw new IllegalArgumentException("La categoría del recurso no puede ser nula o vacía.");
        }

        if (categoriaRepository != null && categoriaRepository.buscarPorId(categoria.getId()) == null) {
            throw new IllegalArgumentException("La categoría seleccionada no existe.");
        }

        if (recurso.getDescripcion() == null || recurso.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripción del recurso no puede ser nula o vacía.");
        }
    }

    private void validarId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID del recurso no puede ser nulo o vacío.");
        }
    }
}
