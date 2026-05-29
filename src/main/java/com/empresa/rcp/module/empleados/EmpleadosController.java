package com.empresa.rcp.module.empleados;

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


public class EmpleadosController implements Initializable {

    @FXML
    private TableView<Empleado> tableEmpleados;
    @FXML
    private TableColumn<Empleado, String> colId;
    @FXML
    private TableColumn<Empleado, String> colNombre;
    @FXML
    private TableColumn<Empleado, String> colEmail;
    @FXML
    private TableColumn<Empleado, String> colPuesto;
    @FXML
    private TableColumn<Empleado, String> colSalario;

    @FXML
    private TextField txtBuscar;
    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtApellido;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtTelefono;
    @FXML
    private TextField txtPuesto;
    @FXML
    private TextField txtSalario;

    private final ObservableList<Empleado> empleadosList = FXCollections.observableArrayList();
    private FilteredList<Empleado> filteredEmpleados;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColumnas();

        if (tableEmpleados != null)
            tableEmpleados.setPlaceholder(new Label("No hay empleados registrados."));

        filteredEmpleados = new FilteredList<>(empleadosList, p -> true);
        if (tableEmpleados != null)
            tableEmpleados.setItems(filteredEmpleados);

        if (txtBuscar != null) {
            txtBuscar.textProperty().addListener((obs, oldV, newV) -> {
                filteredEmpleados.setPredicate(emp -> {
                    if (newV == null || newV.isBlank())
                        return true;
                    String low = newV.toLowerCase();
                    return emp.getNombre().toLowerCase().contains(low) ||
                            emp.getApellido().toLowerCase().contains(low) ||
                            emp.getPuesto().toLowerCase().contains(low);
                });
            });
        }

        cargarDesdeBD();

        // Autocompletar al seleccionar
        if (tableEmpleados != null) {
            tableEmpleados.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                if (newSel != null) {
                    txtNombre.setText(newSel.getNombre());
                    txtApellido.setText(newSel.getApellido());
                    txtEmail.setText(newSel.getEmail());
                    txtTelefono.setText(newSel.getTelefono());
                    txtPuesto.setText(newSel.getPuesto());
                    txtSalario.setText(newSel.getSalario());
                }
            });
        }
    }

    private void configurarColumnas() {
        if (colId != null)
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colNombre != null)
            colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        if (colEmail != null)
            colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (colPuesto != null)
            colPuesto.setCellValueFactory(new PropertyValueFactory<>("puesto"));
        if (colSalario != null)
            colSalario.setCellValueFactory(new PropertyValueFactory<>("salario"));
    }

    private void cargarDesdeBD() {
        empleadosList.clear();
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM empleados ORDER BY id DESC")) {
            while (rs.next()) {
                empleadosList.add(new Empleado(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("nombre"),
                        rs.getString("apellido"),
                        rs.getString("email"),
                        rs.getString("telefono"),
                        rs.getString("puesto"),
                        String.valueOf(rs.getDouble("salario"))));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNuevoEmpleado() {
        if (!validarCampos())
            return;
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO empleados (nombre, apellido, email, telefono, puesto, salario) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, txtNombre.getText());
            ps.setString(2, txtApellido.getText());
            ps.setString(3, txtEmail.getText());
            ps.setString(4, txtTelefono.getText());
            ps.setString(5, txtPuesto.getText());
            ps.setDouble(6, txtSalario.getText().isEmpty() ? 0 : Double.parseDouble(txtSalario.getText()));
            ps.executeUpdate();

            DatabaseManager.getInstance().registrarActividad(
                com.empresa.rcp.MainController.getActiveUser(),
                "Agregó empleado: " + txtNombre.getText() + " " + txtApellido.getText()
            );

            limpiarCampos();
            cargarDesdeBD();
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onEditarEmpleado() {
        Empleado sel = tableEmpleados.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("⚠️ Selecciona un empleado de la tabla para editar.");
            return;
        }
        if (!validarCampos())
            return;
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE empleados SET nombre=?, apellido=?, email=?, telefono=?, puesto=?, salario=? WHERE id=?")) {
            ps.setString(1, txtNombre.getText());
            ps.setString(2, txtApellido.getText());
            ps.setString(3, txtEmail.getText());
            ps.setString(4, txtTelefono.getText());
            ps.setString(5, txtPuesto.getText());
            ps.setDouble(6, txtSalario.getText().isEmpty() ? 0 : Double.parseDouble(txtSalario.getText()));
            ps.setInt(7, Integer.parseInt(sel.getId()));
            ps.executeUpdate();

            DatabaseManager.getInstance().registrarActividad(
                com.empresa.rcp.MainController.getActiveUser(),
                "Editó empleado: " + txtNombre.getText() + " (ID: " + sel.getId() + ")"
            );

            limpiarCampos();
            cargarDesdeBD();
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onEliminarEmpleado() {
        Empleado sel = tableEmpleados.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("⚠️ Selecciona un empleado para eliminar.");
            return;
        }
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM empleados WHERE id=?")) {
            ps.setInt(1, Integer.parseInt(sel.getId()));
            ps.executeUpdate();

            DatabaseManager.getInstance().registrarActividad(
                com.empresa.rcp.MainController.getActiveUser(),
                "Eliminó empleado: " + sel.getNombre() + " (ID: " + sel.getId() + ")"
            );

            limpiarCampos();
            cargarDesdeBD();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void limpiarCampos() {
        txtNombre.clear();
        txtApellido.clear();
        txtEmail.clear();
        txtTelefono.clear();
        txtPuesto.clear();
        txtSalario.clear();
        tableEmpleados.getSelectionModel().clearSelection();
    }

    private boolean validarCampos() {
        if (txtNombre.getText().isBlank() || txtApellido.getText().isBlank() || txtEmail.getText().isBlank()
                || txtTelefono.getText().isBlank() || txtPuesto.getText().isBlank() || txtSalario.getText().isBlank()) {
            mostrarAlerta("⚠️ Por favor, rellena todos los campos.");
            return false;
        }
        if (!txtEmail.getText().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            mostrarAlerta("⚠️ El formato del correo electrónico es inválido.");
            return false;
        }
        if (!txtTelefono.getText().matches("^\\d+$")) {
            mostrarAlerta("⚠️ El teléfono debe contener únicamente números.");
            return false;
        }
        try {
            Double.parseDouble(txtSalario.getText());
        } catch (NumberFormatException e) {
            mostrarAlerta("⚠️ El salario debe ser un valor numérico (ej: 1500.50).");
            return false;
        }
        return true;
    }

    private void mostrarAlerta(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    // --- MODELO INTERNO ---
    public static class Empleado {
        private final SimpleStringProperty id, nombre, apellido, email, telefono, puesto, salario;

        public Empleado(String id, String nombre, String apellido, String email, String telefono, String puesto,
                String salario) {
            this.id = new SimpleStringProperty(id);
            this.nombre = new SimpleStringProperty(nombre);
            this.apellido = new SimpleStringProperty(apellido);
            this.email = new SimpleStringProperty(email);
            this.telefono = new SimpleStringProperty(telefono);
            this.puesto = new SimpleStringProperty(puesto);
            this.salario = new SimpleStringProperty(salario);
        }

        public String getId() {
            return id.get();
        }

        public String getNombre() {
            return nombre.get();
        }

        public String getApellido() {
            return apellido.get();
        }

        public String getEmail() {
            return email.get();
        }

        public String getTelefono() {
            return telefono.get();
        }

        public String getPuesto() {
            return puesto.get();
        }

        public String getSalario() {
            return salario.get();
        }
    }

    @FXML
    private void onImportarCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importar Empleados desde CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV (*.csv)", "*.csv"));
        File file = fileChooser.showOpenDialog(tableEmpleados.getScene().getWindow());
        if (file == null) return;

        int importados = 0;
        int errores = 0;
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "INSERT INTO empleados (nombre, apellido, email, telefono, puesto, salario) VALUES (?,?,?,?,?,?)";
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            String line = reader.readLine(); // Leer encabezado
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("[,;]");
                if (parts.length >= 6) {
                    try {
                        ps.setString(1, parts[0].trim());
                        ps.setString(2, parts[1].trim());
                        ps.setString(3, parts[2].trim());
                        ps.setString(4, parts[3].trim());
                        ps.setString(5, parts[4].trim());
                        ps.setDouble(6, parts[5].trim().isEmpty() ? 0.0 : Double.parseDouble(parts[5].trim()));
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
                "Importó " + importados + " empleados desde " + file.getName()
            );
            
            cargarDesdeBD();
            mostrarAlerta("✅ Importación completada.\nEmpleados importados: " + importados + "\nFilas con error: " + errores);
        } catch (Exception e) {
            mostrarAlerta("❌ Error al importar CSV: " + e.getMessage());
        }
    }
}