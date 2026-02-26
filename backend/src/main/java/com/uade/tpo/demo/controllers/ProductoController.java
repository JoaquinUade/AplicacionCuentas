package com.uade.tpo.demo.controllers;

import com.uade.tpo.demo.entity.Categoria;
import com.uade.tpo.demo.entity.Producto;
import com.uade.tpo.demo.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @PostMapping
    public ResponseEntity<Producto> crearProducto(@RequestBody Producto producto) {
        Producto productoCreado = productoService.crearProducto(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(productoCreado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrarProducto(@PathVariable Long id) {
        productoService.borrarProducto(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> modificarProducto(@PathVariable Long id, @RequestBody Producto producto) {
        Producto productoModificado = productoService.modificarProducto(id, producto);
        return ResponseEntity.ok(productoModificado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerProductoById(@PathVariable Long id) {
        Optional<Producto> producto = productoService.obtenerProductoById(id);
        return producto.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Producto>> obtenerTodosLosProductos() {
        List<Producto> productos = productoService.obtenerTodosLosProductos();
        return ResponseEntity.ok(productos);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Producto> actualizarParcialProducto(@PathVariable Long id, @RequestBody Producto producto) {
        Producto modificado = productoService.modificarProducto(id, producto);
        return ResponseEntity.ok(modificado);
    }

    @GetMapping("/filtro/nombre")
    public ResponseEntity<List<Producto>> filtrarPorNombre(@RequestParam String nombre) {
        List<Producto> productos = productoService.filtrarPorNombre(nombre);
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/filtro/categoria")
    public ResponseEntity<List<Producto>> filtrarPorCategoria(@RequestParam Categoria categoria) {
        List<Producto> productos = productoService.filtrarPorCategoria(categoria);
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/filtro/nombre-categoria")
    public ResponseEntity<List<Producto>> filtrarPorNombreYCategoria(@RequestParam String nombre, @RequestParam Categoria categoria) {
        List<Producto> productos = productoService.filtrarPorNombreYCategoria(nombre, categoria);
        return ResponseEntity.ok(productos);
    }
}
