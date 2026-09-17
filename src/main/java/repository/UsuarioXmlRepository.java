package repository;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import model.Usuario;
import model.Rol;
import model.UsuariosWrapper;

import java.io.File;
import java.util.List;

public class UsuarioXmlRepository implements UsuarioRepository {

    private static final String ARCHIVO = "usuarios.xml";

    private UsuariosWrapper cargar() {
        File archivo = new File(ARCHIVO);
        if (!archivo.exists()) {
            UsuariosWrapper datos = new UsuariosWrapper();
            crearAdminPorDefecto(datos);
            return datos;
        }
        try {
            JAXBContext contexto = JAXBContext.newInstance(UsuariosWrapper.class);
            Unmarshaller unmarshaller = contexto.createUnmarshaller();
            return (UsuariosWrapper) unmarshaller.unmarshal(archivo);
        } catch (JAXBException e) {
            throw new RuntimeException("No se pudo leer " + ARCHIVO, e);
        }
    }

    private void crearAdminPorDefecto(UsuariosWrapper datos) {
        // Para que siempre exista un usuario con el que entrar la primera vez
        Usuario admin = new Usuario("admin", "admin", Rol.ADMINISTRADOR);
        datos.getUsuarios().add(admin);
        guardarEnArchivo(datos);
    }

    private void guardarEnArchivo(UsuariosWrapper datos) {
        try {
            JAXBContext contexto = JAXBContext.newInstance(UsuariosWrapper.class);
            Marshaller marshaller = contexto.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(datos, new File(ARCHIVO));
        } catch (JAXBException e) {
            throw new RuntimeException("No se pudo guardar " + ARCHIVO, e);
        }
    }

    @Override
    public List<Usuario> listarTodos() {
        return cargar().getUsuarios();
    }

    @Override
    public Usuario buscarPorId(String id) {
        return cargar().getUsuarios().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void guardar(Usuario usuario) {
        UsuariosWrapper datos = cargar();
        datos.getUsuarios().removeIf(u -> u.getId().equals(usuario.getId()));
        datos.getUsuarios().add(usuario);
        guardarEnArchivo(datos);
    }

    @Override
    public void eliminar(String id) {
        UsuariosWrapper datos = cargar();
        datos.getUsuarios().removeIf(u -> u.getId().equals(id));
        guardarEnArchivo(datos);
    }
}