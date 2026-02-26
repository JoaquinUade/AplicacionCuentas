package com.uade.tpo.demo.entity.dto;

import java.time.LocalDate;

public class VentaResumenDiarioDTO {
    private LocalDate fecha;
    private Double ventaTotal;
    private Double transferencia;
    private Double debe;
    private Double efectivo;
    private Double mercadoPago;
    private Double debito;
    private Double credito;

    public VentaResumenDiarioDTO() {
        this.ventaTotal = 0.0;
        this.transferencia = 0.0;
        this.debe = 0.0;
        this.efectivo = 0.0;
        this.mercadoPago = 0.0;
        this.debito = 0.0;
        this.credito = 0.0;
    }

    public VentaResumenDiarioDTO(LocalDate fecha) {
        this.fecha = fecha;
        this.ventaTotal = 0.0;
        this.transferencia = 0.0;
        this.debe = 0.0;
        this.efectivo = 0.0;
        this.mercadoPago = 0.0;
        this.debito = 0.0;
        this.credito = 0.0;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Double getVentaTotal() {
        return ventaTotal;
    }

    public void setVentaTotal(Double ventaTotal) {
        this.ventaTotal = ventaTotal;
    }

    public Double getTransferencia() {
        return transferencia;
    }

    public void setTransferencia(Double transferencia) {
        this.transferencia = transferencia;
    }

    public Double getDebe() {
        return debe;
    }

    public void setDebe(Double debe) {
        this.debe = debe;
    }

    public Double getEfectivo() {
        return efectivo;
    }

    public void setEfectivo(Double efectivo) {
        this.efectivo = efectivo;
    }

    public Double getMercadoPago() {
        return mercadoPago;
    }

    public void setMercadoPago(Double mercadoPago) {
        this.mercadoPago = mercadoPago;
    }

    public Double getDebito() {
        return debito;
    }

    public void setDebito(Double debito) {
        this.debito = debito;
    }

    public Double getCredito() {
        return credito;
    }

    public void setCredito(Double credito) {
        this.credito = credito;
    }
}
