package controller;

import model.Recurso;
import model.CategoriaRecurso;
import report.PdfReportService;
import report.ReportHeader;
import report.ReportTable;
import repository.RecursoXmlRepository;
import service.RecursoService;
import service.CategoriaService;
import service.SessionManager;
import view.Tema;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URL;
import java.util.List;

public class RecursoController extends JPanel {

    private final CategoriaService categoriaService;
    private final RecursoService recursoService;

    // Filtro
    private JComboBox<CategoriaRecurso> cmbFiltroCategoria;
    private JTextField txtFiltroDescripcion;
    private JButton btnBuscar;
    private JButton btnImprimir;

    // Formulario
    private JTextField txtId;
    private JComboBox<CategoriaRecurso> cmbCategoria;
    private JTextField txtDescripcion;
    private JButton btnAgregar;
    private JButton btnModificar;
    private JButton btnEliminar;
    private JButton btnLimpiar;
    private JButton btnConsultar;
    private JTable tabla;
    private DefaultTableModel modeloTabla;

    public RecursoController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
        this.recursoService = new RecursoService(new RecursoXmlRepository());
        construirPantalla();
        cargarCategorias();
        cargarTabla(recursoService.listarTodos());
        registrarEventos();
        Tema.aplicar(this);
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

        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));
        panelSuperior.add(construirPanelFiltro(), BorderLayout.NORTH);
        panelSuperior.add(construirFormulario(), BorderLayout.CENTER);

        add(panelSuperior, BorderLayout.NORTH);
        add(construirTabla(), BorderLayout.CENTER);
    }

    private JPanel construirPanelFiltro() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Filtro"));

        panel.add(new JLabel("Categoría:"));
        cmbFiltroCategoria = new JComboBox<>();
        cmbFiltroCategoria.setPreferredSize(new Dimension(160, 26));
        panel.add(cmbFiltroCategoria);

        panel.add(new JLabel("Descripción:"));
        txtFiltroDescripcion = new JTextField(16);
        panel.add(txtFiltroDescripcion);

        btnBuscar = new JButton("Buscar");
        btnImprimir = new JButton("Imprimir");
        panel.add(btnBuscar);
        panel.add(btnImprimir);

        return panel;
    }

    private JPanel construirFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Recurso"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("ID:"), gbc);
        txtId = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(txtId, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Categoría:"), gbc);
        cmbCategoria = new JComboBox<>();
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(cmbCategoria, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Descripción:"), gbc);
        txtDescripcion = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(txtDescripcion, gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btnAgregar = new JButton("Guardar");
        btnModificar = new JButton("Modificar");
        btnEliminar = new JButton("Borrar");
        btnLimpiar = new JButton("Limpiar");
        btnConsultar = new JButton("Consultar");
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

        botones.add(btnAgregar);
        botones.add(btnModificar);
        botones.add(btnEliminar);
        botones.add(btnLimpiar);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(botones, gbc);

        return panel;
    }

    private JScrollPane construirTabla() {
        modeloTabla = new DefaultTableModel(new String[]{"ID", "Categoría", "Descripción"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tabla = new JTable(modeloTabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder("Listado"));
        return scroll;
    }

    private void registrarEventos() {
        btnBuscar.addActionListener(e -> buscar());
        btnImprimir.addActionListener(e -> imprimir());
        btnAgregar.addActionListener(e -> guardar());
        btnModificar.addActionListener(e -> modificarRecurso());
        btnEliminar.addActionListener(e -> eliminarRecurso());
        btnLimpiar.addActionListener(e -> limpiarCampos());
        tabla.getSelectionModel().addListSelectionListener(this::alSeleccionarFila);
    }

    private void cargarCategorias() {
        List<CategoriaRecurso> cats = categoriaService.listarTodos();

        cmbFiltroCategoria.removeAllItems();
        cmbFiltroCategoria.addItem(new CategoriaRecurso("", "-- Todas --"));
        for (CategoriaRecurso c : cats) cmbFiltroCategoria.addItem(c);

        cmbCategoria.removeAllItems();
        for (CategoriaRecurso c : cats) cmbCategoria.addItem(c);
    }

    private void cargarTabla(List<Recurso> recursos) {
        modeloTabla.setRowCount(0);
        for (Recurso r : recursos) {
            modeloTabla.addRow(new Object[]{
                    r.getId(),
                    r.getCategoria() != null ? r.getCategoria().getDescripcion() : "",
                    r.getDescripcion()
            });
        }
    }

    private void buscar() {
        CategoriaRecurso catFiltro = (CategoriaRecurso) cmbFiltroCategoria.getSelectedItem();
        String descFiltro = txtFiltroDescripcion.getText().trim().toLowerCase();

        List<Recurso> todos = recursoService.listarTodos();
        List<Recurso> filtrados = todos.stream()
                .filter(r -> catFiltro == null || catFiltro.getId().isBlank()
                        || (r.getCategoria() != null && catFiltro.getId().equals(r.getCategoria().getId())))
                .filter(r -> descFiltro.isBlank()
                        || r.getDescripcion().toLowerCase().contains(descFiltro))
                .toList();

        cargarTabla(filtrados);
    }

    private void imprimir() {
        CategoriaRecurso catFiltro = (CategoriaRecurso) cmbFiltroCategoria.getSelectedItem();
        String descFiltro = txtFiltroDescripcion.getText().trim().toLowerCase();

        List<Recurso> todos = recursoService.listarTodos();
        List<Recurso> filtrados = todos.stream()
                .filter(r -> catFiltro == null || catFiltro.getId().isBlank()
                        || (r.getCategoria() != null && catFiltro.getId().equals(r.getCategoria().getId())))
                .filter(r -> descFiltro.isBlank()
                        || r.getDescripcion().toLowerCase().contains(descFiltro))
                .toList();

        try {
            ReportTable tabla = new ReportTable(List.of("ID", "Categoría", "Descripción"));
            for (Recurso r : filtrados) {
                tabla.agregarFila(r.getId(),
                        r.getCategoria() != null ? r.getCategoria().getDescripcion() : "",
                        r.getDescripcion());
            }
            String usuario = SessionManager.getInstancia().getUsuarioActual().getId();
            ReportHeader header = new ReportHeader("Listado de Recursos", usuario, null);

            JFileChooser selector = new JFileChooser();
            selector.setDialogTitle("Guardar reporte PDF");
            selector.setSelectedFile(new java.io.File("recursos.pdf"));
            selector.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos PDF (*.pdf)", "pdf"));
            if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            String ruta = selector.getSelectedFile().getAbsolutePath();
            if (!ruta.toLowerCase().endsWith(".pdf")) ruta += ".pdf";

            new PdfReportService().generarReporteTabla(ruta, header, tabla);
            JOptionPane.showMessageDialog(this, "Reporte generado: " + ruta);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al generar el reporte: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void alSeleccionarFila(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        txtId.setText(String.valueOf(modeloTabla.getValueAt(fila, 0)));
        // buscar categoría por descripción para seleccionarla en el combo
        String descCat = String.valueOf(modeloTabla.getValueAt(fila, 1));
        for (int i = 0; i < cmbCategoria.getItemCount(); i++) {
            if (cmbCategoria.getItemAt(i).getDescripcion().equals(descCat)) {
                cmbCategoria.setSelectedIndex(i);
                break;
            }
        }
        txtDescripcion.setText(String.valueOf(modeloTabla.getValueAt(fila, 2)));
    }

    private Recurso construirRecurso() {
        CategoriaRecurso cat = (CategoriaRecurso) cmbCategoria.getSelectedItem();
        if (cat == null) throw new IllegalArgumentException("Debe seleccionar una categoría.");
        return new Recurso(txtId.getText(), cat, txtDescripcion.getText());
    }

    private void guardar() {
        try {
            recursoService.registrar(construirRecurso());
            cargarTabla(recursoService.listarTodos());
            limpiarCampos();
            JOptionPane.showMessageDialog(this, "Recurso guardado correctamente.");
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modificarRecurso() {
        try {
            recursoService.actualizar(construirRecurso());
            cargarTabla(recursoService.listarTodos());
            JOptionPane.showMessageDialog(this, "Recurso modificado correctamente.");
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarRecurso() {
        try {
            recursoService.eliminar(construirRecurso());
            cargarTabla(recursoService.listarTodos());
            limpiarCampos();
            JOptionPane.showMessageDialog(this, "Recurso eliminado correctamente.");
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarCampos() {
        txtId.setText("");
        txtDescripcion.setText("");
        if (cmbCategoria.getItemCount() > 0) cmbCategoria.setSelectedIndex(0);
        tabla.clearSelection();
    }
}
