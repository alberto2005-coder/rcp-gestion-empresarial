package com.empresa.rcp.module.productos;

import com.empresa.rcp.db.DatabaseManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class ProductosController implements Initializable {

    @FXML
    private TableView<Producto> tablaProductos;
    @FXML
    private TableColumn<Producto, String> colId;
    @FXML
    private TableColumn<Producto, String> colNombre;
    @FXML
    private TableColumn<Producto, String> colCategoria;
    @FXML
    private TableColumn<Producto, String> colPrecio;
    @FXML
    private TableColumn<Producto, String> colStock;
    @FXML
    private TableColumn<Producto, String> colEstado;
    @FXML
    private TextField txtBuscar;
    @FXML
    private Label lblTotal;

    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private FilteredList<Producto> filteredProductos;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColumnas();
        filteredProductos = new FilteredList<>(productos, p -> true);
        tablaProductos.setItems(filteredProductos);
        tablaProductos.setPlaceholder(new Label("No hay productos. Usa 'Nuevo Producto' para añadir."));
        txtBuscar.textProperty().addListener((obs, o, n) -> filtrar(n));
        cargarDesdeBD();
    }

    private void cargarDesdeBD() {
        productos.clear();
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM productos ORDER BY nombre")) {
            while (rs.next()) {
                productos.add(new Producto(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        String.format("%.2f €", rs.getDouble("precio")),
                        String.valueOf(rs.getInt("stock")),
                        rs.getString("categoria"),
                        rs.getString("estado")));
            }
        } catch (SQLException e) {
            mostrarAlerta("Error al cargar productos: " + e.getMessage());
        }
        actualizarTotal();
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(e);
                setStyle(switch (e) {
                    case "Activo" -> "-fx-text-fill: #10b981; -fx-font-weight: bold;";
                    case "Inactivo" -> "-fx-text-fill: #ef4444; -fx-font-weight: bold;";
                    default -> "-fx-text-fill: #f59e0b; -fx-font-weight: bold;";
                });
            }
        });

        // Stock — rojo si bajo
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(s);
                try {
                    int val = Integer.parseInt(s);
                    setStyle(val <= 5 ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;" : "");
                } catch (NumberFormatException ignored) {
                    setStyle("");
                }
            }
        });
    }

    private void filtrar(String texto) {
        filteredProductos.setPredicate(p -> {
            if (texto == null || texto.isBlank())
                return true;
            String low = texto.toLowerCase();
            return p.getNombre().toLowerCase().contains(low)
                    || p.getCategoria().toLowerCase().contains(low)
                    || p.getEstado().toLowerCase().contains(low);
        });
        actualizarTotal();
    }

    private void actualizarTotal() {
        lblTotal.setText(filteredProductos.size() + " productos");
    }

    @FXML
    private void onNuevoProducto() {
        mostrarDialogo(null);
    }

    @FXML
    private void onEditarProducto() {
        Producto sel = tablaProductos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("Selecciona un producto para editar.");
            return;
        }
        mostrarDialogo(sel);
    }

    @FXML
    private void onEliminarProducto() {
        Producto sel = tablaProductos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("Selecciona un producto para eliminar.");
            return;
        }
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar \"" + sel.getNombre() + "\"?", ButtonType.YES, ButtonType.NO);
        c.setHeaderText(null);
        c.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                Connection conn = DatabaseManager.getInstance().getConnection();
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM productos WHERE id=?")) {
                    ps.setInt(1, Integer.parseInt(sel.getId()));
                    ps.executeUpdate();
                    cargarDesdeBD();
                } catch (SQLException e) {
                    mostrarAlerta("Error: " + e.getMessage());
                }
            }
        });
    }

    private void mostrarDialogo(Producto p) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(p == null ? "Nuevo Producto" : "Editar Producto");
        dialog.setHeaderText(null);

        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);

        TextField fNombre = campo(p != null ? p.getNombre() : "");
        TextField fDesc = campo(p != null ? p.getDescripcion() : "");
        TextField fPrecio = campo(p != null ? p.getPrecioRaw() : "0");
        TextField fStock = campo(p != null ? p.getStock() : "0");
        TextField fCat = campo(p != null ? p.getCategoria() : "");
        ComboBox<String> fEstado = new ComboBox<>(
                FXCollections.observableArrayList("Activo", "Inactivo", "Agotado"));
        fEstado.setValue(p != null ? p.getEstado() : "Activo");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        String[] labels = { "Nombre:", "Descripción:", "Precio (€):", "Stock:", "Categoría:", "Estado:" };
        javafx.scene.Node[] fields = { fNombre, fDesc, fPrecio, fStock, fCat, fEstado };
        for (int i = 0; i < labels.length; i++) {
            grid.add(new Label(labels[i]), 0, i);
            grid.add(fields[i], 1, i);
        }
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == guardar) {
                try {
                    Connection conn = DatabaseManager.getInstance().getConnection();
                    double precio = parseDouble(fPrecio.getText());
                    int stock = parseInt(fStock.getText());
                    if (p == null) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO productos(nombre,descripcion,precio,stock,categoria,estado) VALUES(?,?,?,?,?,?)")) {
                            ps.setString(1, fNombre.getText());
                            ps.setString(2, fDesc.getText());
                            ps.setDouble(3, precio);
                            ps.setInt(4, stock);
                            ps.setString(5, fCat.getText());
                            ps.setString(6, fEstado.getValue());
                            ps.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE productos SET nombre=?,descripcion=?,precio=?,stock=?,categoria=?,estado=? WHERE id=?")) {
                            ps.setString(1, fNombre.getText());
                            ps.setString(2, fDesc.getText());
                            ps.setDouble(3, precio);
                            ps.setInt(4, stock);
                            ps.setString(5, fCat.getText());
                            ps.setString(6, fEstado.getValue());
                            ps.setInt(7, Integer.parseInt(p.getId()));
                            ps.executeUpdate();
                        }
                    }
                    cargarDesdeBD();
                } catch (SQLException e) {
                    mostrarAlerta("Error: " + e.getMessage());
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s.replace(",", "."));
        } catch (Exception e) {
            return 0;
        }
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private TextField campo(String v) {
        TextField tf = new TextField(v);
        tf.setPrefWidth(260);
        return tf;
    }

    private void mostrarAlerta(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    // ── Modelo ──────────────────────────────────────────────────────────────
    public static class Producto {
        private final SimpleStringProperty id, nombre, descripcion, precio, stock, categoria, estado;

        public Producto(String id, String nombre, String desc, String precio, String stock, String cat, String estado) {
            this.id = new SimpleStringProperty(id);
            this.nombre = new SimpleStringProperty(nombre);
            this.descripcion = new SimpleStringProperty(desc);
            this.precio = new SimpleStringProperty(precio);
            this.stock = new SimpleStringProperty(stock);
            this.categoria = new SimpleStringProperty(cat);
            this.estado = new SimpleStringProperty(estado);
        }

        public String getId() {
            return id.get();
        }

        public String getNombre() {
            return nombre.get();
        }

        public String getDescripcion() {
            return descripcion.get();
        }

        public String getPrecio() {
            return precio.get();
        }

        public String getPrecioRaw() {
            return precio.get().replace(" €", "").trim();
        }

        public String getStock() {
            return stock.get();
        }

        public String getCategoria() {
            return categoria.get();
        }

        public String getEstado() {
            return estado.get();
        }
    }
}
