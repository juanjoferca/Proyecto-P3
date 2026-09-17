package view;


import repository.ReservaRepository;

import javax.swing.*;
import java.awt.*;

/**
 * Pantalla general de Estadísticas: une en pestañas la estadística de
 * recursos (JPanel construido por el Integrante 1) y la estadística
 * de actividades (JPanel construido por el Integrante 3).
 *
 * Es un JPanel (no JFrame) porque a su vez debe encajar dentro de la
 * pestaña "Estadísticas" de la ventana principal (MainView).
 */
public class EstadisticasPanel extends JPanel {

    private final JTabbedPane tabbedPane;
    private final EstadisticasActividadesPanel panelActividades;

    public EstadisticasPanel() {
        setLayout(new BorderLayout());
        tabbedPane = new JTabbedPane();
        panelActividades = new EstadisticasActividadesPanel();
        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Se debe llamar después de construir este panel, para inyectar
     * las dependencias reales y el panel ya armado por Integrante 1.
     *
     * @param reservaRepo               repositorio real de reservas (Integrante 2)
     * @param panelEstadisticasRecursos JPanel ya construido por Integrante 1
*/
    public void configurarDependencias(ReservaRepository reservaRepo, JPanel panelEstadisticasRecursos) {
        panelActividades.configurarDependencias(reservaRepo);

        tabbedPane.addTab("Estadísticas de Recursos", panelEstadisticasRecursos);
        tabbedPane.addTab("Estadísticas de Actividades", panelActividades);
        Tema.aplicar(this);
    }
}
