package graficador;

// Pasa un punto 3D a 2D usando la matriz de cada proyeccion del PDF.
// Devuelve un arreglo {X2D, Y2D}. Todavia NO son pixeles: el paso a
// coordenadas de pantalla (escala, centrar, invertir Y) lo hace el Lienzo.
public class Proyecciones {

    public static double[] proyectar(Punto3D p, TipoProyeccion tipo, double a, double b) {
        switch (tipo) {
            case ISOMETRICA:
                return isometrica(p, a, b);
            case OBLICUA:
                return oblicua(p, a);
            default:
                return axonometrica(p, a, b);
        }
    }

    // | X2D |   | -sen(beta)  sen(alfa)  0 |   | X |
    // | Y2D | = | -cos(beta)  cos(alfa)  1 | * | Y |
    //                                          | Z |
    private static double[] isometrica(Punto3D p, double alfaGrados, double betaGrados) {
        double alfa = Math.toRadians(alfaGrados);
        double beta = Math.toRadians(betaGrados);

        double x2d = -Math.sin(beta) * p.getX() + Math.sin(alfa) * p.getY();
        double y2d = -Math.cos(beta) * p.getX() + Math.cos(alfa) * p.getY() + p.getZ();
        return new double[]{x2d, y2d};
    }

    // Oblicua (caballera):
    // | X2D |   | cos(alfa)  1  0 |   | X |
    // | Y2D | = | sen(alfa)  0  1 | * | Y |
    //                                 | Z |
    private static double[] oblicua(Punto3D p, double alfaGrados) {
        double alfa = Math.toRadians(alfaGrados);

        double x2d = Math.cos(alfa) * p.getX() + p.getY();
        double y2d = Math.sin(alfa) * p.getX() + p.getZ();
        return new double[]{x2d, y2d};
    }

    // Axonometrica (el observador esta a una distancia rho, con angulos theta y phi):
    // | Xp |   | -sen(t)         cos(t)          0      |   | X |   | 0   |
    // | Yp | = | -cos(t)sen(f)  -sen(t)sen(f)    cos(f) | * | Y | + | 0   |
    // | Zp |   | -cos(t)cos(f)  -sen(t)cos(f)   -sen(f) |   | Z |   | rho |
    // Como dice el PDF: X2D = Xp y Y2D = Yp. Por eso rho (que solo afecta a Zp,
    // la profundidad) no cambia el dibujo y no hace falta pedirlo.
    private static double[] axonometrica(Punto3D p, double thetaGrados, double phiGrados) {
        double t = Math.toRadians(thetaGrados);
        double f = Math.toRadians(phiGrados);

        double x2d = -Math.sin(t) * p.getX() + Math.cos(t) * p.getY();
        double y2d = -Math.cos(t) * Math.sin(f) * p.getX()
                - Math.sin(t) * Math.sin(f) * p.getY()
                + Math.cos(f) * p.getZ();
        return new double[]{x2d, y2d};
    }
}
