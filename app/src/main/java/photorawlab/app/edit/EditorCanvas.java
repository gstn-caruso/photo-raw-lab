package photorawlab.app.edit;

import photorawlab.domain.edit.*;
import photorawlab.domain.edit.Point;
import photorawlab.engine.FloatDevelopmentEngine.Rendered;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

public final class EditorCanvas extends JPanel {
    public interface Gestures {
        void stroke(List<Point> points);

        void crop(double x, double y, double w, double h);

        void whiteBalance(double u, double v);
    }

    private BufferedImage image;
    private Rendered rendered;
    private double zoom = 1, panX, panY;
    private boolean fit = true;
    private String tool = "Desplazar";
    private final List<Point> stroke = new ArrayList<>();
    private final List<java.awt.Point> screenStroke = new ArrayList<>();
    private java.awt.Point start, last, cursor;
    private boolean panning;
    private double radius = .04;
    private final Gestures gestures;

    public EditorCanvas(Gestures gestures) {
        this.gestures = gestures;
        setBackground(new Color(35, 35, 35));
        setFocusable(true);
        MouseAdapter mouse =
                new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        requestFocusInWindow();
                        start = last = e.getPoint();
                        panning = SwingUtilities.isMiddleMouseButton(e) || tool.equals("Desplazar");
                        stroke.clear();
                        screenStroke.clear();
                        if (!panning) add(e.getPoint());
                    }

                    public void mouseDragged(MouseEvent e) {
                        if (image == null) return;
                        if (panning) {
                            panX += e.getX() - last.x;
                            panY += e.getY() - last.y;
                            last = e.getPoint();
                        } else add(e.getPoint());
                        cursor = e.getPoint();
                        repaint();
                    }

                    public void mouseReleased(MouseEvent e) {
                        if (image == null || start == null) return;
                        if (!panning && tool.equals("Recortar")) {
                            Point a = normalized(start), b = normalized(e.getPoint());
                            double x = Math.max(0, Math.min(a.x(), b.x())),
                                    y = Math.max(0, Math.min(a.y(), b.y()));
                            double w = Math.min(1, Math.max(a.x(), b.x())) - x,
                                    h = Math.min(1, Math.max(a.y(), b.y())) - y;
                            if (w > .01 && h > .01) gestures.crop(x, y, w, h);
                        } else if (!panning && tool.equals("Balance de blancos")) {
                            Point p = normalized(e.getPoint());
                            gestures.whiteBalance(p.x(), p.y());
                        } else if (!panning && !stroke.isEmpty())
                            gestures.stroke(List.copyOf(stroke));
                        start = null;
                        stroke.clear();
                        screenStroke.clear();
                        repaint();
                    }

                    public void mouseMoved(MouseEvent e) {
                        cursor = e.getPoint();
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        cursor = null;
                        repaint();
                    }

                    public void mouseWheelMoved(MouseWheelEvent e) {
                        if (image == null) return;
                        double old = scale();
                        Point p = normalized(e.getPoint());
                        fit = false;
                        zoom =
                                Math.max(
                                        .05,
                                        Math.min(
                                                20,
                                                old
                                                        * Math.pow(
                                                                1.15,
                                                                -e.getPreciseWheelRotation())));
                        panX = e.getX() - getWidth() / 2.0 - (p.x() - .5) * image.getWidth() * zoom;
                        panY =
                                e.getY()
                                        - getHeight() / 2.0
                                        - (p.y() - .5) * image.getHeight() * zoom;
                        repaint();
                    }
                };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public void setRadius(double value) {
        radius = value;
        repaint();
    }

    public void showImage(BufferedImage image, Rendered rendered) {
        if (!fit && this.image != null && image != null)
            zoom *= (double) this.image.getWidth() / image.getWidth();
        this.image = image;
        this.rendered = rendered;
        repaint();
    }

    public void fit() {
        fit = true;
        panX = panY = 0;
        repaint();
    }

    public void actualPixels() {
        if (rendered == null) return;
        fit = false;
        zoom = rendered.pixelScale();
        panX = panY = 0;
        repaint();
    }

    private double scale() {
        return image == null
                ? 1
                : fit
                        ? Math.min(
                                (double) getWidth() / image.getWidth(),
                                (double) getHeight() / image.getHeight())
                        : zoom;
    }

    private double left() {
        return (getWidth() - image.getWidth() * scale()) / 2 + panX;
    }

    private double top() {
        return (getHeight() - image.getHeight() * scale()) / 2 + panY;
    }

    private Point normalized(java.awt.Point p) {
        return new Point(
                (p.x - left()) / (image.getWidth() * scale()),
                (p.y - top()) / (image.getHeight() * scale()));
    }

    private void add(java.awt.Point screen) {
        if (image == null || rendered == null) return;
        Point p = normalized(screen);
        if (p.x() < 0 || p.x() > 1 || p.y() < 0 || p.y() > 1) return;
        Point original = rendered.originalAt(p.x(), p.y());
        if (stroke.isEmpty()
                || Math.hypot(
                                original.x() - stroke.getLast().x(),
                                original.y() - stroke.getLast().y())
                        > .001) {
            stroke.add(original);
            screenStroke.add(screen);
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (image == null) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(
                    image,
                    (int) left(),
                    (int) top(),
                    (int) (image.getWidth() * scale()),
                    (int) (image.getHeight() * scale()),
                    null);
            g.setColor(new Color(255, 110, 80, 180));
            g.setStroke(new BasicStroke(2));
            for (int i = 1; i < screenStroke.size(); i++)
                g.drawLine(
                        screenStroke.get(i - 1).x,
                        screenStroke.get(i - 1).y,
                        screenStroke.get(i).x,
                        screenStroke.get(i).y);
            if (start != null && tool.equals("Recortar") && cursor != null)
                g.drawRect(
                        Math.min(start.x, cursor.x),
                        Math.min(start.y, cursor.y),
                        Math.abs(start.x - cursor.x),
                        Math.abs(start.y - cursor.y));
            if (cursor != null && (tool.equals("Pincel") || tool.equals("Borrador"))) {
                double r = radius * rendered.originalHeight() / rendered.pixelScale() * scale();
                g.setColor(Color.WHITE);
                g.drawOval(
                        (int) (cursor.x - r), (int) (cursor.y - r), (int) (2 * r), (int) (2 * r));
            }
        } finally {
            g.dispose();
        }
    }
}
