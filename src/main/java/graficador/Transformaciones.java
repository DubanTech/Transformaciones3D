package graficador;
// Las tres transformaciones fundamentales en 3D (traslacion, rotacion y
// escalamiento). Cada metodo recibe un punto y devuelve un punto NUEVO,
// asi el punto original nunca se modifica por accidente.
// Las ecuaciones son las mismas que aparecen en el PDF de la tarea.
public class Transformaciones {

    // x' = x + tx ; y' = y + ty ; z' = z + tz
    public static Punto3D trasladar(Punto3D p, double tx, double ty, double tz) {
        return new Punto3D(p.getX() + tx, p.getY() + ty, p.getZ() + tz);
    }

    // Rotacion de "grados" alrededor del eje indicado
    public static Punto3D rotar(Punto3D p, Eje eje, double grados) {
        double t = Math.toRadians(grados);
        double cos = Math.cos(t);
        double sen = Math.sin(t);
        double x = p.getX();
        double y = p.getY();
        double z = p.getZ();

        switch (eje) {
            case X:
                // x' = x ; y' = y cos - z sen ; z' = y sen + z cos
                return new Punto3D(x, y * cos - z * sen, y * sen + z * cos);
            case Y:
                // x' = z sen + x cos ; y' = y ; z' = z cos - x sen
                return new Punto3D(z * sen + x * cos, y, z * cos - x * sen);
            default:
                // x' = x cos - y sen ; y' = x sen + y cos ; z' = z
                return new Punto3D(x * cos - y * sen, x * sen + y * cos, z);
        }
    }

    // x' = sx * x ; y' = sy * y ; z' = sz * z
    public static Punto3D escalar(Punto3D p, double sx, double sy, double sz) {
        return new Punto3D(p.getX() * sx, p.getY() * sy, p.getZ() * sz);
    }
}
