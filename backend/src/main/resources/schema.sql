-- Schema for MySQL: creates tables with explicit column order

CREATE TABLE IF NOT EXISTS clientes (
  id_cliente BIGINT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(255) NOT NULL,
  tipo_cliente VARCHAR(50) NOT NULL,
  PRIMARY KEY (id_cliente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ventas (
  id_venta BIGINT NOT NULL AUTO_INCREMENT,
  fecha DATETIME NOT NULL,
  id_cliente BIGINT NOT NULL,
  descripcion VARCHAR(255) NOT NULL,
  estado VARCHAR(50) NOT NULL,
  monto DOUBLE NOT NULL,
  observaciones TEXT,
  PRIMARY KEY (id_venta),
  CONSTRAINT fk_ventas_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla productos (orden: id_producto, nombre, precio, categoria)
CREATE TABLE IF NOT EXISTS productos (
  id_producto BIGINT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(255) NOT NULL,
  precio DOUBLE NOT NULL,
  categoria VARCHAR(50) NOT NULL,
  PRIMARY KEY (id_producto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
