package integration.calendarizacion;

import model.CategoriaRecurso;
import model.DetalleReserva;
import model.EstadoReserva;
import model.FilaCalendarizacion;
import model.Recurso;
import model.Reserva;
import model.ResultadoCalendarizacion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.RecursoXmlRepository;
import repository.ReservaXmlRepository;
import service.CalendarizacionRecursosService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalendarizacionRecursosIT {

    private static final Path RECURSOS_PATH = Paths.get("src", "main", "resources", "data", "recursos.xml");
    private static final Path RESERVAS_PATH = Paths.get("src", "main", "resources", "data", "reservas.xml");

    // CategoriaXmlRepository lee/escribe "categorias.xml" en la raíz del proyecto
    // (ruta distinta a la de recursos/reservas) y se invoca indirectamente al
    // listar recursos y al leer los detalles de una reserva, así que también
    // debe respaldarse para no contaminar el archivo real del proyecto.
    private static final String ARCHIVO_CATEGORIAS = "categorias.xml";

    private String recursosOriginal;
    private String reservasOriginal;
    private File respaldoCategorias;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(RECURSOS_PATH.getParent());

        recursosOriginal = Files.exists(RECURSOS_PATH) ? Files.readString(RECURSOS_PATH) : null;
        reservasOriginal = Files.exists(RESERVAS_PATH) ? Files.readString(RESERVAS_PATH) : null;

        Files.writeString(RECURSOS_PATH, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><recursos/>");
        Files.writeString(RESERVAS_PATH, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><reservas/>");

        File categoriasOriginal = new File(ARCHIVO_CATEGORIAS);
        if (categoriasOriginal.exists()) {
            respaldoCategorias = new File(ARCHIVO_CATEGORIAS + ".bak");
            categoriasOriginal.renameTo(respaldoCategorias);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        if (recursosOriginal == null) {
            Files.deleteIfExists(RECURSOS_PATH);
        } else {
            Files.writeString(RECURSOS_PATH, recursosOriginal);
        }

        if (reservasOriginal == null) {
            Files.deleteIfExists(RESERVAS_PATH);
        } else {
            Files.writeString(RESERVAS_PATH, reservasOriginal);
        }

        new File(ARCHIVO_CATEGORIAS).delete();
        if (respaldoCategorias != null && respaldoCategorias.exists()) {
            respaldoCategorias.renameTo(new File(ARCHIVO_CATEGORIAS));
        }
    }

    @Test
    void construyeLaMatrizConDatosPersistidosEnLosXmlReales() {
        RecursoXmlRepository recursoRepository = new RecursoXmlRepository();
        Recurso recurso = new Recurso("R-001", CategoriaRecurso.valueOf("SALA"), "Sala de reuniones");
        recursoRepository.guardar(recurso);

        ReservaXmlRepository reservaRepository = new ReservaXmlRepository(recursoRepository);
        Reserva reserva = new Reserva("RES-500", "Reunión de equipo",
                LocalDate.of(2026, 9, 24), LocalTime.of(8, 0), LocalTime.of(9, 0),
                "F-100", EstadoReserva.ACTIVA);
        reserva.setDetalles(List.of(new DetalleReserva(CategoriaRecurso.valueOf("SALA"), recurso)));
        reservaRepository.guardar(reserva);

        CalendarizacionRecursosService servicio =
                new CalendarizacionRecursosService(recursoRepository, reservaRepository);
        ResultadoCalendarizacion resultado = servicio.construirMatriz(LocalDate.of(2026, 9, 24), "SALA");

        assertFalse(resultado.estaVacio());
        FilaCalendarizacion fila = resultado.getFilas().stream()
                .filter(f -> f.getHoraTexto().equals("08:00"))
                .findFirst()
                .orElseThrow();
        assertEquals("Reunión de equipo - F-100", fila.getValor("R-001"));
    }

    @Test
    void unaCategoriaSinRecursosPersistidosDevuelveMatrizVacia() {
        RecursoXmlRepository recursoRepository = new RecursoXmlRepository();
        ReservaXmlRepository reservaRepository = new ReservaXmlRepository(recursoRepository);
        CalendarizacionRecursosService servicio =
                new CalendarizacionRecursosService(recursoRepository, reservaRepository);

        ResultadoCalendarizacion resultado = servicio.construirMatriz(LocalDate.of(2026, 9, 24), "SIN-RECURSOS");

        assertTrue(resultado.estaVacio());
        assertTrue(resultado.getFilas().isEmpty());
    }

    @Test
    void unaReservaCanceladaPersistidaNoOcupaLaMatriz() {
        RecursoXmlRepository recursoRepository = new RecursoXmlRepository();
        Recurso recurso = new Recurso("R-002", CategoriaRecurso.valueOf("SALA"), "Sala de reuniones");
        recursoRepository.guardar(recurso);

        ReservaXmlRepository reservaRepository = new ReservaXmlRepository(recursoRepository);
        Reserva reserva = new Reserva("RES-501", "Reunión cancelada",
                LocalDate.of(2026, 9, 24), LocalTime.of(8, 0), LocalTime.of(9, 0),
                "F-100", EstadoReserva.CANCELADA);
        reserva.setDetalles(List.of(new DetalleReserva(CategoriaRecurso.valueOf("SALA"), recurso)));
        reservaRepository.guardar(reserva);

        CalendarizacionRecursosService servicio =
                new CalendarizacionRecursosService(recursoRepository, reservaRepository);
        ResultadoCalendarizacion resultado = servicio.construirMatriz(LocalDate.of(2026, 9, 24), "SALA");

        FilaCalendarizacion fila = resultado.getFilas().stream()
                .filter(f -> f.getHoraTexto().equals("08:00"))
                .findFirst()
                .orElseThrow();
        assertEquals("", fila.getValor("R-002"));
    }
}
