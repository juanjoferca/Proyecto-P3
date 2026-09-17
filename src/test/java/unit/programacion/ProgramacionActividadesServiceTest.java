package unit.programacion;

import model.EstadoReserva;
import model.FilaActividad;
import model.Reserva;
import model.ResultadoProgramacionActividades;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.ProgramacionActividadesService;
import util.DateUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProgramacionActividadesServiceTest {

    private ReservaRepositoryEnMemoria reservas;
    private ProgramacionActividadesService servicio;

    @BeforeEach
    void prepararCadaPrueba() {
        reservas = new ReservaRepositoryEnMemoria();
        servicio = new ProgramacionActividadesService(reservas);
    }

    private Reserva crearReserva(String id, String actividad, LocalDate fecha, LocalTime inicio, LocalTime fin,
                                  String funcionarioId, EstadoReserva estado) {
        return new Reserva(id, actividad, fecha, inicio, fin, funcionarioId, estado);
    }

    private FilaActividad filaDeHora(List<FilaActividad> filas, String horaTexto) {
        return filas.stream()
                .filter(f -> f.getHoraTexto().equals(horaTexto))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void construyeLaMatrizSemanalConUnaActividadEnElDiaYHoraCorrectos() {
        LocalDate fechaReferencia = LocalDate.of(2026, 9, 16);
        LocalDate lunes = DateUtils.inicioSemana(fechaReferencia);

        reservas.guardar(crearReserva("RES-001", "Capacitación", lunes,
                LocalTime.of(8, 0), LocalTime.of(9, 0), "F-100", EstadoReserva.ACTIVA));

        ResultadoProgramacionActividades resultado = servicio.construirMatrizSemana(fechaReferencia);

        assertEquals(7, resultado.getDias().size());
        assertEquals(lunes, resultado.getDias().get(0));

        FilaActividad fila = filaDeHora(resultado.getFilas(), "08:00");
        assertEquals("Capacitación (F-100)", fila.getValor(lunes));
    }

    @Test
    void variasActividadesEnLaMismaCeldaSeAcumulan() {
        LocalDate fechaReferencia = LocalDate.of(2026, 9, 16);
        LocalDate lunes = DateUtils.inicioSemana(fechaReferencia);

        reservas.guardar(crearReserva("RES-001", "Capacitación", lunes,
                LocalTime.of(8, 0), LocalTime.of(9, 0), "F-100", EstadoReserva.ACTIVA));
        reservas.guardar(crearReserva("RES-002", "Reunión", lunes,
                LocalTime.of(8, 0), LocalTime.of(9, 0), "F-200", EstadoReserva.ACTIVA));

        ResultadoProgramacionActividades resultado = servicio.construirMatrizSemana(fechaReferencia);

        FilaActividad fila = filaDeHora(resultado.getFilas(), "08:00");
        assertEquals("Capacitación (F-100) | Reunión (F-200)", fila.getValor(lunes));
    }

    @Test
    void lasReservasCanceladasQuedanExcluidasDeLaProgramacion() {
        LocalDate fechaReferencia = LocalDate.of(2026, 9, 16);
        LocalDate lunes = DateUtils.inicioSemana(fechaReferencia);

        reservas.guardar(crearReserva("RES-001", "Capacitación", lunes,
                LocalTime.of(8, 0), LocalTime.of(9, 0), "F-100", EstadoReserva.CANCELADA));

        ResultadoProgramacionActividades resultado = servicio.construirMatrizSemana(fechaReferencia);

        FilaActividad fila = filaDeHora(resultado.getFilas(), "08:00");
        assertEquals("", fila.getValor(lunes));
    }

    @Test
    void unaSemanaSinActividadesDevuelveTodasLasCeldasVacias() {
        LocalDate fechaReferencia = LocalDate.of(2026, 9, 16);

        ResultadoProgramacionActividades resultado = servicio.construirMatrizSemana(fechaReferencia);

        for (FilaActividad fila : resultado.getFilas()) {
            for (LocalDate dia : resultado.getDias()) {
                assertEquals("", fila.getValor(dia));
            }
        }
    }

    @Test
    void rechazaUnaFechaDeReferenciaNula() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.construirMatrizSemana(null));
    }
}
