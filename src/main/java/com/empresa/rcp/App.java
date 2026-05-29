package com.empresa.rcp;

import atlantafx.base.theme.NordDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Aplicar tema moderno oscuro AtlantaFX
        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());

        // Búsqueda de FXML de manera robusta
        URL fxmlUrl = App.class.getResource("/fxml/login.fxml");
        if (fxmlUrl == null) {
            fxmlUrl = App.class.getResource("login.fxml"); // Fallback a ruta relativa
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(loader.load(), 800, 600);

        // Búsqueda de CSS de manera robusta
        URL cssUrl = App.class.getResource("/css/styles.css");
        if (cssUrl == null)
            cssUrl = App.class.getResource("styles/app.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        URL darkThemeUrl = App.class.getResource("/css/dark-theme.css");
        if (darkThemeUrl != null) {
            scene.getStylesheets().add(darkThemeUrl.toExternalForm());
        }

        stage.setTitle("RCP Empresarial — Panel de Control");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        
        // Cargar el icono de la ventana
        try {
            stage.getIcons().add(new javafx.scene.image.Image(App.class.getResourceAsStream("/images/logo.png")));
        } catch (Exception e) {
            System.err.println("No se pudo cargar el icono de la app: " + e.getMessage());
        }

        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        // Animación de entrada suave
        stage.setOpacity(0);
        animateFadeIn(stage);
    }

    private void animateFadeIn(Stage stage) {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(stage.opacityProperty(), 0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(400),
                        new javafx.animation.KeyValue(stage.opacityProperty(), 1,
                                javafx.animation.Interpolator.EASE_OUT)));
        timeline.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
