package com.docufind.ui;

import com.docufind.security.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginFrame extends JFrame {
    private final AuthService authService;
    private final Runnable onLoginSuccess;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JCheckBox chkShowPassword;
    private JLabel lblStatus;
    private JButton btnLogin;
    private JButton btnSwitchRegister;

    // Registration fields
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private JTextField txtRegUsername;
    private JTextField txtRegEmail;
    private JPasswordField txtRegPassword;
    private JPasswordField txtRegConfirmPassword;
    private JLabel lblRegStatus;

    public LoginFrame(Runnable onLoginSuccess) {
        this.authService = new AuthService();
        this.onLoginSuccess = onLoginSuccess;

        setTitle("DocuFind — Offline Personal Search Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 680);
        setMinimumSize(new Dimension(460, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_MAIN);
        setLayout(new BorderLayout());

        initComponents();
    }

    private void initComponents() {
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        cardPanel.add(buildLoginPanel(), "LOGIN");
        cardPanel.add(buildRegisterPanel(), "REGISTER");

        add(cardPanel, BorderLayout.CENTER);
    }

    private JPanel buildLoginPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel card = UITheme.createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(400, 560));
        card.setMaximumSize(new Dimension(420, 580));

        // Header illustration / title
        JLabel lblIcon = new JLabel("🔍", SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("DocuFind");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.SIDEBAR_BG);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel("Offline Personal Document Search");
        lblSubtitle.setFont(UITheme.FONT_SMALL);
        lblSubtitle.setForeground(UITheme.TEXT_MUTED);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Offline storage badge
        JLabel lblBadge = new JLabel("🔒 Local Storage & Oracle SQL", SwingConstants.CENTER);
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblBadge.setForeground(UITheme.ACCENT_PRIMARY);
        lblBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalStrut(10));
        card.add(lblIcon);
        card.add(Box.createVerticalStrut(6));
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(4));
        card.add(lblSubtitle);
        card.add(Box.createVerticalStrut(8));
        card.add(lblBadge);
        card.add(Box.createVerticalStrut(25));

        // Username Field
        JLabel lblUser = new JLabel("Username or Email");
        lblUser.setFont(UITheme.FONT_BOLD);
        lblUser.setForeground(UITheme.TEXT_MAIN);
        lblUser.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtUsername = new JTextField(20);
        txtUsername.setFont(UITheme.FONT_REGULAR);
        txtUsername.setMaximumSize(new Dimension(340, 36));

        card.add(wrapField(lblUser, txtUsername));
        card.add(Box.createVerticalStrut(12));

        // Password Field
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(UITheme.FONT_BOLD);
        lblPass.setForeground(UITheme.TEXT_MAIN);

        txtPassword = new JPasswordField(20);
        txtPassword.setFont(UITheme.FONT_REGULAR);
        txtPassword.setMaximumSize(new Dimension(340, 36));

        card.add(wrapField(lblPass, txtPassword));
        card.add(Box.createVerticalStrut(6));

        // Show/hide password
        chkShowPassword = new JCheckBox("Show password");
        chkShowPassword.setFont(UITheme.FONT_SMALL);
        chkShowPassword.setOpaque(false);
        chkShowPassword.setAlignmentX(Component.CENTER_ALIGNMENT);
        chkShowPassword.addActionListener(e -> {
            if (chkShowPassword.isSelected()) {
                txtPassword.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar('•');
            }
        });
        card.add(chkShowPassword);
        card.add(Box.createVerticalStrut(10));

        // Status Label
        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.ERROR);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblStatus);
        card.add(Box.createVerticalStrut(10));

        // Login Button
        btnLogin = UITheme.createButton("Log In", true);
        btnLogin.setMaximumSize(new Dimension(340, 40));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.addActionListener(this::handleLogin);
        card.add(btnLogin);
        card.add(Box.createVerticalStrut(12));

        // Register Button
        btnSwitchRegister = UITheme.createButton("Create New Account", false);
        btnSwitchRegister.setMaximumSize(new Dimension(340, 40));
        btnSwitchRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSwitchRegister.addActionListener(e -> {
            lblRegStatus.setText(" ");
            cardLayout.show(cardPanel, "REGISTER");
        });
        card.add(btnSwitchRegister);

        // Bind Enter key
        txtPassword.addActionListener(this::handleLogin);
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());

        wrapper.add(card);
        return wrapper;
    }

    private JPanel buildRegisterPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel card = UITheme.createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(400, 580));
        card.setMaximumSize(new Dimension(420, 600));

        JLabel lblTitle = new JLabel("Create Account");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.SIDEBAR_BG);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel("First registered user gets ADMIN role");
        lblSubtitle.setFont(UITheme.FONT_SMALL);
        lblSubtitle.setForeground(UITheme.ACCENT_PRIMARY);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalStrut(10));
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(4));
        card.add(lblSubtitle);
        card.add(Box.createVerticalStrut(20));

        // Fields
        txtRegUsername = new JTextField(20);
        txtRegUsername.setMaximumSize(new Dimension(340, 34));
        card.add(wrapField(new JLabel("Username (min 3 chars)"), txtRegUsername));
        card.add(Box.createVerticalStrut(10));

        txtRegEmail = new JTextField(20);
        txtRegEmail.setMaximumSize(new Dimension(340, 34));
        card.add(wrapField(new JLabel("Email Address"), txtRegEmail));
        card.add(Box.createVerticalStrut(10));

        txtRegPassword = new JPasswordField(20);
        txtRegPassword.setMaximumSize(new Dimension(340, 34));
        card.add(wrapField(new JLabel("Password (min 6 chars)"), txtRegPassword));
        card.add(Box.createVerticalStrut(10));

        txtRegConfirmPassword = new JPasswordField(20);
        txtRegConfirmPassword.setMaximumSize(new Dimension(340, 34));
        card.add(wrapField(new JLabel("Confirm Password"), txtRegConfirmPassword));
        card.add(Box.createVerticalStrut(8));

        lblRegStatus = new JLabel(" ", SwingConstants.CENTER);
        lblRegStatus.setFont(UITheme.FONT_SMALL);
        lblRegStatus.setForeground(UITheme.ERROR);
        lblRegStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblRegStatus);
        card.add(Box.createVerticalStrut(10));

        JButton btnRegister = UITheme.createButton("Register", true);
        btnRegister.setMaximumSize(new Dimension(340, 40));
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegister.addActionListener(this::handleRegister);
        card.add(btnRegister);
        card.add(Box.createVerticalStrut(10));

        JButton btnBackToLogin = UITheme.createButton("Back to Login", false);
        btnBackToLogin.setMaximumSize(new Dimension(340, 40));
        btnBackToLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnBackToLogin.addActionListener(e -> cardLayout.show(cardPanel, "LOGIN"));
        card.add(btnBackToLogin);

        wrapper.add(card);
        return wrapper;
    }

    private JPanel wrapField(JLabel label, JComponent field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(340, 60));
        label.setFont(UITheme.FONT_BOLD);
        label.setForeground(UITheme.TEXT_MAIN);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(label);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        return p;
    }

    private void handleLogin(ActionEvent e) {
        lblStatus.setText(" ");
        String username = txtUsername.getText().trim();
        char[] password = txtPassword.getPassword();

        if (username.isEmpty() || password.length == 0) {
            lblStatus.setText("Please enter username and password.");
            return;
        }

        btnLogin.setEnabled(false);
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        lblStatus.setText("Authenticating with Oracle...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return authService.login(username, password);
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                try {
                    boolean success = get();
                    if (success) {
                        lblStatus.setForeground(UITheme.SUCCESS);
                        lblStatus.setText("Login successful!");
                        dispose();
                        onLoginSuccess.run();
                    } else {
                        lblStatus.setForeground(UITheme.ERROR);
                        lblStatus.setText("Invalid credentials or account inactive.");
                    }
                } catch (Exception ex) {
                    lblStatus.setForeground(UITheme.ERROR);
                    lblStatus.setText("Connection error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleRegister(ActionEvent e) {
        lblRegStatus.setText(" ");
        String username = txtRegUsername.getText().trim();
        String email = txtRegEmail.getText().trim();
        char[] pass = txtRegPassword.getPassword();
        char[] confirm = txtRegConfirmPassword.getPassword();

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return authService.registerUser(username, email, pass, confirm);
            }

            @Override
            protected void done() {
                try {
                    String err = get();
                    if (err == null) {
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Account registered successfully! You may now log in.",
                                "Registration Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        txtUsername.setText(username);
                        txtPassword.setText("");
                        cardLayout.show(cardPanel, "LOGIN");
                    } else {
                        lblRegStatus.setForeground(UITheme.ERROR);
                        lblRegStatus.setText(err);
                    }
                } catch (Exception ex) {
                    lblRegStatus.setForeground(UITheme.ERROR);
                    lblRegStatus.setText("Registration failed: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
