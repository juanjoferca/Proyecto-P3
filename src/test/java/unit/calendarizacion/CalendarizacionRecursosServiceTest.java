package unit.calendarizacion;

import model.CategoriaRecurso;
import model.DetalleReserva;
import model.EstadoReserva;
import model.FilaCalendarizacion;
import model.Recurso;
import model.Reserva;
import model.ResultadoCalendarizacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.CalendarizacionRecursosService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalendarizacionRecursosServiceTest {

    private RecursoRepositoryEnMemoria recursos;
    private ReservaRepositoryEnMemoria reservas;
    private CalendarizacionRecursosService servicio;

    private CategoriaRecurso sala;
    private Recurso salaA;

    @BeforeEach
    void prepararCadaPrueba() {
        recursos = new RecursoRepositoryEnMemoria();
        reservas = new ReservaRepositoryEnMemoria();
        servicio = new CalendarizacionRecursosService(recursos, reservas);

        sala = new CategoriaRecurso("CAT-SALA", "Salas de reuniones");
        salaA = new Recurso("R-001", sala, "Sala A");
        recursos.guardar(salaA);
    }

    private Reserva crearReserva(String id, LocalDate fecha, LocalTime inicio, LocalTime fin,
                                  EstadoReserva estado, Recurso recurso) {
        Reserva reserva = new Reserva(id, "Reunión de equipo", fecha, inicio, fin, "F-100", estado);
        reserva.getDetalles().add(new DetalleReserva(recurso.getCategoria(), recurso));
        return reserva;
    }

    private FilaCalendarizacion filaDeHora(List<FilaCalendarizacion> filas, String horaTexto) {
        return filas.stream()
                .filter(f -> f.getHoraTexto().equals(horaTexto))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void construyeLaMatrizConLaOcupacionCorrectaParaUnaReservaActiva() {
        LocalDate fecha = LocalDate.of(2026, 9, 24);
        reservas.guardar(crearReserva("RES-001", fecha, LocalTime.of(8, 0), LocalTime.of(9, 0),
                EstadoReserva.ACTIVA, salaA));

        ResultadoCalendarizacion resultado = servicio.construirMatriz(fecha, "CAT-SALA");

        assertFalse(resultado.estaVacio());
        assertEquals(1, resultado.getRecursos().size());

        FilaCalendarizacion fila = filaDeHora(resultado.getFilas(), "08:00");
        assertEquals("Reunión de equipo - F-100", fila.getValor("R-001"));
    }

    @Test
    void unaHoraFueraDelRangoDeLaReservaQuedaLibre() {
        LocalDate fecha = LocalDate.of(2026, 9, 24);
        reservas.guardar(crearReserva("RES-001", fecha, LocalTime.of(8, 0), LocalTime.of(9, 0),
                EstadoReserva.ACTIVA, salaA));

        ResultadoCalendarizacion resultado = servicio.construirMatriz(fecha, "CAT-SALA");

        FilaCalendarizacion fila = filaDeHora(resultado.getFilas(), "10:00");
        assertEquals("", fila.getValor("R-001"));
    }

    @Test
    void unaCategoriaSinRecursosDevuelveUnaMatrizVacia() {
        ResultadoCalendarizacion resultado = servicio.construirMatriz(LocalDate.of(2026, 9, 24), "CAT-INEXISTENTE");

        assertTrue(resultado.estaVacio());
        assertTrue(resultado.getFilas().isEmpty());
    }

    @Test
    void lasReservasCanceladasNoOcupanLaMatriz() {
        LocalDate fecha = LocalDate.of(2026, 9, 24);
        reservas.guardar(crearReserva("RES-002", fecha, LocalTime.of(8, 0), LocalTime.of(9, 0),
                EstadoReserva.CANCELADA, salaA));

        ResultadoCalendarizacion resultado = servicio.construirMatriz(fecha, "CAT-SALA");

        FilaCalendarizacion fila = filaDeHora(resultado.getFilas(), "08:00");
        assertEquals("", fila.getValor("R-001"));
    }

    @Test
    void rechazaUnaFechaNula() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.construirMatriz(null, "CAT-SALA"));
    }

    @Test
    void rechazaUnaCategoriaNulaOEnBlanco() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.construirMatriz(LocalDate.of(2026, 9, 24), null));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.construirMatriz(LocalDate.of(2026, 9, 24), "   "));
    }
}
