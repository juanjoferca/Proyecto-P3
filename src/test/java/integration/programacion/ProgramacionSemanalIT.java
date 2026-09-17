package integration.programacion;

import model.EstadoReserva;
import model.FilaActividad;
import model.Reserva;
import model.ResultadoProgramacionActividades;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.ReservaXmlRepository;
import service.ProgramacionActividadesService;
import util.DateUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ProgramacionSemanalIT {

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
    void construyeLaMatrizSemanalConUnaReservaRealPersistida() {
        ReservaXmlRepository reservaRepository = new ReservaXmlRepository();
        LocalDate fechaReferencia = LocalDate.of(2026, 9, 16);
        LocalDate lunes = DateUtils.inicioSemana(fechaReferencia);

        Reserva reserva = new Reserva("RES-600", "Capacitación", lunes,
                LocalTime.of(8, 0), LocalTime.of(9, 0), "F-100", EstadoReserva.ACTIVA);
        reservaRepository.guardar(reserva);

        ProgramacionActividadesService servicio = new ProgramacionActividadesService(reservaRepository);
        ResultadoProgramacionActividades resultado = servicio.construirMatrizSemana(fechaReferencia);

        FilaActividad fila = resultado.getFilas().stream()
                .filter(f -> f.getHoraTexto().equals("08:00"))
                .findFirst()
                .orElseThrow();
        assertEquals("Capacitación (F-100)", fila.getValor(lunes));
    }

    @Test
    void variasActividadesRealesEnLaMismaCeldaSeAcumulan() {
        ReservaXmlRepository reservaRepository = new ReservaXmlRepository();
        LocalDate fechaReferencia = LocalDate.of(2026, 9, 16);
        LocalDate lunes = DateUtils.inicioSemana(fechaReferencia);

        reservaRepository.guardar(new Reserva("RES-601", "Capacitación", lunes,
                LocalTime.of(8, 0), LocalTime.of(9, 0), "F-100", EstadoReserva.ACTIVA));
        reservaRepository.guardar(new Reserva("RES-602", "Reunión", lunes,
                LocalTime.of(8, 0), LocalTime.of(9, 0), "F-200", EstadoReserva.ACTIVA));

        ProgramacionActividadesService servicio = new ProgramacionActividadesService(reservaRepository);
        ResultadoProgramacionActividades resultado = servicio.construirMatrizSemana(fechaReferencia);

        FilaActividad fila = resultado.getFilas().stream()
                .filter(f -> f.getHoraTexto().equals("08:00"))
                .findFirst()
                .orElseThrow();
        assertEquals("Capacitación (F-100) | Reunión (F-200)", fila.getValor(lunes));
    }

    @Test
    void unaReservaCanceladaRealQuedaExcluidaDeLaProgramacion() {
        ReservaXmlRepository reservaRepository = new ReservaXmlRepository();
        LocalDate fechaReferencia = LocalDate.of(2026, 9, 16);
        LocalDate lunes = DateUtils.inicioSemana(fechaReferencia);

        reservaRepository.guardar(new Reserva("RES-603", "Capacitación", lunes,
                LocalTime.of(8, 0), LocalTime.of(9, 0), "F-100", EstadoReserva.CANCELADA));

        ProgramacionActividadesService servicio = new ProgramacionActividadesService(reservaRepository);
        ResultadoProgramacionActividades resultado = servicio.construirMatrizSemana(fechaReferencia);

        FilaActividad fila = resultado.getFilas().stream()
                .filter(f -> f.getHoraTexto().equals("08:00"))
                .findFirst()
                .orElseThrow();
        assertEquals("", fila.getValor(lunes));
    }

    @Test
    void unaSemanaSinReservasPersistidasDevuelveCeldasVacias() {
        ReservaXmlRepository reservaRepository = new ReservaXmlRepository();
        LocalDate fechaReferencia = LocalDate.of(2026, 9, 16);

        ProgramacionActividadesService servicio = new ProgramacionActividadesService(reservaRepository);
        ResultadoProgramacionActividades resultado = servicio.construirMatrizSemana(fechaReferencia);

        for (FilaActividad fila : resultado.getFilas()) {
            for (LocalDate dia : resultado.getDias()) {
                assertEquals("", fila.getValor(dia));
            }
        }
    }
}
