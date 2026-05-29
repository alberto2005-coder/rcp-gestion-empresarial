package com.empresa.rcp.chart;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom dark-themed pie chart drawn with JavaFX Canvas.
 */
public class PieChartCanvas extends Pane {

    public record Slice(String label, double value, Color color) {}

    private final Canvas canvas = new Canvas();
    private final List<Slice> slices = new ArrayList<>();
    private final DoubleProperty animProgress = new SimpleDoubleProperty(0);

    private static final Color[] PALETTE = {
        Color.web("#3b82f6"),  // blue
        Color.web("#10b981"),  // emerald
        Color.web("#f59e0b"),  // amber
        Color.web("#8b5cf6"),  // violet
        Color.web("#ef4444"),  // red
        Color.web("#06b6d4"),  // cyan
    };

    public PieChartCanvas() {
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);
        canvas.widthProperty().addListener(e -> draw());
        canvas.heightProperty().addListener(e -> draw());
        animProgress.addListener(e -> draw());
    }

    public void setData(List<Slice> newSlices) {
        slices.clear();
        slices.addAll(newSlices);
        animProgress.set(0);
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(animProgress, 0)),
            new KeyFrame(Duration.millis(800), new KeyValue(animProgress, 1))
        );
        tl.play();
    }

    private void draw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0 || slices.isEmpty()) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double total = slices.stream().mapToDouble(Slice::value).sum();
        double legendH = 28;
        double diameter = Math.min(w * 0.55, h - legendH - 10);
        double cx = w * 0.35;
        double cy = (h - legendH) / 2.0;
        double r = diameter / 2.0;

        double startAngle = -90;
        double maxSweep = 360 * animProgress.get();

        for (int i = 0; i < slices.size(); i++) {
            Slice s = slices.get(i);
            double sweep = (s.value() / total) * 360;
            double actualSweep = Math.min(sweep, maxSweep);

            Color c = i < PALETTE.length ? PALETTE[i] : PALETTE[i % PALETTE.length];

            // Slice
            gc.setFill(c);
            gc.fillArc(cx - r, cy - r, diameter, diameter, startAngle, -actualSweep,
                javafx.scene.shape.ArcType.ROUND);

            // Inner hole (donut style)
            gc.setFill(Color.web("#0d1b2a"));
            double innerR = r * 0.55;
            gc.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

            // Percentage label if big enough
            if (sweep > 18 && animProgress.get() > 0.9) {
                double midAngle = Math.toRadians(startAngle - actualSweep / 2.0);
                double lx = cx + Math.cos(midAngle) * r * 0.78;
                double ly = cy + Math.sin(midAngle) * r * 0.78;
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Segoe UI", 10));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(String.format("%.0f%%", s.value() / total * 100), lx, ly + 4);
            }

            maxSweep -= sweep;
            if (maxSweep <= 0) break;
            startAngle -= sweep;
        }

        // Legend
        double lx = w * 0.6;
        double ly = 30;
        gc.setFont(Font.font("Segoe UI", 12));
        gc.setTextAlign(TextAlignment.LEFT);
        for (int i = 0; i < slices.size(); i++) {
            Slice s = slices.get(i);
            Color c = i < PALETTE.length ? PALETTE[i] : PALETTE[i % PALETTE.length];
            gc.setFill(c);
            gc.fillRoundRect(lx, ly - 9, 12, 12, 3, 3);
            gc.setFill(Color.web("#94a3b8"));
            gc.fillText(s.label(), lx + 18, ly);
            gc.setFill(Color.web("#f1f5f9"));
            gc.setFont(Font.font("Segoe UI", 11));
            gc.fillText(String.format("%.0f%%", s.value() / total * 100), lx + 18, ly + 14);
            gc.setFont(Font.font("Segoe UI", 12));
            ly += 40;
        }
    }
}
