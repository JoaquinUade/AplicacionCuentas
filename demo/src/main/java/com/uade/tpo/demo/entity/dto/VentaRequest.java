package com.uade.tpo.demo.entity.dto;

import com.uade.tpo.demo.entity.TipoDePago;
import java.util.List;

public class VentaRequest {
    private Long idCliente;
    private List<Long> idProductos;
    private List<Integer> cantidades;
    private TipoDePago estado;
    private String observaciones;

    public VentaRequest() {
    }

    public VentaRequest(Long idCliente, List<Long> idProductos, List<Integer> cantidades, TipoDePago estado, String observaciones) {
        this.idCliente = idCliente;
        this.idProductos = idProductos;
        this.cantidades = cantidades;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public List<Long> getIdProductos() {
        return idProductos;
    }

    public void setIdProductos(List<Long> idProductos) {
        this.idProductos = idProductos;
    }

    public List<Integer> getCantidades() {
        return cantidades;
    }

    public void setCantidades(List<Integer> cantidades) {
        this.cantidades = cantidades;
    }

    public TipoDePago getEstado() {
        return estado;
    }

    public void setEstado(TipoDePago estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
