package firedetection;

import java.awt.event.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public class GUI {

    public static void main(String[] args) {
        GUI i = new GUI();
    }
  
    static JFrame f;
    JTabbedPane jtp;

    public GUI() {
        //JFrame.setDefaultLookAndFeelDecorated(true);
        f = new JFrame();
        jtp = new JTabbedPane();
        jtp.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent e) {
                if (KeyEvent.getKeyText(e.getKeyChar()).equals("Escape")) {
                    int s = JOptionPane.showOptionDialog(f, "     Cerrar Aplicación?\n(\"ESC\" para cancelar)",
                            "SALIR SISTEMA", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,
                            null, (new String[]{"SI", "NO"}), "SI");
                    if (s == 0) {
                        System.exit(0);
                    } else if (s == 1) {
                    }
                }
            }
        });
        jtp.addTab("Database", new BD());//Base de Datos
        // jtp.addTab("Real Time",new TR());//Tiempo Real
        f.add(jtp);
        /*try {
            f.setIconImage(ImageIO.read(new java.net.URL("http://www.itmexicali.edu.mx/images2/bufalo.jpg")));
        } catch (Exception e) {e.printStackTrace();}
        */
        
        f.setVisible(true);
        f.setTitle("Fire Detection");
        f.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        f.pack();
        f.setLocationRelativeTo(null);
    }
}
