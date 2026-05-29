package com.empresa.rcp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import com.empresa.rcp.module.RCPModule;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import com.empresa.rcp.db.DatabaseManager;


public class MainController implements Initializable {

    private static boolean isDarkTheme = true;
    private String currentFxml = "/fxml/dashboard.fxml";
    private static String activeUser = "Anónimo";
    private static String activeRol = "Empleado";

    public static boolean isDark() {
        return isDarkTheme;
    }

    public static String getActiveUser() {
        return activeUser;
    }

    public static String getActiveRol() {
        return activeRol;
    }

    public void setSesion(String usuario, String rol) {
        activeUser = usuario;
        activeRol = rol;
        if (lblUsuario != null) {
            lblUsuario.setText("👤 " + usuario + " (" + rol + ")");
        }
        if (btnConfiguracion != null) {
            btnConfiguracion.setDisable(!"Administrador".equalsIgnoreCase(rol));
        }
    }

    @FXML
    private BorderPane mainPane;
    @FXML
    private Label lblUsuario;
    @FXML
    private VBox sidebar;
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnClientes;
    @FXML
    private Button btnEmpleados;
    @FXML
    private Button btnPedidos;
    @FXML
    private Button btnReportes;
    @FXML
    private Button btnConfiguracion;
    @FXML
    private Button btnCambiarTema;
    @FXML
    private VBox contentArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Al arrancar, cargamos automáticamente la vista del Dashboard
        cargarSubVista("/fxml/dashboard.fxml");

        if (mainPane != null) {
            mainPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    configurarInactividad(newScene);
                }
            });
        }

        cargarModulosDinamicos();
    }

    private void cargarModulosDinamicos() {
        if (sidebar == null) return;
        try {
            ServiceLoader<RCPModule> loader = ServiceLoader.load(RCPModule.class);
            for (RCPModule modulo : loader) {
                try {
                    modulo.initializeModule();
                    Button btn = new Button(modulo.getModuleName());
                    btn.getStyleClass().add("sidebar-button");
                    btn.setMaxWidth(Double.MAX_VALUE);
                    VBox.setMargin(btn, new javafx.geometry.Insets(5, 0, 0, 0));
                    
                    if (modulo.getIconCode() != null && !modulo.getIconCode().isEmpty()) {
                        try {
                            org.kordamp.ikonli.javafx.FontIcon icon = new org.kordamp.ikonli.javafx.FontIcon(modulo.getIconCode());
                            btn.setGraphic(icon);
                        } catch (Exception ex) {
                            // Fallback
                        }
                    }
                    
                    btn.setOnAction(e -> {
                        Parent view = modulo.getView();
                        if (view != null) {
                            VBox.setVgrow(view, Priority.ALWAYS);
                            contentArea.getChildren().setAll(view);
                        }
                    });
                    
                    int insertIndex = sidebar.getChildren().size();
                    if (insertIndex > 0 && sidebar.getChildren().get(insertIndex - 1) instanceof Region) {
                        insertIndex--;
                    }
                    sidebar.getChildren().add(insertIndex, btn);
                } catch (Exception e) {
                    System.err.println("Error cargando modulo dinamico: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error buscando modulos dinamicos: " + e.getMessage());
        }
    }

    /**
     * Método genérico para cargar FXMLs dinámicamente dentro del contenedor central
     */
    private void cargarSubVista(String rutaFxml) {
        try {
            // Guardar ruta actual
            this.currentFxml = rutaFxml;
            
            URL fxmlUrl = getClass().getResource(rutaFxml);
            if (fxmlUrl == null) {
                // Probar fallback para reportes si está en la ruta del paquete original
                if (rutaFxml.contains("reportes.fxml")) {
                    fxmlUrl = getClass().getResource("/com/empresa/rcp/reportes.fxml");
                }
            }
            if (fxmlUrl == null) {
                System.err.println("❌ ERROR: No se encontró el archivo FXML: " + rutaFxml);
                return; // Evita el crash si el archivo no existe
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent subVista = loader.load();

            // Reemplazamos el contenido actual del contenedor por la nueva pantalla
            VBox.setVgrow(subVista, Priority.ALWAYS);
            contentArea.getChildren().setAll(subVista);
        } catch (IOException e) {
            System.err.println("Error crítico cargando la vista interna en: " + rutaFxml);
            e.printStackTrace();
        }
    }

    @FXML
    private void onDashboard(ActionEvent event) {
        cargarSubVista("/fxml/dashboard.fxml");
    }

    @FXML
    private void onClientes(ActionEvent event) {
        cargarSubVista("/fxml/clientes.fxml");
    }

    @FXML
    private void onEmpleados(ActionEvent event) {
        cargarSubVista("/fxml/empleados.fxml");
    }

    @FXML
    private void onPedidos(ActionEvent event) {
        cargarSubVista("/fxml/pedidos.fxml");
    }

    @FXML
    private void onReportes(ActionEvent event) {
        cargarSubVista("/com/empresa/rcp/reportes.fxml");
    }

    @FXML
    private void onConfiguracion(ActionEvent event) {
        cargarSubVista("/fxml/configuracion.fxml");
    }

    @FXML
    private void onCambiarTema(ActionEvent event) {
        isDarkTheme = !isDarkTheme;
        
        // Cambiar el tema de AtlantaFX
        if (isDarkTheme) {
            javafx.application.Application
                    .setUserAgentStylesheet(new atlantafx.base.theme.NordDark().getUserAgentStylesheet());
            if (btnCambiarTema != null)
                btnCambiarTema.setText("🌙 Cambiar Tema");
        } else {
            javafx.application.Application
                    .setUserAgentStylesheet(new atlantafx.base.theme.NordLight().getUserAgentStylesheet());
            if (btnCambiarTema != null)
                btnCambiarTema.setText("☀️ Cambiar Tema");
        }

        // Cargar dinámicamente el stylesheet de tema personalizado
        if (mainPane != null && mainPane.getScene() != null) {
            javafx.scene.Scene scene = mainPane.getScene();
            scene.getStylesheets().removeIf(s -> s.contains("dark-theme.css") || s.contains("light-theme.css"));
            
            String themeCss = isDarkTheme ? "/css/dark-theme.css" : "/css/light-theme.css";
            URL themeUrl = getClass().getResource(themeCss);
            if (themeUrl != null) {
                scene.getStylesheets().add(themeUrl.toExternalForm());
            }
        }

        // Refrescar subvista activa para que dibuje gráficos con nuevos colores
        cargarSubVista(currentFxml);
    }

    // --- TEMPORIZADOR DE INACTIVIDAD ---
    private Timeline inactivityTimeline;
    private static final double INACTIVITY_LIMIT_MINUTES = 5.0;

    private void configurarInactividad(javafx.scene.Scene scene) {
        if (inactivityTimeline != null) {
            inactivityTimeline.stop();
        }

        inactivityTimeline = new Timeline(new KeyFrame(Duration.minutes(INACTIVITY_LIMIT_MINUTES), event -> {
            logoutPorInactividad();
        }));
        inactivityTimeline.setCycleCount(1);
        inactivityTimeline.play();

        // Escuchar clics y teclas a nivel escena para reiniciar
        scene.addEventFilter(javafx.scene.input.MouseEvent.ANY, e -> reiniciarTemporizador());
        scene.addEventFilter(javafx.scene.input.KeyEvent.ANY, e -> reiniciarTemporizador());
    }

    private void reiniciarTemporizador() {
        if (inactivityTimeline != null) {
            inactivityTimeline.playFromStart();
        }
    }

    private void logoutPorInactividad() {
        if (inactivityTimeline != null) {
            inactivityTimeline.stop();
        }

        DatabaseManager.getInstance().registrarActividad(activeUser, "Sesión cerrada automáticamente por inactividad");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            javafx.stage.Stage stage = (javafx.stage.Stage) mainPane.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);

            String themeCss = isDarkTheme ? "/css/dark-theme.css" : "/css/light-theme.css";
            URL themeUrl = getClass().getResource(themeCss);
            if (themeUrl != null) {
                scene.getStylesheets().add(themeUrl.toExternalForm());
            }

            stage.setScene(scene);
            stage.show();

            Alert alert = new Alert(Alert.AlertType.WARNING, "⚠️ Tu sesión ha expirado por inactividad. Inicia sesión nuevamente.", ButtonType.OK);
            alert.setHeaderText("Sesión Expirada");
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}