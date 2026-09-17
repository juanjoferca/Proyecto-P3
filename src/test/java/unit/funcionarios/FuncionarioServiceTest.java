package unit.funcionarios;

import model.EstadoReserva;
import model.Funcionario;
import model.Reserva;
import model.Rol;
import model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.AuthService;
import service.FuncionarioService;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class FuncionarioServiceTest {

    private FuncionarioService servicio;
    private UsuarioRepositoryEnMemoria usuarios;
    private ReservaRepositoryEnMemoria reservas;

    @BeforeEach
    void prepararCadaPrueba() {
        usuarios = new UsuarioRepositoryEnMemoria();
        reservas = new ReservaRepositoryEnMemoria();
        servicio = new FuncionarioService(new FuncionarioRepositoryEnMemoria(), usuarios, reservas);
    }

    @Test
    void alIncluirSeCreaElFuncionario() {
        servicio.incluir("111", "Juan Perez", "3323");

        Funcionario funcionario = servicio.buscarPorId("111");
        assertNotNull(funcionario);
        assertEquals("Juan Perez", funcionario.getNombre());
        assertEquals("3323", funcionario.getTelefono());
    }

    @Test
    void alIncluirSeCreaSuUsuarioConClaveIgualAlIdHasheada() {
        servicio.incluir("111", "Juan Perez", "3323");

        Usuario usuario = usuarios.buscarPorId("111");
        assertNotNull(usuario);
        assertEquals(AuthService.hashear("111"), usuario.getClave());
        assertEquals(Rol.FUNCIONARIO, usuario.getRol());
    }

    @Test
    void noPermiteIdRepetido() {
        servicio.incluir("111", "Juan Perez", "3323");

        assertThrows(IllegalArgumentException.class,
                () -> servicio.incluir("111", "Maria Perez", "222222"));
    }

    @Test
    void noPermiteCamposVacios() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.incluir("", "Juan Perez", "3323"));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.incluir("111", "", "3323"));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.incluir("111", "Juan Perez", ""));
    }

    @Test
    void modificarCambiaNombreYTelefonoPeroNoElId() {
        servicio.incluir("111", "Juan Perez", "3323");

        servicio.modificar("111", "Juan Perez Mora", "88887777");

        Funcionario funcionario = servicio.buscarPorId("111");
        assertEquals("111", funcionario.getId());
        assertEquals("Juan Perez Mora", funcionario.getNombre());
        assertEquals("88887777", funcionario.getTelefono());
    }

    @Test
    void noModificaUnFuncionarioInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.modificar("999", "Nadie", "0000"));
    }

    @Test
    void eliminarBorraTambienSuUsuario() {
        servicio.incluir("111", "Juan Perez", "3323");

        servicio.eliminar("111");

        assertNull(servicio.buscarPorId("111"));
        assertNull(usuarios.buscarPorId("111"));
    }

    @Test
    void noEliminaUnFuncionarioInexistente() {
        assertThrows(IllegalArgumentException.class, () -> servicio.eliminar("999"));
    }

    @Test
    void noEliminaUnFuncionarioConReservasFuturasActivas() {
        servicio.incluir("111", "Juan Perez", "3323");
        reservas.guardar(new Reserva("RES-000001", "Reunion", LocalDate.now().plusDays(5),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "111", EstadoReserva.ACTIVA));

        assertThrows(IllegalArgumentException.class, () -> servicio.eliminar("111"));
        assertNotNull(servicio.buscarPorId("111"));
    }

    @Test
    void siEliminaUnFuncionarioConReservasCanceladasOPasadas() {
        servicio.incluir("111", "Juan Perez", "3323");
        reservas.guardar(new Reserva("RES-000002", "Reunion", LocalDate.now().plusDays(5),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "111", EstadoReserva.CANCELADA));
        reservas.guardar(new Reserva("RES-000003", "Reunion pasada", LocalDate.now().minusDays(5),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "111", EstadoReserva.ACTIVA));

        assertDoesNotThrow(() -> servicio.eliminar("111"));
        assertNull(servicio.buscarPorId("111"));
    }

    @Test
    void laBusquedaPorIdEsExacta() {
        servicio.incluir("111", "Juan Perez", "3323");
        servicio.incluir("222", "Maria Perez", "222222");

        assertEquals(1, servicio.buscar("111", "").size());
        assertEquals(0, servicio.buscar("999", "").size());
    }

    @Test
    void laBusquedaPorNombreEsParcialYSinMayusculas() {
        servicio.incluir("111", "Juan Perez", "3323");
        servicio.incluir("222", "Maria Perez", "222222");
        servicio.incluir("333", "Carlos Ramirez", "8888888");

        assertEquals(2, servicio.buscar("", "perez").size());
        assertEquals(1, servicio.buscar("", "CARLOS").size());
        assertEquals(3, servicio.buscar("", "").size());
    }
}