package com.empresa.rcp.module.reportes;

import com.empresa.rcp.chart.BarChartCanvas;
import com.empresa.rcp.chart.LineChartCanvas;
import com.empresa.rcp.db.DatabaseManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;

import java.io.FileOutputStream;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class ReportesController implements Initializable {

    @FXML
    private DatePicker dpDesde;
    @FXML
    private DatePicker dpHasta;
    @FXML
    private ComboBox<String> cbTipo;
    @FXML
    private Pane paneAreaChart;
    @FXML
    private Pane paneBarChart;
    @FXML
    private Label lblResultado;
    @FXML
    private Label lblTotal;
    @FXML
    private Label lblPromedio;
    @FXML
    private Label lblMaximo;

    private LineChartCanvas lineChart;
    private BarChartCanvas barChart;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpDesde.setValue(LocalDate.now().minusMonths(6));
        dpHasta.setValue(LocalDate.now());

        cbTipo.getItems().addAll("Ventas Mensuales", "Pedidos por Estado", "Clientes por Ciudad");
        cbTipo.setValue("Ventas Mensuales");

        lineChart = new LineChartCanvas();
        lineChart.prefWidthProperty().bind(paneAreaChart.widthProperty());
        lineChart.prefHeightProperty().bind(paneAreaChart.heightProperty());
        paneAreaChart.getChildren().add(lineChart);

        barChart = new BarChartCanvas();
        barChart.prefWidthProperty().bind(paneBarChart.widthProperty());
        barChart.prefHeightProperty().bind(paneBarChart.heightProperty());
        paneBarChart.getChildren().add(barChart);

        // Actualizar automáticamente las gráficas cuando se cambie el selector o las
        // fechas
        cbTipo.valueProperty().addListener((obs, oldVal, newVal) -> generarReporte());
        dpDesde.valueProperty().addListener((obs, oldVal, newVal) -> generarReporte());
        dpHasta.valueProperty().addListener((obs, oldVal, newVal) -> generarReporte());

        generarReporte();
    }

    @FXML
    private void generarReporte() {
        String tipo = cbTipo.getValue();
        if (tipo == null)
            return;

        switch (tipo) {
            case "Ventas Mensuales" -> reporteVentas();
            case "Pedidos por Estado" -> reportePedidos();
            case "Clientes por Ciudad" -> reporteCiudades();
        }
        lblResultado.setText("Reporte generado: " + tipo +
                " | " + dpDesde.getValue() + " → " + dpHasta.getValue());
    }

    // ── Ventas mensuales desde SQLite ──────────────────────────────────────
    private void reporteVentas() {
        try {
            Map<String, Double> ventas = DatabaseManager.getInstance().getVentasUltimosMeses(12);
            if (ventas.isEmpty()) {
                lblTotal.setText("Sin datos");
                lblPromedio.setText("—");
                lblMaximo.setText("—");
                return;
            }

            String[] meses = ventas.keySet().toArray(new String[0]);
            double[] vals = ventas.values().stream().mapToDouble(d -> d).toArray();
            double total = 0, max = 0;
            for (double v : vals) {
                total += v;
                if (v > max)
                    max = v;
            }
            double media = total / vals.length;
            double[] obj = new double[vals.length];
            for (int i = 0; i < obj.length; i++)
                obj[i] = media * 1.1;

            lineChart.setData(meses,
                    new LineChartCanvas.Series("Ventas", vals, Color.web("#3b82f6"), false),
                    new LineChartCanvas.Series("Objetivo", obj, Color.web("#64748b"), true));

            // Bar chart: mismos datos
            Map<String, Double> barData = new LinkedHashMap<>(ventas);
            barChart.setData(barData);

            lblTotal.setText(String.format("%,.0f €", total));
            lblPromedio.setText(String.format("%,.0f €", media));
            lblMaximo.setText(String.format("%,.0f €", max));

        } catch (Exception e) {
            lblTotal.setText("Error BD");
            e.printStackTrace();
        }
    }

    // ── Pedidos por estado ─────────────────────────────────────────────────
    private void reportePedidos() {
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT estado, COUNT(*) as cnt, COALESCE(SUM(total),0) as total FROM pedidos GROUP BY estado")) {

            Map<String, Double> barData = new LinkedHashMap<>();
            double total = 0, max = 0;
            while (rs.next()) {
                double cnt = rs.getDouble("cnt");
                barData.put(rs.getString("estado"), cnt);
                total += cnt;
                if (cnt > max)
                    max = cnt;
            }
            if (barData.isEmpty()) {
                lblTotal.setText("Sin pedidos");
                return;
            }

            String[] meses = barData.keySet().toArray(new String[0]);
            double[] vals = barData.values().stream().mapToDouble(d -> d).toArray();
            double[] obj = new double[vals.length];
            for (int i = 0; i < obj.length; i++)
                obj[i] = total / vals.length;

            lineChart.setData(meses,
                    new LineChartCanvas.Series("Pedidos", vals, Color.web("#f59e0b"), false),
                    new LineChartCanvas.Series("Promedio", obj, Color.web("#64748b"), true));
            barChart.setData(barData);

            lblTotal.setText(String.format("%.0f pedidos", total));
            lblPromedio.setText(String.format("%.1f / estado", total / barData.size()));
            lblMaximo.setText(String.format("%.0f (máximo)", max));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── Clientes por ciudad ────────────────────────────────────────────────
    private void reporteCiudades() {
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT ciudad, COUNT(*) as cnt FROM clientes GROUP BY ciudad ORDER BY cnt DESC")) {

            Map<String, Double> barData = new LinkedHashMap<>();
            double total = 0, max = 0;
            while (rs.next()) {
                double cnt = rs.getDouble("cnt");
                barData.put(rs.getString("ciudad"), cnt);
                total += cnt;
                if (cnt > max)
                    max = cnt;
            }
            if (barData.isEmpty()) {
                lblTotal.setText("Sin clientes");
                return;
            }

            String[] ciudades = barData.keySet().toArray(new String[0]);
            double[] vals = barData.values().stream().mapToDouble(d -> d).toArray();
            lineChart.setData(ciudades,
                    new LineChartCanvas.Series("Clientes", vals, Color.web("#10b981"), false));
            barChart.setData(barData);

            lblTotal.setText(String.format("%.0f clientes", total));
            lblPromedio.setText(String.format("%.1f / ciudad", total / barData.size()));
            lblMaximo.setText(String.format("%.0f (máximo)", max));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void exportarCSV() {
        String tipo = cbTipo.getValue();
        if (tipo == null) tipo = "General";
        String filename = "reporte_" + tipo.toLowerCase().replace(" ", "_") + "_" + LocalDate.now() + ".csv";
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new FileOutputStream(filename))) {
            writer.println("Concepto,Valor");
            writer.println("Reporte," + tipo);
            writer.println("Fecha de Generacion," + LocalDate.now());
            writer.println("Periodo," + dpDesde.getValue() + " -> " + dpHasta.getValue());
            writer.println("Total," + lblTotal.getText());
            writer.println("Promedio," + lblPromedio.getText());
            writer.println("Maximo," + lblMaximo.getText());
            
            DatabaseManager.getInstance().registrarActividad(com.empresa.rcp.MainController.getActiveUser(), "Exportó reporte a CSV (" + filename + ")");

            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "✅ Reporte CSV generado correctamente en el archivo:\n" + filename, ButtonType.OK);
            alert.setHeaderText("Exportación Exitosa");
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "❌ Error al exportar CSV: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void exportarPDF() {
        String tipo = cbTipo.getValue();
        if (tipo == null) tipo = "General";
        
        String filename = "reporte_" + tipo.toLowerCase().replace(" ", "_") + "_" + LocalDate.now() + ".pdf";
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filename));
            document.open();
            
            // Document Title
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
            document.add(new Paragraph("RCP GESTION EMPRESARIAL - REPORTE", titleFont));
            document.add(new Paragraph("Tipo de Reporte: " + tipo));
            document.add(new Paragraph("Fecha de Generacion: " + LocalDate.now()));
            document.add(new Paragraph("Periodo: " + dpDesde.getValue() + " a " + dpHasta.getValue()));
            document.add(new Paragraph(" ")); // Spacer
            
            // Add KPI summary
            document.add(new Paragraph("Resumen de Metricas:"));
            document.add(new Paragraph(" - Total: " + lblTotal.getText()));
            document.add(new Paragraph(" - Promedio Mensual: " + lblPromedio.getText()));
            document.add(new Paragraph(" - Maximo: " + lblMaximo.getText()));
            document.add(new Paragraph(" ")); // Spacer
            
            // Add a data table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            
            // Headers
            PdfPCell cell1 = new PdfPCell(new Paragraph("Concepto / Mes / Ciudad"));
            PdfPCell cell2 = new PdfPCell(new Paragraph("Metrica / Valor"));
            table.addCell(cell1);
            table.addCell(cell2);
            
            // Fetch data based on the type
            if ("Ventas Mensuales".equals(tipo)) {
                Map<String, Double> ventas = DatabaseManager.getInstance().getVentasUltimosMeses(12);
                ventas.forEach((mes, valor) -> {
                    table.addCell(mes);
                    table.addCell(String.format("%,.2f €", valor));
                });
            } else {
                table.addCell("Detalles adicionales");
                table.addCell("Ver panel interactivo en la aplicacion");
            }
            
            document.add(table);
            document.close();
            
            DatabaseManager.getInstance().registrarActividad(com.empresa.rcp.MainController.getActiveUser(), "Exportó reporte a PDF (" + filename + ")");

            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "✅ Reporte PDF generado correctamente en el archivo:\n" + filename, ButtonType.OK);
            alert.setHeaderText("Exportación Exitosa");
            alert.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "❌ Error al exportar PDF: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }
}
