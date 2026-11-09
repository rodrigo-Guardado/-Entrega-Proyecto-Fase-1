
package vista;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;

public final class Estilos {

    public static final Color AZUL = new Color(28, 55, 92);
    public static final Color AZUL_CLARO = new Color(54, 94, 145);
    public static final Color FONDO = new Color(245, 247, 250);
    public static final Color TEXTO = new Color(40, 48, 60);

    private static final Color AZUL_PRESIONADO =
            new Color(18, 39, 68);

    private static final Color GRIS_DESHABILITADO =
            new Color(220, 225, 232);

    private static final Color TEXTO_DESHABILITADO =
            new Color(100, 110, 125);

    private Estilos() {}

    public static void aplicar() {
        UIManager.put("Panel.background", FONDO);
        UIManager.put("OptionPane.messageFont",
                new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("Button.font",
                new Font("SansSerif", Font.BOLD, 13));
        UIManager.put("Label.foreground", TEXTO);
    }

    
public static void boton(JButton b) {

    b.setUI(new BasicButtonUI());

    b.setFont(new Font("SansSerif", Font.BOLD, 13));
    b.setFocusPainted(false);
    b.setBorderPainted(true);
    b.setContentAreaFilled(true);
    b.setOpaque(true);
    b.setRolloverEnabled(true);

    b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AZUL, 1),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
    ));

    b.setBackground(AZUL);
    b.setForeground(Color.WHITE);
    b.setBackground(AZUL);
    b.setForeground(Color.WHITE);
   

    b.getModel().addChangeListener(e -> {

        if (!b.isEnabled()) {
            b.setBackground(GRIS_DESHABILITADO);
            b.setForeground(TEXTO_DESHABILITADO);

        } else if (b.getModel().isPressed()) {
            b.setBackground(AZUL_PRESIONADO);
            b.setForeground(Color.WHITE);

        } else if (b.getModel().isRollover()) {
            b.setBackground(AZUL_CLARO);
            b.setForeground(Color.WHITE);

        } else {
            b.setBackground(AZUL);
            b.setForeground(Color.WHITE);
        }
    });
}


    public static void campo(JTextField t) {

        t.setFont(new Font("SansSerif", Font.PLAIN, 13));

        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(205, 213, 224)),
                BorderFactory.createEmptyBorder(7, 8, 7, 8)
        ));
    }
}
