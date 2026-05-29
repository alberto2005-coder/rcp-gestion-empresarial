package com.empresa.rcp.module.dashboard;

import com.empresa.rcp.db.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML
    private Label lblEmpleados;
    @FXML
    private Label lblClientes;
    @FXML
    private Label lblPedidos;
    @FXML
    private Label lblVentas;

    @FXML
    private BarChart<String, Number> barChart;
    @FXML
    private PieChart pieChart;
    @FXML
    private LineChart<String, Number> lineChart;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            DatabaseManager.KPIs kpis = DatabaseManager.getInstance().getKPIs();

            int clientesCount = kpis.clientesActivos();
            int pedidosCount = kpis.pedidosPendientes();
            double ventasValue = kpis.ingresosAnio();
            int empleadosCount = 0;

            Connection conn = DatabaseManager.getInstance().getConnection();

            // 1. Obtener empleados reales
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM empleados")) {
                if (rs.next())
                    empleadosCount = rs.getInt(1);
            }

            // Animar todos los KPIs progresivamente
            animarConteo(lblClientes, clientesCount, false);
            animarConteo(lblPedidos, pedidosCount, false);
            animarConteo(lblVentas, ventasValue, true);
            animarConteo(lblEmpleados, empleadosCount, false);

            // 2. Gráfico Circular: Estado de Pedidos
            if (pieChart != null) {
                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                try (Statement st = conn.createStatement();
                        ResultSet rs = st.executeQuery("SELECT estado, COUNT(*) FROM pedidos GROUP BY estado")) {
                    while (rs.next())
                        pieData.add(new PieChart.Data(rs.getString(1), rs.getInt(2)));
                }
                pieChart.setData(pieData);
            }

            // 3. Gráficos de barras y líneas: Ventas Mensuales
            Map<String, Double> ventas = DatabaseManager.getInstance().getVentasUltimosMeses(6);

            if (barChart != null) {
                XYChart.Series<String, Number> seriesBar = new XYChart.Series<>();
                seriesBar.setName("Ventas Mensuales");
                ventas.forEach((mes, total) -> seriesBar.getData().add(new XYChart.Data<>(mes, total)));
                barChart.getData().add(seriesBar);
            }

            if (lineChart != null) {
                XYChart.Series<String, Number> seriesLine = new XYChart.Series<>();
                seriesLine.setName("Tendencia");
                ventas.forEach((mes, total) -> seriesLine.getData().add(new XYChart.Data<>(mes, total)));
                lineChart.getData().add(seriesLine);
            }

        } catch (Exception e) {
            System.err.println("Error cargando dashboard: " + e.getMessage());
        }
    }

    private void animarConteo(Label label, double valorFinal, boolean esMoneda) {
        if (label == null) return;

        // 1 segundo de duración (50 frames con 20ms de separación)
        int totalFrames = 50;
        int frameDurationMs = 20;

        javafx.animation.Timeline timeline = new javafx.animation.Timeline();
        for (int i = 0; i <= totalFrames; i++) {
            final double progreso = (double) i / totalFrames;
            final double valorActual = valorFinal * progreso;

            javafx.animation.KeyFrame keyFrame = new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(i * frameDurationMs),
                event -> {
                    if (esMoneda) {
                        label.setText(String.format("$%.2f", valorActual));
                    } else {
                        label.setText(String.valueOf((int) Math.round(valorActual)));
                    }
                }
            );
            timeline.getKeyFrames().add(keyFrame);
        }
        timeline.play();
    }
}