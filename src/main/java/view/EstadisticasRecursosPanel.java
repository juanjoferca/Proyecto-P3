package view;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import repository.ReservaRepository;
import report.PdfReportService;
import report.ReportException;
import report.ReportHeader;
import report.ReportTable;
import service.EstadisticasRecursosService;
import service.SessionManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public class EstadisticasRecursosPanel extends JPanel {

    private final JSpinner spnDesde;
    private final JSpinner spnHasta;
    private final JButton btnCalcular;
    private final JButton btnImprimir;
    private final JLabel lblMensaje;
    private final JTable tablaEstadisticas;
    private final DefaultTableModel modeloTabla;
    private final DefaultCategoryDataset dataset;
    private final JFreeChart grafico;
    private final ChartPanel panelGrafico;

    private EstadisticasRecursosService servicio;
    private final PdfReportService pdfReportService = new PdfReportService();

    private Map<String, Integer> ultimoResultado;
    private String ultimoPeriodo;

    public EstadisticasRecursosPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titulo = new JLabel("Estadísticas de Recursos");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.add(titulo, BorderLayout.NORTH);

        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));

        spnDesde = new JSpinner(new SpinnerDateModel());
        spnDesde.setEditor(new JSpinner.DateEditor(spnDesde, "dd/MM/yyyy"));

        spnHasta = new JSpinner(new SpinnerDateModel());
        spnHasta.setEditor(new JSpinner.DateEditor(spnHasta, "dd/MM/yyyy"));

        btnCalcular = new JButton("Calcular");
        btnImprimir = new JButton("Imprimir");
        btnImprimir.setEnabled(false);

        URL urlImprimir = getClass().getResource("/icons/imprimir.png");
        if (urlImprimir != null) {
            ImageIcon original = new ImageIcon(urlImprimir);
            Image img = original.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnImprimir.setIcon(new ImageIcon(img));
        }

        URL urlCalcular = getClass().getResource("/icons/cargar.png");
        if (urlCalcular != null) {
            ImageIcon original = new ImageIcon(urlCalcular);
            Image img = original.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnCalcular.setIcon(new ImageIcon(img));
        }

        panelFiltros.add(new JLabel("Desde:"));
        panelFiltros.add(spnDesde);
        panelFiltros.add(new JLabel("Hasta:"));
        panelFiltros.add(spnHasta);
        panelFiltros.add(btnCalcular);
        panelFiltros.add(btnImprimir);
        panelSuperior.add(panelFiltros, BorderLayout.CENTER);

        lblMensaje = new JLabel(" ");
        lblMensaje.setForeground(new Color(180, 0, 0));

        modeloTabla = new DefaultTableModel(new Object[]{"Categoría", "Cantidad"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) { return false; }
        };
        tablaEstadisticas = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaEstadisticas);
        scrollTabla.setPreferredSize(new Dimension(350, 160));

        dataset = new DefaultCategoryDataset();
        grafico = ChartFactory.createBarChart(
                "Recursos Usados", "Categoría", "Cantidad",
                dataset, PlotOrientation.VERTICAL, false, true, false);
        org.jfree.chart.plot.CategoryPlot plot = grafico.getCategoryPlot();
        ((org.jfree.chart.renderer.category.BarRenderer) plot.getRenderer())
                .setSeriesPaint(0, new Color(109, 40, 217));
        panelGrafico = new ChartPanel(grafico);
        panelGrafico.setPreferredSize(new Dimension(500, 320));

        JPanel panelCentro = new JPanel(new BorderLayout(10, 10));
        panelCentro.add(scrollTabla, BorderLayout.WEST);
        panelCentro.add(panelGrafico, BorderLayout.CENTER);

        add(panelSuperior, BorderLayout.NORTH);
        add(lblMensaje, BorderLayout.SOUTH);
        add(panelCentro, BorderLayout.CENTER);

        btnCalcular.addActionListener(e -> onCalcular());
        btnImprimir.addActionListener(e -> onImprimir());
        Tema.aplicar(this);
    }

    public void configurarDependencias(ReservaRepository reservaRepo) {
        this.servicio = new EstadisticasRecursosService(reservaRepo);
    }

    private void onCalcular() {
        if (servicio == null) {
            mostrarError("El servicio no está configurado.");
            return;
        }
        try {
            LocalDate desde = obtenerFecha(spnDesde);
            LocalDate hasta = obtenerFecha(spnHasta);

            Map<String, Integer> resultado = servicio.contarPorCategoria(desde, hasta);

            ultimoResultado = resultado;
            ultimoPeriodo = "Del " + desde + " al " + hasta;

            modeloTabla.setRowCount(0);
            dataset.clear();

            for (Map.Entry<String, Integer> entrada : resultado.entrySet()) {
                modeloTabla.addRow(new Object[]{entrada.getKey(), entrada.getValue()});
                dataset.addValue(entrada.getValue(), "Recurso", entrada.getKey());
            }

            if (resultado.isEmpty()) {
                mostrarError("No hay reservas en el período indicado.");
            } else {
                lblMensaje.setText(" ");
            }
            btnImprimir.setEnabled(true);

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private LocalDate obtenerFecha(JSpinner spinner) {
        java.util.Date fechaUtil = (java.util.Date) spinner.getValue();
        return fechaUtil.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void onImprimir() {
        if (ultimoResultado == null) {
            mostrarError("Primero debe calcular las estadísticas.");
            return;
        }
        try {
            ReportTable tabla = new ReportTable(List.of("Categoría", "Cantidad"));
            for (Map.Entry<String, Integer> entrada : ultimoResultado.entrySet())
                tabla.agregarFila(entrada.getKey(), String.valueOf(entrada.getValue()));

            ReportHeader header = new ReportHeader(
                    "Estadísticas de Recursos",
                    SessionManager.getInstancia().getUsuarioActual().getId(),
                    ultimoPeriodo);

            byte[] imagenGrafico = capturarGraficoComoPng();
            String ruta = elegirRutaGuardado();
            if (ruta == null) return;

            pdfReportService.reporteGrafico(ruta, header, tabla, imagenGrafico);
            lblMensaje.setForeground(new Color(0, 122, 47));
            lblMensaje.setText("Reporte generado en: " + ruta);

        } catch (ReportException e) {
            mostrarError("Error al generar el PDF: " + e.getMessage());
        } catch (Exception e) {
            mostrarError("Error: " + e.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setForeground(new Color(180, 0, 0));
        lblMensaje.setText(mensaje);
    }

    private byte[] capturarGraficoComoPng() throws Exception {
        BufferedImage imagen = grafico.createBufferedImage(480, 320);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", buffer);
        return buffer.toByteArray();
    }

    private String elegirRutaGuardado() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar reporte PDF");
        selector.setSelectedFile(new java.io.File("estadisticas_recursos.pdf"));
        selector.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos PDF (*.pdf)", "pdf"));

        int resultado = selector.showSaveDialog(this);
        if (resultado != JFileChooser.APPROVE_OPTION) return null;

        java.io.File archivo = selector.getSelectedFile();
        String ruta = archivo.getAbsolutePath();
        if (!ruta.toLowerCase().endsWith(".pdf")) ruta += ".pdf";
        return ruta;
    }
}
