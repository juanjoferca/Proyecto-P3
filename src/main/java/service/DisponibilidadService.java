package service;

import model.DetalleReserva;
import model.EstadoReserva;
import model.Recurso;
import model.Reserva;
import model.CategoriaRecurso;
import repository.RecursoRepository;
import repository.ReservaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;


public class DisponibilidadService {

    private final Object lock = new Object();
    private final RecursoRepository recursoRepository;
    private final ReservaRepository reservaRepository;
    public DisponibilidadService(){
        this.reservaRepository = null;
        this.recursoRepository = null;
    }

    public DisponibilidadService(RecursoRepository recursoRepository, ReservaRepository reservaRepository) {
        this.recursoRepository = recursoRepository;
        this.reservaRepository = reservaRepository;
    }

    boolean estaDisponible(Recurso recurso, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin){
        List<Reserva> reservas = reservaRepository.listar();

        for (Reserva reserva : reservas) {
            if (reserva.getEstado() != EstadoReserva.ACTIVA) continue;
            if (!reserva.getFecha().equals(fecha)) continue;

            // Verificar si hay superposición de horas
            boolean hayConflicto = !horaFin.isAfter(reserva.getHoraInicio()) ||
                    !horaInicio.isBefore(reserva.getHoraFin());

            if (!hayConflicto) {
                // Verificar si este recurso está involucrado en la reserva
                for (DetalleReserva detalle : reserva.getDetalles()) {
                    if (recurso.getId().equals(detalle.getRecurso().getId())) {
                        return false;
                    }
                }
            }
        }

        return true;  // Recurso está disponible
    }

    public Recurso buscarRecursoDisponible(CategoriaRecurso categoria, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        synchronized (lock) {
            List<Recurso> recursosDeCategoria = recursoRepository.buscarPorCategoria(categoria.getId());

            for (Recurso recurso : recursosDeCategoria) {
                if (estaDisponible(recurso, fecha, horaInicio, horaFin)) {
                    return recurso;
                }
            }

            return null;
        }
    }
}
