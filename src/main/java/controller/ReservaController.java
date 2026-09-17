package controller;

import model.*;
import repository.*;
import service.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import view.Tema;

public class ReservaController extends JPanel {

    private final ReservaService reservaService;
    private final DisponibilidadService disponibilidadService;
    private final RecursoRepository recursoRepo;
    private final CategoriaService categoriaService;
    private final AiReservationService aiReservationService = new AiReservationService();

    private JTextArea txtFrase;
    private JButton btnExtraerIA;
    private JTextField txtActividad;
    private JSpinner spnFecha;
    private JComboBox<String> cmbHoraInicio;
    private JComboBox<String> cmbHoraFin;
    private JList<CategoriaRecurso> listCategorias;
    private DefaultListModel<CategoriaRecurso> modeloCategorias;
    private JButton btnReservar;
    private JButton btnCancelar;
    private JButton btnLimpiar;
    private JButton btnImprimir;
    private JTable tabla;
    private DefaultTableModel modeloTabla;

    public ReservaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
        this.recursoRepo = new RecursoXmlRepository();
        ReservaXmlRepository reservaRepo = new ReservaXmlRepository();
        this.disponibilidadService = new DisponibilidadService(recursoRepo, reservaRepo);
        this.reservaService = new ReservaService(disponibilidadService, reservaRepo);

        construirPantalla();
        cargarCategorias();
        cargarTabla();
        registrarEventos();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                cargarCategorias();
                cargarTabla();
            }
        });
    }

    private void construirPantalla() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(construirPanelNuevaReserva(), BorderLayout.NORTH);
        add(construirPanelMisReservas(), BorderLayout.CENTER);
        Tema.aplicar(this);
    }

    private JPanel construirPanelNuevaReserva() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Nueva reserva"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtFrase = new JTextArea(2, 40);
        txtFrase.setLineWrap(true);
        txtFrase.setWrapStyleWord(true);
        JScrollPane scrollFrase = new JScrollPane(txtFrase);

        btnExtraerIA = new JButton("Extraer");
        URL urlIA = getClass().getResource("/icons/ia.png");
        if (urlIA != null) {
            Image img = new ImageIcon(urlIA).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnExtraerIA.setIcon(new ImageIcon(img));
        }

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Frase"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 3;
        panel.add(scrollFrase, gbc);
        gbc.gridx = 4; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(btnExtraerIA, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Actividad"), gbc);
        txtActividad = new JTextField();
        gbc.gridx = 1; gbc.gridwidth = 4; gbc.weightx = 1;
        panel.add(txtActividad, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(new JLabel("Fecha"), gbc);
        spnFecha = new JSpinner(new SpinnerDateModel());
        spnFecha.setEditor(new JSpinner.DateEditor(spnFecha, "dd 'de' MMMM 'de' yyyy"));
        gbc.gridx = 1; gbc.weightx = 0.5;
        panel.add(spnFecha, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("Hora inicio"), gbc);
        cmbHoraInicio = new JComboBox<>(generarHoras());
        gbc.gridx = 3; gbc.gridwidth = 2; gbc.weightx = 0.5;
        panel.add(cmbHoraInicio, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(new JLabel("Categorías requeridas (selección múltiple)"), gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("Hora fin"), gbc);
        cmbHoraFin = new JComboBox<>(generarHoras());
        gbc.gridx = 3; gbc.gridwidth = 2; gbc.weightx = 0.5;
        panel.add(cmbHoraFin, gbc);

        modeloCategorias = new DefaultListModel<>();
        listCategorias = new JList<>(modeloCategorias);
        listCategorias.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listCategorias.setBackground(Color.WHITE);
        listCategorias.setSelectionBackground(new Color(70, 130, 180));
        listCategorias.setSelectionForeground(Color.WHITE);
        listCategorias.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int i0, int i1) {
                if (i0 == i1) {
                    if (isSelectedIndex(i0)) super.removeSelectionInterval(i0, i0);
                    else super.addSelectionInterval(i0, i0);
                } else {
                    super.setSelectionInterval(i0, i1);
                }
            }
        });
        JScrollPane scrollCats = new JScrollPane(listCategorias);
        scrollCats.setBorder(BorderFactory.createTitledBorder("Categorías"));
        scrollCats.setPreferredSize(new Dimension(0, 90));

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 5; gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollCats, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btnReservar = new JButton("Reservar");
        btnCancelar = new JButton("Cancelar reserva seleccionada");
        btnLimpiar = new JButton("Limpiar");

        setIcono(btnReservar, "/icons/guardar.png");
        setIcono(btnCancelar, "/icons/eliminar.png");
        setIcono(btnLimpiar, "/icons/limpiar.png");

        botones.add(btnReservar);
        botones.add(btnCancelar);
        botones.add(btnLimpiar);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 5; gbc.weightx = 1;
        panel.add(botones, gbc);

        return panel;
    }

    private JPanel construirPanelMisReservas() {
        modeloTabla = new DefaultTableModel(
                new String[]{"Id", "Actividad", "Fecha", "Horario", "Recursos", "Estado"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(modeloTabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder("Mis reservas"));

        btnImprimir = new JButton("Imprimir");
        setIcono(btnImprimir, "/icons/imprimir.png");

        JPanel panelDerecha = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelDerecha.add(btnImprimir);

        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(panelDerecha, BorderLayout.EAST);
        return panel;
    }

    private void setIcono(JButton btn, String ruta) {
        URL url = getClass().getResource(ruta);
        if (url != null) {
            Image img = new ImageIcon(url).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(img));
        }
    }

    private String[] generarHoras() {
        List<String> horas = new ArrayList<>();
        for (int h = 0; h < 24; h++)
            for (int m = 0; m < 60; m += 30)
                horas.add(String.format("%02d:%02d", h, m));
        return horas.toArray(new String[0]);
    }

    private void cargarCategorias() {
        modeloCategorias.clear();
        for (CategoriaRecurso cat : new CategoriaService(new CategoriaXmlRepository()).listarTodos())
            modeloCategorias.addElement(cat);
        listCategorias.revalidate();
        listCategorias.repaint();
    }

    private void cargarTabla() {
        modeloTabla.setRowCount(0);
        String usuarioActual = SessionManager.getInstancia().getUsuarioActual() != null
                ? SessionManager.getInstancia().getUsuarioActual().getId() : null;
        for (Reserva reserva : reservaService.listarTodos()) {
            if (usuarioActual != null && !reserva.getFuncionarioId().equals(usuarioActual)) continue;
            String horario = reserva.getHoraInicio() + " - " + reserva.getHoraFin();
            String recursos = reserva.getDetalles().stream()
                    .filter(d -> d.getRecurso() != null)
                    .map(d -> d.getRecurso().getId())
                    .reduce((a, b) -> a + ", " + b).orElse("");
            modeloTabla.addRow(new Object[]{
                    reserva.getId(), reserva.getActividad(), reserva.getFecha(),
                    horario, recursos, reserva.getEstado()
            });
        }
    }

    private void registrarEventos() {
        btnReservar.addActionListener(e -> agregarReserva());
        btnCancelar.addActionListener(e -> cancelarReserva());
        btnLimpiar.addActionListener(e -> limpiarCampos());
        btnExtraerIA.addActionListener(e -> extraerConIA());
        btnImprimir.addActionListener(e -> imprimir());
        tabla.getSelectionModel().addListSelectionListener(this::alSeleccionarFila);
    }

    private void extraerConIA() {
        String frase = txtFrase.getText();
        btnExtraerIA.setEnabled(false);
        new SwingWorker<AiReservationResponse, Void>() {
            @Override protected AiReservationResponse doInBackground() throws Exception {
                return aiReservationService.interpretarSolicitud(frase);
            }
            @Override protected void done() {
                btnExtraerIA.setEnabled(true);
                try {
                    AiReservationResponse r = get();
                    txtActividad.setText(r.getActividad());
                    spnFecha.setValue(Date.from(r.getFecha().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    seleccionarHora(cmbHoraInicio, r.getHoraInicio());
                    seleccionarHora(cmbHoraFin, r.getHoraFin());
                    seleccionarCategoriasSugeridas(r.getCategorias());
                    mostrarMensaje("Datos extraídos. Revíselos y presione \"Reservar\" para confirmar.");
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    mostrarError("No fue posible interpretar la solicitud: " + causa.getMessage());
                }
            }
        }.execute();
    }

    private void seleccionarHora(JComboBox<String> cmb, LocalTime hora) {
        int minRedondeado = (hora.getMinute() / 30) * 30;
        String texto = String.format("%02d:%02d", hora.getHour(), minRedondeado);
        for (int i = 0; i < cmb.getItemCount(); i++) {
            if (cmb.getItemAt(i).equals(texto)) { cmb.setSelectedIndex(i); return; }
        }
    }

    private void seleccionarCategoriasSugeridas(List<CategoriaRecurso> sugeridas) {
        DefaultListSelectionModel modeloBase = (DefaultListSelectionModel) listCategorias.getSelectionModel();
        modeloBase.clearSelection();
        if (sugeridas == null) return;
        for (int i = 0; i < modeloCategorias.size(); i++) {
            CategoriaRecurso cat = modeloCategorias.get(i);
            boolean coincide = sugeridas.stream().anyMatch(s ->
                    (s.getDescripcion() != null && s.getDescripcion().equalsIgnoreCase(cat.getDescripcion()))
                    || (s.getId() != null && s.getId().equalsIgnoreCase(cat.getDescripcion()))
                    || (s.getId() != null && s.getId().equalsIgnoreCase(cat.getId())));
            if (coincide) modeloBase.addSelectionInterval(i, i);
        }
    }

    private List<CategoriaRecurso> getCategoriasSeleccionadas() {
        return listCategorias.getSelectedValuesList();
    }

    private void agregarReserva() {
        List<CategoriaRecurso> seleccionadas = getCategoriasSeleccionadas();
        if (seleccionadas.isEmpty()) { mostrarError("Debe seleccionar al menos una categoría."); return; }
        try {
            Reserva reserva = construirReservaBase();
            List<DetalleReserva> detalles = new ArrayList<>();
            List<String> noDisponibles = new ArrayList<>();
            for (CategoriaRecurso cat : seleccionadas) {
                Recurso disponible = disponibilidadService.buscarRecursoDisponible(
                        cat, reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());
                if (disponible == null) noDisponibles.add(cat.getDescripcion());
                else detalles.add(new DetalleReserva(cat, disponible));
            }
            if (!noDisponibles.isEmpty()) {
                mostrarError("No hay disponibilidad para: " + String.join(", ", noDisponibles)
                        + ". No se registró ningún recurso.");
                return;
            }
            reserva.setDetalles(detalles);
            reservaService.registrar(reserva);
            cargarTabla();
            limpiarCampos();
            mostrarMensaje("Reserva registrada correctamente.");
        } catch (RuntimeException ex) { mostrarError(ex.getMessage()); }
    }

    private void cancelarReserva() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { mostrarError("Seleccione una reserva de la tabla."); return; }
        String id = String.valueOf(modeloTabla.getValueAt(fila, 0));
        try {
            Reserva reserva = reservaService.buscarPorId(id);
            if (reserva == null) { mostrarError("No se encontró la reserva."); return; }
            if (reserva.getEstado() != EstadoReserva.ACTIVA) {
                mostrarError("Solo se pueden cancelar reservas activas.");
                return;
            }
            int confirmacion = JOptionPane.showConfirmDialog(this,
                    "¿Desea cancelar la reserva " + id + "?", "Confirmar cancelación",
                    JOptionPane.YES_NO_OPTION);
            if (confirmacion != JOptionPane.YES_OPTION) return;
            reserva.setEstado(EstadoReserva.CANCELADA);
            reservaService.actualizar(reserva);
            cargarTabla();
            limpiarCampos();
            mostrarMensaje("Reserva cancelada correctamente.");
        } catch (RuntimeException ex) { mostrarError(ex.getMessage()); }
    }

    private void alSeleccionarFila(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        txtActividad.setText(String.valueOf(modeloTabla.getValueAt(fila, 1)));
        Object fechaObj = modeloTabla.getValueAt(fila, 2);
        if (fechaObj instanceof LocalDate fecha)
            spnFecha.setValue(Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        String horario = String.valueOf(modeloTabla.getValueAt(fila, 3));
        String[] partes = horario.split(" - ");
        if (partes.length == 2) {
            seleccionarHora(cmbHoraInicio, LocalTime.parse(partes[0]));
            seleccionarHora(cmbHoraFin, LocalTime.parse(partes[1]));
        }
    }

    private Reserva construirReservaBase() {
        String usuarioId = SessionManager.getInstancia().getUsuarioActual() != null
                ? SessionManager.getInstancia().getUsuarioActual().getId() : "";
        LocalDate fecha = ((Date) spnFecha.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalTime inicio = LocalTime.parse((String) cmbHoraInicio.getSelectedItem());
        LocalTime fin = LocalTime.parse((String) cmbHoraFin.getSelectedItem());
        int siguiente = reservaService.listarTodos().size() + 1;
        String id = "RES-" + String.format("%06d", siguiente);
        return new Reserva(id, txtActividad.getText(), fecha, inicio, fin, usuarioId, EstadoReserva.ACTIVA);
    }

    private void limpiarCampos() {
        txtFrase.setText("");
        txtActividad.setText("");
        spnFecha.setValue(new Date());
        cmbHoraInicio.setSelectedIndex(0);
        cmbHoraFin.setSelectedIndex(0);
        listCategorias.clearSelection();
        tabla.clearSelection();
    }

    private void imprimir() {
        try {
            report.ReportTable reportTabla = new report.ReportTable(
                    java.util.List.of("Id", "Actividad", "Fecha", "Horario", "Recursos", "Estado"));
            for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                reportTabla.agregarFila(
                        String.valueOf(modeloTabla.getValueAt(i, 0)),
                        String.valueOf(modeloTabla.getValueAt(i, 1)),
                        String.valueOf(modeloTabla.getValueAt(i, 2)),
                        String.valueOf(modeloTabla.getValueAt(i, 3)),
                        String.valueOf(modeloTabla.getValueAt(i, 4)),
                        String.valueOf(modeloTabla.getValueAt(i, 5)));
            }
            String usuario = SessionManager.getInstancia().getUsuarioActual() != null
                    ? SessionManager.getInstancia().getUsuarioActual().getId() : "";
            report.ReportHeader header = new report.ReportHeader("Mis Reservas", usuario, null);
            JFileChooser selector = new JFileChooser();
            selector.setDialogTitle("Guardar reporte PDF");
            selector.setCurrentDirectory(new java.io.File(System.getProperty("user.home")));
            selector.setSelectedFile(new java.io.File("mis_reservas.pdf"));
            selector.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF", "pdf"));
            if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            String ruta = selector.getSelectedFile().getAbsolutePath();
            if (!ruta.toLowerCase().endsWith(".pdf")) ruta += ".pdf";
            new report.PdfReportService().generarReporteTabla(ruta, header, reportTabla);
            mostrarMensaje("Reporte generado: " + ruta);
        } catch (Exception ex) {
            mostrarError("Error al generar el reporte: " + ex.getMessage());
        }
    }

    private void mostrarMensaje(String m) { JOptionPane.showMessageDialog(this, m); }
    private void mostrarError(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }
}
