package com.uade.tpo.demo.repository;

import com.uade.tpo.demo.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
/*JpaRepository te da las funciones 
findAll() → buscar todos los clientes
findById(id) → buscar un cliente por ID
save(cliente) → insertar o actualizar
delete(cliente) → borrar
deleteById(id) → borrar por ID
count() → contar filas*/
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
