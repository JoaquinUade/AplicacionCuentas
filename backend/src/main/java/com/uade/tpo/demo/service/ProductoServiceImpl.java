package com.uade.tpo.demo.service;

import com.uade.tpo.demo.entity.Categoria;
import com.uade.tpo.demo.entity.Producto;
import com.uade.tpo.demo.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductoServiceImpl implements ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Override
    public Producto crearProducto(Producto producto) {
        return productoRepository.save(producto);
    }

    @Override
    public void borrarProducto(Long id) {
        productoRepository.deleteById(id);
    }


    @Override
    public Producto modificarProducto(Long id, Producto producto) {
        Optional<Producto> productoExistente = productoRepository.findById(id);
        if (productoExistente.isPresent()) {
            Producto p = productoExistente.get();
            if (producto.getNombre() != null) {
                p.setNombre(producto.getNombre());
            }
            if (producto.getPrecio() != null) {
                p.setPrecio(producto.getPrecio());
            }
            if (producto.getCategoria() != null) {
                p.setCategoria(producto.getCategoria());
            }
            return productoRepository.save(p);
        }
        throw new RuntimeException("Producto no encontrado con id: " + id);
    }

    @Override
    public Optional<Producto> obtenerProductoById(Long id) {
        return productoRepository.findById(id);
    }

    @Override
    public List<Producto> obtenerTodosLosProductos() {
        return productoRepository.findAll();
    }

    @Override
    public List<Producto> filtrarPorNombre(String nombre) {
        return productoRepository.findAll()
                .stream()
                .filter(p -> p.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Producto> filtrarPorCategoria(Categoria categoria) {
        return productoRepository.findAll()
                .stream()
                .filter(p -> p.getCategoria() == categoria)
                .collect(Collectors.toList());
    }

    @Override
    public List<Producto> filtrarPorNombreYCategoria(String nombre, Categoria categoria) {
        return productoRepository.findAll()
                .stream()
                .filter(p -> p.getNombre().toLowerCase().contains(nombre.toLowerCase()) && p.getCategoria() == categoria)
                .collect(Collectors.toList());
    }
}
