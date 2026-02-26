package com.uade.tpo.demo.entity.dto;

import java.time.LocalDateTime;

public class VentaDTO {
    private LocalDateTime fecha;
    private String nombreCliente;
    private String descripcion;
    private Double monto;
    //se usa para resumen de venta, porque solo mostramos nombre, fecha, monto y descripcion. Si lees esto dai te amo<3
    public VentaDTO() {
    }

    public VentaDTO(LocalDateTime fecha, String nombreCliente, String descripcion, Double monto) {
        this.fecha = fecha;
        this.nombreCliente = nombreCliente;
        this.descripcion = descripcion;
        this.monto = monto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }
}
