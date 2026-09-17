package controller;

import model.DetalleReserva;
import model.EstadoReserva;
import model.Recurso;
import model.Reserva;
import model.CategoriaRecurso;
import report.PdfReportService;
import report.ReportHeader;
import report.ReportTable;
import repository.RecursoRepository;
import repository.RecursoXmlRepository;
import repository.ReservaXmlRepository;
import service.*;
import repository.CategoriaXmlRepository;
import view.Tema;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReservaController extends JPanel {

    private final ReservaService reservaService;
    private final DisponibilidadService disponibilidadService;
    private final RecursoRepository recursoRepo;
    private final CategoriaService categoriaService;
    private final AiReservationService aiReservationService = new AiReservationService();

    private JTextField txtId;
    private JTextField txtActividad;
    private JSpinner spnFecha;
    private JTextField txtHoraInicio;
    private JTextField txtHoraFin;
    private JTextField txtFuncionarioId;
    private JComboBox<EstadoReserva> cmbEstado;
    private JButton btnAgregar;
    private JButton btnConsultar;
    private JButton btnModificar;
    private JButton btnEliminar;
    private JButton btnLimpiar;
    private JButton btnReporte;
    private JTable tabla;
    private DefaultTableModel modeloTabla;
    private PdfReportService pdfReportService = new PdfReportService();

    // --- Sección de Inteligencia Artificial ---
    private JTextArea txtFrase;
    private JButton btnExtraerIA;
    private JPanel panelChecksCategorias;
    private final List<JCheckBox> checksCategorias = new ArrayList<>();

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
            }
        });
    }

    private void construirPantalla() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titulo = new JLabel("Gestión de Reservas");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));
        panelSuperior.add(titulo, BorderLayout.NORTH);
        panelSuperior.add(construirPanelIA(), BorderLayout.CENTER);
        panelSuperior.add(construirFormulario(), BorderLayout.SOUTH);

        add(panelSuperior, BorderLayout.NORTH);
        add(construirTabla(), BorderLayout.CENTER);
        Tema.aplicar(this);
    }

    /** Zona de "llenar con IA": frase en lenguaje natural + extracción automática. */
    private JPanel construirPanelIA() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Reservar usando IA"));

        txtFrase = new JTextArea(2, 40);
        txtFrase.setLineWrap(true);
        txtFrase.setWrapStyleWord(true);
        JScrollPane scrollFrase = new JScrollPane(txtFrase);

        btnExtraerIA = new JButton("Extraer con IA");

        JPanel panelFrase = new JPanel(new BorderLayout(8, 8));
        panelFrase.add(new JLabel("Describa la reserva:"), BorderLayout.NORTH);
        panelFrase.add(scrollFrase, BorderLayout.CENTER);
        panelFrase.add(btnExtraerIA, BorderLayout.EAST);

        panel.add(panelFrase, BorderLayout.NORTH);
        return panel;
    }

    private JPanel construirFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Reserva"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("ID:"), gbc);
        txtId = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(txtId, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Actividad:"), gbc);
        txtActividad = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(txtActividad, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Fecha:"), gbc);
        spnFecha = new JSpinner(new SpinnerDateModel());
        spnFecha.setEditor(new JSpinner.DateEditor(spnFecha, "dd/MM/yyyy"));
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(spnFecha, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Hora inicio (HH:mm):"), gbc);
        txtHoraInicio = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(txtHoraInicio, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Hora fin (HH:mm):"), gbc);
        txtHoraFin = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 4;
        panel.add(txtHoraFin, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("Funcionario ID:"), gbc);
        txtFuncionarioId = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 5;
        panel.add(txtFuncionarioId, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(new JLabel("Estado:"), gbc);
        cmbEstado = new JComboBox<>(EstadoReserva.values());
        gbc.gridx = 1; gbc.gridy = 6;
        panel.add(cmbEstado, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        panel.add(new JLabel("Categorías requeridas:"), gbc);
        panelChecksCategorias = new JPanel();
        panelChecksCategorias.setLayout(new BoxLayout(panelChecksCategorias, BoxLayout.Y_AXIS));
        JScrollPane scrollCategorias = new JScrollPane(panelChecksCategorias);
        scrollCategorias.setPreferredSize(new Dimension(200, 80));
        gbc.gridx = 1; gbc.gridy = 7;
        panel.add(scrollCategorias, gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btnAgregar = new JButton("Reservar");
        btnConsultar = new JButton("Consultar");
        btnModificar = new JButton("Modificar");
        btnEliminar = new JButton("Eliminar");
        btnLimpiar = new JButton("Limpiar");
        btnReporte = new JButton("Generar Reporte PDF");

        URL urlBorrar = getClass().getResource("/icons/eliminar.png");
        if (urlBorrar != null) {
            ImageIcon original = new ImageIcon(urlBorrar);
            Image img = original.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnEliminar.setIcon(new ImageIcon(img));
        }

        URL urlLimpiar = getClass().getResource("/icons/limpiar.png");
        if (urlLimpiar != null) {
            ImageIcon original = new ImageIcon(urlLimpiar);
            Image img = original.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnLimpiar.setIcon(new ImageIcon(img));
        }

        URL urlAgregar = getClass().getResource("/icons/agregar.png");
        if (urlAgregar != null) {
            ImageIcon original = new ImageIcon(urlAgregar);
            Image img = original.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnAgregar.setIcon(new ImageIcon(img));
        }

        URL urlConsultar = getClass().getResource("/icons/consultar.png");
        if (urlConsultar != null) {
            ImageIcon original = new ImageIcon(urlConsultar);
            Image img = original.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnConsultar.setIcon(new ImageIcon(img));
        }

        URL urlModificar = getClass().getResource("/icons/modificar.png");
        if (urlModificar != null) {
            ImageIcon original = new ImageIcon(urlModificar);
            Image img = original.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnModificar.setIcon(new ImageIcon(img));
        }

        URL urlExtraerIA = getClass().getResource("/icons/ia.png");
        if (urlExtraerIA != null) {
            ImageIcon original = new ImageIcon(urlExtraerIA);
            Image img = original.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnExtraerIA.setIcon(new ImageIcon(img));
        }

        URL urlReporte = getClass().getResource("/icons/imprimir.png");
        if (urlReporte != null) {
            ImageIcon original = new ImageIcon(urlReporte);
            Image img = original.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnReporte.setIcon(new ImageIcon(img));
        }

        botones.add(btnAgregar);
        botones.add(btnConsultar);
        botones.add(btnModificar);
        botones.add(btnEliminar);
        botones.add(btnLimpiar);
        botones.add(btnReporte);

        gbc.gridx = 0; gbc.gridy = 8;
        gbc.gridwidth = 2;
        panel.add(botones, gbc);

        return panel;
    }

    private JScrollPane construirTabla() {
        modeloTabla = new DefaultTableModel(new String[]{"ID", "Actividad", "Fecha", "Inicio", "Fin", "Funcionario", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabla = new JTable(modeloTabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder("Listado"));
        return scroll;
    }

    private void cargarCategorias() {
        checksCategorias.clear();
        panelChecksCategorias.removeAll();
        for (CategoriaRecurso categoria : new CategoriaService(new CategoriaXmlRepository()).listarTodos()) {
            JCheckBox chk = new JCheckBox(categoria.getDescripcion());
            chk.putClientProperty("categoria", categoria);
            chk.setBackground(Tema.FONDO);
            checksCategorias.add(chk);
            panelChecksCategorias.add(chk);
        }
        panelChecksCategorias.revalidate();
        panelChecksCategorias.repaint();
    }

    private void registrarEventos() {
        btnAgregar.addActionListener(e -> agregarReserva());
        btnConsultar.addActionListener(e -> consultarReserva());
        btnModificar.addActionListener(e -> modificarReserva());
        btnEliminar.addActionListener(e -> eliminarReserva());
        btnLimpiar.addActionListener(e -> limpiarCampos());
        btnExtraerIA.addActionListener(e -> extraerConIA());
        btnReporte.addActionListener(e -> generarReportePDF());
        tabla.getSelectionModel().addListSelectionListener(this::alSeleccionarFila);
    }

    private void generarReportePDF() {
        try {
            List<Reserva> reservas = reservaService.listarTodos();

            if (reservas.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No hay reservas para reportar.",
                        "Información",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            ReportTable tabla = new ReportTable(List.of("ID", "Actividad", "Fecha", "Inicio", "Fin", "Funcionario", "Estado"));

            for (Reserva reserva : reservas) {
                tabla.agregarFila(
                        reserva.getId(),
                        reserva.getActividad(),
                        reserva.getFecha().toString(),
                        reserva.getHoraInicio().toString(),
                        reserva.getHoraFin().toString(),
                        reserva.getFuncionarioId(),
                        reserva.getEstado().toString()
                );
            }

            ReportHeader header = new ReportHeader(
                    "Sistema de Reserva de Recursos",
                    "Reporte de Reservas",
                    SessionManager.getInstancia().getUsuarioActual().getId()
            );

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setSelectedFile(new File("reporte_reservas.pdf"));

            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String rutaArchivo = fileChooser.getSelectedFile().getAbsolutePath();
                pdfReportService.generarReporteTabla(rutaArchivo, header, tabla);

                JOptionPane.showMessageDialog(this,
                        "Reporte generado exitosamente en:\n" + rutaArchivo,
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al generar reporte: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /** Envía la frase escrita a la IA y llena el formulario con lo que interpretó. */
    private void extraerConIA() {
        String frase = txtFrase.getText();
        btnExtraerIA.setEnabled(false);
        btnExtraerIA.setText("Procesando...");

        new SwingWorker<AiReservationResponse, Void>() {
            @Override
            protected AiReservationResponse doInBackground() throws Exception {
                return aiReservationService.interpretarSolicitud(frase);
            }

            @Override
            protected void done() {
                btnExtraerIA.setEnabled(true);
                btnExtraerIA.setText("Extraer con IA");
                try {
                    AiReservationResponse resultado = get();
                    txtActividad.setText(resultado.getActividad());
                    spnFecha.setValue(Date.from(resultado.getFecha().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    txtHoraInicio.setText(resultado.getHoraInicio().toString());
                    txtHoraFin.setText(resultado.getHoraFin().toString());
                    seleccionarCategoriasSugeridas(resultado.getCategorias());
                    mostrarMensaje("Datos extraídos. Revíselos y presione \"Reservar\" para confirmar.");
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    mostrarError("No fue posible interpretar la solicitud: " + causa.getMessage());
                }
            }
        }.execute();
    }

    private List<CategoriaRecurso> getCategoriasSeleccionadas() {
        List<CategoriaRecurso> seleccionadas = new ArrayList<>();
        for (JCheckBox chk : checksCategorias) {
            if (chk.isSelected()) {
                seleccionadas.add((CategoriaRecurso) chk.getClientProperty("categoria"));
            }
        }
        return seleccionadas;
    }

    /** Marca en la lista las categorías cuyo texto coincide con lo que devolvió la IA. */
    private void seleccionarCategoriasSugeridas(List<CategoriaRecurso> categoriasSugeridas) {
        for (JCheckBox chk : checksCategorias) {
            CategoriaRecurso cat = (CategoriaRecurso) chk.getClientProperty("categoria");
            boolean coincide = categoriasSugeridas != null && categoriasSugeridas.stream()
                    .anyMatch(s -> s.getDescripcion() != null
                            && s.getDescripcion().equalsIgnoreCase(cat.getDescripcion()));
            chk.setSelected(coincide);
        }
    }

    private void cargarTabla() {
        modeloTabla.setRowCount(0);
        List<Reserva> reservas = reservaService.listarTodos();
        for (Reserva reserva : reservas) {
            modeloTabla.addRow(new Object[]{
                    reserva.getId(),
                    reserva.getActividad(),
                    reserva.getFecha(),
                    reserva.getHoraInicio(),
                    reserva.getHoraFin(),
                    reserva.getFuncionarioId(),
                    reserva.getEstado()
            });
        }
    }

    private void alSeleccionarFila(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        txtId.setText(String.valueOf(modeloTabla.getValueAt(fila, 0)));
        txtActividad.setText(String.valueOf(modeloTabla.getValueAt(fila, 1)));
        LocalDate fechaFila = (LocalDate) modeloTabla.getValueAt(fila, 2);
        spnFecha.setValue(Date.from(fechaFila.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        txtHoraInicio.setText(String.valueOf(modeloTabla.getValueAt(fila, 3)));
        txtHoraFin.setText(String.valueOf(modeloTabla.getValueAt(fila, 4)));
        txtFuncionarioId.setText(String.valueOf(modeloTabla.getValueAt(fila, 5)));
        cmbEstado.setSelectedItem(EstadoReserva.valueOf(String.valueOf(modeloTabla.getValueAt(fila, 6))));
    }

    private Reserva construirReservaBase() {
        LocalDate fecha = ((Date) spnFecha.getValue()).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return new Reserva(
                txtId.getText(),
                txtActividad.getText(),
                fecha,
                LocalTime.parse(txtHoraInicio.getText()),
                LocalTime.parse(txtHoraFin.getText()),
                txtFuncionarioId.getText(),
                (EstadoReserva) cmbEstado.getSelectedItem()
        );
    }

    /**
     * Busca disponibilidad para cada categoría seleccionada. Si alguna
     * categoría no tiene recurso libre, no se asigna nada (registro
     * completo, nunca parcial) y se informa cuál falló.
     */
    private void agregarReserva() {
        List<CategoriaRecurso> categoriasSeleccionadas = getCategoriasSeleccionadas();
        if (categoriasSeleccionadas.isEmpty()) {
            mostrarError("Debe seleccionar al menos una categoría.");
            return;
        }

        try {
            Reserva reserva = construirReservaBase();

            List<DetalleReserva> detalles = new ArrayList<>();
            List<String> categoriasNoDisponibles = new ArrayList<>();

            for (CategoriaRecurso categoria : categoriasSeleccionadas) {
                Recurso disponible = disponibilidadService.buscarRecursoDisponible(
                        categoria, reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());

                if (disponible == null) {
                    categoriasNoDisponibles.add(categoria.getDescripcion());
                } else {
                    detalles.add(new DetalleReserva(categoria, disponible));
                }
            }

            if (!categoriasNoDisponibles.isEmpty()) {
                mostrarError("No hay disponibilidad para: " + String.join(", ", categoriasNoDisponibles)
                        + ". No se registró ningún recurso.");
                return;
            }

            reserva.setDetalles(detalles);
            reservaService.registrar(reserva);
            cargarTabla();
            limpiarCampos();
            mostrarMensaje("Reserva registrada correctamente.");

        } catch (RuntimeException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void consultarReserva() {
        try {
            Reserva reserva = reservaService.buscarPorId(txtId.getText());
            if (reserva == null) {
                mostrarMensaje("No se encontró la reserva.");
                return;
            }
            mostrarEnFormulario(reserva);
        } catch (RuntimeException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void modificarReserva() {
        try {
            reservaService.actualizar(construirReservaBase());
            cargarTabla();
            mostrarMensaje("Reserva modificada correctamente.");
        } catch (RuntimeException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void eliminarReserva() {
        try {
            reservaService.eliminar(construirReservaBase());
            cargarTabla();
            limpiarCampos();
            mostrarMensaje("Reserva eliminada correctamente.");
        } catch (RuntimeException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void mostrarEnFormulario(Reserva reserva) {
        txtId.setText(reserva.getId());
        txtActividad.setText(reserva.getActividad());
        spnFecha.setValue(Date.from(reserva.getFecha().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        txtHoraInicio.setText(reserva.getHoraInicio().toString());
        txtHoraFin.setText(reserva.getHoraFin().toString());
        txtFuncionarioId.setText(reserva.getFuncionarioId());
        cmbEstado.setSelectedItem(reserva.getEstado());
    }

    private void limpiarCampos() {
        txtId.setText("");
        txtActividad.setText("");
        spnFecha.setValue(new Date());
        txtHoraInicio.setText("");
        txtHoraFin.setText("");
        txtFuncionarioId.setText("");
        txtFrase.setText("");
        cmbEstado.setSelectedIndex(0);
        for (JCheckBox chk : checksCategorias) chk.setSelected(false);
        tabla.clearSelection();
    }

    private void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
