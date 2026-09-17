package integration.reportes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import report.PdfReportService;
import report.ReportHeader;
import report.ReportTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeneracionPdfIT {

    private PdfReportService servicio;
    private Path archivoTabla;
    private Path archivoMatriz;

    @BeforeEach
    void prepararCadaPrueba() throws IOException {
        servicio = new PdfReportService();
        archivoTabla = Files.createTempFile("reporte-tabla", ".pdf");
        archivoMatriz = Files.createTempFile("reporte-matriz", ".pdf");
        Files.deleteIfExists(archivoTabla);
        Files.deleteIfExists(archivoMatriz);
    }

    @AfterEach
    void limpiarArchivosGenerados() throws IOException {
        Files.deleteIfExists(archivoTabla);
        Files.deleteIfExists(archivoMatriz);
    }

    @Test
    void generaUnArchivoPdfRealConContenidoParaUnReporteDeTabla() throws Exception {
        ReportHeader header = new ReportHeader("Calendarización de recursos", "admin", "Categoría: SALA");
        ReportTable tabla = new ReportTable(List.of("Hora", "R-001"));
        tabla.agregarFila("08:00", "Reunión - F-100");

        servicio.generarReporteTabla(archivoTabla.toString(), header, tabla);

        assertTrue(Files.exists(archivoTabla));
        assertTrue(Files.size(archivoTabla) > 0);
    }

    @Test
    void generaUnArchivoPdfRealConContenidoParaUnReporteDeMatriz() throws Exception {
        ReportHeader header = new ReportHeader("Programación semanal", "admin", null);
        ReportTable matriz = new ReportTable(List.of("Hora", "Lunes", "Martes"));
        matriz.agregarFila("08:00", "Capacitación (F-100)", "");

        servicio.generarReporteMatriz(archivoMatriz.toString(), header, matriz);

        assertTrue(Files.exists(archivoMatriz));
        assertTrue(Files.size(archivoMatriz) > 0);
    }
}
