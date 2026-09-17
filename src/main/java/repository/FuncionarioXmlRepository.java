package repository;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import model.Funcionario;
import model.FuncionariosWrapper;

import java.io.File;
import java.util.List;

public class FuncionarioXmlRepository implements FuncionarioRepository {

    private static final String ARCHIVO = "funcionarios.xml";

    private FuncionariosWrapper cargar() {
        File archivo = new File(ARCHIVO);
        if (!archivo.exists()) {
            FuncionariosWrapper datos = new FuncionariosWrapper();
            guardarEnArchivo(datos);
            return datos;
        }
        try {
            JAXBContext contexto = JAXBContext.newInstance(FuncionariosWrapper.class);
            Unmarshaller unmarshaller = contexto.createUnmarshaller();
            return (FuncionariosWrapper) unmarshaller.unmarshal(archivo);
        } catch (JAXBException e) {
            throw new RuntimeException("No se pudo leer " + ARCHIVO, e);
        }
    }

    private void guardarEnArchivo(FuncionariosWrapper datos) {
        try {
            JAXBContext contexto = JAXBContext.newInstance(FuncionariosWrapper.class);
            Marshaller marshaller = contexto.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(datos, new File(ARCHIVO));
        } catch (JAXBException e) {
            throw new RuntimeException("No se pudo guardar " + ARCHIVO, e);
        }
    }

    @Override
    public List<Funcionario> listarTodos() {
        return cargar().getFuncionarios();
    }

    @Override
    public Funcionario buscarPorId(String id) {
        return cargar().getFuncionarios().stream()
                .filter(f -> f.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void guardar(Funcionario funcionario) {
        FuncionariosWrapper datos = cargar();
        datos.getFuncionarios().removeIf(f -> f.getId().equals(funcionario.getId()));
        datos.getFuncionarios().add(funcionario);
        guardarEnArchivo(datos);
    }

    @Override
    public void eliminar(String id) {
        FuncionariosWrapper datos = cargar();
        datos.getFuncionarios().removeIf(f -> f.getId().equals(id));
        guardarEnArchivo(datos);
    }
}