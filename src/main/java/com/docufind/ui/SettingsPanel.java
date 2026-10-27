package com.docufind.ui;

import com.docufind.config.AppConfig;
import com.docufind.db.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SettingsPanel extends JPanel {
    private JTextField txtDbUrl;
    private JTextField txtDbUser;
    private JPasswordField txtDbPass;
    private JTextField txtMaxFileSize;
    private JTextField txtPreviewLimit;
    private JLabel lblStatus;

    public SettingsPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initComponents();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Application Settings");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JLabel lblDesc = new JLabel("Manage Oracle SQL connection parameters and document indexing constraints.");
        lblDesc.setFont(UITheme.FONT_REGULAR);
        lblDesc.setForeground(UITheme.TEXT_MUTED);

        topPanel.add(lblTitle);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(lblDesc);
        topPanel.add(Box.createVerticalStrut(20));

        add(topPanel, BorderLayout.NORTH);

        // Center Form Card
        JPanel cardForm = UITheme.createCard();
        cardForm.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        txtDbUrl = new JTextField(AppConfig.get("db.url"), 30);
        txtDbUser = new JTextField(AppConfig.get("db.user"), 30);
        txtDbPass = new JPasswordField(AppConfig.get("db.password"), 30);
        txtMaxFileSize = new JTextField(String.valueOf(AppConfig.getInt("app.max_file_size_mb", 25)), 10);
        txtPreviewLimit = new JTextField(String.valueOf(AppConfig.getInt("app.preview_char_limit", 3000)), 10);

        int row = 0;
        addFormRow(cardForm, gbc, row++, "Oracle JDBC URL:", txtDbUrl);
        addFormRow(cardForm, gbc, row++, "Oracle Username:", txtDbUser);
        addFormRow(cardForm, gbc, row++, "Oracle Password:", txtDbPass);
        addFormRow(cardForm, gbc, row++, "Max Index File Size (MB):", txtMaxFileSize);
        addFormRow(cardForm, gbc, row++, "Preview Character Limit:", txtPreviewLimit);

        lblStatus = new JLabel(" ");
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        gbc.gridx = 1;
        gbc.gridy = row++;
        cardForm.add(lblStatus, gbc);

        // Buttons Row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);

        JButton btnTest = UITheme.createButton("🔌 Test Oracle Connection", false);
        btnTest.addActionListener(e -> handleTestConnection());

        JButton btnSave = UITheme.createButton("💾 Save Preferences", true);
        btnSave.addActionListener(e -> handleSaveSettings());

        btnRow.add(btnTest);
        btnRow.add(btnSave);

        gbc.gridx = 1;
        gbc.gridy = row++;
        cardForm.add(btnRow, gbc);

        // Wrap card in flow to prevent vertical over-stretching
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        centerWrapper.setOpaque(false);
        cardForm.setPreferredSize(new Dimension(750, 360));
        centerWrapper.add(cardForm);

        add(centerWrapper, BorderLayout.CENTER);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_MAIN);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        comp.setFont(UITheme.FONT_REGULAR);
        comp.setPreferredSize(new Dimension(comp.getPreferredSize().width, 32));
        panel.add(comp, gbc);
    }

    private void handleTestConnection() {
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        lblStatus.setText("Testing Oracle connection...");

        String url = txtDbUrl.getText().trim();
        String user = txtDbUser.getText().trim();
        String pass = new String(txtDbPass.getPassword());

        AppConfig.set("db.url", url);
        AppConfig.set("db.user", user);
        AppConfig.set("db.password", pass);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return DBConnection.testConnection();
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        lblStatus.setForeground(UITheme.SUCCESS);
                        lblStatus.setText("✅ Successfully connected to Oracle Database!");
                    } else {
                        lblStatus.setForeground(UITheme.ERROR);
                        lblStatus.setText("❌ Connection failed. Verify URL, port, service name, and credentials.");
                    }
                } catch (Exception ex) {
                    lblStatus.setForeground(UITheme.ERROR);
                    lblStatus.setText("Error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleSaveSettings() {
        AppConfig.set("db.url", txtDbUrl.getText().trim());
        AppConfig.set("db.user", txtDbUser.getText().trim());
        AppConfig.set("db.password", new String(txtDbPass.getPassword()));
        AppConfig.set("app.max_file_size_mb", txtMaxFileSize.getText().trim());
        AppConfig.set("app.preview_char_limit", txtPreviewLimit.getText().trim());

        lblStatus.setForeground(UITheme.SUCCESS);
        lblStatus.setText("Preferences updated in active runtime session.");
        JOptionPane.showMessageDialog(this, "Application preferences saved successfully!", "Saved", JOptionPane.INFORMATION_MESSAGE);
    }
}
