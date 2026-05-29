package com.empresa.rcp.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseManager {
    private static DatabaseManager instance;
    private HikariDataSource dataSource;
    private Connection sharedConnection;

    private DatabaseManager() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:rcp_gestion.db");
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            dataSource = new HikariDataSource(config);
            crearTablas();
            crearUsuarioAdminPorDefecto();
            sincronizarUsuariosConEmpleados();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public synchronized Connection getConnection() {
        try {
            if (sharedConnection == null || sharedConnection.isClosed()) {
                sharedConnection = dataSource.getConnection();
            }
            return sharedConnection;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void crearTablas() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS clientes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nombre TEXT, email TEXT, telefono TEXT, ciudad TEXT, estado TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS empleados (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nombre TEXT, apellido TEXT, email TEXT, telefono TEXT, puesto TEXT, salario REAL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS pedidos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "numero_pedido TEXT, cliente_id INTEGER, fecha_pedido TEXT, " +
                    "total REAL, estado TEXT, " +
                    "FOREIGN KEY(cliente_id) REFERENCES clientes(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS usuarios (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE, password_hash TEXT, email TEXT UNIQUE, " +
                    "rol TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS codigos_recuperacion (" +
                    "email TEXT, codigo TEXT, expiracion DATETIME)");

            stmt.execute("CREATE TABLE IF NOT EXISTS logs_actividad (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "usuario TEXT, accion TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS sync_queue (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "tabla TEXT, " +
                    "operacion TEXT, " +
                    "registro_id INTEGER, " +
                    "datos TEXT, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registrarActividad(String usuario, String accion) {
        String sql = "INSERT INTO logs_actividad (usuario, accion) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario != null && !usuario.isEmpty() ? usuario : "Sistema/Anónimo");
            ps.setString(2, accion);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error al registrar actividad en auditoría: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void crearUsuarioAdminPorDefecto() {
        String sql = "INSERT OR IGNORE INTO usuarios (username, password_hash, email, rol) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "admin");
                String hash = org.mindrot.jbcrypt.BCrypt.hashpw("admin123", org.mindrot.jbcrypt.BCrypt.gensalt());
                ps.setString(2, hash);
                ps.setString(3, "admin@empresa.com");
                ps.setString(4, "Administrador");
                ps.executeUpdate();
            }

            // Verificar e insertar admin en empleados
            String checkSql = "SELECT COUNT(*) FROM empleados WHERE nombre = 'admin'";
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(checkSql)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String empSql = "INSERT INTO empleados (nombre, apellido, email, telefono, puesto, salario) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement empPs = conn.prepareStatement(empSql)) {
                        empPs.setString(1, "admin");
                        empPs.setString(2, "Usuario");
                        empPs.setString(3, "admin@empresa.com");
                        empPs.setString(4, "");
                        empPs.setString(5, "Administrador");
                        empPs.setDouble(6, 0.0);
                        empPs.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sincronizarUsuariosConEmpleados() {
        String sqlUsuarios = "SELECT username, email, rol FROM usuarios";
        String sqlCheck = "SELECT COUNT(*) FROM empleados WHERE LOWER(nombre) = LOWER(?) OR LOWER(email) = LOWER(?)";
        String sqlInsert = "INSERT INTO empleados (nombre, apellido, email, telefono, puesto, salario) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sqlUsuarios)) {
             
            while (rs.next()) {
                String username = rs.getString("username");
                String email = rs.getString("email");
                String rol = rs.getString("rol");
                
                try (PreparedStatement checkPs = conn.prepareStatement(sqlCheck)) {
                    checkPs.setString(1, username);
                    checkPs.setString(2, email);
                    try (ResultSet checkRs = checkPs.executeQuery()) {
                        if (checkRs.next() && checkRs.getInt(1) == 0) {
                            try (PreparedStatement insertPs = conn.prepareStatement(sqlInsert)) {
                                insertPs.setString(1, username);
                                insertPs.setString(2, "Usuario");
                                insertPs.setString(3, email);
                                insertPs.setString(4, "");
                                insertPs.setString(5, rol);
                                insertPs.setDouble(6, 0.0);
                                insertPs.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public KPIs getKPIs() {
        int clientes = 0, pedidos = 0;
        double ventas = 0.0;
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM clientes WHERE estado='Activo'");
            if (rs.next())
                clientes = rs.getInt(1);

            rs = st.executeQuery("SELECT COUNT(*) FROM pedidos WHERE estado='Pendiente'");
            if (rs.next())
                pedidos = rs.getInt(1);

            rs = st.executeQuery("SELECT SUM(total) FROM pedidos WHERE estado='Completado'");
            if (rs.next())
                ventas = rs.getDouble(1);
        } catch (Exception e) {
            System.err.println("Error obteniendo KPIs: " + e.getMessage());
            e.printStackTrace();
        }
        return new KPIs(clientes, pedidos, ventas);
    }

    public record KPIs(int clientesActivos, int pedidosPendientes, double ingresosAnio) {
    }

    public Map<String, Double> getVentasUltimosMeses(int meses) {
        Map<String, Double> ventas = new LinkedHashMap<>();
        String sql = "SELECT strftime('%Y-%m', fecha_pedido) as mes, SUM(total) as total_ventas " +
                "FROM pedidos WHERE estado = 'Completado' " +
                "GROUP BY mes ORDER BY mes ASC LIMIT ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, meses);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getString("mes") != null) {
                    ventas.put(rs.getString("mes"), rs.getDouble("total_ventas"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ventas;
    }

    public Map<String, Integer> getProductosMasVendidos(int limite) {
        Map<String, Integer> productos = new LinkedHashMap<>();
        // Datos de ejemplo para el gráfico hasta que se implemente la tabla de
        // detalle_pedidos
        productos.put("Servicio Básico", 120);
        productos.put("Licencia Pro", 85);
        productos.put("Soporte Premium", 50);
        productos.put("Consultoría", 30);
        return productos;
    }
}