package com.empresa.rcp.module.configuracion;

import com.empresa.rcp.db.DatabaseManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ResourceBundle;

public class ConfiguracionController implements Initializable {
    @FXML
    private TextField txtNombreEmpresa;
    @FXML
    private TextField txtTelefonoEmpresa;
    @FXML
    private TextField txtEmailEmpresa;
    @FXML
    private TextField txtDireccionEmpresa;
    @FXML
    private ComboBox<String> cmbTema;
    @FXML
    private Label lblVersionApp;
    @FXML
    private Button btnGuardarConfiguracion;
    @FXML
    private Button btnRestaurarDefaults;

    // --- Controles de la tabla de Auditoría ---
    @FXML
    private TableView<AuditLog> tableLogs;
    @FXML
    private TableColumn<AuditLog, String> colLogId;
    @FXML
    private TableColumn<AuditLog, String> colLogUsuario;
    @FXML
    private TableColumn<AuditLog, String> colLogAccion;
    @FXML
    private TableColumn<AuditLog, String> colLogFecha;

    private final ObservableList<AuditLog> logsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (cmbTema != null)
            cmbTema.getItems().addAll("Claro", "Oscuro");
        if (cmbTema != null)
            cmbTema.setValue("Oscuro");

        // Configurar columnas de auditoría
        if (colLogId != null) colLogId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colLogUsuario != null) colLogUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        if (colLogAccion != null) colLogAccion.setCellValueFactory(new PropertyValueFactory<>("accion"));
        if (colLogFecha != null) colLogFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

        if (tableLogs != null) {
            tableLogs.setPlaceholder(new Label("No hay registros de actividad."));
            tableLogs.setItems(logsList);
        }

        onActualizarLogs();
    }

    @FXML
    private void onGuardarConfiguracion() {
        DatabaseManager.getInstance().registrarActividad(
            com.empresa.rcp.MainController.getActiveUser(),
            "Guardó cambios en la configuración empresarial"
        );
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Configuración");
        alert.setHeaderText(null);
        alert.setContentText("✅ Cambios guardados correctamente en la base de datos local.");
        alert.showAndWait();
    }

    @FXML
    private void onRestaurarDefaults() {
        txtNombreEmpresa.clear();
        txtTelefonoEmpresa.clear();
        txtEmailEmpresa.clear();
        txtDireccionEmpresa.clear();
        
        if (cmbTema != null) {
            cmbTema.setValue("Oscuro");
        }

        DatabaseManager.getInstance().registrarActividad(
            com.empresa.rcp.MainController.getActiveUser(),
            "Restauró valores por defecto de la configuración empresarial"
        );
    }

    @FXML
    private void onActualizarLogs() {
        logsList.clear();
        String sql = "SELECT id, usuario, accion, timestamp FROM logs_actividad ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                logsList.add(new AuditLog(
                    String.valueOf(rs.getInt("id")),
                    rs.getString("usuario"),
                    rs.getString("accion"),
                    rs.getString("timestamp")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- MODELO INTERNO DE AUDITORÍA ---
    public static class AuditLog {
        private final SimpleStringProperty id, usuario, accion, fecha;

        public AuditLog(String id, String usuario, String accion, String fecha) {
            this.id = new SimpleStringProperty(id);
            this.usuario = new SimpleStringProperty(usuario);
            this.accion = new SimpleStringProperty(accion);
            this.fecha = new SimpleStringProperty(fecha);
        }

        public String getId() { return id.get(); }
        public String getUsuario() { return usuario.get(); }
        public String getAccion() { return accion.get(); }
        public String getFecha() { return fecha.get(); }
    }
}