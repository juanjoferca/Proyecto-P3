package service;

import model.Usuario;
import repository.UsuarioRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthService {

    private static final int LARGO_MINIMO_CLAVE = 6;

    private final UsuarioRepository usuarioRepository;

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario iniciarSesion(String id, String clave) {
        if (id == null || id.isBlank() || clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("El ID y la clave no pueden estar vacíos.");
        }

        Usuario usuario = usuarioRepository.buscarPorId(id);
        if (usuario == null) {
            throw new IllegalArgumentException("No existe un usuario con ese ID.");
        }

        if (!coincideConClaveGuardada(usuario, clave)) {
            throw new IllegalArgumentException("La clave es incorrecta.");
        }

        SessionManager.getInstancia().iniciarSesion(usuario);
        return usuario;
    }

    /** Verifica que el ID corresponda a un usuario registrado. */
    public void verificarExiste(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el ID del usuario.");
        }
        if (usuarioRepository.buscarPorId(id) == null) {
            throw new IllegalArgumentException("No existe un usuario con ese ID.");
        }
    }

    /**
     * Cambio de clave desde el login: el ID viene de la pantalla anterior,
     * por lo que no requiere una sesión activa.
     */
    public void cambiarClave(String id, String claveActual, String claveNueva, String confirmacion) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el ID del usuario.");
        }

        Usuario usuario = usuarioRepository.buscarPorId(id);
        if (usuario == null) {
            throw new IllegalArgumentException("No existe un usuario con ese ID.");
        }

        aplicarCambio(usuario, claveActual, claveNueva, confirmacion);
    }

    /**
     * Cambio de clave desde el sistema: usa el usuario de la sesión activa.
     */
    public void cambiarClave(String claveActual, String claveNueva, String confirmacion) {
        Usuario usuario = SessionManager.getInstancia().getUsuarioActual();
        if (usuario == null) {
            throw new IllegalStateException("No hay una sesión activa.");
        }

        aplicarCambio(usuario, claveActual, claveNueva, confirmacion);
    }

    private void aplicarCambio(Usuario usuario, String claveActual, String claveNueva, String confirmacion) {
        if (!coincideConClaveGuardada(usuario, claveActual)) {
            throw new IllegalArgumentException("La clave actual no coincide.");
        }

        if (claveNueva == null || claveNueva.isBlank()) {
            throw new IllegalArgumentException("La clave nueva no puede estar vacía.");
        }

        if (claveNueva.length() < LARGO_MINIMO_CLAVE) {
            throw new IllegalArgumentException("La clave nueva debe tener al menos "
                    + LARGO_MINIMO_CLAVE + " caracteres.");
        }

        if (!claveNueva.equals(confirmacion)) {
            throw new IllegalArgumentException("Las claves nuevas no coinciden.");
        }

        usuario.setClave(hashear(claveNueva));
        usuarioRepository.guardar(usuario);
    }

    private boolean coincideConClaveGuardada(Usuario usuario, String claveIngresada) {
        String claveGuardada = usuario.getClave();

        if (claveGuardada.equals(hashear(claveIngresada))) {
            return true;
        }

        if (claveGuardada.equals(claveIngresada)) {
            usuario.setClave(hashear(claveIngresada));
            usuarioRepository.guardar(usuario);
            return true;
        }

        return false;
    }

    public static String hashear(String clave) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(clave.getBytes(StandardCharsets.UTF_8));
            StringBuilder resultado = new StringBuilder();
            for (byte b : hash) {
                resultado.append(String.format("%02x", b));
            }
            return resultado.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la clave.", e);
        }
    }
}