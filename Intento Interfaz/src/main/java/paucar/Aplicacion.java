package paucar;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Aplicacion extends Application {

    @Override
    public void start(Stage stage) {

        // Layout principal
        BorderPane root = new BorderPane();

        Scene scene = new Scene(root, 1000, 700);

        // Cargar CSS (asegurate que app.css esté en resources)
        scene.getStylesheets().add(
                getClass().getResource("/app.css").toExternalForm());
        // =====================
        // BARRA LATERAL IZQUIERDA
        // =====================

// ===== Menú lateral =====
        VBox menu = new VBox(12);
        menu.getStyleClass().add("menu"); // estilo del panel izquierdo
        menu.setPadding(new Insets(20));
        menu.setPrefWidth(200);

        // ===== Logo decorativo como primer "ítem" del menú =====
        Image logoImg = new Image(getClass().getResourceAsStream("/img/logo paucar.png"));
        ImageView logoView = new ImageView(logoImg);
        logoView.setFitWidth(110);            // ajustá a gusto
        logoView.setPreserveRatio(true);
        logoView.setSmooth(true);
        logoView.setCache(true);
        logoView.setMouseTransparent(true);  // no capta clics, es decorativo

        StackPane logoItem = new StackPane(logoView);
        logoItem.getStyleClass().add("menu-logo"); // clase CSS para espaciar/centrar
        StackPane.setAlignment(logoView, Pos.CENTER);

// Insertar el logo antes que los botones
        menu.getChildren().add(logoItem);

        Button btnVentas = crearBotonConIcono("VENTAS", "/img/ventas.png");
        Button btnResumen = crearBotonConIcono("RESUMEN", "/img/resumen.png");
        Button btnGastos = crearBotonConIcono("GASTOS", "/img/gastos.png");
        Button btnStock = crearBotonConIcono("STOCK", "/img/stock.png");
        Button btnCalcula = crearBotonConIcono("CALCULA", "/img/calcula.png");

        // Marcar “activo” (estado visual)
        btnVentas.getStyleClass().add("active");

        Button[] botones = new Button[]{btnVentas, btnResumen, btnGastos, btnStock, btnCalcula};
        for (Button b : botones) {
            b.setMaxWidth(Double.MAX_VALUE);
        }/*Esto hace que los botones tomen la medida de la navtab izquierda */

        menu.getChildren().addAll(
                btnVentas,
                btnResumen,
                btnGastos,
                btnStock,
                btnCalcula
        );

        // ===== Contenido central =====
        VBox contenido = new VBox(30);
        contenido.getStyleClass().add("content");
        contenido.setAlignment(Pos.TOP_CENTER);

        Label titulo = new Label("Bienvenido UwU");
        titulo.getStyleClass().add("title-xl");

        VBox lineas = new VBox(20);
        for (int i = 0; i < 4; i++) {
            Region linea = new Region();
            linea.getStyleClass().add("line");
            linea.setPrefHeight(50);//grosor de linea
            linea.setMaxWidth(600);//ancho maximo de la linea
            //lineas.getChildren().add(linea);
        }

        contenido.getChildren().addAll(titulo, lineas);

        // Armado final (con scroll en la navtab)
        ScrollPane menuScroll = new ScrollPane(menu);
        menuScroll.setFitToWidth(true); // el VBox ocupa el ancho del scroll
        menuScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);   // sin barra horizontal
        menuScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // barra vertical solo si hace falta

// (opcional pero recomendado) sin borde del ScrollPane
        menuScroll.setPannable(true); // permite “arrastrar” con el mouse (agradable)
        menuScroll.setFocusTraversable(false); // no roba el foco al iniciar

        root.setLeft(menuScroll);
        root.setCenter(contenido);

        stage.setTitle("Interfaz");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        // ===== Demo: al hacer clic cambiamos el “activo” =====
        btnResumen.setOnAction(e -> marcarActivo(btnResumen, btnVentas, btnGastos, btnStock, btnCalcula));
        btnGastos.setOnAction(e -> marcarActivo(btnGastos, btnVentas, btnResumen, btnStock, btnCalcula));
        btnStock.setOnAction(e -> marcarActivo(btnStock, btnVentas, btnResumen, btnGastos, btnCalcula));
        btnCalcula.setOnAction(e -> marcarActivo(btnCalcula, btnVentas, btnResumen, btnGastos, btnStock));
        btnVentas.setOnAction(e -> marcarActivo(btnVentas, btnResumen, btnGastos, btnStock, btnCalcula));
    }

    private Button crearBotonConIcono(String texto, String rutaIcono) {
        Image img = new Image(getClass().getResourceAsStream(rutaIcono));
        ImageView icono = new ImageView(img);

        icono.setFitWidth(55);
        icono.setFitHeight(55);
        icono.setPreserveRatio(true);

        Button btn = new Button(texto);
        btn.setGraphic(icono);
        btn.setContentDisplay(ContentDisplay.TOP); // Imagen arriba del texto
        btn.setGraphicTextGap(8);

        return btn;
    }

    /**
     * Marca un botón como activo y limpia el resto.
     */
    private void marcarActivo(Button activo, Button... otros) {
        if (!activo.getStyleClass().contains("active")) {
            activo.getStyleClass().add("active");
        }
        for (Button b : otros) {
            b.getStyleClass().remove("active");
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
