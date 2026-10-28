package graficador;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Lienzo3D extends JPanel {

    // Puntos tal cual se leyeron del archivo (nunca se modifican)
    private List<Punto3D> puntosOriginales = new ArrayList<>();
    // Puntos con las transformaciones aplicadas hasta ahora
    private List<Punto3D> puntosActuales = new ArrayList<>();

    private TipoProyeccion proyeccion = TipoProyeccion.ISOMETRICA;
    // Parametros de la proyeccion (angulos en grados):
    //  Isometrica: a = alfa, b = beta | Oblicua: a = alfa | Axonometrica: a = theta, b = phi
    private double parametroA = 120;
    private double parametroB = 60;

    // Opciones que activa el usuario desde los checkbox
    private boolean unirPuntos = false;
    private boolean rellenar = false;
    private boolean mostrarCuadricula = true;
    private boolean mostrarCoordenadas = false;

    // Vista: cuantos pixeles vale una unidad del mundo 3D (se cambia con la rueda del mouse)
    // y cuanto se movio el dibujo con el clic derecho.
    // Ojo: esto es solo la "camara", no es la transformacion de escalamiento de los puntos.
    private static final double ZOOM_INICIAL = 50;
    private static final double MINIMO_ZOOM = 0.001;
    private double pixelesPorUnidad = ZOOM_INICIAL;
    private double desplazamientoX = 0;
    private double desplazamientoY = 0;

    // Tamano minimo de los ejes y la cuadricula (de -5 a 5). Si la figura se sale,
    // cada eje crece por su cuenta (ver actualizarLimites)
    private static final int LARGO_EJES = 5;
    private int limiteX = LARGO_EJES;
    private int limiteY = LARGO_EJES;
    private int limiteZ = LARGO_EJES;
    // Cada cuantas unidades hay linea de cuadricula y numero en los ejes
    private int paso = 1;

    // Punto sobre el que esta el mouse (-1 si no hay ninguno)
    private int puntoCercano = -1;
    private int ultimoX;
    private int ultimoY;

    public Lienzo3D() {
        setBackground(Color.WHITE);

        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                ultimoX = e.getX();
                ultimoY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - ultimoX;
                int dy = e.getY() - ultimoY;

                if (SwingUtilities.isRightMouseButton(e)) {
                    // Clic derecho: mover el dibujo por la pantalla
                    desplazamientoX += dx;
                    desplazamientoY += dy;
                    repaint();
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    // Clic izquierdo: rotar el objeto. Horizontal = eje Z, vertical = eje X
                    rotar(Eje.Z, dx * 0.5);
                    rotar(Eje.X, dy * 0.5);
                }

                ultimoX = e.getX();
                ultimoY = e.getY();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                buscarPuntoCercano(e.getX(), e.getY());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                puntoCercano = -1;
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Rueda hacia arriba acerca, hacia abajo aleja
                double factor = e.getWheelRotation() < 0 ? 1.1 : 1 / 1.1;
                pixelesPorUnidad = Math.max(MINIMO_ZOOM, Math.min(400, pixelesPorUnidad * factor));
                repaint();
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);
        addMouseWheelListener(raton);
    }

    // ---------- Manejo de los puntos ----------

    public void setPuntos(List<Punto3D> puntos) {
        puntosOriginales = new ArrayList<>(puntos);
        restaurar();
    }

    public int getCantidadPuntos() {
        return puntosActuales.size();
    }

    // Vuelve a la figura como estaba al cargar el archivo (y a la vista inicial)
    public void restaurar() {
        puntosActuales = new ArrayList<>();
        for (Punto3D p : puntosOriginales) {
            puntosActuales.add(new Punto3D(p.getX(), p.getY(), p.getZ()));
        }
        pixelesPorUnidad = ZOOM_INICIAL;
        desplazamientoX = 0;
        desplazamientoY = 0;
        puntoCercano = -1;
        ajustarVista(true);
        repaint();
    }

    // ---------- Transformaciones ----------

    public void trasladar(double tx, double ty, double tz) {
        List<Punto3D> nuevos = new ArrayList<>();
        for (Punto3D p : puntosActuales) {
            nuevos.add(Transformaciones.trasladar(p, tx, ty, tz));
        }
        puntosActuales = nuevos;
        ajustarVista(true);
        repaint();
    }

    public void rotar(Eje eje, double grados) {
        List<Punto3D> nuevos = new ArrayList<>();
        for (Punto3D p : puntosActuales) {
            nuevos.add(Transformaciones.rotar(p, eje, grados));
        }
        puntosActuales = nuevos;
        // Al rotar solo se aleja si hace falta, para que el zoom no se mueva mientras se arrastra
        ajustarVista(false);
        repaint();
    }

    public void escalar(double factor) {
        List<Punto3D> nuevos = new ArrayList<>();
        for (Punto3D p : puntosActuales) {
            nuevos.add(Transformaciones.escalar(p, factor, factor, factor));
        }
        puntosActuales = nuevos;
        ajustarVista(true);
        repaint();
    }

    // ---------- Opciones de vista ----------

    public void setProyeccion(TipoProyeccion tipo, double a, double b) {
        this.proyeccion = tipo;
        this.parametroA = a;
        this.parametroB = b;
        ajustarVista(true);
        repaint();
    }

    public void setUnirPuntos(boolean unir) {
        this.unirPuntos = unir;
        repaint();
    }

    public void setRellenar(boolean rellenar) {
        this.rellenar = rellenar;
        repaint();
    }

    public void setMostrarCuadricula(boolean mostrar) {
        this.mostrarCuadricula = mostrar;
        repaint();
    }

    public void setMostrarCoordenadas(boolean mostrar) {
        this.mostrarCoordenadas = mostrar;
        repaint();
    }

    // ---------- Ejes que crecen y vista que sigue a la figura ----------

    // Calcula hasta donde llega cada eje: minimo 5, pero si algun punto se sale por
    // ese eje, el eje crece hasta cubrirlo. Si los valores son muy grandes, los numeros
    // y las lineas de la cuadricula se ponen cada "paso" unidades para que no se amontonen.
    private void actualizarLimites() {
        double maxX = 0;
        double maxY = 0;
        double maxZ = 0;
        for (Punto3D p : puntosActuales) {
            maxX = Math.max(maxX, Math.abs(p.getX()));
            maxY = Math.max(maxY, Math.abs(p.getY()));
            maxZ = Math.max(maxZ, Math.abs(p.getZ()));
        }

        double mayor = Math.max(maxX, Math.max(maxY, maxZ));
        paso = Math.max(1, (int) Math.ceil(mayor / 10.0));

        limiteX = redondearAlPaso(maxX);
        limiteY = redondearAlPaso(maxY);
        limiteZ = redondearAlPaso(maxZ);
    }

    // Sube el valor hasta el siguiente multiplo de "paso" (nunca menos de LARGO_EJES)
    private int redondearAlPaso(double valor) {
        return (int) Math.ceil(Math.max(LARGO_EJES, valor) / paso) * paso;
    }

    // Hace que todo (figura, ejes y cuadricula) quepa en el panel cambiando el zoom.
    // Si permitirAcercar es true, tambien vuelve a acercar cuando la figura se hace mas chica.
    private void ajustarVista(boolean permitirAcercar) {
        actualizarLimites();
        if (getWidth() <= 0 || getHeight() <= 0) {
            return; // el panel todavia no se muestra
        }

        // Puntos que tienen que verse: la figura, el final de cada eje y las esquinas de la cuadricula
        List<Punto3D> importantes = new ArrayList<>(puntosActuales);
        importantes.add(new Punto3D(limiteX, 0, 0));
        importantes.add(new Punto3D(-limiteX, 0, 0));
        importantes.add(new Punto3D(0, limiteY, 0));
        importantes.add(new Punto3D(0, -limiteY, 0));
        importantes.add(new Punto3D(0, 0, limiteZ));
        importantes.add(new Punto3D(0, 0, -limiteZ));
        importantes.add(new Punto3D(limiteX, limiteY, 0));
        importantes.add(new Punto3D(-limiteX, limiteY, 0));
        importantes.add(new Punto3D(limiteX, -limiteY, 0));
        importantes.add(new Punto3D(-limiteX, -limiteY, 0));

        // Que tan lejos del centro llega todo, ya proyectado (en unidades, todavia no en pixeles)
        double mayorX = 0;
        double mayorY = 0;
        for (Punto3D p : importantes) {
            double[] p2d = Proyecciones.proyectar(p, proyeccion, parametroA, parametroB);
            mayorX = Math.max(mayorX, Math.abs(p2d[0]));
            mayorY = Math.max(mayorY, Math.abs(p2d[1]));
        }

        // Pixeles que hay desde el centro hasta el borde (dejando espacio para flechas y letras)
        double disponibleX = Math.max(50, getWidth() / 2.0 - 60 - Math.abs(desplazamientoX));
        double disponibleY = Math.max(50, getHeight() / 2.0 - 50 - Math.abs(desplazamientoY));

        double quecabe = Math.min(disponibleX / Math.max(mayorX, 0.001),
                disponibleY / Math.max(mayorY, 0.001));
        quecabe = Math.max(MINIMO_ZOOM, quecabe);

        if (pixelesPorUnidad > quecabe) {
            pixelesPorUnidad = quecabe;
        } else if (permitirAcercar && pixelesPorUnidad < ZOOM_INICIAL) {
            pixelesPorUnidad = Math.min(ZOOM_INICIAL, quecabe);
        }
    }

    // ---------- Proyeccion a pantalla ----------

    // Pasa un punto 3D a un pixel de la pantalla:
    // 1) proyecta a 2D, 2) escala a pixeles, 3) centra en el panel (mas el desplazamiento)
    // 4) invierte Y (en pantalla Y crece hacia abajo, en el mundo hacia arriba)
    private Point2D.Double aPantalla(Punto3D p) {
        double[] p2d = Proyecciones.proyectar(p, proyeccion, parametroA, parametroB);
        double px = getWidth() / 2.0 + desplazamientoX + p2d[0] * pixelesPorUnidad;
        double py = getHeight() / 2.0 + desplazamientoY - p2d[1] * pixelesPorUnidad;
        return new Point2D.Double(px, py);
    }

    private Point2D.Double aPantalla(double x, double y, double z) {
        return aPantalla(new Punto3D(x, y, z));
    }

    // Busca si el mouse esta a menos de 10 pixeles de algun punto
    private void buscarPuntoCercano(int mx, int my) {
        int anterior = puntoCercano;
        puntoCercano = -1;
        double mejorDistancia = 10;

        for (int i = 0; i < puntosActuales.size(); i++) {
            Point2D.Double p = aPantalla(puntosActuales.get(i));
            double d = Math.hypot(p.x - mx, p.y - my);
            if (d < mejorDistancia) {
                mejorDistancia = d;
                puntoCercano = i;
            }
        }

        if (anterior != puntoCercano) {
            repaint();
        }
    }

    // ---------- Dibujo ----------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Se revisa hasta donde tienen que llegar los ejes con la figura tal como esta ahora
        actualizarLimites();

        if (mostrarCuadricula) {
            dibujarCuadricula(g2);
        }
        dibujarEjes(g2);

        // Se calculan una sola vez los pixeles de todos los puntos
        List<Point2D.Double> enPantalla = new ArrayList<>();
        for (Punto3D p : puntosActuales) {
            enPantalla.add(aPantalla(p));
        }

        if (rellenar) {
            dibujarRelleno(g2, enPantalla);
        }
        if (unirPuntos) {
            dibujarLineas(g2, enPantalla);
        }
        dibujarPuntos(g2, enPantalla);

        if (puntoCercano >= 0 && puntoCercano < enPantalla.size()) {
            dibujarCartel(g2, enPantalla.get(puntoCercano), puntosActuales.get(puntoCercano), puntoCercano);
        }

        g2.setColor(Color.DARK_GRAY);
        g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString("Puntos: " + puntosActuales.size() + "  |  Proyección: " + proyeccion
                + "  |  Zoom: " + Math.round(pixelesPorUnidad / ZOOM_INICIAL * 100) + "%", 10, getHeight() - 10);
    }

    // Cuadricula sobre el "piso" (plano z = 0), una linea cada "paso" unidades.
    // Llega hasta el limite de cada eje, asi crece junto con ellos.
    private void dibujarCuadricula(Graphics2D g2) {
        g2.setColor(new Color(228, 228, 228));

        for (int m = -limiteX / paso; m <= limiteX / paso; m++) {
            if (m == 0) {
                continue; // esas lineas las cubren los ejes
            }
            double x = m * paso;
            dibujarLinea3D(g2, x, -limiteY, 0, x, limiteY, 0);
        }

        for (int m = -limiteY / paso; m <= limiteY / paso; m++) {
            if (m == 0) {
                continue;
            }
            double y = m * paso;
            dibujarLinea3D(g2, -limiteX, y, 0, limiteX, y, 0);
        }
    }

    private void dibujarLinea3D(Graphics2D g2, double x1, double y1, double z1,
                                double x2, double y2, double z2) {
        Point2D.Double a = aPantalla(x1, y1, z1);
        Point2D.Double b = aPantalla(x2, y2, z2);
        dibujarLineaPantalla(g2, a, b, 1, false);
    }

    // Ejes X (rojo), Y (verde) y Z (azul) con flecha, nombre y numeros de cada "paso".
    // Cada eje llega hasta su propio limite. Pasan por la misma proyeccion,
    // asi se ve como cambia la vista al elegir otra.
    private void dibujarEjes(Graphics2D g2) {
        Color[] colores = {new Color(210, 30, 30), new Color(0, 150, 0), new Color(30, 60, 220)};
        String[] nombres = {"X", "Y", "Z"};
        double[][] direcciones = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        int[] limites = {limiteX, limiteY, limiteZ};
        Point2D.Double origen = aPantalla(0, 0, 0);

        for (int i = 0; i < 3; i++) {
            double dx = direcciones[i][0];
            double dy = direcciones[i][1];
            double dz = direcciones[i][2];
            int limite = limites[i];
            Point2D.Double fin = aPantalla(dx * limite, dy * limite, dz * limite);
            Point2D.Double negativo = aPantalla(-dx * limite, -dy * limite, -dz * limite);

            g2.setColor(colores[i]);

            // Parte negativa: linea punteada y delgada
            dibujarLineaPantalla(g2, origen, negativo, 1, true);

            // Parte positiva: linea gruesa con flecha
            dibujarLineaPantalla(g2, origen, fin, 2, false);
            dibujarFlecha(g2, origen, fin);

            // Numeros de cada "paso" (-5 ... 5 si la figura es chica) para leer las coordenadas
            g2.setFont(getFont().deriveFont(Font.PLAIN, 10f));
            for (int m = -limite / paso; m <= limite / paso; m++) {
                if (m == 0) {
                    continue;
                }
                int k = m * paso;
                Point2D.Double marca = aPantalla(dx * k, dy * k, dz * k);
                g2.fill(new java.awt.geom.Ellipse2D.Double(marca.x - 2, marca.y - 2, 4, 4));
                g2.drawString(String.valueOf(k), (float) marca.x + 4, (float) marca.y + 13);
            }

            // Nombre del eje
            g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
            g2.drawString(nombres[i], (float) fin.x + 8, (float) fin.y - 6);
        }
    }

    private void dibujarFlecha(Graphics2D g2, Point2D.Double desde, Point2D.Double hasta) {
        double angulo = Math.atan2(hasta.y - desde.y, hasta.x - desde.x);
        double largo = 11;

        Path2D.Double punta = new Path2D.Double();
        punta.moveTo(hasta.x, hasta.y);
        punta.lineTo(hasta.x - largo * Math.cos(angulo - 0.4), hasta.y - largo * Math.sin(angulo - 0.4));
        punta.lineTo(hasta.x - largo * Math.cos(angulo + 0.4), hasta.y - largo * Math.sin(angulo + 0.4));
        punta.closePath();
        g2.fill(punta);
    }

    // Rellena la figura: se dibuja el contorno exterior de todos los puntos
    // (envolvente convexa) ya proyectados, con un color semitransparente.
    private void dibujarRelleno(Graphics2D g2, List<Point2D.Double> enPantalla) {
        List<Point2D.Double> contorno = envolventeConvexa(enPantalla);
        if (contorno.size() < 3) {
            return;
        }

        Path2D.Double poligono = new Path2D.Double();
        poligono.moveTo(contorno.get(0).x, contorno.get(0).y);
        for (int i = 1; i < contorno.size(); i++) {
            poligono.lineTo(contorno.get(i).x, contorno.get(i).y);
        }
        poligono.closePath();

        g2.setColor(new Color(70, 130, 220, 70));
        g2.fill(poligono);

        // El borde del poligono se dibuja lado por lado con Bresenham
        g2.setColor(new Color(40, 90, 200));
        for (int i = 0; i < contorno.size(); i++) {
            Point2D.Double a = contorno.get(i);
            Point2D.Double b = contorno.get((i + 1) % contorno.size());
            dibujarLineaPantalla(g2, a, b, 2, false);
        }
    }

    // Lineas entre puntos consecutivos, en el orden del archivo
    private void dibujarLineas(Graphics2D g2, List<Point2D.Double> enPantalla) {
        g2.setColor(new Color(90, 90, 90, 170));
        for (int i = 0; i < enPantalla.size() - 1; i++) {
            dibujarLineaPantalla(g2, enPantalla.get(i), enPantalla.get(i + 1), 1, false);
        }
    }

    private void dibujarPuntos(Graphics2D g2, List<Point2D.Double> enPantalla) {
        g2.setStroke(new BasicStroke(1.5f));
        g2.setFont(getFont().deriveFont(Font.PLAIN, 10f));

        for (int i = 0; i < enPantalla.size(); i++) {
            Point2D.Double p = enPantalla.get(i);
            boolean resaltado = (i == puntoCercano);
            double radio = resaltado ? 6 : 4.5;

            g2.setColor(resaltado ? new Color(230, 60, 40) : new Color(20, 60, 200));
            g2.fill(new java.awt.geom.Ellipse2D.Double(p.x - radio, p.y - radio, radio * 2, radio * 2));
            g2.setColor(Color.WHITE);
            g2.draw(new java.awt.geom.Ellipse2D.Double(p.x - radio, p.y - radio, radio * 2, radio * 2));

            // Coordenadas junto a cada punto (si el usuario lo pidio)
            if (mostrarCoordenadas) {
                Punto3D p3 = puntosActuales.get(i);
                g2.setColor(new Color(60, 60, 60));
                g2.drawString(String.format("(%.2f, %.2f, %.2f)", p3.getX(), p3.getY(), p3.getZ()),
                        (float) p.x + 8, (float) p.y - 6);
            }
        }
    }

    // Cartelito con las coordenadas del punto sobre el que esta el mouse
    private void dibujarCartel(Graphics2D g2, Point2D.Double pantalla, Punto3D p, int indice) {
        String texto = String.format("P%d  (%.3f, %.3f, %.3f)", indice + 1, p.getX(), p.getY(), p.getZ());
        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        FontMetrics fm = g2.getFontMetrics();
        int ancho = fm.stringWidth(texto) + 12;
        int alto = fm.getHeight() + 6;

        int x = (int) pantalla.x + 12;
        int y = (int) pantalla.y - alto - 8;
        // Si se sale del panel, se acomoda
        if (x + ancho > getWidth()) {
            x = (int) pantalla.x - ancho - 12;
        }
        if (y < 0) {
            y = (int) pantalla.y + 12;
        }

        g2.setColor(new Color(255, 255, 220));
        g2.fillRoundRect(x, y, ancho, alto, 8, 8);
        g2.setColor(Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, ancho, alto, 8, 8);
        g2.drawString(texto, x + 6, y + fm.getAscent() + 3);
    }

    // ---------- Linea recta: algoritmo de Bresenham (el del minipaint) ----------

    // Dibuja una linea entre dos puntos de pantalla usando Bresenham.
    // grosor: 1 = un pixel, 2 = pixeles de 2x2, etc.
    // punteada: alterna 4 pixeles pintados y 4 sin pintar (para la parte negativa de los ejes)
    private void dibujarLineaPantalla(Graphics2D g2, Point2D.Double a, Point2D.Double b,
                                      int grosor, boolean punteada) {
        int mitad = grosor / 2;
        int[] contador = {0}; // cuenta los pixeles que pasa el algoritmo, para el patron punteado

        PintorDePunto pintor = (x, y) -> {
            boolean hueco = punteada && (contador[0] / 4) % 2 == 1;
            contador[0]++;
            if (!hueco) {
                g2.fillRect(x - mitad, y - mitad, grosor, grosor);
            }
        };

        dibujarLineaBresenham((int) Math.round(a.x), (int) Math.round(a.y),
                (int) Math.round(b.x), (int) Math.round(b.y), pintor);
    }

    private void dibujarLineaBresenham(int x1, int y1, int x2, int y2, PintorDePunto pintor) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = (x1 < x2) ? 1 : -1;
        int sy = (y1 < y2) ? 1 : -1;
        boolean empinada = dy > dx;

        int x = x1;
        int y = y1;

        if (empinada) {
            int d = 2 * dx - dy;
            int incE = 2 * dx;
            int incNE = 2 * (dx - dy);

            pintor.pintar(x, y);
            while (y != y2) {
                y += sy;
                if (d <= 0) {
                    d += incE;
                } else {
                    d += incNE;
                    pintor.pintar(x, y); // codo: x viejo, y nuevo
                    x += sx;
                }
                pintor.pintar(x, y);
            }
        } else {
            int d = 2 * dy - dx;
            int incE = 2 * dy;
            int incNE = 2 * (dy - dx);

            pintor.pintar(x, y);
            while (x != x2) {
                x += sx;
                if (d <= 0) {
                    d += incE;
                } else {
                    d += incNE;
                    pintor.pintar(x, y); // codo: x nuevo, y viejo
                    y += sy;
                }
                pintor.pintar(x, y);
            }
        }
    }

    // ---------- Envolvente convexa (algoritmo de la cadena monotona) ----------

    // Devuelve solo los puntos que forman el contorno exterior
    private List<Point2D.Double> envolventeConvexa(List<Point2D.Double> puntos) {
        List<Point2D.Double> ordenados = new ArrayList<>(puntos);
        ordenados.sort(Comparator.comparingDouble((Point2D.Double a) -> a.x).thenComparingDouble(a -> a.y));

        if (ordenados.size() < 3) {
            return ordenados;
        }

        List<Point2D.Double> contorno = new ArrayList<>();

        // Mitad de abajo
        for (Point2D.Double p : ordenados) {
            while (contorno.size() >= 2 && producto(contorno.get(contorno.size() - 2),
                    contorno.get(contorno.size() - 1), p) <= 0) {
                contorno.remove(contorno.size() - 1);
            }
            contorno.add(p);
        }

        // Mitad de arriba
        int minimo = contorno.size() + 1;
        for (int i = ordenados.size() - 2; i >= 0; i--) {
            Point2D.Double p = ordenados.get(i);
            while (contorno.size() >= minimo && producto(contorno.get(contorno.size() - 2),
                    contorno.get(contorno.size() - 1), p) <= 0) {
                contorno.remove(contorno.size() - 1);
            }
            contorno.add(p);
        }

        contorno.remove(contorno.size() - 1); // el ultimo se repite con el primero
        return contorno;
    }

    
    // Producto cruz de (b - a) x (c - a): dice si a -> b -> c gira a la izquierda o a la derecha
    private double producto(Point2D.Double a, Point2D.Double b, Point2D.Double c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }
}