package paucar.service;

/*define el contenedor que agrupa clases, interfaces y subpaquetes relacionados */
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.tpo.demo.entity.TipoCliente;

public class VentasBackend {

    private final String BASE_URL;/*Variableque contiene la URL base del backend (por ejemplo "http://localhost:8080/api"). */

    private final HttpClient http;/*protocolo de transferencia de hipertexto (Hypertext Transfer Protocol)
                                  para enviar y recibir datos Se utiliza principalmente a través de la API
                                  HttpClient para realizar peticiones GET, POST, PUT y DELETE*/

    private final ObjectMapper TraductorJSON;/*ObjectMapper es una clase de la librería Jackson que sirve para convertir
                                  datos JSON en objetos Java y viceversa*/

    private final ClientesService clientesService;

    public enum TipoDePago {/*enum de tipo de pago, reemplazar en el futuro por el tipo de pago del backend */
        TRANSFERENCIA, DEBE, EFECTIVO, MERCADO_PAGO, DEBITO, CREDITO
    }

    public record VentaFilaDto(String nombre, String descripcion, BigDecimal monto, TipoDePago estado,
            Long idCliente, TipoCliente tipoCliente) {

    }

    // --- Constructor ---
    public VentasBackend(String BASE_URL, ClientesService clientesService) {/*Recibe un parámetro llamado BASE_URL (un String) que debería ser
                                            la URL base del backend */

        this.BASE_URL = Objects.requireNonNull(BASE_URL);/*si el parámetro es null, lanza un NullPointerException
                                                          (excepción en tiempo de ejecución (RuntimeException)
                                                          que ocurre cuando el programa intenta utilizar una
                                                          referencia de objeto que apunta a null)) inmediatamente.
                                                          Esto evita crear una instancia mal configurada */

        this.http = HttpClient.newHttpClient();/*Crea una instancia por defecto de HttpClient (Java 11+), que
                                               vas a usar para hacer GET/POST al backend */

        this.TraductorJSON = new ObjectMapper();/*El campo traductorJson ahora va a contener un ObjectMapper nuevo */

        this.clientesService = Objects.requireNonNull(clientesService);/*valida que el servicio de clientes no sea
                                                                       null, sino lanza una excepción inmediatamente */
    }

    // ============================================================
    //                    VENTAS
    // ============================================================
    public boolean GuardarPedidos(
            Long idCliente,
            List<Long> idProductos,
            List<Integer> cantidades,
            TipoDePago estado,
            String observaciones) {

        try {
            //if (idCliente == null || idProductos.isEmpty() || cantidades.isEmpty()) {/*valida los requerimientos
            //minimos de un pedido valido */
            // System.err.println("Venta inválida");
            // return false;
            // }

            var FichaPedido = TraductorJSON.createObjectNode()/*Creás un objeto JSON vacío */
                    .put("idCliente", idCliente)/*añadimos como variable a rellenar idCliente*/
                    .put("estado", estado == null ? "DEBE" : estado.name())/*añadimos estado con un valor predeterminado si no se le asigna valor */
                    .put("observaciones", observaciones == null ? "" : observaciones);/*añade el campo observaciones, que si no tiene contenido, lo deja vacio */

            var ListaIds = FichaPedido.putArray("idProductos");/*osea que añade otro campo mas a
                                                                          fichapedido, pero que a diferencia de
                                                                          el otro que lo rellena el usuario, se
                                                                          va a rellenar con la data que tienen los
                                                                          productos en una lista, ya que si se
                                                                          piden mas de un producto, recibiria
                                                                          mas de un id*/
            idProductos.forEach(ListaIds::add);

            var ListaCantidades = FichaPedido.putArray("cantidades");/*En el JSON FichaPedido,
                                                                                   agregá un NUEVO CAMPO llamado
                                                                                   cantidades, cuyo valor será
                                                                                   un array vacío  al cual hay
                                                                                   que rellenar con el contenido
                                                                                   de el vector cantidades*/
            cantidades.forEach(ListaCantidades::add);

            var solicitud = HttpRequest.newBuilder()/*Voy a construir una solicitud HTTP nueva */
                    .uri(URI.create(BASE_URL + "/ventas"))
                    .header("Content-Type", "application/json")/*aclaro que el cuerpo que voy a
                                                                           enviar está en formato JSON */
                    .POST(HttpRequest.BodyPublishers.ofString(FichaPedido.toString()))/*la funcion de esta
                                                                                      solicitud es esta
                                                                                      precisamente, postear el
                                                                                      pedido que ya hicimos en
                                                                                      fichapedido */
                    .build();

            var response = http.send(solicitud, HttpResponse.BodyHandlers.ofString());/*se manda la solicitud 
                                                                                      y cuando el servidor
                                                                                      responda converti su
                                                                                      respuesta en un string */
            return response.statusCode() >= 200 && response.statusCode() < 300;/*retorna el codigo que entregue
                                                                              el response si el codigo esta entre
                                                                              200 y 299 */

        } catch (java.io.IOException | InterruptedException e) {
            System.err.println("guardarVentaCliente: " + e.getMessage());
            return false;/*sino retorna falso y te da error */
        }
    }

    public boolean GuardarPedidoMesas(
            String nombreMesa,
            List<Long> idProductos,
            List<Integer> cantidades,
            TipoDePago estado,
            String observaciones) {

        if (idProductos == null || cantidades == null || idProductos.isEmpty() || cantidades.isEmpty()) {
            System.err.println("Pedido (MESA) inválido por carecer de cantidad o de productos válidos");
            return false;
        }
        if (nombreMesa == null || nombreMesa.isBlank()) {
            System.err.println("Pedido (MESA) inválido: nombreMesa vacío");
            return false;
        }

        // Resolver ID de mesa usando el ClientesService INYECTADO
        Long idMesa = clientesService.obtenerClienteIdPorNombre(nombreMesa);
        if (idMesa == null) {
            System.err.println("Mesa no encontrada: " + nombreMesa);
            return false;
        }
        return GuardarPedidos(idMesa, idProductos, cantidades, estado, observaciones);
    }

    public List<VentaFilaDto> cargarVentasDelDia(LocalDate fecha) {
        try {

            var solicitud = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/ventas?fecha=" + fecha.toString()))
                    .GET()
                    .build();

            var response = http.send(solicitud, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {

                var array = TraductorJSON.readTree(response.body());
                var out = new ArrayList<VentaFilaDto>();

                if (array.isArray()) {

                    for (var n : array) {
                        String nombre
                                = n.hasNonNull("clienteNombre") ? n.get("clienteNombre").asText()
                                : n.hasNonNull("nombreCliente") ? n.get("nombreCliente").asText()
                                : n.hasNonNull("nombreMesa") ? n.get("nombreMesa").asText()
                                : "";

                        var desc = n.hasNonNull("descripcion") ? n.get("descripcion").asText() : "";

                        BigDecimal monto = BigDecimal.ZERO;
                        if (n.hasNonNull("monto")) {
                            monto = new BigDecimal(n.get("monto").asText())
                                    .setScale(2, RoundingMode.HALF_UP);
                        }

                        TipoDePago estado = TipoDePago.EFECTIVO;
                        if (n.hasNonNull("estado")) {
                            try {
                                estado = TipoDePago.valueOf(n.get("estado").asText());
                            } catch (Exception ignore) {
                            }
                        }

// NUEVO: opcionalmente mapear idCliente y tipoCliente si vienen
                        Long idCliente = null;/*inicializamos la variable idCliente valiendo null */

                        if (n.hasNonNull("idCliente")) {/* si el objeto JSON n tiene la clave
                                                                  "idCliente" y no es nula entramos*/

                            idCliente = n.get("idCliente").asLong();/*Entrá dentro del objeto JSON
                                                                                  n, buscá el campo llamado
                                                                                  idCliente, sacá el valor que
                                                                                  tenga y guardalo en la variable
                                                                                  Java idCliente*/

                        } else if (n.hasNonNull("cliente")/*si adentro de el objeto json que es n hay
                                                                      un campo llamado cliente*/
                                && n.get("cliente").isObject()/* y ademas esa propiedad es un objeto
                                                                           json*/
                                && n.get("cliente").hasNonNull("idCliente")) {/*y a su
                                                                                                vez dentro de ese
                                                                                           objeto existe un campo
                                                                                           idCliente entramos*/

                            idCliente = n.get("cliente").get("idCliente").asLong();/*obtenemos el id dentro
                                                                                                            de el objeto json cliente
                                                                                                            dentro de n */
                        }
                        TipoCliente tipoCli = null;/*creo una variable del tipo cliente(backend) y lo dejo en
                                                    null por ahora */
                        if (n.hasNonNull("tipoCliente")) {/*pregunto si n tiene el campo tipocliente
                                                                      y si ese mismo tiene valor, si no es null
                                                                      entramos */

                            tipoCli = TipoCliente.valueOf(n.get("tipoCliente").asText());/*La
                                                                                                   línea toma el
                                                                                   valor textual "tipoCliente" del
                                                                                 JSON y lo convierte en uno de
                                                                                 los valores del enum TipoCliente
                                                                                  (CLIENTE, EMPRESA o MESA)*/

                        } else if (n.hasNonNull("cliente")/*si n tiene el campo cliente y ese no es null */
                                && n.get("cliente").isObject()/*y a su vez cliente es un objeto json */
                                && n.get("cliente").hasNonNull("tipoCliente")) {/*y a su vez
                                                                                                cliente tiene un
                                                                                              campo tipocliente y
                                                                                         tiene valor por lo tanto
                                                                                         no es null*/
                            tipoCli = TipoCliente.valueOf(n.get("cliente").get("tipoCliente").asText());/*Convierte el texto que viene en
                                                                                                                                el JSON dentro de cliente.tipoCliente
                                                                                                                                en uno de los valores del enum TipoCliente
                                                                                                                                (CLIENTE, EMPRESA o MESA) y lo guarda en
                                                                                                                                la variable tipoCli */

                        } else if (nombre.toLowerCase().startsWith("mesa ")) {
                            tipoCli = TipoCliente.MESA; // fallback si solo vino nombre tipo "MESA X"
                        }

                        out.add(new VentaFilaDto(nombre, desc, monto, estado, idCliente, tipoCli));
                    }
                }
                return out;
            }

        } catch (java.io.IOException | InterruptedException e) {
            System.err.println("Error recargar ventas: " + e.getMessage());
        }

        return List.of();
    }
    // =====================
// LISTAR CLIENTES POR TIPO (EMPRESA/CLIENTE/MESA)
// =====================
// NUEVO

    public java.util.List<String> obtenerClientesPorTipo(TipoCliente tipoBuscado) {
        try {
            var solicitud = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/clientes"))
                    .GET()
                    .build();

            var response = http.send(solicitud, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                var json = TraductorJSON.readTree(response.body());
                var out = new java.util.ArrayList<String>();
                if (json.isArray()) {
                    for (var n : json) {
                        String nombre = n.hasNonNull("nombre") ? n.get("nombre").asText() : null;
                        String tipo = n.hasNonNull("tipoCliente") ? n.get("tipoCliente").asText() : null;
                        if (nombre != null && !nombre.isBlank() && tipo != null) {
                            if (tipo.equalsIgnoreCase(tipoBuscado.name())) {
                                out.add(nombre.trim());
                            }
                        }
                    }
                }
                return out.stream()
                        .distinct()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .collect(java.util.stream.Collectors.toList());
            }
        } catch (java.io.IOException | InterruptedException e) {
            System.err.println("obtenerClientesPorTipo: " + e.getMessage());
        }
        return java.util.List.of();
    }
}
