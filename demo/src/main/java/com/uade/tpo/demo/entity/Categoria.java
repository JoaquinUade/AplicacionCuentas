package com.uade.tpo.demo.entity;

public enum Categoria {
    ENTRADA("Entrada"),
    BEBIDA("Bebida"),
    DESAYUNO("Desayuno"),
    OTROS("Otros"),
    GUARNICIONES("Guarniciones"),
    SANDWICHES("Sandwiches"),
    POSTRES("Postres"),
    FAJITAS("Fajitas"),
    WOKS("Woks"),
    PASTAS("Pastas"),
    ENSALADAS("Ensaladas"),
    MILANESAS("Milanesas"),
    VINOS("Vinos");

    private final String descripcion;

    Categoria(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}