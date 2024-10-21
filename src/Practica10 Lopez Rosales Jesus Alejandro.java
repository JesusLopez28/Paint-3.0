import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

class Paint3 extends JFrame {
    private final DrawingPanel drawingPanel;

    public Paint3() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Paint 3.0");

        drawingPanel = new DrawingPanel(800, 600);
        add(drawingPanel);

        setupMenuBar();

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuFiguras = createMenu("Figuras",
                createMenuItem("Línea", "linea.png", _ -> drawingPanel.setCurrentShape("Línea")),
                createMenuItem("Triángulo", "triangulo.png", _ -> drawingPanel.setCurrentShape("Triángulo")),
                createMenuItem("Círculo/Ovalo", "circulo.png", _ -> drawingPanel.setCurrentShape("Círculo/Ovalo")),
                createMenuItem("Rectángulo/Cuadrado", "rectangulo.png", _ -> drawingPanel.setCurrentShape("Rectángulo/Cuadrado"))
        );

        JMenu menuFigurasRelleno = createMenu("Figuras con Relleno",
                createMenuItem("Triángulo Relleno", "trianguloR.png", _ -> drawingPanel.setCurrentShape("Triángulo Relleno")),
                createMenuItem("Círculo/Ovalo Relleno", "circuloR.png", _ -> drawingPanel.setCurrentShape("Círculo/Ovalo Relleno")),
                createMenuItem("Rectángulo/Cuadrado Relleno", "rectanguloR.png", _ -> drawingPanel.setCurrentShape("Rectángulo/Cuadrado Relleno"))
        );


        JMenu menuColores = createMenu("Colores",
                createMenuItem("Rojo", "rojo.png", _ -> drawingPanel.setCurrentColor(Color.RED)),
                createMenuItem("Verde", "verde.png", _ -> drawingPanel.setCurrentColor(Color.GREEN)),
                createMenuItem("Azul", "azul.png", _ -> drawingPanel.setCurrentColor(Color.BLUE))
        );

        JMenu menuLineas = createMenu("Tipos de Líneas",
                createMenuItem("Sólida", "linea.png", _ -> drawingPanel.setCurrentStroke(new BasicStroke(2))),
                createMenuItem("Punteada", "linea_punteada.png",
                        _ -> drawingPanel.setCurrentStroke(new BasicStroke(2, BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_MITER, 10, new float[]{10, 10}, 0))),
                createMenuItem("Discontinua", "linea_discontinua.png",
                        _ -> drawingPanel.setCurrentStroke(new BasicStroke(2, BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_MITER, 10, new float[]{20, 10, 5, 10}, 0)))
        );

        JMenu menuTransformaciones = createMenu("Transformaciones",
                createMenuItem("Traslación", "traslacion.png", _ -> drawingPanel.setTranslateMode(true)),
                createMenuItem("Escalado", "escalado.png", _ -> drawingPanel.setEscaleMode(true)),
                createMenuItem("Rotación", "rotacion.png", _ -> drawingPanel.setRotateMode(true)),
                createMenuItem("Sesgado", "sesgado.png", _ -> drawingPanel.setSesgadoMode(true))
        );

        JMenu menuBorrar = createMenu("Borrar",
                createMenuItem("Borrar Pantalla", "borrar_pantalla.png", _ -> drawingPanel.clearCanvas()),
                createMenuItem("Borrar Figura", "borrar_figura.png", _ -> drawingPanel.enableEraseMode(true))
        );

        menuBar.add(menuFiguras);
        menuBar.add(menuFigurasRelleno);
        menuBar.add(menuColores);
        menuBar.add(menuLineas);
        menuBar.add(menuTransformaciones);
        menuBar.add(menuBorrar);

        setJMenuBar(menuBar);
    }

    private JMenu createMenu(String title, JMenuItem... items) {
        JMenu menu = new JMenu(title);
        for (JMenuItem item : items) {
            menu.add(item);
        }
        menu.addMouseListener(new MenuMouseAdapter());
        return menu;
    }

    private JMenuItem createMenuItem(String text, String iconPath, ActionListener listener) {
        String rootPath = "src/icons/";
        JMenuItem item = new JMenuItem(text, new ImageIcon(rootPath + iconPath));
        item.addActionListener(listener);
        return item;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Paint3 paint = new Paint3();
            paint.setVisible(true);
        });
    }
}

class DrawingPanel extends JPanel {
    private final List<ShapeInfo> shapes = new ArrayList<>();
    private BufferedImage buffer;
    private Graphics2D bufferGraphics;
    private ShapeInfo currentShape;
    private Color currentColor = Color.BLACK;
    private BasicStroke currentStroke = new BasicStroke(2);
    private String currentShapeType = "Línea";
    private boolean eraseMode = false;
    private boolean translateMode = false;
    private boolean escalaMode = false;
    private boolean rotateMode = false;
    private boolean sesgadoMode = false;
    private Point startPoint;
    private final int width;
    private final int height;

    public DrawingPanel(int width, int height) {
        this.width = width;
        this.height = height;
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.WHITE);
        setupListeners();
        createBuffer();
    }

    private void createBuffer() {
        buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        bufferGraphics = buffer.createGraphics();
        bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        bufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        bufferGraphics.setColor(Color.WHITE);
        bufferGraphics.fillRect(0, 0, width, height);
        redrawShapes();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    private void setupListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                setCursor(eraseMode ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.CROSSHAIR_CURSOR));
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private void handleMousePressed(MouseEvent e) {
        startPoint = e.getPoint();
        if (eraseMode) {
            removeShapeAt(startPoint);
        } else if (translateMode || escalaMode || rotateMode || sesgadoMode) {
            for (ShapeInfo shape : shapes) {
                if (shape.contains(startPoint.x, startPoint.y)) {
                    currentShape = shape;
                    break;
                }
            }
        } else {
            boolean filled = currentShapeType.contains("Relleno");
            currentShape = new ShapeInfo(currentShapeType, startPoint.x, startPoint.y,
                    startPoint.x, startPoint.y, currentColor, currentStroke, filled);
        }
    }

    private void handleMouseDragged(MouseEvent e) {
        int dx = e.getX() - startPoint.x;
        int dy = e.getY() - startPoint.y;

        if (translateMode && currentShape != null) {
            currentShape.translate(dx, dy);
        } else if (escalaMode && currentShape != null) {
            currentShape.scale(dx, dy);
        } else if (rotateMode && currentShape != null) {
            currentShape.rotate(dx);
        } else if (sesgadoMode && currentShape != null) {
            currentShape.sesgado(dx, dy);
        } else if (!eraseMode && currentShape != null) {
            currentShape.setEndPoint(e.getPoint());
        }

        startPoint = e.getPoint();
        redrawShapes();
    }

    private void handleMouseReleased(MouseEvent e) {
        if (translateMode || escalaMode || rotateMode || sesgadoMode) {
            currentShape = null;
        } else if (!eraseMode && currentShape != null) {
            currentShape.setEndPoint(e.getPoint());
            shapes.add(currentShape);
            currentShape = null;
        }
        redrawShapes();
    }

    private void removeShapeAt(Point point) {
        for (int i = shapes.size() - 1; i >= 0; i--) {
            if (shapes.get(i).contains(point.x, point.y)) {
                shapes.remove(i);
                redrawShapes();
                break;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (buffer == null) {
            createBuffer();
        }

        g.drawImage(buffer, 0, 0, null);

        if (!eraseMode && currentShape != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            currentShape.draw(g2d);
        }
    }

    private void redrawShapes() {
        bufferGraphics.setColor(Color.WHITE);
        bufferGraphics.fillRect(0, 0, getWidth(), getHeight());

        for (ShapeInfo shape : shapes) {
            shape.draw(bufferGraphics);
        }

        repaint();
    }

    public void setCurrentShape(String shape) {
        currentShapeType = shape;
        eraseMode = false;
        translateMode = false;
        escalaMode = false;
        rotateMode = false;
        sesgadoMode = false;
    }

    public void setCurrentColor(Color color) {
        currentColor = color;
        eraseMode = false;
        translateMode = false;
        escalaMode = false;
        rotateMode = false;
        sesgadoMode = false;
    }

    public void setCurrentStroke(BasicStroke stroke) {
        currentStroke = stroke;
        eraseMode = false;
        translateMode = false;
        escalaMode = false;
        rotateMode = false;
        sesgadoMode = false;
    }

    public void clearCanvas() {
        shapes.clear();
        redrawShapes();
    }

    public void enableEraseMode(boolean mode) {
        this.eraseMode = mode;
        translateMode = false;
        escalaMode = false;
        rotateMode = false;
        sesgadoMode = false;
    }

    public void setTranslateMode(boolean mode) {
        this.translateMode = mode;
        eraseMode = false;
        escalaMode = false;
        rotateMode = false;
        sesgadoMode = false;
    }

    public void setEscaleMode(boolean mode) {
        this.escalaMode = mode;
        eraseMode = false;
        translateMode = false;
        rotateMode = false;
        sesgadoMode = false;
    }

    public void setRotateMode(boolean mode) {
        this.rotateMode = mode;
        eraseMode = false;
        translateMode = false;
        escalaMode = false;
        sesgadoMode = false;
    }

    public void setSesgadoMode(boolean mode) {
        this.sesgadoMode = mode;
        eraseMode = false;
        translateMode = false;
        escalaMode = false;
        rotateMode = false;
    }
}

class ShapeInfo {
    private final String shapeType;
    private final Color color;
    private final BasicStroke stroke;
    private final boolean filled;
    private int startX, startY;
    private int endX, endY;

    public ShapeInfo(String shapeType, int startX, int startY, int endX, int endY,
                     Color color, BasicStroke stroke, boolean filled) {
        this.shapeType = shapeType;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.color = color;
        this.stroke = stroke;
        this.filled = filled;
    }

    public void setEndPoint(Point p) {
        this.endX = p.x;
        this.endY = p.y;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.setStroke(stroke);

        switch (shapeType) {
            case "Línea":
                g2d.drawLine(startX, startY, endX, endY);
                break;
            case "Círculo/Ovalo":
            case "Círculo/Ovalo Relleno":
                drawOval(g2d);
                break;
            case "Rectángulo/Cuadrado":
            case "Rectángulo/Cuadrado Relleno":
                drawRectangle(g2d);
                break;
            case "Triángulo":
            case "Triángulo Relleno":
                drawTriangle(g2d);
                break;
        }
    }

    private void drawOval(Graphics2D g2d) {
        int x = Math.min(startX, endX);
        int y = Math.min(startY, endY);
        int width = Math.abs(endX - startX);
        int height = Math.abs(endY - startY);
        if (filled) {
            g2d.fillOval(x, y, width, height);
        } else {
            g2d.drawOval(x, y, width, height);
        }
    }

    private void drawRectangle(Graphics2D g2d) {
        int x = Math.min(startX, endX);
        int y = Math.min(startY, endY);
        int width = Math.abs(endX - startX);
        int height = Math.abs(endY - startY);
        if (filled) {
            g2d.fillRect(x, y, width, height);
        } else {
            g2d.drawRect(x, y, width, height);
        }
    }

    private void drawTriangle(Graphics2D g2d) {
        int[] xPoints = {startX, endX, (startX + endX) / 2};
        int[] yPoints = {endY, endY, startY};
        if (filled) {
            g2d.fillPolygon(xPoints, yPoints, 3);
        } else {
            g2d.drawPolygon(xPoints, yPoints, 3);
        }
    }

    public boolean contains(int x, int y) {
        switch (shapeType) {
            case "Línea":
                return distanceFromLine(x, y) < 5;
            case "Círculo/Ovalo":
            case "Círculo/Ovalo Relleno":
            case "Rectángulo/Cuadrado":
            case "Rectángulo/Cuadrado Relleno":
                Rectangle bounds = new Rectangle(
                        Math.min(startX, endX),
                        Math.min(startY, endY),
                        Math.abs(endX - startX),
                        Math.abs(endY - startY)
                );
                return bounds.contains(x, y);
            case "Triángulo":
            case "Triángulo Relleno":
                Polygon triangle = new Polygon();
                triangle.addPoint(startX, endY);
                triangle.addPoint(endX, endY);
                triangle.addPoint((startX + endX) / 2, startY);
                return triangle.contains(x, y);
            default:
                return false;
        }
    }

    private double distanceFromLine(int x, int y) {
        double normalLength = Math.hypot(endX - startX, endY - startY);
        return Math.abs((x - startX) * (endY - startY) - (y - startY) * (endX - startX)) / normalLength;
    }

    public void translate(int dx, int dy) {
        startX += dx;
        startY += dy;
        endX += dx;
        endY += dy;
    }

    public void scale(int dx, int dy) {
        endX += dx;
        endY += dy;
    }

    public void rotate(int dx) {
        double angle = Math.toRadians(dx);
        double x = (double) (startX + endX) / 2;
        double y = (double) (startY + endY) / 2;
        double x1 = startX - x;
        double y1 = startY - y;
        double x2 = endX - x;
        double y2 = endY - y;
        startX = (int) (x + x1 * Math.cos(angle) - y1 * Math.sin(angle));
        startY = (int) (y + x1 * Math.sin(angle) + y1 * Math.cos(angle));
        endX = (int) (x + x2 * Math.cos(angle) - y2 * Math.sin(angle));
        endY = (int) (y + x2 * Math.sin(angle) + y2 * Math.cos(angle));
    }

    public void sesgado(int dx, int dy) {
        double centerX = (startX + endX) / 2.0;
        double centerY = (startY + endY) / 2.0;

        double width = endX - startX;
        double height = endY - startY;

        double newStartX = startX + dx * (startY - centerY) / height;
        double newEndX = endX + dx * (endY - centerY) / height;
        double newStartY = startY + dy * (startX - centerX) / width;
        double newEndY = endY + dy * (endX - centerX) / width;

        startX = (int) newStartX;
        endX = (int) newEndX;
        startY = (int) newStartY;
        endY = (int) newEndY;
    }
}


class MenuMouseAdapter extends MouseAdapter {
    @Override
    public void mouseEntered(MouseEvent e) {
        Component component = (Component) e.getSource();
        component.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        Component component = (Component) e.getSource();
        component.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
    }
}