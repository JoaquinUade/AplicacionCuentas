package com.uade.tpo.demo.entity;

public enum Categoria {
    ENTRADA("Entrada"),
    BEBIDA("Bebida"),
    DESAYUNO("Desayuno"),
    ALMUERZO("Almuerzo");

    private final String descripcion;

    Categoria(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
