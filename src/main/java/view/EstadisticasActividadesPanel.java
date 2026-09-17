package view;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import report.PdfReportService;
import report.ReportHeader;
import report.ReportTable;
import repository.ReservaRepository;
import service.EstadisticasActividadesService;
import service.SessionManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EstadisticasActividadesPanel extends JPanel {

    private final JSpinner fechaDesdeSpinner;
    private final JSpinner fechaHastaSpinner;
    private final JButton btnCalcular;
    private final JButton btnImprimir;
    private final JLabel lblMensaje;
    private final JTable tablaEstadisticas;
    private final DefaultTableModel modeloTabla;
    private final DefaultCategoryDataset dataset;
    private final JFreeChart grafico;
    private final ChartPanel panelGrafico;

    private EstadisticasActividadesService service;
    private final PdfReportService pdfReportService = new PdfReportService();

    private LinkedHashMap<String, Integer> ultimoResultado;
    private LocalDate ultimoDesde;
    private LocalDate ultimoHasta;

    public EstadisticasActividadesPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titulo = new JLabel("Estadísticas de Actividades");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fechaDesdeSpinner = new JSpinner(new SpinnerDateModel());
        fechaDesdeSpinner.setEditor(new JSpinner.DateEditor(fechaDesdeSpinner, "dd/MM/yyyy"));
        fechaHastaSpinner = new JSpinner(new SpinnerDateModel());
        fechaHastaSpinner.setEditor(new JSpinner.DateEditor(fechaHastaSpinner, "dd/MM/yyyy"));
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
        panelFiltros.add(fechaDesdeSpinner);
        panelFiltros.add(new JLabel("Hasta:"));
        panelFiltros.add(fechaHastaSpinner);
        panelFiltros.add(btnCalcular);
        panelFiltros.add(btnImprimir);

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.add(titulo, BorderLayout.NORTH);
        panelSuperior.add(panelFiltros, BorderLayout.CENTER);

        lblMensaje = new JLabel(" ");
        lblMensaje.setForeground(new Color(180, 0, 0));

        modeloTabla = new DefaultTableModel(new Object[]{"Semana", "Cantidad"}, 0);
        tablaEstadisticas = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaEstadisticas);
        scrollTabla.setPreferredSize(new Dimension(400, 160));

        dataset = new DefaultCategoryDataset();
        grafico = ChartFactory.createBarChart(
                "Actividades por semana", "Semana", "Cantidad",
                dataset, PlotOrientation.VERTICAL, false, true, false);
        org.jfree.chart.plot.CategoryPlot plot = grafico.getCategoryPlot();
        ((org.jfree.chart.renderer.category.BarRenderer) plot.getRenderer())
                .setSeriesPaint(0, new Color(167, 139, 250));
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
        this.service = new EstadisticasActividadesService(reservaRepo);
    }

    private void onCalcular() {
        if (service == null) {
            lblMensaje.setText("El servicio no está configurado.");
            return;
        }
        lblMensaje.setText(" ");
        LocalDate desde = obtenerFecha(fechaDesdeSpinner);
        LocalDate hasta = obtenerFecha(fechaHastaSpinner);

        try {
            LinkedHashMap<String, Integer> resultado = service.calcular(desde, hasta);
            this.ultimoResultado = resultado;
            this.ultimoDesde = desde;
            this.ultimoHasta = hasta;

            modeloTabla.setRowCount(0);
            dataset.clear();
            for (Map.Entry<String, Integer> entry : resultado.entrySet()) {
                modeloTabla.addRow(new Object[]{entry.getKey(), entry.getValue()});
                dataset.addValue(entry.getValue(), "Actividades", entry.getKey());
            }

            if (resultado.isEmpty()) {
                lblMensaje.setText("No se encontraron actividades en el período seleccionado.");
            }
            btnImprimir.setEnabled(true);

        } catch (IllegalArgumentException e) {
            lblMensaje.setText(e.getMessage());
            btnImprimir.setEnabled(false);
        }
    }

    private LocalDate obtenerFecha(JSpinner spinner) {
        java.util.Date fechaUtil = (java.util.Date) spinner.getValue();
        return fechaUtil.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private void onImprimir() {
        if (ultimoResultado == null) {
            lblMensaje.setText("Primero debe calcular las estadísticas.");
            return;
        }

        try {
            ReportTable tabla = new ReportTable(List.of("Semana", "Cantidad de actividades"));
            for (Map.Entry<String, Integer> entry : ultimoResultado.entrySet()) {
                tabla.agregarFila(entry.getKey(), String.valueOf(entry.getValue()));
            }

            String filtros = "Desde: " + ultimoDesde + " | Hasta: " + ultimoHasta;
            ReportHeader header = new ReportHeader(
                    "Estadísticas de Actividades",
                    SessionManager.getInstancia().getUsuarioActual().getId(),
                    filtros
            );

            byte[] imagenGrafico = capturarGraficoComoPng();

            String nombreSugerido = "estadisticas_actividades_" + ultimoDesde + "_" + ultimoHasta + ".pdf";
            String ruta = elegirRutaGuardado(nombreSugerido);
            if (ruta == null) {
                return;
            }

            pdfReportService.reporteGrafico(ruta, header, tabla, imagenGrafico);
            lblMensaje.setForeground(new Color(0, 122, 47));
            lblMensaje.setText("Reporte generado en: " + ruta);

        } catch (Exception e) {
            lblMensaje.setForeground(new Color(180, 0, 0));
            lblMensaje.setText("Error al generar el PDF: " + e.getMessage());
        }
    }

    private byte[] capturarGraficoComoPng() throws Exception {
        BufferedImage imagen = grafico.createBufferedImage(480, 320);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", buffer);
        return buffer.toByteArray();
    }

    private String elegirRutaGuardado(String nombreSugerido) {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar reporte PDF");
        selector.setSelectedFile(new java.io.File(nombreSugerido));
        selector.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos PDF (*.pdf)", "pdf"));

        int resultado = selector.showSaveDialog(this);
        if (resultado != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        java.io.File archivo = selector.getSelectedFile();
        String ruta = archivo.getAbsolutePath();
        if (!ruta.toLowerCase().endsWith(".pdf")) {
            ruta += ".pdf";
        }
        return ruta;
    }
}
