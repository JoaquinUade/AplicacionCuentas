package com.uade.tpo.demo.service;

import com.uade.tpo.demo.entity.Cliente;
import com.uade.tpo.demo.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteServiceImpl implements ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public Cliente crearCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    @Override
    public void borrarCliente(Long id) {
        clienteRepository.deleteById(id);
    }

    @Override
    public Cliente modificarCliente(Long id, Cliente cliente) {
        return clienteRepository.findById(id).map(existing -> {
            if (cliente.getNombre() != null) {
                existing.setNombre(cliente.getNombre());
            }
            if (cliente.getTipoCliente() != null) {
                existing.setTipoCliente(cliente.getTipoCliente());
            }
            return clienteRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Cliente no encontrado con id: " + id));
    }


    @Override
    public Optional<Cliente> obtenerClienteById(Long id) {
        return clienteRepository.findById(id);
    }

    @Override
    public List<Cliente> obtenerTodosLosClientes() {
        return clienteRepository.findAll();
    }
}
