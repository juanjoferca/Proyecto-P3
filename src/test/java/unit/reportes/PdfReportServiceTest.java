package unit.reportes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import report.PdfReportService;
import report.ReportException;
import report.ReportHeader;
import report.ReportTable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfReportServiceTest {

    private PdfReportService servicio;
    private Path archivoSalida;

    @BeforeEach
    void prepararCadaPrueba() throws IOException {
        servicio = new PdfReportService();
        archivoSalida = Files.createTempFile("reporte", ".pdf");
        Files.deleteIfExists(archivoSalida);
    }

    @AfterEach
    void limpiarArchivoGenerado() throws IOException {
        Files.deleteIfExists(archivoSalida);
    }

    private ReportHeader crearHeader() {
        return new ReportHeader("Calendarización de recursos", "admin", null);
    }

    private ReportTable crearTablaConDatos() {
        ReportTable tabla = new ReportTable(List.of("Hora", "R-001"));
        tabla.agregarFila("08:00", "Reunión - F-001");
        return tabla;
    }

    private byte[] crearImagenPngValida() throws IOException {
        BufferedImage imagen = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    @Test
    void generarReporteTablaConTablaValidaNoLanzaExcepcionYCreaElArchivo() {
        assertDoesNotThrow(() ->
                servicio.generarReporteTabla(archivoSalida.toString(), crearHeader(), crearTablaConDatos()));

        assertTrue(Files.exists(archivoSalida));
        assertTrue(archivoSalida.toFile().length() > 0);
    }

    @Test
    void generarReporteTablaConHeaderNuloLanzaReportException() {
        ReportTable tabla = crearTablaConDatos();
        assertThrows(ReportException.class, () ->
                servicio.generarReporteTabla(archivoSalida.toString(), null, tabla));
    }

    @Test
    void generarReporteTablaConTablaNulaLanzaReportException() {
        ReportHeader header = crearHeader();
        assertThrows(ReportException.class, () ->
                servicio.generarReporteTabla(archivoSalida.toString(), header, null));
    }

    @Test
    void generarReporteMatrizConMatrizValidaNoLanzaExcepcionYCreaElArchivo() {
        assertDoesNotThrow(() ->
                servicio.generarReporteMatriz(archivoSalida.toString(), crearHeader(), crearTablaConDatos()));

        assertTrue(Files.exists(archivoSalida));
        assertTrue(archivoSalida.toFile().length() > 0);
    }

    @Test
    void generarReporteMatrizConHeaderNuloLanzaReportException() {
        ReportTable matriz = crearTablaConDatos();
        assertThrows(ReportException.class, () ->
                servicio.generarReporteMatriz(archivoSalida.toString(), null, matriz));
    }

    @Test
    void generarReporteMatrizConMatrizNulaLanzaReportException() {
        ReportHeader header = crearHeader();
        assertThrows(ReportException.class, () ->
                servicio.generarReporteMatriz(archivoSalida.toString(), header, null));
    }

    @Test
    void reporteGraficoConImagenValidaNoLanzaExcepcionYCreaElArchivo() throws IOException {
        byte[] imagen = crearImagenPngValida();

        assertDoesNotThrow(() ->
                servicio.reporteGrafico(archivoSalida.toString(), crearHeader(), crearTablaConDatos(), imagen));

        assertTrue(Files.exists(archivoSalida));
        assertTrue(archivoSalida.toFile().length() > 0);
    }
}
