package com.empresa.rcp.module.auth;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Properties;

public class EmailService {

    private static String smtpHost = "smtp.gmail.com";
    private static String smtpPort = "587";
    private static String smtpUser = "";
    private static String smtpPassword = "";

    static {
        // Cargar desde archivo local config.properties si existe
        Properties localProps = new Properties();
        File configFile = new File("config.properties");
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                localProps.load(fis);
                smtpHost = localProps.getProperty("smtp.host", "smtp.gmail.com");
                smtpPort = localProps.getProperty("smtp.port", "587");
                smtpUser = localProps.getProperty("smtp.user", "");
                smtpPassword = localProps.getProperty("smtp.password", "");
            } catch (IOException e) {
                System.err.println("[EmailService] Error al leer config.properties: " + e.getMessage());
            }
        }

        // Sobrescribir con variables de entorno si están presentes (mayor prioridad)
        String envUser = System.getenv("SMTP_USER");
        String envPassword = System.getenv("SMTP_PASSWORD");
        String envHost = System.getenv("SMTP_HOST");
        String envPort = System.getenv("SMTP_PORT");

        if (envUser != null && !envUser.isEmpty()) smtpUser = envUser;
        if (envPassword != null && !envPassword.isEmpty()) smtpPassword = envPassword;
        if (envHost != null && !envHost.isEmpty()) smtpHost = envHost;
        if (envPort != null && !envPort.isEmpty()) smtpPort = envPort;
    }

    private static final SecureRandom random = new SecureRandom();

    /**
     * Genera un código aleatorio numérico de 6 dígitos.
     */
    public static String generarCodigo() {
        int numero = random.nextInt(900000) + 100000; // Rango [100000, 999999]
        return String.valueOf(numero);
    }

    /**
     * Envía de forma asíncrona un correo electrónico con el código de verificación.
     */
    public static void enviarCodigoAsincrono(String destinatario, String codigo) {
        new Thread(() -> {
            System.out.println("=================================================");
            System.out.println("CÓDIGO DE RECUPERACIÓN GENERADO: " + codigo);
            System.out.println("Para el usuario/email: " + destinatario);
            System.out.println("=================================================");

            try {
                // Verificar si las credenciales están configuradas
                if (smtpUser == null || smtpUser.isEmpty() || smtpPassword == null || smtpPassword.isEmpty()) {
                    System.out.println("[EmailService] Modo local de pruebas (Consola). Crea config.properties o define SMTP_USER y SMTP_PASSWORD.");
                    return;
                }

                Properties prop = new Properties();
                prop.put("mail.smtp.auth", "true");
                prop.put("mail.smtp.starttls.enable", "true");
                prop.put("mail.smtp.host", smtpHost);
                prop.put("mail.smtp.port", smtpPort);

                Session session = Session.getInstance(prop, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(smtpUser, smtpPassword);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(smtpUser));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
                message.setSubject("Código de Recuperación - RCP Gestión");
                message.setText("Hola,\n\nHemos recibido una solicitud para restablecer tu contraseña.\n\n"
                        + "Tu código de verificación de un solo uso es: " + codigo + "\n\n"
                        + "Este código es válido durante 15 minutos.\n\n"
                        + "Si no solicitaste este cambio, puedes ignorar este correo de forma segura.\n\n"
                        + "Atentamente,\nEl Equipo de RCP Gestión");

                Transport.send(message);
                System.out.println("[EmailService] Correo enviado correctamente a: " + destinatario);
            } catch (Exception e) {
                System.err.println("[EmailService] Error al enviar el correo a " + destinatario + ". Detalle: " + e.getMessage());
                System.out.println("[EmailService] NOTA: Puedes usar el código impreso en la consola superior (" + codigo + ") para continuar las pruebas en tu entorno local.");
            }
        }).start();
    }
}
