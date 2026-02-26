package com.uade.tpo.demo.service;

import com.uade.tpo.demo.entity.Categoria;
import com.uade.tpo.demo.entity.Producto;
import java.util.List;
import java.util.Optional;

public interface ProductoService {
    Producto crearProducto(Producto producto);
    void borrarProducto(Long id);
    Producto modificarProducto(Long id, Producto producto);
    Optional<Producto> obtenerProductoById(Long id);
    List<Producto> obtenerTodosLosProductos();
    List<Producto> filtrarPorNombre(String nombre);
    List<Producto> filtrarPorCategoria(Categoria categoria);
    List<Producto> filtrarPorNombreYCategoria(String nombre, Categoria categoria);
} 
