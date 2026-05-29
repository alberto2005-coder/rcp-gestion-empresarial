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
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom dark-themed line/area chart drawn with JavaFX Canvas.
 */
public class LineChartCanvas extends Pane {

    public record Series(String name, double[] values, Color color, boolean dashed) {}

    private final Canvas canvas = new Canvas();
    private final List<Series> seriesList = new ArrayList<>();
    private String[] categories = {};
    private final DoubleProperty animProgress = new SimpleDoubleProperty(0);
    private Double mouseX = null;
    private Double mouseY = null;

    public LineChartCanvas() {
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

    public void setData(String[] categories, Series... series) {
        this.categories = categories;
        seriesList.clear();
        for (Series s : series) seriesList.add(s);
        animProgress.set(0);
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(animProgress, 0)),
            new KeyFrame(Duration.millis(1000), new KeyValue(animProgress, 1))
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
        Color emptyTextColor = isDark ? Color.web("#64748b") : Color.web("#94a3b8");

        if (w <= 0 || h <= 0 || seriesList.isEmpty() || categories.length == 0) {
            gc.setFill(emptyTextColor);
            gc.setFont(Font.font("Segoe UI", 13));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText("Sin datos disponibles", w / 2, h / 2);
            return;
        }

        double padL = 52, padR = 20, padT = 20, padB = 44;
        double chartW = w - padL - padR;
        double chartH = h - padT - padB;

        double maxVal = seriesList.stream()
            .flatMapToDouble(s -> { double[] vals = new double[s.values().length]; System.arraycopy(s.values(), 0, vals, 0, vals.length); return java.util.Arrays.stream(vals); })
            .max().orElse(1) * 1.1;

        // Grid lines
        gc.setFont(Font.font("Segoe UI", 10));
        gc.setFill(axisTextColor);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setTextBaseline(VPos.CENTER);
        for (int i = 0; i <= 4; i++) {
            double y = padT + chartH - (chartH * i / 4.0);
            gc.setStroke(gridColor);
            gc.setLineWidth(0.5);
            gc.strokeLine(padL, y, padL + chartW, y);
            gc.fillText(formatVal(maxVal * i / 4.0), padL - 6, y);
        }

        // Draw each series
        int n = categories.length;
        double progress = animProgress.get();

        for (Series s : seriesList) {
            double[] pts = s.values();
            int drawCount = Math.max(1, (int) Math.ceil(n * progress));

            double[] xs = new double[drawCount];
            double[] ys = new double[drawCount];
            for (int i = 0; i < drawCount; i++) {
                xs[i] = (n > 1) ? (padL + (i / (double)(n - 1)) * chartW) : (padL + chartW / 2.0);
                ys[i] = padT + chartH - (pts[i] / maxVal) * chartH;
            }

            // Area fill (only for non-dashed)
            if (!s.dashed() && drawCount > 1) {
                gc.beginPath();
                gc.moveTo(xs[0], padT + chartH);
                gc.lineTo(xs[0], ys[0]);
                for (int i = 1; i < drawCount; i++) gc.lineTo(xs[i], ys[i]);
                gc.lineTo(xs[drawCount-1], padT + chartH);
                gc.closePath();
                Color c = s.color();
                gc.setFill(new LinearGradient(0, padT, 0, padT + chartH, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.25)),
                    new Stop(1, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.01))
                ));
                gc.fill();
            }

            // Line
            gc.setStroke(s.color());
            gc.setLineWidth(s.dashed() ? 1.5 : 2.5);
            if (s.dashed()) gc.setLineDashes(6, 4);
            else gc.setLineDashes();
            gc.beginPath();
            gc.moveTo(xs[0], ys[0]);
            for (int i = 1; i < drawCount; i++) gc.lineTo(xs[i], ys[i]);
            gc.stroke();
            gc.setLineDashes();

            // Dots
            gc.setFill(s.color());
            for (int i = 0; i < drawCount; i++) {
                gc.fillOval(xs[i] - 3.5, ys[i] - 3.5, 7, 7);
            }
        }

        // Category labels
        gc.setFill(axisTextColor);
        gc.setFont(Font.font("Segoe UI", 10));
        gc.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < n; i++) {
            double x = (n > 1) ? (padL + (i / (double)(n - 1)) * chartW) : (padL + chartW / 2.0);
            gc.fillText(categories[i], x, padT + chartH + 16);
        }

        // Legend
        double lx = padL;
        double ly = padT + chartH + 30;
        gc.setFont(Font.font("Segoe UI", 10));
        gc.setTextAlign(TextAlignment.LEFT);
        for (Series s : seriesList) {
            gc.setFill(s.color());
            gc.fillRect(lx, ly - 5, 16, 3);
            gc.fillText(s.name(), lx + 20, ly);
            lx += 110;
        }

        // Draw Interactive Tooltip on hover
        if (mouseX != null && n > 0) {
            int closestIdx = 0;
            if (n > 1) {
                closestIdx = (int) Math.round((mouseX - padL) / (chartW / (double)(n - 1)));
                if (closestIdx < 0) closestIdx = 0;
                if (closestIdx >= n) closestIdx = n - 1;
            }
            
            double cx = (n > 1) ? (padL + (closestIdx / (double)(n - 1)) * chartW) : (padL + chartW / 2.0);
            
            if (Math.abs(mouseX - cx) < 20) {
                // Highlight vertical line
                gc.setStroke(Color.web(isDark ? "#475569" : "#cbd5e1"));
                gc.setLineWidth(1);
                gc.setLineDashes(4, 4);
                gc.strokeLine(cx, padT, cx, padT + chartH);
                gc.setLineDashes();
                
                // Show tooltip box
                List<String> lines = new ArrayList<>();
                lines.add(categories[closestIdx]);
                for (Series s : seriesList) {
                    if (closestIdx < s.values().length) {
                        lines.add(s.name() + ": " + formatVal(s.values()[closestIdx]));
                    }
                }
                
                // Calculate dimensions
                double boxW = 120;
                double boxH = 15 + lines.size() * 15;
                double boxX = mouseX + 15;
                double boxY = mouseY - 15;
                
                // Adjust if box goes off screen
                if (boxX + boxW > w) boxX = mouseX - boxW - 15;
                if (boxY + boxH > h) boxY = h - boxH - 10;
                if (boxY < 5) boxY = 5;
                
                // Draw background
                gc.setFill(Color.color(0, 0, 0, 0.8));
                gc.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
                gc.setStroke(Color.web("#6366f1"));
                gc.setLineWidth(1);
                gc.strokeRoundRect(boxX, boxY, boxW, boxH, 8, 8);
                
                // Draw text
                gc.setTextAlign(TextAlignment.LEFT);
                gc.setTextBaseline(VPos.TOP);
                gc.setFont(Font.font("Segoe UI", 10));
                
                gc.setFill(Color.WHITE);
                gc.fillText(lines.get(0), boxX + 10, boxY + 8);
                
                for (int idx = 1; idx < lines.size(); idx++) {
                    gc.setFill(Color.web("#cbd5e1"));
                    gc.fillText(lines.get(idx), boxX + 10, boxY + 8 + idx * 15);
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
