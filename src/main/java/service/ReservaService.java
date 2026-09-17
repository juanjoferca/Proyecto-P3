package service;

import model.DetalleReserva;
import model.EstadoReserva;
import model.Recurso;
import model.Reserva;
import repository.RecursoXmlRepository;
import repository.ReservaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public class ReservaService {

    private final DisponibilidadService disponibilidadService;
    private final ReservaRepository reservaRepository;

    public ReservaService(){
        this.reservaRepository = null;
        this.disponibilidadService = null;
    }

    public ReservaService(DisponibilidadService disponibilidadService, ReservaRepository reservaRepository) {
        this.disponibilidadService = disponibilidadService;
        this.reservaRepository = reservaRepository;
    }

    public ReservaService(ReservaRepository reservaRepository) {
        this(new DisponibilidadService(new RecursoXmlRepository(), reservaRepository), reservaRepository);
    }

    public List<Reserva> listarTodos() {
        return reservaRepository.listar();
    }

    public Reserva buscarPorId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID de la reserva no puede ser nulo o vacío");
        }
        return reservaRepository.buscarPorId(id.trim());
    }

    public void registrar(Reserva reserva) {
        validarReservaBase(reserva);

        //Validar que haya al menos un detalle
        if (reserva.getDetalles() == null || reserva.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una categoría de recurso.");
        }

        // Validar disponibilidad de cada recurso
        for (DetalleReserva detalle : reserva.getDetalles()) {
            if (detalle.getCategoria() == null || detalle.getRecurso() == null) {
                throw new IllegalArgumentException("Los detalles de la reserva deben tener categoría y recurso.");
            }

            Recurso recursoDisponible = disponibilidadService.buscarRecursoDisponible(detalle.getCategoria(),
                    reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());

            if (recursoDisponible == null || !Objects.equals(recursoDisponible.getId(), detalle.getRecurso().getId())) {
                throw new IllegalArgumentException("El recurso de la categoría " + detalle.getCategoria().getId() +
                                " no está disponible en el horario solicitado.");
            }
        }
        reservaRepository.guardar(reserva);
    }

    public void actualizar(Reserva reserva) {
        validarReservaBase(reserva);
        if (reservaRepository.buscarPorId(reserva.getId().trim()) == null) {
            throw new IllegalArgumentException("No existe una reserva con ese ID.");
        }
        reservaRepository.actualizar(reserva);
    }

    public void eliminar(Reserva reserva) {
        validarReservaBase(reserva);
        Reserva reservaExistente = reservaRepository.buscarPorId(reserva.getId().trim());
        if (reservaExistente == null) {
            throw new IllegalArgumentException("No existe una reserva con ese ID.");
        }

        // Validar disponibilidad de recursos para la nueva fecha y la hora
        if (reserva.getDetalles() != null && !reserva.getDetalles().isEmpty()) {
            // Si la fecha u horas cambiaron, validar que los recursos sigan disponibles
            boolean cambioFechaOHora = !reservaExistente.getFecha().equals(reserva.getFecha()) ||
                    !reservaExistente.getHoraInicio().equals(reserva.getHoraInicio()) ||
                    !reservaExistente.getHoraFin().equals(reserva.getHoraFin());

            if (cambioFechaOHora) {
                for (DetalleReserva detalle : reserva.getDetalles()) {
                    Recurso recursoDisponible = disponibilidadService.buscarRecursoDisponible(detalle.getCategoria(),
                            reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());

                    if (recursoDisponible == null || !Objects.equals(recursoDisponible.getId(), detalle.getRecurso().getId())) {
                        throw new IllegalArgumentException("El recurso " + detalle.getRecurso().getId() +
                                " no está disponible " + "para el nuevo horario (" + reserva.getFecha() + " " +
                                reserva.getHoraInicio() + "-" + reserva.getHoraFin() + ")");
                    }
                }
            }
        }

        reservaRepository.actualizar(reserva);
    }

    private void validarReservaBase(Reserva reserva) {
        if (reserva == null) {
            throw new IllegalArgumentException("La reserva no puede ser nula");
        }
        if (reserva.getId() == null || reserva.getId().isBlank()) {
            throw new IllegalArgumentException("El ID de la reserva no puede ser nulo o vacío");
        }
        if (reserva.getActividad() == null || reserva.getActividad().isBlank()) {
            throw new IllegalArgumentException("La actividad no puede ser nula o vacía");
        }
        if (reserva.getFecha() == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        //Validar que la fecha sea en el futuro (u hoy)
        if (reserva.getFecha().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se puede crear una reserva para una fecha pasada");
        }
        if (reserva.getHoraInicio() == null) {
            throw new IllegalArgumentException("La hora de inicio no puede ser nula");
        }
        if (reserva.getHoraFin() == null) {
            throw new IllegalArgumentException("La hora de fin no puede ser nula");
        }
        //Validar que las horas estén dentro del rango de operación
        LocalTime horaMin = LocalTime.of(6, 0);
        LocalTime horaMax = LocalTime.of(22, 0);

        if (reserva.getHoraInicio().isBefore(horaMin) || reserva.getHoraInicio().isAfter(horaMax)) {
            throw new IllegalArgumentException("La hora de inicio debe estar entre 06:00 y 22:00");
        }
        if (reserva.getHoraFin().isBefore(horaMin) || reserva.getHoraFin().isAfter(horaMax)) {
            throw new IllegalArgumentException("La hora de fin debe estar entre 06:00 y 22:00");
        }
        //Validar duración mínima de 30 minutos
        long minutosDuracion = java.time.temporal.ChronoUnit.MINUTES.between(reserva.getHoraInicio(), reserva.getHoraFin());

        if (minutosDuracion < 30) {
            throw new IllegalArgumentException("La duración mínima de una reserva es 30 minutos");
        }
        if (reserva.getFuncionarioId() == null || reserva.getFuncionarioId().isBlank()) {
            throw new IllegalArgumentException("El funcionario no puede ser nulo o vacío");
        }
        if (reserva.getEstado() == null) {
            throw new IllegalArgumentException("El estado de la reserva no puede ser nulo");
        }
        if (EstadoReserva.ACTIVA.equals(reserva.getEstado()) && reserva.getHoraInicio().isAfter(reserva.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio no puede ser posterior a la hora de fin");
        }
    }
}
