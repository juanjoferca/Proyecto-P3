package unit;
import model.CategoriaRecurso;
import model.EstadoReserva;
import model.Recurso;
import model.Reserva;
import model.DetalleReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.RecursoService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RecursoServiceTest {

    private RecursoService servicio;
    private RecursoRepositoryEnMemoria recursos;
    private CategoriaRepositoryEnMemoria categorias;
    private ReservaRepositoryEnMemoria reservas;
    private CategoriaRecurso categoria;

    @BeforeEach
    void prepararCadaPrueba() {
        recursos = new RecursoRepositoryEnMemoria();
        categorias = new CategoriaRepositoryEnMemoria();
        reservas = new ReservaRepositoryEnMemoria();
        servicio = new RecursoService(recursos, categorias, reservas);

        categoria = new CategoriaRecurso("CAT-000001", "Salas");
        categorias.guardar(categoria);
    }

    @Test
    void registrarRecursoNuloDebeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> {
            RecursoService recursoService = new RecursoService();
            recursoService.registrar(null);
        });
    }

    @Test
    void buscarRecursoPorIdNuloDebeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> {
            RecursoService recursoService = new RecursoService();
            recursoService.buscarPorId(null);
        });
    }

    @Test
    void eliminarRecursoNuloDebeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> {
            RecursoService recursoService = new RecursoService();
            recursoService.eliminar(null);
        });
    }

    @Test
    void idNuloDebeLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> {
            RecursoService recursoService = new RecursoService();
            recursoService.buscarPorId(null);
        });
    }

    @Test
    void registraUnRecursoValido() {
        servicio.registrar(new Recurso("R-001", categoria, "Sala de juntas"));

        Recurso guardado = servicio.buscarPorId("R-001");
        assertNotNull(guardado);
        assertEquals("Sala de juntas", guardado.getDescripcion());
    }

    @Test
    void noPermiteIdDeRecursoRepetido() {
        servicio.registrar(new Recurso("R-001", categoria, "Sala de juntas"));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(new Recurso("R-001", categoria, "Otra sala")));
    }

    @Test
    void noPermiteCategoriaInexistente() {
        CategoriaRecurso categoriaFalsa = new CategoriaRecurso("CAT-999999", "No existe");

        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(new Recurso("R-001", categoriaFalsa, "Sala de juntas")));
    }

    @Test
    void noPermiteDescripcionVacia() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(new Recurso("R-001", categoria, "")));
    }

    @Test
    void noEliminaUnRecursoConReservasFuturasActivas() {
        servicio.registrar(new Recurso("R-001", categoria, "Sala de juntas"));
        Recurso recurso = servicio.buscarPorId("R-001");

        Reserva reserva = new Reserva("RES-000001", "Reunion", LocalDate.now().plusDays(5),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "111", EstadoReserva.ACTIVA);
        reserva.setDetalles(List.of(new DetalleReserva(categoria, recurso)));
        reservas.guardar(reserva);

        assertThrows(IllegalArgumentException.class, () -> servicio.eliminar(recurso));
        assertNotNull(servicio.buscarPorId("R-001"));
    }

    @Test
    void siEliminaUnRecursoConReservasCanceladasOPasadas() {
        servicio.registrar(new Recurso("R-001", categoria, "Sala de juntas"));
        Recurso recurso = servicio.buscarPorId("R-001");

        Reserva cancelada = new Reserva("RES-000002", "Reunion", LocalDate.now().plusDays(5),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "111", EstadoReserva.CANCELADA);
        cancelada.setDetalles(List.of(new DetalleReserva(categoria, recurso)));
        reservas.guardar(cancelada);

        assertDoesNotThrow(() -> servicio.eliminar(recurso));
        assertNull(servicio.buscarPorId("R-001"));
    }
}
