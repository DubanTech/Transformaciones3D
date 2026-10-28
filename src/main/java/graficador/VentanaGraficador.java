package graficador;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class VentanaGraficador extends JFrame {

    private Lienzo3D lienzo;

    private JComboBox<String> comboProyeccion;
    private JLabel etiquetaA;
    private JLabel etiquetaB;
    private JSpinner spinnerA;
    private JSpinner spinnerB;

    private JSpinner campoDistancia;
    private JSpinner campoAngulo;
    private JSpinner campoEscala;

    public VentanaGraficador() {
        setTitle("Graficador 3D");
        setSize(1100, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        lienzo = new Lienzo3D();
        add(lienzo, BorderLayout.CENTER);
        add(crearPanelSuperior(), BorderLayout.NORTH);
        add(crearPanelInferior(), BorderLayout.SOUTH);

        // Deja la proyeccion inicial con sus valores por defecto
        actualizarProyeccion();
    }

    // ---------- Panel de arriba: archivo, proyeccion y opciones de vista ----------

    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel(new GridLayout(2, 1));

        // Fila 1: archivo y proyeccion
        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton botonCargar = new JButton("Cargar archivo .txt");
        botonCargar.addActionListener(e -> cargarArchivo());
        fila1.add(botonCargar);

        fila1.add(new JLabel("   Proyección:"));
        comboProyeccion = new JComboBox<>(new String[]{"Isométrica", "Oblicua (Caballera)", "Axonométrica"});
        comboProyeccion.addActionListener(e -> {
            ponerValoresPorDefecto();
            actualizarProyeccion();
        });
        fila1.add(comboProyeccion);

        etiquetaA = new JLabel("α:");
        etiquetaB = new JLabel("β:");
        spinnerA = new JSpinner(new SpinnerNumberModel(120.0, -360.0, 360.0, 5.0));
        spinnerB = new JSpinner(new SpinnerNumberModel(60.0, -360.0, 360.0, 5.0));
        spinnerA.addChangeListener(e -> actualizarProyeccion());
        spinnerB.addChangeListener(e -> actualizarProyeccion());
        fila1.add(etiquetaA);
        fila1.add(spinnerA);
        fila1.add(etiquetaB);
        fila1.add(spinnerB);
        panel.add(fila1);

        // Fila 2: opciones de vista (las activa el usuario) y ayuda del mouse
        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JCheckBox checkCuadricula = new JCheckBox("Cuadrícula", true);
        checkCuadricula.addActionListener(e -> lienzo.setMostrarCuadricula(checkCuadricula.isSelected()));
        fila2.add(checkCuadricula);

        JCheckBox checkCoordenadas = new JCheckBox("Coordenadas de los puntos");
        checkCoordenadas.addActionListener(e -> lienzo.setMostrarCoordenadas(checkCoordenadas.isSelected()));
        fila2.add(checkCoordenadas);

        JCheckBox checkUnir = new JCheckBox("Unir puntos");
        checkUnir.addActionListener(e -> lienzo.setUnirPuntos(checkUnir.isSelected()));
        fila2.add(checkUnir);

        JCheckBox checkRellenar = new JCheckBox("Rellenar figura");
        checkRellenar.addActionListener(e -> lienzo.setRellenar(checkRellenar.isSelected()));
        fila2.add(checkRellenar);

        JLabel ayuda = new JLabel("     Mouse: arrastrar = rotar  |  clic derecho = mover  |  rueda = zoom");
        ayuda.setForeground(Color.GRAY);
        fila2.add(ayuda);
        panel.add(fila2);

        return panel;
    }

    // ---------- Panel de abajo: transformaciones ----------

    private JPanel crearPanelInferior() {
        JPanel panel = new JPanel(new GridLayout(3, 1));

        // Traslacion
        JPanel filaTraslacion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaTraslacion.add(new JLabel("Trasladar distancia:"));
        campoDistancia = new JSpinner(new SpinnerNumberModel(1.0, -100.0, 100.0, 0.5));
        filaTraslacion.add(campoDistancia);
        JButton botonTx = new JButton("en X");
        JButton botonTy = new JButton("en Y");
        JButton botonTz = new JButton("en Z");
        botonTx.addActionListener(e -> lienzo.trasladar(getDistancia(), 0, 0));
        botonTy.addActionListener(e -> lienzo.trasladar(0, getDistancia(), 0));
        botonTz.addActionListener(e -> lienzo.trasladar(0, 0, getDistancia()));
        filaTraslacion.add(botonTx);
        filaTraslacion.add(botonTy);
        filaTraslacion.add(botonTz);
        panel.add(filaTraslacion);

        // Rotacion
        JPanel filaRotacion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaRotacion.add(new JLabel("Rotar ángulo (grados):"));
        campoAngulo = new JSpinner(new SpinnerNumberModel(15.0, -360.0, 360.0, 5.0));
        filaRotacion.add(campoAngulo);
        JButton botonRx = new JButton("eje X");
        JButton botonRy = new JButton("eje Y");
        JButton botonRz = new JButton("eje Z");
        botonRx.addActionListener(e -> lienzo.rotar(Eje.X, getAngulo()));
        botonRy.addActionListener(e -> lienzo.rotar(Eje.Y, getAngulo()));
        botonRz.addActionListener(e -> lienzo.rotar(Eje.Z, getAngulo()));
        filaRotacion.add(botonRx);
        filaRotacion.add(botonRy);
        filaRotacion.add(botonRz);
        panel.add(filaRotacion);

        // Escalamiento y restaurar
        JPanel filaEscala = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaEscala.add(new JLabel("Escalar al:"));
        agregarBotonEscala(filaEscala, "60%", 0.6);
        agregarBotonEscala(filaEscala, "50%", 0.5);
        agregarBotonEscala(filaEscala, "10%", 0.1);
        filaEscala.add(new JLabel(" otro:"));
        campoEscala = new JSpinner(new SpinnerNumberModel(75.0, 1.0, 1000.0, 5.0));
        filaEscala.add(campoEscala);
        filaEscala.add(new JLabel("%"));
        JButton botonEscalar = new JButton("Escalar");
        botonEscalar.addActionListener(e -> lienzo.escalar(getPorcentaje() / 100.0));
        filaEscala.add(botonEscalar);
        filaEscala.add(new JLabel("        "));
        JButton botonRestaurar = new JButton("Restaurar figura original");
        botonRestaurar.addActionListener(e -> lienzo.restaurar());
        filaEscala.add(botonRestaurar);
        panel.add(filaEscala);

        return panel;
    }

    private void agregarBotonEscala(JPanel fila, String texto, double factor) {
        JButton boton = new JButton(texto);
        boton.addActionListener(e -> lienzo.escalar(factor));
        fila.add(boton);
    }

    // ---------- Logica de la ventana ----------

    private double getDistancia() {
        return ((Number) campoDistancia.getValue()).doubleValue();
    }

    private double getAngulo() {
        return ((Number) campoAngulo.getValue()).doubleValue();
    }
    
    private double getPorcentaje() {
        return ((Number) campoEscala.getValue()).doubleValue();
    }

    private void cargarArchivo() {
        JFileChooser selector = new JFileChooser(new File("."));
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            List<Punto3D> puntos = LectorArchivo.leerPuntos(selector.getSelectedFile());
            lienzo.setPuntos(puntos);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al leer el archivo", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Cada proyeccion usa distintos angulos, aca se dejan valores que se ven bien
    private void ponerValoresPorDefecto() {
        switch (comboProyeccion.getSelectedIndex()) {
            case 0:
                spinnerA.setValue(120.0);
                spinnerB.setValue(60.0);
                break;
            case 1:
                spinnerA.setValue(45.0);
                break;
            default:
                spinnerA.setValue(45.0);
                spinnerB.setValue(30.0);
                break;
        }
    }

    // Cambia los nombres/visibilidad de los angulos segun la proyeccion y se la pasa al lienzo
    private void actualizarProyeccion() {
        int indice = comboProyeccion.getSelectedIndex();
        TipoProyeccion tipo;

        if (indice == 0) {
            tipo = TipoProyeccion.ISOMETRICA;
            etiquetaA.setText("α:");
            etiquetaB.setText("β:");
        } else if (indice == 1) {
            tipo = TipoProyeccion.OBLICUA;
            etiquetaA.setText("α:");
        } else {
            tipo = TipoProyeccion.AXONOMETRICA;
            etiquetaA.setText("θ:");
            etiquetaB.setText("φ:");
        }

        // La oblicua solo usa un angulo
        etiquetaB.setVisible(tipo != TipoProyeccion.OBLICUA);
        spinnerB.setVisible(tipo != TipoProyeccion.OBLICUA);

        double a = ((Number) spinnerA.getValue()).doubleValue();
        double b = ((Number) spinnerB.getValue()).doubleValue();
        lienzo.setProyeccion(tipo, a, b);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaGraficador ventana = new VentanaGraficador();
            ventana.setVisible(true);
        });
    }
}