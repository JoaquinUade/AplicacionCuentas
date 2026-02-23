package paucar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.uade.tpo.demo.entity.TipoCliente;

import javafx.application.Platform;
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
import paucar.service.VentasBackend;


public final class Ventas extends BorderPane {

    private static final Locale LOCALE_AR = new Locale("es", "AR");
    private final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(LOCALE_AR);

    // ===== Dependencia (backend) =====
    private final VentasBackend backend = new VentasBackend("http://localhost:4002/api");

    // ===== Modelo de una fila (UI) =====
    public static class Fila {

        private final StringProperty nombre = new SimpleStringProperty("");
        private final StringProperty descripcion = new SimpleStringProperty("");
        private final ObjectProperty<BigDecimal> monto = new SimpleObjectProperty<>(BigDecimal.ZERO);
        private final ObjectProperty<VentasBackend.TipoDePago> estado =
                new SimpleObjectProperty<>(VentasBackend.TipoDePago.EFECTIVO);

        public String getNombre() { return nombre.get(); }
        public void setNombre(String v) { nombre.set(v); }
        public StringProperty nombreProperty() { return nombre; }

        public String getDescripcion() { return descripcion.get(); }
        public void setDescripcion(String v) { descripcion.set(v); }
        public StringProperty descripcionProperty() { return descripcion; }

        public BigDecimal getMonto() { return monto.get(); }
        public void setMonto(BigDecimal v) { monto.set(v); }
        public ObjectProperty<BigDecimal> montoProperty() { return monto; }

        public VentasBackend.TipoDePago getEstado() { return estado.get(); }
        public void setEstado(VentasBackend.TipoDePago v) { estado.set(v); }
        public ObjectProperty<VentasBackend.TipoDePago> estadoProperty() { return estado; }
    }

    // ===== Estado de la vista =====
    private final ObservableList<Fila> filas = FXCollections.observableArrayList();
    private final ObjectProperty<BigDecimal> total = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // ===== Componentes =====
    private final TableView<Fila> tabla = new TableView<>(filas);
    private final Button btnAgregar = new Button("+ Agregar");
    private final Button btnQuitar = new Button("Quitar seleccionado");

    // ===== Clientes (sugerencias) =====
    private final ObservableList<String> clientes = FXCollections.observableArrayList();
    private final FilteredList<String> clientesFiltrados = new FilteredList<>(clientes, s -> true);

    // ===== Productos (sugerencias) =====
    private static class Producto {
        final Long id;
        final String nombre;
        Producto(Long id, String nombre) { this.id = id; this.nombre = nombre; }
        @Override public String toString() { return nombre; } // para mostrar en ComboBox
    }
    private final ObservableList<Producto> productos = FXCollections.observableArrayList();

    public Ventas() {
        setPadding(new Insets(16));
        setTop(crearHeader());
        setCenter(crearTabla());
        setBottom(crearFooter());

        // Cargas iniciales (asíncronas)
        cargarClientesAsync();
        cargarProductosAsync();
        recargarDelBackend();

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

        // Columna: Descripción (no editable, solo muestra)
        var colDesc = new TableColumn<Fila, String>("Descripción");
        colDesc.setCellValueFactory(c -> c.getValue().descripcionProperty());
        colDesc.setCellFactory(TextFieldTableCell.forTableColumn());
        colDesc.setEditable(false);
        colDesc.setPrefWidth(420);

        // Columna: Monto (formateado)
        var colMonto = new TableColumn<Fila, String>("Monto");
        colMonto.setCellValueFactory(c -> Bindings.createStringBinding(
                () -> formatear(c.getValue().getMonto()), c.getValue().montoProperty()));
        colMonto.setCellFactory(TextFieldTableCell.forTableColumn());
        colMonto.setEditable(false);
        colMonto.setPrefWidth(140);

        // Columna: Estado (ComboBox por fila, usando enum del backend)
        var colEstado = new TableColumn<Fila, VentasBackend.TipoDePago>("Estado");
        colEstado.setCellValueFactory(c -> c.getValue().estadoProperty());
        colEstado.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<VentasBackend.TipoDePago> combo = new ComboBox<>();
            {
                combo.getItems().setAll(VentasBackend.TipoDePago.values());
                combo.valueProperty().addListener((o, a, b) -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        getTableView().getItems().get(getIndex()).setEstado(b);
                    }
                });
            }
            @Override
            protected void updateItem(VentasBackend.TipoDePago item, boolean empty) {
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
                // Solo quita de la vista local (sin DELETE backend por ahora)
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

    // ===== Utilitarios =====
    private void recomputeTotal() {
        BigDecimal t = filas.stream()
                .map(Fila::getMonto)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        total.set(t.setScale(2, RoundingMode.HALF_UP));
    }

    private String formatear(BigDecimal v) {
        if (v == null) return "$ 0,00";
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
        if (nombre == null) return TipoCliente.CLIENTE;
        String n = nombre.trim().toLowerCase();

        if (n.startsWith("mesa ")) return TipoCliente.MESA;

        if (n.contains(" srl") || n.endsWith(" srl") || n.contains(" s.a") || n.contains(" sa")
                || n.contains("empresa") || n.contains("estudio") || n.contains("industria")) {
            return TipoCliente.EMPRESA;
        }
        return TipoCliente.CLIENTE;
    }

    // ====== Cargas iniciales (asíncronas) ======
    private void cargarClientesAsync() {
        CompletableFuture
                .supplyAsync(() -> backend.obtenerTodosLosClientesMenosMesas())
                .thenAccept(lista -> Platform.runLater(() -> clientes.setAll(lista)));
    }

    private void cargarProductosAsync() {
        CompletableFuture
                .supplyAsync(backend::cargarProductos)
                .thenAccept(items -> Platform.runLater(() -> {
                    var mapped = items.stream()
                            .map(p -> new Producto(p.id(), p.nombre()))
                            .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.nombre, b.nombre))
                            .collect(Collectors.toList());
                    productos.setAll(mapped);
                }));
    }

    public void recargarDelBackend() {
        CompletableFuture
                .supplyAsync(() -> backend.cargarVentasDelDia(LocalDate.now()))
                .thenAccept(lista -> Platform.runLater(() -> {
                    var nuevas = FXCollections.<Fila>observableArrayList();
                    for (VentasBackend.VentaFilaDto dto : lista) {
                        Fila f = new Fila();
                        f.setNombre(dto.nombre());
                        f.setDescripcion(dto.descripcion());
                        f.setMonto(dto.monto());
                        f.setEstado(dto.estado());
                        nuevas.add(f);
                    }
                    filas.setAll(nuevas);
                    recomputeTotal();
                }));
    }

    // ===== Diálogo Agregar =====

    private static class PedidoNuevo {
        String nombreCliente;
        java.util.List<Long> idProductos = new java.util.ArrayList<>();
        java.util.List<Integer> cantidades = new java.util.ArrayList<>();
        VentasBackend.TipoDePago estado;
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
            if (!cbCliente.isShowing() && !txt.isEmpty()) cbCliente.show();
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
        ComboBox<VentasBackend.TipoDePago> cbEstado = new ComboBox<>();
        cbEstado.getItems().setAll(VentasBackend.TipoDePago.values());
        cbEstado.setValue(VentasBackend.TipoDePago.TRANSFERENCIA); // por defecto

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
            if ((nombre == null || nombre.isBlank()) && cbCliente.getValue() == null) return true;

            for (var n : contLineas.getChildren()) {
                if (n instanceof HBox fila) {
                    @SuppressWarnings("unchecked")
                    ComboBox<Producto> cb = (ComboBox<Producto>) fila.getChildren().get(0);
                    TextField tf = (TextField) fila.getChildren().get(1);
                    if (cb.getValue() != null && tf.getText() != null && !tf.getText().isBlank()) {
                        try {
                            int c = Integer.parseInt(tf.getText());
                            if (c >= 1) return false;
                        } catch (NumberFormatException ignore) {}
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
                if (nombre == null || nombre.isBlank()) nombre = cbCliente.getValue();
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
                            } catch (NumberFormatException ignore) {}
                        }
                    }
                }
                return p;
            }
            return null;
        });

        var res = dialog.showAndWait();
        res.ifPresent(this::confirmarPedidoAsync);
    }

    // ===== Lógica final al confirmar (usa backend de forma asíncrona) =====
    private void confirmarPedidoAsync(PedidoNuevo p) {
        TipoCliente tipo = deducirTipoCliente(p.nombreCliente);

        if (tipo == TipoCliente.MESA) {
            CompletableFuture
                .supplyAsync(() -> backend.GuardarPedidoMesas(
                        p.nombreCliente, p.idProductos, p.cantidades, p.estado, p.observaciones))
                .thenAccept(ok -> Platform.runLater(() -> {
                    if (ok) recargarDelBackend();
                }));
            return;
        }

        CompletableFuture
            .runAsync(() -> backend.crearClienteSiNoExiste(p.nombreCliente, tipo))
            .thenCompose(v -> CompletableFuture.supplyAsync(() -> backend.obtenerClienteIdPorNombre(p.nombreCliente)))
            .thenCompose(idCliente -> {
                if (idCliente == null) {
                    Platform.runLater(() -> {
                        var dlg = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                        dlg.setTitle("Cliente no encontrado");
                        dlg.setHeaderText("No se pudo obtener el ID del cliente");
                        dlg.setContentText("Verificá el nombre del cliente o volvé a intentar.");
                        dlg.showAndWait();
                    });
                    return CompletableFuture.completedFuture(false);
                }
                return CompletableFuture.supplyAsync(() -> backend.GuardarPedidos(
                        idCliente, p.idProductos, p.cantidades, p.estado, p.observaciones));
            })
            .thenAccept(ok -> Platform.runLater(() -> {
                if (ok) {
                    if (!clientes.contains(p.nombreCliente)) {
                        clientes.add(p.nombreCliente);
                        FXCollections.sort(clientes, String.CASE_INSENSITIVE_ORDER);
                    }
                    recargarDelBackend();
                }
            }));
    }
}
