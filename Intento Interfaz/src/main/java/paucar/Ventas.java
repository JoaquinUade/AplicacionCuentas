package paucar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.tpo.demo.entity.TipoCliente;

public class Ventas extends BorderPane {

    // ===== Enum local (frontend) =====
    // Cuando tengas el backend en el classpath, podés borrar este enum
    // y volver a importar com.uade.tpo.demo.entity.TipoDePago
    public enum TipoDePago {
        TRANSFERENCIA, DEBE, EFECTIVO, MERCADO_PAGO, DEBITO, CREDITO
    }

    private static final Locale LOCALE_AR = Locale.of("es", "AR"); // si tu JDK < 19, usa: new Locale("es","AR")
    private final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(LOCALE_AR);

    // ===== Modelo de una fila =====
    public static class Fila {

        private final StringProperty nombre = new SimpleStringProperty("");
        private final StringProperty descripcion = new SimpleStringProperty("");
        private final ObjectProperty<BigDecimal> monto = new SimpleObjectProperty<>(BigDecimal.ZERO);
        private final ObjectProperty<TipoDePago> estado = new SimpleObjectProperty<>(TipoDePago.EFECTIVO);

        public String getNombre() {
            return nombre.get();
        }

        public void setNombre(String v) {
            nombre.set(v);
        }

        public StringProperty nombreProperty() {
            return nombre;
        }

        public String getDescripcion() {
            return descripcion.get();
        }

        public void setDescripcion(String v) {
            descripcion.set(v);
        }

        public StringProperty descripcionProperty() {
            return descripcion;
        }

        public BigDecimal getMonto() {
            return monto.get();
        }

        public void setMonto(BigDecimal v) {
            monto.set(v);
        }

        public ObjectProperty<BigDecimal> montoProperty() {
            return monto;
        }

        public TipoDePago getEstado() {
            return estado.get();
        }

        public void setEstado(TipoDePago v) {
            estado.set(v);
        }

        public ObjectProperty<TipoDePago> estadoProperty() {
            return estado;
        }
    }

    // ===== Estado de la vista =====
    private final ObservableList<Fila> filas = FXCollections.observableArrayList();
    private final ObjectProperty<BigDecimal> total = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // ===== Componentes =====
    private final TableView<Fila> tabla = new TableView<>(filas);
    private final Button btnAgregar = new Button("+ Agregar");
    private final Button btnQuitar = new Button("Quitar seleccionado");
    // ===== Clientes (sugerencias) + HTTP =====
    private final ObservableList<String> clientes = FXCollections.observableArrayList();
    private final FilteredList<String> clientesFiltrados = new FilteredList<>(clientes, s -> true);

    // ===== Productos (sugerencias) + HTTP =====
    private static class Producto {

        final Long id;
        final String nombre;

        Producto(Long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        } // Para mostrar en ComboBox
    }
    private final ObservableList<Producto> productos = FXCollections.observableArrayList();

// Endpoint base (ajustá host/puerto si corresponde)
    private static final String BASE_URL = "http://localhost:4002/api";
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    public Ventas() {
        setPadding(new Insets(16));
        setTop(crearHeader());
        setCenter(crearTabla());
        setBottom(crearFooter());
        cargarClientesDesdeBackend();
        cargarProductosDesdeBackend(); // NUEVO: llenar ComboBox de productos
        recargarDelBackend(); // ← NUEVO: carga las ventas del día al abrir la vista

        // Total se recalcula ante cambios de filas o montos (cuando se edita)
        filas.addListener((javafx.collections.ListChangeListener<Fila>) c -> recomputeTotal());
    }

    // ===== Encabezado con fecha y botón + =====
    private Node crearHeader() {
        var hoy = LocalDate.now();
        var dow = hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_AR).toUpperCase();
        var lblTitulo = new Label(dow + " " + hoy.getDayOfMonth() + "/" + hoy.getMonthValue() + "/" + hoy.getYear());
        lblTitulo.getStyleClass().add("title-xl");

        btnAgregar.getStyleClass().add("btn-success");
        btnAgregar.setOnAction(e -> abrirDialogoAgregar());

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var barra = new HBox(12, lblTitulo, spacer, btnAgregar);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(0, 0, 10, 0));
        return barra;
    }

    // ===== Tabla principal =====
    private Node crearTabla() {
        tabla.setEditable(true);

        // Columna: Nombre (editable)
        var colNombre = new TableColumn<Fila, String>("Nombre");
        colNombre.setCellValueFactory(c -> c.getValue().nombreProperty());
        colNombre.setCellFactory(TextFieldTableCell.forTableColumn());
        colNombre.setOnEditCommit(e -> e.getRowValue().setNombre(e.getNewValue()));
        colNombre.setPrefWidth(200);

        // Columna: Descripción (editable)
        var colDesc = new TableColumn<Fila, String>("Descripción");
        colDesc.setCellValueFactory(c -> c.getValue().descripcionProperty());
        colDesc.setCellFactory(TextFieldTableCell.forTableColumn());
        colDesc.setOnEditCommit(e -> e.getRowValue().setDescripcion(e.getNewValue()));
        colDesc.setPrefWidth(420);
        colDesc.setEditable(false);

        // Columna: Monto (editable con formateo AR)
        var colMonto = new TableColumn<Fila, String>("Monto");
        colMonto.setCellValueFactory(c -> Bindings.createStringBinding(
                () -> formatear(c.getValue().getMonto()), c.getValue().montoProperty()));
        colMonto.setCellFactory(TextFieldTableCell.forTableColumn());
        colMonto.setOnEditCommit(e -> {
            var v = parseMoneda(e.getNewValue());
            e.getRowValue().setMonto(v);
            recomputeTotal();
        });
        colMonto.setPrefWidth(140);
        colMonto.setEditable(false);

        // Columna: Estado (ComboBox por fila, usando enum)
        var colEstado = new TableColumn<Fila, TipoDePago>("Estado");
        colEstado.setCellValueFactory(c -> c.getValue().estadoProperty());
        colEstado.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<TipoDePago> combo = new ComboBox<>();

            {
                combo.getItems().setAll(TipoDePago.values());
                combo.valueProperty().addListener((o, a, b) -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        getTableView().getItems().get(getIndex()).setEstado(b);
                    }
                });
            }

            @Override
            protected void updateItem(TipoDePago item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    combo.setValue(item);
                    setGraphic(combo);
                }
            }
        });
        colEstado.setPrefWidth(180);

        // Botón quitar
        btnQuitar.getStyleClass().add("btn-danger");
        btnQuitar.disableProperty().bind(Bindings.isNull(tabla.getSelectionModel().selectedItemProperty()));
        btnQuitar.setOnAction(e -> {
            var sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                // Por ahora quita solo de la vista local.
                // TODO: implementar DELETE /api/ventas/{id} cuando sumemos idVenta a Fila.
                filas.remove(sel);
            }
            recomputeTotal();
        });

        tabla.getColumns().setAll(java.util.List.of(colNombre, colDesc, colMonto, colEstado));

        var cont = new VBox(8, tabla, btnQuitar);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        return cont;
    }

    // ===== Footer con total =====
    private Node crearFooter() {
        var lblTitulo = new Label("Total:");
        lblTitulo.getStyleClass().add("total-title");

        var lblTotal = new Label();
        lblTotal.getStyleClass().add("total-amount");
        lblTotal.textProperty().bind(Bindings.createStringBinding(
                () -> formatear(total.get()), total));

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var box = new HBox(10, spacer, lblTitulo, lblTotal);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(10, 0, 0, 0));
        return box;
    }
    // Contenedor para devolver datos del diálogo (para armar VentaRequest)

    private static class PedidoNuevo {

        String nombreCliente;
        java.util.List<Long> idProductos = new java.util.ArrayList<>();
        java.util.List<Integer> cantidades = new java.util.ArrayList<>();
        TipoDePago estado;
        String observaciones;
    }

    private void abrirDialogoAgregar() {
        Dialog<PedidoNuevo> dialog = new Dialog<>();
        dialog.setTitle("Agregar pedido");

        ButtonType okType = new ButtonType("Agregar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        // === Cliente con autocompletar ===
        ComboBox<String> cbCliente = new ComboBox<>(clientesFiltrados);
        cbCliente.setEditable(true);
        cbCliente.setPromptText("Nombre (cliente/mesa)");
        cbCliente.getEditor().textProperty().addListener((obs, old, val) -> {
            String txt = (val == null ? "" : val.trim().toLowerCase());
            clientesFiltrados.setPredicate(s -> s == null || txt.isEmpty() || s.toLowerCase().contains(txt));
            if (!cbCliente.isShowing() && !txt.isEmpty()) {
                cbCliente.show();
            }
        });

        // === Sección de líneas (Producto + Cantidad) ===
        VBox contLineas = new VBox(6);
        contLineas.setPadding(new Insets(6));

        Runnable agregarLinea = () -> {
            ComboBox<Producto> cbProd = new ComboBox<>(productos);
            cbProd.setPrefWidth(280);
            cbProd.setPromptText("Producto");

            TextField tfCant = new TextField();
            tfCant.setPromptText("Cant.");
            tfCant.setPrefWidth(70);
            tfCant.textProperty().addListener((o, a, b) -> {
                if (b != null && !b.matches("\\d*")) {
                    tfCant.setText(b.replaceAll("[^\\d]", ""));
                }
            });

            Button btnDel = new Button("✕");
            btnDel.getStyleClass().add("btn-danger");

            HBox fila = new HBox(6, cbProd, tfCant, btnDel);
            fila.setAlignment(Pos.CENTER_LEFT);
            btnDel.setOnAction(e -> contLineas.getChildren().remove(fila));

            contLineas.getChildren().add(fila);
        };

        Button btnAgregarLinea = new Button("+ Producto");
        btnAgregarLinea.getStyleClass().add("btn-primary");
        btnAgregarLinea.setOnAction(e -> agregarLinea.run());
        agregarLinea.run(); // al menos una línea

        // === Estado y observaciones ===
        ComboBox<TipoDePago> cbEstado = new ComboBox<>();
        cbEstado.getItems().setAll(TipoDePago.values());
        cbEstado.setValue(TipoDePago.TRANSFERENCIA); // por defecto

        TextField tfObs = new TextField();
        tfObs.setPromptText("Observaciones (opcional)");

        // === Layout ===
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        int r = 0;

        grid.add(new Label("Nombre:"), 0, r);
        grid.add(cbCliente, 1, r++);

        grid.add(new Label("Productos:"), 0, r);
        VBox productosBox = new VBox(6, contLineas, btnAgregarLinea);
        grid.add(productosBox, 1, r++);

        grid.add(new Label("Estado:"), 0, r);
        grid.add(cbEstado, 1, r++);

        grid.add(new Label("Observaciones:"), 0, r);
        grid.add(tfObs, 1, r++);

        // Validación: nombre no vacío + al menos 1 línea válida
        Node okBtn = dialog.getDialogPane().lookupButton(okType);
        okBtn.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            String nombre = cbCliente.getEditor().getText();
            if ((nombre == null || nombre.isBlank()) && cbCliente.getValue() == null) {
                return true;
            }

            for (var n : contLineas.getChildren()) {
                if (n instanceof HBox fila) {
                    @SuppressWarnings("unchecked")
                    ComboBox<Producto> cb = (ComboBox<Producto>) fila.getChildren().get(0);
                    TextField tf = (TextField) fila.getChildren().get(1);
                    if (cb.getValue() != null && tf.getText() != null && !tf.getText().isBlank()) {
                        try {
                            int c = Integer.parseInt(tf.getText());
                            if (c >= 1) {
                                return false;
                            }
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }
            }
            return true;
        }, cbCliente.getEditor().textProperty(), contLineas.getChildren()));

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == okType) {
                PedidoNuevo p = new PedidoNuevo();
                String nombre = cbCliente.getEditor().getText();
                if (nombre == null || nombre.isBlank()) {
                    nombre = cbCliente.getValue();
                }
                p.nombreCliente = (nombre == null ? "" : nombre.trim());
                p.estado = cbEstado.getValue();
                p.observaciones = tfObs.getText() == null ? "" : tfObs.getText().trim();

                // recorrer líneas y llenar idProductos + cantidades
                for (var n : contLineas.getChildren()) {
                    if (n instanceof HBox fila) {
                        @SuppressWarnings("unchecked")
                        ComboBox<Producto> cb = (ComboBox<Producto>) fila.getChildren().get(0);
                        TextField tf = (TextField) fila.getChildren().get(1);
                        var prod = cb.getValue();
                        if (prod != null && tf.getText() != null && !tf.getText().isBlank()) {
                            try {
                                int c = Integer.parseInt(tf.getText());
                                if (c >= 1) {
                                    p.idProductos.add(prod.id);
                                    p.cantidades.add(c);
                                }
                            } catch (NumberFormatException ignore) {
                            }
                        }
                    }
                }
                return p;
            }
            return null;
        });

        var res = dialog.showAndWait();
        res.ifPresent(p -> {
            // 0) Determinar tipo por nombre
            TipoCliente tipo = deducirTipoCliente(p.nombreCliente);

            if (tipo == TipoCliente.MESA) {
                // 1) MESA: no crear ni buscar cliente; guardar con nombreMesa
                boolean ok = guardarVentaEnBackend_VentaRequestMesa(
                        p.nombreCliente, p.idProductos, p.cantidades, p.estado, p.observaciones
                );

                // 2) Sugerencias locales: no agregamos MESA a la lista de clientes
                if (ok) {
                    recargarDelBackend();
                }
                return;
            }

            // 3) CLIENTE o EMPRESA: asegurar cliente con tipo correcto
            crearClienteSiNoExisteConTipo(p.nombreCliente, tipo);

            // 4) Buscar id_cliente por nombre
            Long idCliente = obtenerClienteIdPorNombre(p.nombreCliente);
            if (idCliente == null) {
                var dlg = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                dlg.setTitle("Cliente no encontrado");
                dlg.setHeaderText("No se pudo obtener el ID del cliente");
                dlg.setContentText("Verificá el nombre del cliente o volvé a intentar.");
                dlg.showAndWait();
                return; // no sigue al POST
            }

            // 5) Guardar venta con idCliente
            boolean ok = guardarVentaEnBackend_VentaRequest(
                    idCliente, p.idProductos, p.cantidades, p.estado, p.observaciones
            );

            // 6) Mantener sugerencias locales (solo si no es MESA)
            if (!clientes.contains(p.nombreCliente)) {
                clientes.add(p.nombreCliente);
                FXCollections.sort(clientes, String.CASE_INSENSITIVE_ORDER);
            }

            // 7) Recargar tabla si todo salió bien
            if (ok) {
                recargarDelBackend();
            }
        });
    }

    // ===== Utilitarios =====
    private void recomputeTotal() {
        BigDecimal t = filas.stream()
                .map(Fila::getMonto)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        total.set(t.setScale(2, RoundingMode.HALF_UP));
    }

    private String formatear(BigDecimal v) {
        if (v == null) {
            return "$ 0,00";
        }
        return MONEDA.format(v);
    }

    private BigDecimal parseMoneda(String s) {
        try {
            // Acepta "121000", "121.000,00" o "$ 121.000,00"
            String limpio = s.replace("$", "").replace(" ", "").replace(".", "").replace(",", ".");
            return new BigDecimal(limpio).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }
    // ===== Deducción de tipo de cliente por nombre (heurística simple) =====

    private TipoCliente deducirTipoCliente(String nombre) {
        if (nombre == null) {
            return TipoCliente.CLIENTE;
        }
        String n = nombre.trim().toLowerCase();

        // Regla: si empieza con "mesa " lo tratamos como MESA
        if (n.startsWith("mesa ")) {
            return TipoCliente.MESA;
        }

        // Regla: algunos indicadores típicos de razón social
        if (n.contains(" srl") || n.endsWith(" srl") || n.contains(" s.a") || n.contains(" sa")
                || n.contains("empresa") || n.contains("estudio") || n.contains("industria")) {
            return TipoCliente.EMPRESA;
        }

        return TipoCliente.CLIENTE;
    }
    // === HTTP helpers ===

// Descarga todos los clientes y carga sus nombres en 'clientes'
    private void cargarClientesDesdeBackend() {
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/clientes"))
                    .GET()
                    .build();

            var res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                // Se espera una lista JSON de objetos Cliente (con al menos 'nombre')
                var lista = om.readTree(res.body());
                var nombres = new java.util.ArrayList<String>();
                if (lista.isArray()) {
                    for (var n : lista) {
                        var nombre = n.hasNonNull("nombre") ? n.get("nombre").asText() : null;
                        String tipo = n.hasNonNull("tipoCliente") ? n.get("tipoCliente").asText() : null;
                        if (nombre != null && !nombre.isBlank()) {
                            // No agregamos MESA a las sugerencias de clientes
                            if (tipo == null || !tipo.equalsIgnoreCase("MESA")) {
                                nombres.add(nombre.trim());
                            }
                        }
                    }
                }
                clientes.setAll(nombres.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .collect(Collectors.toList()));
            } else {
                System.err.println("GET /clientes -> HTTP " + res.statusCode());
            }

        } catch (java.io.IOException | java.lang.InterruptedException ex) {
            System.err.println("No se pudo cargar clientes: " + ex.getMessage());
            Thread.currentThread().interrupt(); // si capturás InterruptedException
        }
    }
    // Descarga todos los productos y carga (id, nombre) en 'productos'

    private void cargarProductosDesdeBackend() {
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/productos")) // <— ajustá si tu ruta difiere
                    .GET()
                    .build();

            var res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                var lista = om.readTree(res.body());
                var items = new java.util.ArrayList<Producto>();
                if (lista.isArray()) {
                    for (var n : lista) {
                        Long id = null;
                        if (n.hasNonNull("idProducto")) {
                            id = n.get("idProducto").asLong();
                        } else if (n.hasNonNull("id")) {
                            id = n.get("id").asLong();
                        } else if (n.hasNonNull("id_producto")) {
                            id = n.get("id_producto").asLong();
                        }

                        String nombre = n.hasNonNull("nombre") ? n.get("nombre").asText() : null;

                        if (id != null && nombre != null && !nombre.isBlank()) {
                            items.add(new Producto(id, nombre.trim()));
                        }
                    }
                }
                productos.setAll(items.stream()
                        .filter(Objects::nonNull)
                        .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.nombre, b.nombre))
                        .collect(Collectors.toList()));
            } else {
                System.err.println("GET /productos -> HTTP " + res.statusCode());
            }
        } catch (Exception ex) {
            System.err.println("No se pudo cargar productos: " + ex.getMessage());
        }
    }
    // Asegura un cliente (EMPRESA o CLIENTE) en el backend según el tipo deducido.
// NOTA: nunca crear "MESA" como cliente.

    private void crearClienteSiNoExisteConTipo(String nombre, TipoCliente tipo) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        if (tipo == TipoCliente.MESA) {
            return; // no persistimos mesas como clientes
        }
        try {
            var payload = om.createObjectNode()
                    .put("nombre", nombre.trim())
                    .put("tipoCliente", tipo.name()); // EMPRESA o CLIENTE

            var req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/clientes"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                    .build();

            var res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            // 201/200 → creado/ok; 409/400 (duplicado/validación) → ignorar para este flujo
            if (!(res.statusCode() == 201 || res.statusCode() == 200
                    || res.statusCode() == 409 || res.statusCode() == 400)) {
                System.err.println("POST /clientes -> HTTP " + res.statusCode() + " body=" + res.body());
            }
        } catch (Exception ex) {
            System.err.println("No se pudo crear/asegurar cliente: " + ex.getMessage());
        }
    }
    // --- NUEVO: buscar id_cliente por nombre ---

    private Long obtenerClienteIdPorNombre(String nombre) {
        try {
            if (nombre == null || nombre.isBlank()) {
                return null;
            }
            // Ajustá el endpoint a tu backend real:
            String url = BASE_URL + "/clientes?nombre=" + java.net.URLEncoder.encode(nombre, java.nio.charset.StandardCharsets.UTF_8);
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                var json = om.readTree(res.body());
                // Acepta lista u objeto. Busca id bajo distintas claves comunes.
                if (json.isArray() && json.size() > 0) {
                    var first = json.get(0);
                    if (first.hasNonNull("idCliente")) {
                        return first.get("idCliente").asLong();
                    }
                    if (first.hasNonNull("id")) {
                        return first.get("id").asLong();
                    }
                    if (first.hasNonNull("id_cliente")) {
                        return first.get("id_cliente").asLong();
                    }
                } else if (json.isObject()) {
                    if (json.hasNonNull("idCliente")) {
                        return json.get("idCliente").asLong();
                    }
                    if (json.hasNonNull("id")) {
                        return json.get("id").asLong();
                    }
                    if (json.hasNonNull("id_cliente")) {
                        return json.get("id_cliente").asLong();
                    }
                }
            } else {
                System.err.println("GET /clientes?nombre= -> HTTP " + res.statusCode());
            }
        } catch (Exception ex) {
            System.err.println("No se pudo obtener id_cliente: " + ex.getMessage());
        }
        return null;
    }
    // Envía VentaRequest (idCliente, idProductos, cantidades, estado, observaciones)

    private boolean guardarVentaEnBackend_VentaRequest(Long idCliente,
            java.util.List<Long> idProductos,
            java.util.List<Integer> cantidades,
            TipoDePago estado,
            String observaciones) {
        try {
            if (idCliente == null || idProductos == null || idProductos.isEmpty()
                    || cantidades == null || cantidades.isEmpty()) {
                System.err.println("VentaRequest inválido: datos faltantes");
                return false;
            }
            var payload = om.createObjectNode()
                    .put("idCliente", idCliente)
                    .put("estado", (estado == null ? "EFECTIVO" : estado.name()))
                    .put("observaciones", (observaciones == null ? "" : observaciones));

            var arrIds = payload.putArray("idProductos");
            for (Long id : idProductos) {
                arrIds.add(id);
            }

            var arrCant = payload.putArray("cantidades");
            for (Integer c : cantidades) {
                arrCant.add(c);
            }

            var req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/ventas")) // mismo endpoint, distinto DTO
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                    .build();

            var res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                return true;
            } else {
                System.err.println("POST /ventas (VentaRequest) -> HTTP " + res.statusCode() + " body=" + res.body());
                return false;
            }
        } catch (Exception ex) {
            System.err.println("No se pudo guardar venta (VentaRequest): " + ex.getMessage());
            return false;
        }
    }
    // Variante MESA: sin idCliente, con nombreMesa

    private boolean guardarVentaEnBackend_VentaRequestMesa(String nombreMesa,
            java.util.List<Long> idProductos,
            java.util.List<Integer> cantidades,
            TipoDePago estado,
            String observaciones) {
        try {
            if (idProductos == null || idProductos.isEmpty()
                    || cantidades == null || cantidades.isEmpty()) {
                System.err.println("VentaRequest (MESA) inválido: datos faltantes");
                return false;
            }

            var payload = om.createObjectNode()
                    .put("nombreMesa", nombreMesa == null ? "" : nombreMesa.trim())
                    .put("estado", (estado == null ? "EFECTIVO" : estado.name()))
                    .put("observaciones", (observaciones == null ? "" : observaciones));

            var arrIds = payload.putArray("idProductos");
            for (Long id : idProductos) {
                arrIds.add(id);
            }

            var arrCant = payload.putArray("cantidades");
            for (Integer c : cantidades) {
                arrCant.add(c);
            }

            var req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/ventas"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                    .build();

            var res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                return true;
            } else {
                System.err.println("POST /ventas (MESA) -> HTTP " + res.statusCode() + " body=" + res.body());
                return false;
            }
        } catch (Exception ex) {
            System.err.println("No se pudo guardar venta (MESA): " + ex.getMessage());
            return false;
        }
    }
// --- NUEVO: recargar ventas del día desde backend ---

    public void recargarDelBackend() {
        try {
            var hoy = java.time.LocalDate.now();
            // Ajustá parámetro según tu API (ej.: /ventas?fecha=YYYY-MM-DD)
            var url = BASE_URL + "/ventas?fecha=" + hoy.toString();
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                var array = om.readTree(res.body());
                var nuevas = FXCollections.<Fila>observableArrayList();
                if (array.isArray()) {
                    for (var n : array) {
                        Fila f = new Fila();

                        String nombre = n.hasNonNull("clienteNombre") ? n.get("clienteNombre").asText()
                                : (n.hasNonNull("nombreCliente") ? n.get("nombreCliente").asText()
                                : (n.hasNonNull("nombreMesa") ? n.get("nombreMesa").asText() : ""));
                        f.setNombre(nombre);

                        f.setDescripcion(n.hasNonNull("descripcion") ? n.get("descripcion").asText() : "");

                        java.math.BigDecimal m = java.math.BigDecimal.ZERO;
                        if (n.hasNonNull("monto")) {
                            m = new java.math.BigDecimal(n.get("monto").asText())
                                    .setScale(2, java.math.RoundingMode.HALF_UP);
                        }
                        f.setMonto(m);

                        TipoDePago tp = TipoDePago.EFECTIVO;
                        if (n.hasNonNull("estado")) {
                            try {
                                tp = TipoDePago.valueOf(n.get("estado").asText());
                            } catch (IllegalArgumentException ignore) {
                                tp = TipoDePago.EFECTIVO;
                            }
                        }
                        f.setEstado(tp);

                        nuevas.add(f);
                    }
                }
                filas.setAll(nuevas);
                recomputeTotal();
            } else {
                System.err.println("GET /ventas -> HTTP " + res.statusCode());
            }
        } catch (Exception ex) {
            System.err.println("No se pudo recargar ventas: " + ex.getMessage());
        }
    }
}
