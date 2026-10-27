package com.docufind.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class UITheme {
    // Exact requested color palette
    public static final Color BG_MAIN = new Color(0xF7, 0xF5, 0xF2);       // #F7F5F2 warm off-white
    public static final Color SIDEBAR_BG = new Color(0x20, 0x2B, 0x46);    // #202B46 deep navy
    public static final Color ACCENT_PRIMARY = new Color(0x27, 0x8C, 0x87);// #278C87 muted teal
    public static final Color ACCENT_SECONDARY = new Color(0xE9, 0xE5, 0xF5);// #E9E5F5 soft lavender
    public static final Color CARD_BG = Color.WHITE;                       // #FFFFFF card white
    public static final Color TEXT_MAIN = new Color(0x25, 0x2A, 0x34);     // #252A34 dark charcoal
    public static final Color TEXT_MUTED = new Color(0x68, 0x73, 0x86);    // #687386 muted slate
    public static final Color SUCCESS = new Color(0x2E, 0x7D, 0x32);       // soft green
    public static final Color WARNING = new Color(0xED, 0x6C, 0x02);       // warm amber
    public static final Color ERROR = new Color(0xD3, 0x2F, 0x2F);         // accessible red
    public static final Color BORDER_COLOR = new Color(0xE2, 0xE0, 0xDC);  // subtle border

    // Typography
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_CARD_TITLE = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_CARD_VALUE = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    /**
     * Creates a styled modern button.
     */
    public static JButton createButton(String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 24, 38));

        if (primary) {
            btn.setBackground(ACCENT_PRIMARY);
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(0xE8, 0xEC, 0xEF));
            btn.setForeground(TEXT_MAIN);
        }

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (primary) {
                    btn.setBackground(new Color(0x1F, 0x73, 0x6E));
                } else {
                    btn.setBackground(new Color(0xD9, 0xE0, 0xE6));
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (primary) {
                    btn.setBackground(ACCENT_PRIMARY);
                } else {
                    btn.setBackground(new Color(0xE8, 0xEC, 0xEF));
                }
            }
        });

        return btn;
    }

    /**
     * Creates a white rounded card panel with subtle border.
     */
    public static JPanel createCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        return card;
    }
}
