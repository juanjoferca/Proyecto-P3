package unit.estadisticas;

import model.EstadoReserva;
import model.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.EstadisticasActividadesService;
import util.DateUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class EstadisticasActividadesServiceTest {

    private ReservaRepositoryEnMemoria reservas;
    private EstadisticasActividadesService servicio;

    @BeforeEach
    void prepararCadaPrueba() {
        reservas = new ReservaRepositoryEnMemoria();
        servicio = new EstadisticasActividadesService(reservas);
    }

    private Reserva crearReserva(String id, LocalDate fecha, EstadoReserva estado) {
        return new Reserva(id, "Actividad", fecha, LocalTime.of(8, 0), LocalTime.of(9, 0), "F-100", estado);
    }

    @Test
    void agrupaCorrectamentePorSemanaDentroDeUnPeriodo() {
        reservas.guardar(crearReserva("RES-001", LocalDate.of(2026, 8, 3), EstadoReserva.ACTIVA));
        reservas.guardar(crearReserva("RES-002", LocalDate.of(2026, 8, 5), EstadoReserva.ACTIVA));
        reservas.guardar(crearReserva("RES-003", LocalDate.of(2026, 8, 12), EstadoReserva.ACTIVA));

        LinkedHashMap<String, Integer> resultado = servicio.calcular(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        String semana1 = DateUtils.etiquetaSemana(LocalDate.of(2026, 8, 3));
        String semana2 = DateUtils.etiquetaSemana(LocalDate.of(2026, 8, 12));

        assertEquals(2, resultado.size());
        assertEquals(2, resultado.get(semana1));
        assertEquals(1, resultado.get(semana2));
    }

    @Test
    void unPeriodoConDesdePosteriorAHastaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.calcular(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 8, 1)));
    }

    @Test
    void lasReservasCanceladasQuedanExcluidasDelConteo() {
        reservas.guardar(crearReserva("RES-001", LocalDate.of(2026, 8, 3), EstadoReserva.ACTIVA));
        reservas.guardar(crearReserva("RES-002", LocalDate.of(2026, 8, 4), EstadoReserva.CANCELADA));

        LinkedHashMap<String, Integer> resultado = servicio.calcular(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        String semana1 = DateUtils.etiquetaSemana(LocalDate.of(2026, 8, 3));
        assertEquals(1, resultado.get(semana1));
    }

    @Test
    void unPeriodoSinActividadesDevuelveUnMapaVacio() {
        LinkedHashMap<String, Integer> resultado = servicio.calcular(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void rechazaFechasNulas() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.calcular(null, LocalDate.of(2026, 8, 31)));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.calcular(LocalDate.of(2026, 8, 1), null));
    }
}
