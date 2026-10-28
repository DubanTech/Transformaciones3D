/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package graficador;

// Representa "pintar un punto (x,y)", sin importar si el destino final
// es la pantalla (Graphics) o una matriz en memoria. Los algoritmos de
// dibujo (Bresenham, etc.) reciben un PintorDePunto y no les
// importa a donde va a parar cada pixel.
@FunctionalInterface
public interface PintorDePunto {
    void pintar(int x, int y);
}
