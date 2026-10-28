package graficador;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Lee el .txt donde cada fila es un punto: "x y z"
public class LectorArchivo {

    public static List<Punto3D> leerPuntos(File archivo) throws IOException {
        List<Punto3D> puntos = new ArrayList<>();

        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            int numeroLinea = 0;

            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                linea = linea.trim();

                // Se ignoran las lineas vacias
                if (linea.isEmpty()) {
                    continue;
                }

                // Separa por espacios o tabulaciones (uno o varios)
                String[] partes = linea.split("\\s+");
                if (partes.length < 3) {
                    throw new IOException("La linea " + numeroLinea + " no tiene 3 columnas: " + linea);
                }

                try {
                    double x = Double.parseDouble(partes[0]);
                    double y = Double.parseDouble(partes[1]);
                    double z = Double.parseDouble(partes[2]);
                    puntos.add(new Punto3D(x, y, z));
                } catch (NumberFormatException e) {
                    throw new IOException("La linea " + numeroLinea + " tiene un valor que no es numero: " + linea);
                }
            }
        }

        return puntos;
    }
}
