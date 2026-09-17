package repository;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import model.CategoriaRecurso;
import model.CategoriasWrapper;

import java.io.File;
import java.util.List;

public class CategoriaXmlRepository implements CategoriaRepository {

    private static final String ARCHIVO = "categorias.xml";

    private CategoriasWrapper cargar() {
        File archivo = new File(ARCHIVO);
        if (!archivo.exists()) {
            CategoriasWrapper datos = new CategoriasWrapper();
            guardarEnArchivo(datos);
            return datos;
        }
        try {
            JAXBContext contexto = JAXBContext.newInstance(CategoriasWrapper.class);
            Unmarshaller unmarshaller = contexto.createUnmarshaller();
            return (CategoriasWrapper) unmarshaller.unmarshal(archivo);
        } catch (JAXBException e) {
            throw new RuntimeException("No se pudo leer " + ARCHIVO, e);
        }
    }

    private void guardarEnArchivo(CategoriasWrapper datos) {
        try {
            JAXBContext contexto = JAXBContext.newInstance(CategoriasWrapper.class);
            Marshaller marshaller = contexto.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(datos, new File(ARCHIVO));
        } catch (JAXBException e) {
            throw new RuntimeException("No se pudo guardar " + ARCHIVO, e);
        }
    }

    @Override
    public List<CategoriaRecurso> listarTodas() {
        return listarTodos();
    }

    @Override
    public List<CategoriaRecurso> listarTodos() {
        return cargar().getCategorias();
    }

    @Override
    public CategoriaRecurso buscarPorId(String id) {
        return cargar().getCategorias().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void guardar(CategoriaRecurso categoria) {
        CategoriasWrapper datos = cargar();
        datos.getCategorias().removeIf(c -> c.getId().equals(categoria.getId()));
        datos.getCategorias().add(categoria);
        guardarEnArchivo(datos);
    }

    @Override
    public void eliminar(String id) {
        CategoriasWrapper datos = cargar();
        datos.getCategorias().removeIf(c -> c.getId().equals(id));
        guardarEnArchivo(datos);
    }
}