package com.uade.tpo.demo.controllers;

import com.uade.tpo.demo.entity.Venta;
import com.uade.tpo.demo.entity.dto.VentaRequest;
import com.uade.tpo.demo.entity.dto.VentaDTO;
import com.uade.tpo.demo.entity.dto.VentaResumenDiarioDTO;
import com.uade.tpo.demo.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
public class VentasController {

    @Autowired
    private VentaService ventaService;

    @PostMapping
    public ResponseEntity<Venta> crearVenta(@RequestBody VentaRequest ventaRequest) {
        Venta venta = ventaService.crearVenta(ventaRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(venta);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Venta> actualizarParcialVenta(@PathVariable Long id, @RequestBody Venta venta) {
        Venta mod = ventaService.modificarVenta(id, venta);
        return ResponseEntity.ok(mod);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrarUnaVenta(@PathVariable Long id) {
        ventaService.borrarVenta(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Venta>> obtenerTodas() {
        List<Venta> ventas = ventaService.obtenerTodas();
        return ResponseEntity.ok(ventas);
    }

    @GetMapping("/filtro/mes")
    public ResponseEntity<List<Venta>> filtrarPorMes(@RequestParam int mes, @RequestParam(required = false) Integer anio) {
        if (anio == null) {
            anio = java.time.LocalDate.now().getYear();
        }
        List<Venta> ventas = ventaService.filtrarPorMes(mes, anio);
        return ResponseEntity.ok(ventas);
    }

    @GetMapping("/filtro/anio")
    public ResponseEntity<List<Venta>> filtrarPorAnio(@RequestParam int anio) {
        List<Venta> ventas = ventaService.filtrarPorAnio(anio);
        return ResponseEntity.ok(ventas);
    }

    @GetMapping("/filtro/dia")
    public ResponseEntity<List<Venta>> filtrarPorDia(@RequestParam int dia) {
        List<Venta> ventas = ventaService.filtrarPorDia(dia);
        return ResponseEntity.ok(ventas);
    }

    @GetMapping("/filtro/anio-mes")
    public ResponseEntity<List<Venta>> filtrarPorAnioYMes(@RequestParam int anio, @RequestParam int mes) {
        List<Venta> ventas = ventaService.filtrarPorAnioYMes(anio, mes);
        return ResponseEntity.ok(ventas);
    }

    @GetMapping("/filtro/anio-mes-dia")
    public ResponseEntity<List<Venta>> filtrarPorAnioMesDia(@RequestParam int anio, @RequestParam int mes, @RequestParam int dia) {
        List<Venta> ventas = ventaService.filtrarPorAnioMesDia(anio, mes, dia);
        return ResponseEntity.ok(ventas);
    }

    @GetMapping("/resumen-por-empresa")
    public ResponseEntity<List<VentaDTO>> obtenerVentasOrdenadas(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        if (anio == null) {
            anio = java.time.LocalDate.now().getYear();
        }
        List<VentaDTO> ventas = ventaService.obtenerVentasOrdenadas(mes, anio);
        return ResponseEntity.ok(ventas);
    }

    @GetMapping("/resumen-diario")
    public ResponseEntity<List<VentaResumenDiarioDTO>> obtenerResumenDiarioPorTipoPago(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        List<VentaResumenDiarioDTO> resumen = ventaService.obtenerResumenDiarioPorTipoPago(mes, anio);
        return ResponseEntity.ok(resumen);
    }
}
