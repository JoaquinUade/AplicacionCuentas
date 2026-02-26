package com.uade.tpo.demo.service;

import com.uade.tpo.demo.entity.dto.VentaRequest;
import com.uade.tpo.demo.entity.dto.VentaDTO;
import com.uade.tpo.demo.entity.dto.VentaResumenDiarioDTO;
import com.uade.tpo.demo.entity.Venta;
import java.util.List;

public interface VentaService {
    Venta crearVenta(VentaRequest ventaRequest);
    List<Venta> filtrarPorMes(int mes, int anio);
    List<Venta> filtrarPorAnio(int anio);
    List<Venta> filtrarPorDia(int dia);
    List<Venta> filtrarPorAnioYMes(int anio, int mes);
    List<Venta> filtrarPorAnioMesDia(int anio, int mes, int dia);
    List<Venta> obtenerTodas();
    List<VentaDTO> obtenerVentasOrdenadas(Integer mes, Integer anio);
    List<VentaResumenDiarioDTO> obtenerResumenDiarioPorTipoPago(Integer mes, Integer anio);

    /**
     * Actualiza parcialmente los datos de una venta existente.
     */
    Venta modificarVenta(Long id, Venta venta);

    /**
     * Borra una venta por su id.
     */
    void borrarVenta(Long id);
} 
