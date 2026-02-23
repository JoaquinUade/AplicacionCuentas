package paucar.service; /*define el contenedor que agrupa clases, interfaces y subpaquetes relacionados */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.tpo.demo.entity.TipoCliente;

public class VentasBackend {

    private final String BASE_URL;/*Variableque contiene la URL base del backend (por ejemplo "http://localhost:8080/api"). */

    private final HttpClient http;/*protocolo de transferencia de hipertexto (Hypertext Transfer Protocol)
                                  para enviar y recibir datos Se utiliza principalmente a través de la API
                                  HttpClient para realizar peticiones GET, POST, PUT y DELETE*/

    private final ObjectMapper TraductorJSON; /*ObjectMapper es una clase de la librería Jackson que sirve para convertir
                                  datos JSON en objetos Java y viceversa*/

    // --- DTOs internos ---
    public record ProductoItem(Long id, String nombre) {/*Un record (Java 16+) es una forma corta de declarar
                                                       una clase pensada para transportar datos (lo que antes
                                                       llamábamos “DTO”) 
                                                       la idea de un record es evitar escribir todo el codigo
                                                       repetitivo del constructor, getters, equals/hashCode, 
                                                       toString.*/
    }

    public enum TipoDePago {/*enum de tipo de pago, reemplazar en el futuro por el tipo de pago del backend */
        TRANSFERENCIA, DEBE, EFECTIVO, MERCADO_PAGO, DEBITO, CREDITO
    }

    public record VentaFilaDto(
            String nombre,
            String descripcion,
            BigDecimal monto,
            TipoDePago estado
            ) {

    }

    // --- Constructor ---
    public VentasBackend(String BASE_URL) {/*Recibe un parámetro llamado BASE_URL (un String) que debería ser
                                            la URL base del backend */

        this.BASE_URL = Objects.requireNonNull(BASE_URL);/*si el parámetro es null, lanza un NullPointerException
                                                          (excepción en tiempo de ejecución (RuntimeException)
                                                          que ocurre cuando el programa intenta utilizar una
                                                          referencia de objeto que apunta a null)) inmediatamente.
                                                          Esto evita crear una instancia mal configurada */

        this.http = HttpClient.newHttpClient();/*Crea una instancia por defecto de HttpClient (Java 11+), que
                                               vas a usar para hacer GET/POST al backend */

        this.TraductorJSON = new ObjectMapper();/*El campo traductorJson ahora va a contener un ObjectMapper nuevo */
    }

    // ============================================================
    //                    CLIENTES
    // ============================================================
    public List<String> obtenerTodosLosClientesMenosMesas() {/*Método que devuelve una lista de nombres que NO
                                                             sean mesas */

        try {/*es try porqeu si la operacion falla tirara error osea ira a catch */

            var solicitud = HttpRequest.newBuilder()/*crea una solicitud http osea hace una solicitud al servidor(API)*/

                    .uri(URI.create(BASE_URL + "/clientes"))/*/ Le pongo la URL de destino (BASE_URL + "/clientes") */
                    .GET()/*Indico que el objetivo de la variable solicitud es hacer un GET (pedir/leer datos)*/
                    .build();/*Termino de construir la solicitud (queda lista, pero todavía sin enviar) */

            var response = http.send(solicitud, HttpResponse.BodyHandlers.ofString());/*Enviar la solicitud al
                                                                                      servidor y guardar la
                                                                                      respuesta como texto */

            if (response.statusCode() >= 200 && response.statusCode() < 300) {/*Si el código es entre 200 y 299,
                                                                              entonces todo salió bien */

                var json = TraductorJSON.readTree(response.body());/*Toma el texto que vino del servidor
                                                                   (normalmente JSON) y lo convierte en un objeto
                                                                   JSON que podés leer por campos. */
                var out = new ArrayList<String>();

                if (json.isArray()) {/*Verifica que 'json' sea un vector */

                    for (var n : json) {/*Recorre cada elemento del vector */
                        var nombre = n.hasNonNull("nombre") ? n.get("nombre").asText() : null;/*Si el objeto tiene la clave 'nombre' y no es null 
                                                                                                                    entonces obtiene su valor como String sino 'nombre'
                                                                                                                    queda null*/

                        var tipo = n.hasNonNull("tipoCliente") ? n.get("tipoCliente").asText() : null;/*si el objeto tiene la clave 'tipoCliente'
                                                                                                                           y no es null lo lee como string, sino 'tipo'
                                                                                                                           queda null */
                        if (nombre != null && !nombre.isBlank()) {/*Filtra: 'nombre' debe existir y NO estar vacío/espacios */
                            if (tipo == null || !tipo.equalsIgnoreCase("MESA")) {/*Si 'tipo' es null O distinto de "MESA" */
                                out.add(nombre.trim());/*entonces agrega el 'nombre' (sin espacios extremos) a la lista */
                            }
                        }
                    }
                }
                return out.stream()
                        .distinct()/*borra duplicados */
                        .sorted(String.CASE_INSENSITIVE_ORDER)/*ordena alfabeticamente */
                        .collect(Collectors.toList());/*Retorna una lista ordenada alfabéticamente sin nombres duplicados */
            }
        } catch (Exception e) {
            System.err.println("Error clientes: " + e.getMessage());
        }

        return List.of();/*Si no pude obtener clientes válidos, te devuelvo una lista vacía para evitar null
                          y que el código llamador no falle, ademas no te muestra la lsita de clientes ya
                          ingresados no me queda claro por que*/
    }

    public void crearClienteSiNoExiste(String nombre, TipoCliente tipoCli) {
        if (nombre == null || nombre.isBlank()) {/*si el nombre no es valido, o es null o solo son espacios
                                                 vacios se salga del metodo*/
            return;
        }
        if (tipoCli == TipoCliente.MESA) {/*si el tipo de cliente es mesa salga del metodo ya que no nos interesa
                                       recordar el historial de compra de la gente de las mesas */
            return;
        }

        try {
            var payload = TraductorJSON.createObjectNode()/*Pensalo como: “arranco un JSON {} para llenarlo con nombre y tipCliente */
            
                    .put("nombre", nombre.trim())/* Agrega al ObjectNode el campo "nombre" y el valor
                                                           que le pases, toma el string nombre y le saca los
                                                           espacios del principio y del final*/
                    .put("tipoCliente", tipoCli.name());/*Agregá al JSON un campo que se llama
                                                                  tipoCliente y poné un valor ahi, ya sea
                                                                  empresa, cliente o mesa*/

            var solicitud = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/clientes"))/* Le pongo la URL de destino (BASE_URL + "/clientes") */

                    .header("Content-Type", "application/json")/* evitá dobles barras accidentales.
                                                                           Si BASE_URL termina con /, no pongas
                                                                           otra / en el path  y aclaro que el
                                                                           contenido está escrito en JSON*/
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))/*Indico que el objetivo de la variable req es hacer un POST (enviar datos) y le paso el objeto lo convierto en JSON*/
                    .build();

            var response = http.send(solicitud, HttpResponse.BodyHandlers.ofString());/*envío la solicitud HTTP al servidor y guardo la respuesta completa en response como texto */

            if (!(response.statusCode() == 200 || response.statusCode() == 201
                    || response.statusCode() == 400 || response.statusCode() == 409)) {/*Si NO es 200, NI 201, NI 400, NI 409 entonces tira error */
                System.err.println("Error al crear cliente: HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("crearClienteSiNoExiste: " + e.getMessage());
        }
    }

    public Long obtenerClienteIdPorNombre(String nombre) {
        try {
            if (nombre == null || nombre.isBlank()) {/*Si el nombre es nulo o está vacío salir del método retornando null */
                return null;
            }

            String url = BASE_URL + "/clientes?nombre="/*añade al final de la url que ya teniamos "/clientes?nombre="" */
                    + URLEncoder.encode(nombre, StandardCharsets.UTF_8);/*codifica la url para que sea valida ya que no acepta ñ o tildes */

            var solicitud = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();/*Arma una solicitud HTTP de tipo GET hacia la URL que construiste, lista para ser enviada */
            var response = http.send(solicitud, HttpResponse.BodyHandlers.ofString());/*Envía la solicitud HTTP solicitud al servidor y recibe la respuesta completa en response, leyendo el cuerpo como texto (String) */

            if (response.statusCode() >= 200 && response.statusCode() < 300) {/*si el codigo http  esta entre 200 y 300*/
                var json = TraductorJSON.readTree(response.body());/*Convierte el texto que vino del servidor en
                                                                   la respuesta (response.body()) a un objeto
                                                                   JSON (JsonNode) usando Jackson */
                JsonNode name = null;/*Está declarando una variable llamada name de tipo JsonNode, y le está
                                  asignando el valor null porque todavía no sabe qué JSON va a guardar ahí */

                if (json.isArray() && json.size() > 0) {/*Si el JSON que vino del servidor es un vector y tiene
                                                        al menos un elemento, entonces guardá el primer elemento
                                                        del vector en n */
                    name = json.get(0);
                }else if (json.isObject()) {/*Si el JSON que vino del servidor es un objeto (no un vector),
                                            entonces guardá ese objeto directamente en name */
                    name = json;
                }/*esto es para que si buscas un nombre y hay varios clientes con nombres similares te salgan
                  todas las opciones validas en el buscador*/

                if (name != null) {/*si el nombre no es nulo */
                    if (name.hasNonNull("idCliente")) {/*revisa que tenga id y que este no sea nulo */
                        return name.get("idCliente").asLong();/*retorna el id */
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("obtenerClienteIdPorNombre: " + e.getMessage());
        }
        return null;
    }

    // ============================================================
    //                    PRODUCTOS
    // ============================================================
    public List<ProductoItem> cargarProductos() {/*este metodo hace que aparezcan los productos para añadir,
                                                 si quitas el if desaparecen todos los productos */
        try {
            var solicitud = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/productos"))
                    .GET()/*prepara solicitud get */
                    .build();

            var response = http.send(solicitud, HttpResponse.BodyHandlers.ofString());/*envia la solicitud y
                                                                                      guarda la response */

            if (response.statusCode() >= 200 && response.statusCode() < 300) {/*si el codigo de la response esta
                                                                              entre 200 y 299*/

                var array = TraductorJSON.readTree(response.body());/*/ convierte el texto JSON en un objeto
                                                                      JsonNode puede contener 1 o muchos productos*/
                var out = new ArrayList<ProductoItem>();/*lista Java donde guardaremos los productos */

                if (array.isArray()) {/*verifica que la variable array es un vector/lista */
                    for (var n : array) {/*recorre con n el vector array de productos */
                        Long id
                                = n.hasNonNull("idProducto") ? n.get("idProducto").asLong()/*/ si existe "idProducto" y no es null, usa ese valor sino deja id como null */
                                : null;

                        String nombre = n.hasNonNull("nombre") ? n.get("nombre").asText() /*Si el JSON n tiene la clave "nombre" y no es null, entonces guardá su valor
                                                                                                                como texto en la variable nombre; si no, poné null */
                        : null;

                        if (id != null && nombre != null && !nombre.isBlank()) {/*si tiene id y nombre valido,
                                                                                se crea un ProductoItem sin
                                                                                antes quitarle los espacios
                                                                                del principio y final
                                                                                con trim()*/
                            out.add(new ProductoItem(id, nombre.trim()));
                        }
                    }
                }
                return out.stream() /*Tomá la lista out (que contiene muchos ProductoItem) y convertila en un
                                     stream para poder aplicarle operaciones como ordenar, filtrar, mapear, etc */

                        .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.nombre(), b.nombre()))/*Ordená los elementos del stream usando un comparador alfabetico ignorando mayúsculas/minúsculas */
                        .collect(Collectors.toList());/*Materializá ese stream ordenado en una lista nueva de
                                                      Java (un List<ProductoItem>), osea devuelve la lista de
                                                      productos al que hay que agregar a los pedidos, ese es el
                                                      objetivo del metodo */
            }

        } catch (Exception e) {
            System.err.println("Error productos: " + e.getMessage());
        }

        return List.of();/*retorna la lista de productos vacia */
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

        } catch (Exception e) {
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

        try {
            if (idProductos.isEmpty() || cantidades.isEmpty()) {/*condicional que evita que se haga un pedido
                                                                 sin productos o cantidades validas */
                System.err.println("Pedido (MESA) inválido por carecer de cantidad o de productos validos");
                return false;
            }

            var FichaPedido = TraductorJSON.createObjectNode()/*creamos un objeto json vacio */
                    .put("nombreMesa", nombreMesa == null ? "" : nombreMesa.trim())/*añadimos el
                                                                                             campo nombremesa y
                                                                                             verifica que el
                                                                                          nombremesa no sea null,
                                                                                        y si es valido, lo guarda
                                                                                        sin antes quitar el
                                                                                        espacio de enfrente y del
                                                                                        final con trim*/
                    .put("estado", estado == null ? "DEBE" : estado.name())/*añadimos el campo estado,
                                                                                     el cual si es valido se
                                                                                     guarda qutiandole el espacio
                                                                                    de el principio y final y si
                                                                                    es null queda DEBE*/
                    .put("observaciones", observaciones == null ? "" : observaciones);

            var ListaIds = FichaPedido.putArray("idProductos");/*añadimos un campo a rellenar
                                                                           llamado idproductos qeu se rellenara
                                                                           con data del backend ya que los
                                                                           productos elegidos poseen id*/
            idProductos.forEach(ListaIds::add);

            var ListaCantidades = FichaPedido.putArray("cantidades");/*añadimos un campo a rellanr
                                                                                  llamado cantidades qeu se
                                                                                  rellenara en un vector ordenado
                                                                                 para que tenga la misma posicion
                                                                                 la cantidad de el producto
                                                                                 repetido */
            cantidades.forEach(ListaCantidades::add);

            var solicitud = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/ventas"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(FichaPedido.toString()))
                    .build();

            var response = http.send(solicitud, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;/*retorna el codigo solo si es
                                                                                mayor o igual a 200 y menor que
                                                                                300 */

        } catch (Exception e) {
            System.err.println("guardarVentaMesa: " + e.getMessage());
            return false;/*sino da error y retorna false */
        }
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

                        out.add(new VentaFilaDto(nombre, desc, monto, estado));
                    }
                }
                return out;
            }

        } catch (Exception e) {
            System.err.println("Error recargar ventas: " + e.getMessage());
        }

        return List.of();
    }
}
