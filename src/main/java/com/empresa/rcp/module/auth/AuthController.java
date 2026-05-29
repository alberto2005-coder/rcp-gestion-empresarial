package com.empresa.rcp.module.auth;

import com.empresa.rcp.MainController;
import com.empresa.rcp.db.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class AuthController implements Initializable {

    // Login FXML fields
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    // Register FXML fields
    @FXML private TextField txtRegUsuario;
    @FXML private TextField txtRegEmail;
    @FXML private ComboBox<String> cmbRegRol;
    @FXML private PasswordField txtRegPassword;
    @FXML private Label lblRegError;
    @FXML private Label lblRegSuccess;

    // Recovery FXML fields
    @FXML private TextField txtRecUsuario;
    @FXML private VBox vboxFormularioRecuperacion;
    @FXML private Label lblPreguntaSeguridad;
    @FXML private TextField txtRecRespuesta;
    @FXML private PasswordField txtRecNuevaPassword;
    @FXML private Label lblRecError;
    @FXML private Label lblRecSuccess;
    @FXML private Button btnRestablecer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (cmbRegRol != null) {
            cmbRegRol.setItems(FXCollections.observableArrayList("Administrador", "Empleado"));
            cmbRegRol.setValue("Empleado");
        }
        // cmbRegPregunta removed
    }

    private void cambiarEscena(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene;
            if (txtUsuario != null) scene = txtUsuario.getScene();
            else if (txtRegUsuario != null) scene = txtRegUsuario.getScene();
            else scene = txtRecUsuario.getScene();
            
            scene.setRoot(root);
        } catch (IOException e) {
            System.err.println("Error cambiando escena a: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    private void onIrARegistro(ActionEvent event) {
        cambiarEscena("/fxml/registro.fxml");
    }

    @FXML
    private void onIrARecuperar(ActionEvent event) {
        cambiarEscena("/fxml/recuperar_pass.fxml");
    }

    @FXML
    private void onIrALogin(ActionEvent event) {
        cambiarEscena("/fxml/login.fxml");
    }

    @FXML
    private void onLogin(ActionEvent event) {
        String username = txtUsuario.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Por favor, completa todos los campos.");
            return;
        }

        String sql = "SELECT password_hash, rol FROM usuarios WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    String rol = rs.getString("rol");

                    if (BCrypt.checkpw(password, hash)) {
                        DatabaseManager.getInstance().registrarActividad(username, "Inicio de sesión exitoso");

                        // Cargar panel principal y pasar datos de sesión
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                        Parent root = loader.load();
                        
                        MainController mainController = loader.getController();
                        mainController.setSesion(username, rol);

                        Scene scene = txtUsuario.getScene();
                        
                        // Cargar tema dinámicamente según estado en MainController
                        scene.getStylesheets().removeIf(s -> s.contains("dark-theme.css") || s.contains("light-theme.css"));
                        String themeCss = MainController.isDark() ? "/css/dark-theme.css" : "/css/light-theme.css";
                        URL themeUrl = getClass().getResource(themeCss);
                        if (themeUrl != null) {
                            scene.getStylesheets().add(themeUrl.toExternalForm());
                        }

                        scene.setRoot(root);
                    } else {
                        DatabaseManager.getInstance().registrarActividad(username, "Fallo al iniciar sesión: contraseña incorrecta");
                        lblError.setText("Contraseña incorrecta.");
                    }
                } else {
                    DatabaseManager.getInstance().registrarActividad(username, "Fallo al iniciar sesión: usuario inexistente");
                    lblError.setText("El usuario no existe.");
                }
            }
        } catch (Exception e) {
            lblError.setText("Error en la conexión a la base de datos.");
            e.printStackTrace();
        }
    }

    @FXML
    private void onRegistrar(ActionEvent event) {
        String username = txtRegUsuario.getText().trim();
        String email = txtRegEmail.getText().trim();
        String rolSeleccionado = cmbRegRol.getValue();
        String password = txtRegPassword.getText().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            lblRegError.setText("Por favor, completa todos los campos.");
            lblRegSuccess.setText("");
            return;
        }

        if (password.length() < 6) {
            lblRegError.setText("La contraseña debe tener al menos 6 caracteres.");
            lblRegSuccess.setText("");
            return;
        }

        // 1. Verificar si el usuario o correo ya tiene cuenta activa en la tabla 'usuarios'
        String sqlCheckUsuario = "SELECT COUNT(*) FROM usuarios WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            try (PreparedStatement checkUserPs = conn.prepareStatement(sqlCheckUsuario)) {
                checkUserPs.setString(1, username);
                checkUserPs.setString(2, email);
                try (ResultSet checkUserRs = checkUserPs.executeQuery()) {
                    if (checkUserRs.next() && checkUserRs.getInt(1) > 0) {
                        lblRegError.setText("El nombre de usuario o correo ya está registrado.");
                        lblRegSuccess.setText("");
                        return;
                    }
                }
            }

            // 2. Verificar si ya existe un perfil en la tabla 'empleados'
            String sqlCheckEmpleado = "SELECT id, puesto, email FROM empleados WHERE LOWER(nombre) = LOWER(?) OR LOWER(email) = LOWER(?)";
            int empleadoId = -1;
            String puestoOriginal = null;
            try (PreparedStatement checkEmpPs = conn.prepareStatement(sqlCheckEmpleado)) {
                checkEmpPs.setString(1, username);
                checkEmpPs.setString(2, email);
                try (ResultSet checkEmpRs = checkEmpPs.executeQuery()) {
                    if (checkEmpRs.next()) {
                        empleadoId = checkEmpRs.getInt("id");
                        puestoOriginal = checkEmpRs.getString("puesto");
                    }
                }
            }

            String finalRol;
            boolean crearNuevoEmpleado = false;

            if (empleadoId != -1) {
                // Existe en empleados: Vincular cuenta heredando el rol/puesto
                finalRol = (puestoOriginal != null && (puestoOriginal.equalsIgnoreCase("Administrador") || puestoOriginal.toLowerCase().contains("admin"))) 
                        ? "Administrador" 
                        : "Empleado";
            } else {
                // No existe en empleados: Validar rol del formulario
                if ("Administrador".equalsIgnoreCase(rolSeleccionado)) {
                    lblRegError.setText("No puedes registrarte como Administrador sin pre-registro.");
                    lblRegSuccess.setText("");
                    return;
                }
                finalRol = "Empleado";
                crearNuevoEmpleado = true;
            }

            conn.setAutoCommit(false);
            try {
                // Insertar usuario
                String sqlInsertUsuario = "INSERT INTO usuarios (username, password_hash, email, rol) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertUserPs = conn.prepareStatement(sqlInsertUsuario)) {
                    insertUserPs.setString(1, username);
                    insertUserPs.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
                    insertUserPs.setString(3, email);
                    insertUserPs.setString(4, finalRol);
                    insertUserPs.executeUpdate();
                }

                // Si no existía el empleado, crearlo automáticamente
                if (crearNuevoEmpleado) {
                    String sqlInsertEmpleado = "INSERT INTO empleados (nombre, apellido, email, telefono, puesto, salario) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertEmpPs = conn.prepareStatement(sqlInsertEmpleado)) {
                        insertEmpPs.setString(1, username);
                        insertEmpPs.setString(2, "Usuario");
                        insertEmpPs.setString(3, email);
                        insertEmpPs.setString(4, "");
                        insertEmpPs.setString(5, finalRol);
                        insertEmpPs.setDouble(6, 0.0);
                        insertEmpPs.executeUpdate();
                    }
                }

                conn.commit();
                DatabaseManager.getInstance().registrarActividad(username, "Registro de usuario exitoso (Rol: " + finalRol + ")");
                lblRegSuccess.setText("¡Cuenta vinculada y creada correctamente!");
                lblRegError.setText("");

                // Limpiar campos
                txtRegUsuario.clear();
                txtRegEmail.clear();
                txtRegPassword.clear();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            lblRegError.setText("Error en el registro de la base de datos.");
            lblRegSuccess.setText("");
            e.printStackTrace();
        }
    }

    @FXML
    private void onCargarPregunta(ActionEvent event) {
        String username = txtRecUsuario.getText().trim();
        if (username.isEmpty()) {
            lblRecError.setText("Ingresa tu nombre de usuario.");
            return;
        }

        String sql = "SELECT email FROM usuarios WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String email = rs.getString("email");
                    String codigo = EmailService.generarCodigo();

                    // Guardar el código en la base de datos
                    String sqlDelete = "DELETE FROM codigos_recuperacion WHERE email = ?";
                    try (PreparedStatement delPs = conn.prepareStatement(sqlDelete)) {
                        delPs.setString(1, email);
                        delPs.executeUpdate();
                    }

                    String sqlInsert = "INSERT INTO codigos_recuperacion (email, codigo, expiracion) VALUES (?, ?, datetime('now', '+15 minutes'))";
                    try (PreparedStatement insPs = conn.prepareStatement(sqlInsert)) {
                        insPs.setString(1, email);
                        insPs.setString(2, codigo);
                        insPs.executeUpdate();
                    }

                    // Enviar email de forma asíncrona
                    EmailService.enviarCodigoAsincrono(email, codigo);

                    lblPreguntaSeguridad.setText("Código enviado al correo: " + enmascararEmail(email));
                    
                    vboxFormularioRecuperacion.setVisible(true);
                    vboxFormularioRecuperacion.setManaged(true);
                    btnRestablecer.setVisible(true);
                    btnRestablecer.setManaged(true);

                    lblRecError.setText("");
                    lblRecSuccess.setText("");
                } else {
                    lblRecError.setText("El usuario no existe.");
                    vboxFormularioRecuperacion.setVisible(false);
                    vboxFormularioRecuperacion.setManaged(false);
                    btnRestablecer.setVisible(false);
                    btnRestablecer.setManaged(false);
                }
            }
        } catch (Exception e) {
            lblRecError.setText("Error consultando la base de datos.");
            e.printStackTrace();
        }
    }

    private String enmascararEmail(String email) {
        if (email == null || !email.contains("@")) return "****@****.com";
        int atIndex = email.indexOf("@");
        String userPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        if (userPart.length() <= 2) {
            return userPart + "****" + domainPart;
        }
        return userPart.substring(0, 2) + "****" + domainPart;
    }

    @FXML
    private void onRestablecerPassword(ActionEvent event) {
        String username = txtRecUsuario.getText().trim();
        String codigoIntroducido = txtRecRespuesta.getText().trim();
        String nuevaPassword = txtRecNuevaPassword.getText().trim();

        if (codigoIntroducido.isEmpty() || nuevaPassword.isEmpty()) {
            lblRecError.setText("Completa el código y la nueva contraseña.");
            lblRecSuccess.setText("");
            return;
        }

        if (nuevaPassword.length() < 6) {
            lblRecError.setText("La contraseña debe tener al menos 6 caracteres.");
            lblRecSuccess.setText("");
            return;
        }

        // Validar código y tiempo de expiración
        String sql = "SELECT email FROM usuarios WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String email = rs.getString("email");

                    String sqlCheck = "SELECT COUNT(*) FROM codigos_recuperacion WHERE email = ? AND codigo = ? AND datetime(expiracion) > datetime('now')";
                    try (PreparedStatement checkPs = conn.prepareStatement(sqlCheck)) {
                        checkPs.setString(1, email);
                        checkPs.setString(2, codigoIntroducido);
                        try (ResultSet rsCheck = checkPs.executeQuery()) {
                            if (rsCheck.next() && rsCheck.getInt(1) > 0) {
                                // Código válido: actualizar contraseña
                                String updateSql = "UPDATE usuarios SET password_hash = ? WHERE username = ?";
                                try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                                    updatePs.setString(1, BCrypt.hashpw(nuevaPassword, BCrypt.gensalt()));
                                    updatePs.setString(2, username);
                                    updatePs.executeUpdate();

                                    // Borrar código utilizado
                                    String deleteSql = "DELETE FROM codigos_recuperacion WHERE email = ?";
                                    try (PreparedStatement delPs = conn.prepareStatement(deleteSql)) {
                                        delPs.setString(1, email);
                                        delPs.executeUpdate();
                                    }

                                    DatabaseManager.getInstance().registrarActividad(username, "Restableció su contraseña con éxito mediante código por correo");

                                    lblRecSuccess.setText("¡Contraseña actualizada con éxito!");
                                    lblRecError.setText("");

                                    txtRecRespuesta.clear();
                                    txtRecNuevaPassword.clear();
                                }
                            } else {
                                DatabaseManager.getInstance().registrarActividad(username, "Fallo al restablecer contraseña: código incorrecto o expirado");
                                lblRecError.setText("Código de verificación incorrecto o expirado.");
                                lblRecSuccess.setText("");
                            }
                        }
                    }
                } else {
                    lblRecError.setText("El usuario no existe.");
                }
            }
        } catch (Exception e) {
            lblRecError.setText("Error actualizando la contraseña.");
            e.printStackTrace();
        }
    }
}
