package com.empresa.rcp.chart;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom dark-themed bar chart drawn with JavaFX Canvas.
 */
public class BarChartCanvas extends Pane {

    private final Canvas canvas = new Canvas();
    private final Map<String, Double> data = new LinkedHashMap<>();
    private final DoubleProperty animProgress = new SimpleDoubleProperty(0);
    private Double mouseX = null;
    private Double mouseY = null;

    public BarChartCanvas() {
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);
        canvas.widthProperty().addListener(e -> draw());
        canvas.heightProperty().addListener(e -> draw());
        animProgress.addListener(e -> draw());

        canvas.setOnMouseMoved(e -> handleMouseMoved(e.getX(), e.getY()));
        canvas.setOnMouseExited(e -> handleMouseExited());
    }

    private void handleMouseMoved(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
        draw();
    }

    private void handleMouseExited() {
        this.mouseX = null;
        this.mouseY = null;
        draw();
    }

    public void setData(Map<String, Double> newData) {
        data.clear();
        data.putAll(newData);
        // Animate bars growing up
        animProgress.set(0);
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(animProgress, 0)),
            new KeyFrame(Duration.millis(900), new KeyValue(animProgress, 1))
        );
        tl.play();
    }

    private void draw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        boolean isDark = com.empresa.rcp.MainController.isDark();
        Color gridColor = isDark ? Color.web("#334155") : Color.web("#e2e8f0");
        Color axisTextColor = isDark ? Color.web("#94a3b8") : Color.web("#475569");
        Color valueTextColor = isDark ? Color.web("#93c5fd") : Color.web("#2563eb");
        Color barColorFrom = isDark ? Color.web("#4f46e5") : Color.web("#6366f1");
        Color barColorTo = isDark ? Color.web("#818cf8") : Color.web("#4f46e5");
        Color emptyTextColor = isDark ? Color.web("#64748b") : Color.web("#94a3b8");

        if (w <= 0 || h <= 0 || data.isEmpty()) {
            gc.setFill(emptyTextColor);
            gc.setFont(Font.font("Segoe UI", 13));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText("Sin datos disponibles", w / 2, h / 2);
            return;
        }

        double padL = 52, padR = 16, padT = 20, padB = 40;
        double chartW = w - padL - padR;
        double chartH = h - padT - padB;

        double maxVal = data.values().stream().mapToDouble(d -> d).max().orElse(1);

        // Grid lines (5)
        gc.setStroke(gridColor);
        gc.setLineWidth(0.5);
        gc.setFont(Font.font("Segoe UI", 10));
        gc.setFill(axisTextColor);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setTextBaseline(VPos.CENTER);
        for (int i = 0; i <= 5; i++) {
            double y = padT + chartH - (chartH * i / 5.0);
            gc.strokeLine(padL, y, padL + chartW, y);
            double val = maxVal * i / 5.0;
            gc.fillText(formatVal(val), padL - 6, y);
        }

        // Bars
        String[] keys = data.keySet().toArray(new String[0]);
        double barGroupW = chartW / keys.length;
        double barW = Math.min(barGroupW * 0.55, 40);

        for (int i = 0; i < keys.length; i++) {
            double val = data.get(keys[i]);
            double barH = (val / maxVal) * chartH * animProgress.get();
            double x = padL + i * barGroupW + (barGroupW - barW) / 2.0;
            double y = padT + chartH - barH;

            // Gradient bar
            LinearGradient grad = new LinearGradient(
                x, y, x, y + barH, false, CycleMethod.NO_CYCLE,
                new Stop(0, barColorTo), new Stop(1, barColorFrom)
            );
            gc.setFill(grad);

            // Rounded top corners
            gc.fillRoundRect(x, y, barW, barH, 5, 5);

            // Value label above bar
            if (animProgress.get() > 0.85) {
                gc.setFill(valueTextColor);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFont(Font.font("Segoe UI", 9));
                gc.fillText(formatVal(val), x + barW / 2, y - 5);
            }

            // Category label below
            gc.setFill(axisTextColor);
            gc.setFont(Font.font("Segoe UI", 10));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(keys[i], x + barW / 2, padT + chartH + 14);
        }

        // X-axis line
        gc.setStroke(gridColor);
        gc.setLineWidth(1);
        gc.strokeLine(padL, padT + chartH, padL + chartW, padT + chartH);

        // Draw Interactive Tooltip
        if (mouseX != null && mouseY != null) {
            for (int i = 0; i < keys.length; i++) {
                double val = data.get(keys[i]);
                double barH = (val / maxVal) * chartH * animProgress.get();
                double x = padL + i * barGroupW + (barGroupW - barW) / 2.0;
                double y = padT + chartH - barH;
                
                if (mouseX >= x - 5 && mouseX <= x + barW + 5) {
                    // Highlight bar
                    gc.setStroke(Color.web("#6366f1"));
                    gc.setLineWidth(1.5);
                    gc.strokeRoundRect(x - 2, y - 2, barW + 4, barH + 2, 6, 6);
                    
                    double boxW = 120;
                    double boxH = 45;
                    double boxX = mouseX + 15;
                    double boxY = mouseY - 15;
                    
                    if (boxX + boxW > w) boxX = mouseX - boxW - 15;
                    if (boxY + boxH > h) boxY = h - boxH - 10;
                    if (boxY < 5) boxY = 5;
                    
                    gc.setFill(Color.color(0, 0, 0, 0.8));
                    gc.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
                    gc.setStroke(Color.web("#6366f1"));
                    gc.setLineWidth(1);
                    gc.strokeRoundRect(boxX, boxY, boxW, boxH, 8, 8);
                    
                    gc.setTextAlign(TextAlignment.LEFT);
                    gc.setTextBaseline(VPos.TOP);
                    gc.setFont(Font.font("Segoe UI", 10));
                    
                    gc.setFill(Color.WHITE);
                    gc.fillText(keys[i], boxX + 10, boxY + 8);
                    
                    gc.setFill(Color.web("#cbd5e1"));
                    gc.fillText("Total: " + formatVal(val), boxX + 10, boxY + 23);
                    break;
                }
            }
        }
    }

    private String formatVal(double v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (v >= 1_000)     return String.format("%.0fk", v / 1_000);
        return String.format("%.0f", v);
    }
}
