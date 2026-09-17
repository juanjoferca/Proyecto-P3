package integration.estadisticas;

import model.EstadoReserva;
import model.Reserva;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.ReservaXmlRepository;
import service.EstadisticasActividadesService;
import util.DateUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class EstadisticasActividadesIT {

    // Las reservas de estas pruebas no llevan detalles (recurso/categoría),
    // así que ReservaXmlRepository solo lee y escribe reservas.xml.
    private static final Path RESERVAS_PATH = Paths.get("src", "main", "resources", "data", "reservas.xml");

    private String reservasOriginal;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(RESERVAS_PATH.getParent());
        reservasOriginal = Files.exists(RESERVAS_PATH) ? Files.readString(RESERVAS_PATH) : null;
        Files.writeString(RESERVAS_PATH, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><reservas/>");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (reservasOriginal == null) {
            Files.deleteIfExists(RESERVAS_PATH);
        } else {
            Files.writeString(RESERVAS_PATH, reservasOriginal);
        }
    }

    @Test
    void agrupaCorrectamentePorSemanaConReservasRealesPersistidas() {
        ReservaXmlRepository reservaRepository = new ReservaXmlRepository();
        reservaRepository.guardar(new Reserva("RES-700", "Actividad", LocalDate.of(2026, 8, 3),
                LocalTime.of(8, 0), LocalTime.of(9, 0), "F-100", EstadoReserva.ACTIVA));
        reservaRepository.guardar(new Reserva("RES-701", "Actividad", LocalDate.of(2026, 8, 5),
                LocalTime.of(8, 0), LocalTime.of(9, 0), "F-100", EstadoReserva.ACTIVA));
        reservaRepository.guardar(new Reserva("RES-702", "Actividad", LocalDate.of(2026, 8, 12),
                LocalTime.of(8, 0), LocalTime.of(9, 0), "F-100", EstadoReserva.ACTIVA));

        EstadisticasActividadesService servicio = new EstadisticasActividadesService(reservaRepository);
        LinkedHashMap<String, Integer> resultado = servicio.calcular(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        String semana1 = DateUtils.etiquetaSemana(LocalDate.of(2026, 8, 3));
        String semana2 = DateUtils.etiquetaSemana(LocalDate.of(2026, 8, 12));

        assertEquals(2, resultado.size());
        assertEquals(2, resultado.get(semana1));
        assertEquals(1, resultado.get(semana2));
    }

    @Test
    void unPeriodoInvalidoLanzaExcepcionConElRepositorioReal() {
        ReservaXmlRepository reservaRepository = new ReservaXmlRepository();
        EstadisticasActividadesService servicio = new EstadisticasActividadesService(reservaRepository);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.calcular(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 8, 1)));
    }

    @Test
    void lasReservasCanceladasRealesQuedanExcluidasDelConteo() {
        ReservaXmlRepository reservaRepository = new ReservaXmlRepository();
        reservaRepository.guardar(new Reserva("RES-703", "Actividad", LocalDate.of(2026, 8, 3),
                LocalTime.of(8, 0), LocalTime.of(9, 0), "F-100", EstadoReserva.ACTIVA));
        reservaRepository.guardar(new Reserva("RES-704", "Actividad", LocalDate.of(2026, 8, 4),
                LocalTime.of(8, 0), LocalTime.of(9, 0), "F-100", EstadoReserva.CANCELADA));

        EstadisticasActividadesService servicio = new EstadisticasActividadesService(reservaRepository);
        LinkedHashMap<String, Integer> resultado = servicio.calcular(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        String semana1 = DateUtils.etiquetaSemana(LocalDate.of(2026, 8, 3));
        assertEquals(1, resultado.get(semana1));
    }

    @Test
    void unPeriodoSinActividadesPersistidasDevuelveUnMapaVacio() {
        ReservaXmlRepository reservaRepository = new ReservaXmlRepository();
        EstadisticasActividadesService servicio = new EstadisticasActividadesService(reservaRepository);

        LinkedHashMap<String, Integer> resultado = servicio.calcular(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertTrue(resultado.isEmpty());
    }
}
