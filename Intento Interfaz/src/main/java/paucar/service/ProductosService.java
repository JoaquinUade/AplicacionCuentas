package paucar.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ProductosService {
    
private final String BASE_URL;
    private final HttpClient http;
    private final ObjectMapper TraductorJSON;

    // --- DTOs internos ---
    public record ProductoItem(Long id, String nombre) {/*Un record (Java 16+) es una forma corta de declarar
                                                       una clase pensada para transportar datos (lo que antes
                                                       llamábamos “DTO”) 
                                                       la idea de un record es evitar escribir todo el codigo
                                                       repetitivo del constructor, getters, equals/hashCode, 
                                                       toString.*/
    }
 public ProductosService(String BASE_URL) {
        this.BASE_URL = Objects.requireNonNull(BASE_URL);
        this.http = HttpClient.newHttpClient();
        this.TraductorJSON = new ObjectMapper();
    }
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

        } catch (java.io.IOException | InterruptedException e) {
            System.err.println("Error productos: " + e.getMessage());
        }

        return List.of();/*retorna la lista de productos vacia */
    }
}
