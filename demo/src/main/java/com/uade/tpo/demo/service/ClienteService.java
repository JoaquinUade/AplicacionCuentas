package com.uade.tpo.demo.service;

import com.uade.tpo.demo.entity.Cliente;
import java.util.List;
import java.util.Optional;

public interface ClienteService {
    Cliente crearCliente(Cliente cliente);
    void borrarCliente(Long id);
    Optional<Cliente> obtenerClienteById(Long id);
    List<Cliente> obtenerTodosLosClientes();
}
