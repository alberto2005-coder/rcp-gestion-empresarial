package com.empresa.rcp.module.clientes;

import com.empresa.rcp.db.DatabaseManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import com.empresa.rcp.MainController;


public class ClientesController implements Initializable {

    @FXML
    private TableView<Cliente> tablaClientes;
    @FXML
    private TableColumn<Cliente, String> colId;
    @FXML
    private TableColumn<Cliente, String> colNombre;
    @FXML
    private TableColumn<Cliente, String> colEmail;
    @FXML
    private TableColumn<Cliente, String> colTelefono;
    @FXML
    private TableColumn<Cliente, String> colCiudad;
    @FXML
    private TableColumn<Cliente, String> colEstado;
    @FXML
    private TextField txtBuscar;
    @FXML
    private Label lblTotal;

    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtTelefono;
    @FXML
    private TextField txtCiudad;
    @FXML
    private ComboBox<String> cmbEstado;

    private final ObservableList<Cliente> clientes = FXCollections.observableArrayList();
    private FilteredList<Cliente> filteredClientes;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColumnas();
        filteredClientes = new FilteredList<>(clientes, p -> true);
        tablaClientes.setItems(filteredClientes);
        tablaClientes.setPlaceholder(new Label("No hay clientes en la base de datos."));

        txtBuscar.textProperty().addListener((obs, o, n) -> filtrar(n));
        cargarDesdeBD();

        cmbEstado.setItems(FXCollections.observableArrayList("Activo", "Inactivo", "Pendiente"));
        cmbEstado.setValue("Activo");

        // Llenar el formulario automáticamente al hacer clic en la tabla
        tablaClientes.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                txtNombre.setText(newSel.getNombre());
                txtEmail.setText(newSel.getEmail());
                txtTelefono.setText(newSel.getTelefono());
                txtCiudad.setText(newSel.getCiudad());
                cmbEstado.setValue(newSel.getEstado());
            }
        });
    }

    // ── Cargar todos los clientes de la BD ────────────────────────────────
    private void cargarDesdeBD() {
        clientes.clear();
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM clientes ORDER BY nombre")) {
            while (rs.next()) {
                clientes.add(new Cliente(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("nombre"),
                        rs.getString("email"),
                        rs.getString("telefono"),
                        rs.getString("ciudad"),
                        rs.getString("estado")));
            }
        } catch (SQLException e) {
            mostrarAlerta("Error al cargar clientes: " + e.getMessage());
        }
        actualizarTotal();
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCiudad.setCellValueFactory(new PropertyValueFactory<>("ciudad"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(estado);
                setStyle(switch (estado) {
                    case "Activo" -> "-fx-text-fill: #10b981; -fx-font-weight: bold;";
                    case "Inactivo" -> "-fx-text-fill: #ef4444; -fx-font-weight: bold;";
                    default -> "-fx-text-fill: #f59e0b; -fx-font-weight: bold;";
                });
            }
        });
    }

    private void filtrar(String texto) {
        filteredClientes.setPredicate(c -> {
            if (texto == null || texto.isBlank())
                return true;
            String low = texto.toLowerCase();
            return c.getNombre().toLowerCase().contains(low)
                    || c.getEmail().toLowerCase().contains(low)
                    || c.getCiudad().toLowerCase().contains(low)
                    || c.getEstado().toLowerCase().contains(low);
        });
        actualizarTotal();
    }

    private void actualizarTotal() {
        lblTotal.setText(filteredClientes.size() + " clientes");
    }

    // ── CRUD ──────────────────────────────────────────────────────────────
    @FXML
    private void onNuevoCliente() {
        if (!validarCampos())
            return;
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO clientes(nombre,email,telefono,ciudad,estado) VALUES(?,?,?,?,?)")) {
            ps.setString(1, txtNombre.getText());
            ps.setString(2, txtEmail.getText());
            ps.setString(3, txtTelefono.getText());
            ps.setString(4, txtCiudad.getText());
            ps.setString(5, cmbEstado.getValue());
            ps.executeUpdate();
            
            DatabaseManager.getInstance().registrarActividad(
                com.empresa.rcp.MainController.getActiveUser(),
                "Agregó cliente: " + txtNombre.getText()
            );

            limpiarFormulario();
            cargarDesdeBD();
        } catch (SQLException e) {
            mostrarAlerta("Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    private void onEditarCliente() {
        Cliente sel = tablaClientes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("⚠️ Selecciona un cliente de la tabla para editar.");
            return;
        }
        if (!validarCampos())
            return;

        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE clientes SET nombre=?,email=?,telefono=?,ciudad=?,estado=? WHERE id=?")) {
            ps.setString(1, txtNombre.getText());
            ps.setString(2, txtEmail.getText());
            ps.setString(3, txtTelefono.getText());
            ps.setString(4, txtCiudad.getText());
            ps.setString(5, cmbEstado.getValue());
            ps.setInt(6, Integer.parseInt(sel.getId()));
            ps.executeUpdate();
            
            DatabaseManager.getInstance().registrarActividad(
                com.empresa.rcp.MainController.getActiveUser(),
                "Editó cliente: " + txtNombre.getText() + " (ID: " + sel.getId() + ")"
            );

            limpiarFormulario();
            cargarDesdeBD();
        } catch (SQLException e) {
            mostrarAlerta("Error al actualizar: " + e.getMessage());
        }
    }

    @FXML
    private void onEliminarCliente() {
        Cliente sel = tablaClientes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("⚠️ Selecciona un cliente para eliminar.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar \"" + sel.getNombre() + "\" de la base de datos?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                Connection conn = DatabaseManager.getInstance().getConnection();
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM clientes WHERE id=?")) {
                    ps.setInt(1, Integer.parseInt(sel.getId()));
                    ps.executeUpdate();
                    
                    DatabaseManager.getInstance().registrarActividad(
                        com.empresa.rcp.MainController.getActiveUser(),
                        "Eliminó cliente: " + sel.getNombre() + " (ID: " + sel.getId() + ")"
                    );

                    limpiarFormulario();
                    cargarDesdeBD();
                } catch (SQLException e) {
                    mostrarAlerta("Error al eliminar: " + e.getMessage());
                }
            }
        });
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtEmail.clear();
        txtTelefono.clear();
        txtCiudad.clear();
        cmbEstado.setValue("Activo");
        tablaClientes.getSelectionModel().clearSelection();
    }

    private boolean validarCampos() {
        if (txtNombre.getText().isBlank() || txtEmail.getText().isBlank() || txtTelefono.getText().isBlank()
                || txtCiudad.getText().isBlank()) {
            mostrarAlerta("⚠️ Por favor, rellena todos los campos.");
            return false;
        }
        if (!txtEmail.getText().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            mostrarAlerta("⚠️ El formato del correo electrónico es inválido (ej: usuario@empresa.com).");
            return false;
        }
        if (!txtTelefono.getText().matches("^\\d+$")) {
            mostrarAlerta("⚠️ El teléfono debe contener únicamente números.");
            return false;
        }
        return true;
    }

    private void mostrarAlerta(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    // ── Modelo ────────────────────────────────────────────────────────────
    public static class Cliente {
        private final SimpleStringProperty id, nombre, email, telefono, ciudad, estado;

        public Cliente(String id, String nombre, String email,
                String telefono, String ciudad, String estado) {
            this.id = new SimpleStringProperty(id);
            this.nombre = new SimpleStringProperty(nombre);
            this.email = new SimpleStringProperty(email);
            this.telefono = new SimpleStringProperty(telefono);
            this.ciudad = new SimpleStringProperty(ciudad);
            this.estado = new SimpleStringProperty(estado);
        }

        public String getId() {
            return id.get();
        }

        public String getNombre() {
            return nombre.get();
        }

        public String getEmail() {
            return email.get();
        }

        public String getTelefono() {
            return telefono.get();
        }

        public String getCiudad() {
            return ciudad.get();
        }

        public String getEstado() {
            return estado.get();
        }
    }

    @FXML
    private void onImportarCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importar Clientes desde CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV (*.csv)", "*.csv"));
        File file = fileChooser.showOpenDialog(tablaClientes.getScene().getWindow());
        if (file == null) return;

        int importados = 0;
        int errores = 0;
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "INSERT INTO clientes(nombre, email, telefono, ciudad, estado) VALUES(?,?,?,?,?)";
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            String line = reader.readLine(); // Leer encabezado
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("[,;]");
                if (parts.length >= 5) {
                    try {
                        ps.setString(1, parts[0].trim());
                        ps.setString(2, parts[1].trim());
                        ps.setString(3, parts[2].trim());
                        ps.setString(4, parts[3].trim());
                        ps.setString(5, parts[4].trim());
                        ps.executeUpdate();
                        importados++;
                    } catch (Exception ex) {
                        errores++;
                    }
                } else {
                    errores++;
                }
            }
            
            DatabaseManager.getInstance().registrarActividad(
                MainController.getActiveUser(),
                "Importó " + importados + " clientes desde " + file.getName()
            );
            
            cargarDesdeBD();
            mostrarAlerta("✅ Importación completada.\nClientes importados: " + importados + "\nFilas con error: " + errores);
        } catch (Exception e) {
            mostrarAlerta("❌ Error al importar CSV: " + e.getMessage());
        }
    }
}
