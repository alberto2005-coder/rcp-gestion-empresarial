package com.empresa.rcp.module.pedidos;

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
import java.time.LocalDate;
import java.util.ResourceBundle;

public class PedidosController implements Initializable {

    @FXML
    private TableView<Pedido> tablePedidos;
    @FXML
    private TableColumn<Pedido, String> colId;
    @FXML
    private TableColumn<Pedido, String> colNumeroPedido;
    @FXML
    private TableColumn<Pedido, String> colCliente;
    @FXML
    private TableColumn<Pedido, String> colFecha;
    @FXML
    private TableColumn<Pedido, String> colTotal;
    @FXML
    private TableColumn<Pedido, String> colEstado;

    @FXML
    private TextField txtBuscar;
    @FXML
    private TextField txtNumeroPedido;
    @FXML
    private ComboBox<String> cmbClientes;
    @FXML
    private DatePicker datePickerFecha;
    @FXML
    private TextField txtTotal;
    @FXML
    private ComboBox<String> cmbEstado;

    private final ObservableList<Pedido> pedidosList = FXCollections.observableArrayList();
    private FilteredList<Pedido> filteredPedidos;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColumnas();

        if (tablePedidos != null)
            tablePedidos.setPlaceholder(new Label("No hay pedidos registrados."));
        if (cmbEstado != null) {
            cmbEstado.getItems().addAll("Pendiente", "Procesando", "Completado", "Cancelado");
            cmbEstado.setValue("Pendiente");
        }
        if (datePickerFecha != null) {
            datePickerFecha.setValue(LocalDate.now());
        }

        filteredPedidos = new FilteredList<>(pedidosList, p -> true);
        if (tablePedidos != null)
            tablePedidos.setItems(filteredPedidos);

        if (txtBuscar != null) {
            txtBuscar.textProperty().addListener((obs, oldV, newV) -> {
                filteredPedidos.setPredicate(ped -> {
                    if (newV == null || newV.isBlank())
                        return true;
                    String low = newV.toLowerCase();
                    return ped.getNumeroPedido().toLowerCase().contains(low) ||
                            ped.getClienteNombre().toLowerCase().contains(low) ||
                            ped.getEstado().toLowerCase().contains(low);
                });
            });
        }

        cargarClientesCombo();
        cargarDesdeBD();

        if (tablePedidos != null) {
            tablePedidos.getSelectionModel().selectedItemProperty().addListener((obs, old, newSel) -> {
                if (newSel != null) {
                    txtNumeroPedido.setText(newSel.getNumeroPedido());
                    txtTotal.setText(newSel.getTotal());
                    cmbEstado.setValue(newSel.getEstado());
                    try {
                        datePickerFecha.setValue(LocalDate.parse(newSel.getFecha()));
                    } catch (Exception e) {
                    }

                    for (String item : cmbClientes.getItems()) {
                        if (item.startsWith(newSel.getClienteId() + " -")) {
                            cmbClientes.setValue(item);
                            break;
                        }
                    }
                }
            });
        }
    }

    private void configurarColumnas() {
        if (colId != null)
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colNumeroPedido != null)
            colNumeroPedido.setCellValueFactory(new PropertyValueFactory<>("numeroPedido"));
        if (colCliente != null)
            colCliente.setCellValueFactory(new PropertyValueFactory<>("clienteNombre"));
        if (colFecha != null)
            colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        if (colTotal != null)
            colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        if (colEstado != null)
            colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
    }

    private void cargarClientesCombo() {
        cmbClientes.getItems().clear();
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT id, nombre FROM clientes ORDER BY nombre")) {
            while (rs.next()) {
                cmbClientes.getItems().add(rs.getInt("id") + " - " + rs.getString("nombre"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cargarDesdeBD() {
        pedidosList.clear();
        String sql = "SELECT p.*, c.nombre as cliente_nombre FROM pedidos p LEFT JOIN clientes c ON p.cliente_id = c.id ORDER BY p.id DESC";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                pedidosList.add(new Pedido(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("numero_pedido"),
                        String.valueOf(rs.getInt("cliente_id")),
                        rs.getString("cliente_nombre") != null ? rs.getString("cliente_nombre") : "Desconocido",
                        rs.getString("fecha_pedido"),
                        String.valueOf(rs.getDouble("total")),
                        rs.getString("estado")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNuevoPedido() {
        if (!validarCampos())
            return;
        int clienteId = Integer.parseInt(cmbClientes.getValue().split(" - ")[0]);

        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO pedidos (numero_pedido, cliente_id, fecha_pedido, total, estado) VALUES (?,?,?,?,?)")) {
            ps.setString(1, txtNumeroPedido.getText());
            ps.setInt(2, clienteId);
            ps.setString(3, datePickerFecha.getValue().toString());
            ps.setDouble(4, txtTotal.getText().isEmpty() ? 0 : Double.parseDouble(txtTotal.getText()));
            ps.setString(5, cmbEstado.getValue());
            ps.executeUpdate();

            DatabaseManager.getInstance().registrarActividad(
                com.empresa.rcp.MainController.getActiveUser(),
                "Agregó pedido #" + txtNumeroPedido.getText() + " (Total: " + txtTotal.getText() + ")"
            );

            limpiarCampos();
            cargarDesdeBD();
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onEditarPedido() {
        Pedido sel = tablePedidos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("⚠️ Selecciona un pedido de la tabla para editar.");
            return;
        }
        if (!validarCampos())
            return;
        int clienteId = Integer.parseInt(cmbClientes.getValue().split(" - ")[0]);

        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE pedidos SET numero_pedido=?, cliente_id=?, fecha_pedido=?, total=?, estado=? WHERE id=?")) {
            ps.setString(1, txtNumeroPedido.getText());
            ps.setInt(2, clienteId);
            ps.setString(3, datePickerFecha.getValue().toString());
            ps.setDouble(4, txtTotal.getText().isEmpty() ? 0 : Double.parseDouble(txtTotal.getText()));
            ps.setString(5, cmbEstado.getValue());
            ps.setInt(6, Integer.parseInt(sel.getId()));
            ps.executeUpdate();

            DatabaseManager.getInstance().registrarActividad(
                com.empresa.rcp.MainController.getActiveUser(),
                "Editó pedido #" + txtNumeroPedido.getText() + " (ID: " + sel.getId() + ", Estado: " + cmbEstado.getValue() + ")"
            );

            limpiarCampos();
            cargarDesdeBD();
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onCancelarPedido() {
        Pedido sel = tablePedidos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("⚠️ Selecciona un pedido para cancelar.");
            return;
        }
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM pedidos WHERE id=?")) {
            ps.setInt(1, Integer.parseInt(sel.getId()));
            ps.executeUpdate();

            DatabaseManager.getInstance().registrarActividad(
                com.empresa.rcp.MainController.getActiveUser(),
                "Eliminó/canceló pedido #" + sel.getNumeroPedido() + " (ID: " + sel.getId() + ")"
            );

            limpiarCampos();
            cargarDesdeBD();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void limpiarCampos() {
        txtNumeroPedido.clear();
        txtTotal.clear();
        cmbClientes.setValue(null);
        cmbEstado.setValue("Pendiente");
        datePickerFecha.setValue(LocalDate.now());
        tablePedidos.getSelectionModel().clearSelection();
    }

    private boolean validarCampos() {
        if (txtNumeroPedido.getText().isBlank() || cmbClientes.getValue() == null || txtTotal.getText().isBlank()) {
            mostrarAlerta(
                    "⚠️ Por favor, rellena todos los campos. Asegúrate de tener al menos un Cliente seleccionado.");
            return false;
        }
        try {
            Double.parseDouble(txtTotal.getText());
        } catch (NumberFormatException e) {
            mostrarAlerta("⚠️ El total del pedido debe ser un valor numérico válido (ej: 150.50).");
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
    public static class Pedido {
        private final SimpleStringProperty id, numeroPedido, clienteId, clienteNombre, fecha, total, estado;

        public Pedido(String id, String num, String cId, String cNom, String fecha, String total, String estado) {
            this.id = new SimpleStringProperty(id);
            this.numeroPedido = new SimpleStringProperty(num);
            this.clienteId = new SimpleStringProperty(cId);
            this.clienteNombre = new SimpleStringProperty(cNom);
            this.fecha = new SimpleStringProperty(fecha);
            this.total = new SimpleStringProperty(total);
            this.estado = new SimpleStringProperty(estado);
        }

        public String getId() {
            return id.get();
        }

        public String getNumeroPedido() {
            return numeroPedido.get();
        }

        public String getClienteId() {
            return clienteId.get();
        }

        public String getClienteNombre() {
            return clienteNombre.get();
        }

        public String getFecha() {
            return fecha.get();
        }

        public String getTotal() {
            return total.get();
        }

        public String getEstado() {
            return estado.get();
        }
    }
}