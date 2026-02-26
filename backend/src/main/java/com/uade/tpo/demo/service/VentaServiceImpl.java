package com.uade.tpo.demo.service;

import com.uade.tpo.demo.entity.*;
import com.uade.tpo.demo.entity.dto.VentaRequest;
import com.uade.tpo.demo.entity.dto.VentaDTO;
import com.uade.tpo.demo.entity.dto.VentaResumenDiarioDTO;
import com.uade.tpo.demo.repository.ClienteRepository;
import com.uade.tpo.demo.repository.ProductoRepository;
import com.uade.tpo.demo.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VentaServiceImpl implements VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Override
    public Venta crearVenta(VentaRequest ventaRequest) {
        // Obtener el cliente
        Cliente cliente = clienteRepository.findById(ventaRequest.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Calcular monto total y construir descripción
        Double montoTotal = 0.0;
        StringBuilder descripcion = new StringBuilder();

        List<Long> idProductos = ventaRequest.getIdProductos();
        List<Integer> cantidades = ventaRequest.getCantidades();

        for (int i = 0; i < idProductos.size(); i++) {
            final int index = i;
            Producto producto = productoRepository.findById(idProductos.get(index))
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + idProductos.get(index)));

            int cantidad = cantidades.get(index);
            Double precioProducto = producto.getPrecio();

            // Sumar al monto total
            montoTotal += cantidad * precioProducto;

            // Construir descripción
            if (index > 0) {
                descripcion.append(" + ");
            }
            descripcion.append(cantidad).append(" ").append(producto.getNombre());
        }

        // Crear y guardar la venta
        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setDescripcion(descripcion.toString());
        venta.setMonto(montoTotal);
        venta.setEstado(ventaRequest.getEstado());
        venta.setObservaciones(ventaRequest.getObservaciones());
        venta.setFecha(LocalDateTime.now());

        return ventaRepository.save(venta);
    }

    @Override
    public Venta modificarVenta(Long id, Venta venta) {
        return ventaRepository.findById(id).map(existing -> {
            if (venta.getCliente() != null) {
                existing.setCliente(venta.getCliente());
            }
            if (venta.getDescripcion() != null) {
                existing.setDescripcion(venta.getDescripcion());
            }
            if (venta.getMonto() != null) {
                existing.setMonto(venta.getMonto());
            }
            if (venta.getEstado() != null) {
                existing.setEstado(venta.getEstado());
            }
            if (venta.getObservaciones() != null) {
                existing.setObservaciones(venta.getObservaciones());
            }
            // fecha y id no se actualizan aquí
            return ventaRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Venta no encontrada con id: " + id));
    }

    @Override
    public void borrarVenta(Long id) {
        ventaRepository.deleteById(id);
    }


    @Override
    public List<Venta> filtrarPorMes(int mes, int anio) {
        return ventaRepository.findAll()
                .stream()
                .filter(v -> v.getFecha().getMonthValue() == mes && v.getFecha().getYear() == anio)
                .collect(Collectors.toList());
    }

    @Override
    public List<Venta> filtrarPorAnio(int anio) {
        return ventaRepository.findAll()
                .stream()
                .filter(v -> v.getFecha().getYear() == anio)
                .collect(Collectors.toList());
    }

    @Override
    public List<Venta> filtrarPorDia(int dia) {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();

        return ventaRepository.findAll()
            .stream()
            .filter(v -> v.getFecha().getDayOfMonth() == dia
                && v.getFecha().getMonthValue() == currentMonth
                && v.getFecha().getYear() == currentYear)
            .collect(Collectors.toList());
    }

    @Override
    public List<Venta> filtrarPorAnioYMes(int anio, int mes) {
        return ventaRepository.findAll()
                .stream()
                .filter(v -> v.getFecha().getYear() == anio && v.getFecha().getMonthValue() == mes)
                .collect(Collectors.toList());
    }

    @Override
    public List<Venta> filtrarPorAnioMesDia(int anio, int mes, int dia) {
        return ventaRepository.findAll()
                .stream()
                .filter(v -> v.getFecha().getYear() == anio && 
                            v.getFecha().getMonthValue() == mes && 
                            v.getFecha().getDayOfMonth() == dia)
                .collect(Collectors.toList());
    }

    @Override
    public List<Venta> obtenerTodas() {
        return ventaRepository.findAll();
    }

    @Override
    public List<VentaDTO> obtenerVentasOrdenadas(Integer mes, Integer anio) {
        int yearFilter = anio != null ? anio : java.time.LocalDate.now().getYear();
        
        return ventaRepository.findAll()
                .stream()
                .filter(v -> {
                    // Filtrar por año
                    if (v.getFecha().getYear() != yearFilter) {
                        return false;
                    }
                    // Filtrar por mes si se proporciona
                    if (mes != null && v.getFecha().getMonthValue() != mes) {
                        return false;
                    }
                    return true;
                })
                .map(v -> new VentaDTO(
                        v.getFecha(),
                        v.getCliente().getNombre(),
                        v.getDescripcion(),
                        v.getMonto()
                ))
                .sorted((v1, v2) -> {
                    int comparaNombre = v1.getNombreCliente().compareTo(v2.getNombreCliente());
                    if (comparaNombre != 0) {
                        return comparaNombre;
                    }
                    return v1.getFecha().compareTo(v2.getFecha());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<VentaResumenDiarioDTO> obtenerResumenDiarioPorTipoPago(Integer mes, Integer anio) {
        int yearFilter = anio != null ? anio : java.time.LocalDate.now().getYear();
        int mesFilter = mes != null ? mes : java.time.LocalDate.now().getMonthValue();

        return ventaRepository.findAll()
                .stream()
                .filter(v -> v.getFecha().getYear() == yearFilter && v.getFecha().getMonthValue() == mesFilter)
                .collect(Collectors.groupingBy(v -> v.getFecha().toLocalDate()))
                .entrySet()
                .stream()
                .map(entry -> {
                    LocalDate fecha = entry.getKey();
                    List<Venta> ventasDelDia = entry.getValue();

                    VentaResumenDiarioDTO resumen = new VentaResumenDiarioDTO(fecha);

                    for (Venta venta : ventasDelDia) {
                        Double monto = venta.getMonto();
                        resumen.setVentaTotal(resumen.getVentaTotal() + monto);

                        switch (venta.getEstado()) {
                            case TRANSFERENCIA:
                                resumen.setTransferencia(resumen.getTransferencia() + monto);
                                break;
                            case DEBE:
                                resumen.setDebe(resumen.getDebe() + monto);
                                break;
                            case EFECTIVO:
                                resumen.setEfectivo(resumen.getEfectivo() + monto);
                                break;
                            case MERCADO_PAGO:
                                resumen.setMercadoPago(resumen.getMercadoPago() + monto);
                                break;
                            case DEBITO:
                                resumen.setDebito(resumen.getDebito() + monto);
                                break;
                            case CREDITO:
                                resumen.setCredito(resumen.getCredito() + monto);
                                break;
                        }
                    }

                    return resumen;
                })
                .sorted((r1, r2) -> r1.getFecha().compareTo(r2.getFecha()))
                .collect(Collectors.toList());
    }
}
