package br.com.fiap.clinic.notification.domain.service;

import br.com.fiap.clinic.notification.domain.enums.NotificationType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailTemplateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public String buildEmailTemplate(
            NotificationType type,
            String patientName,
            String doctorName,
            String doctorSpecialty,
            LocalDateTime appointmentDate
    ) {
        return switch (type) {
            case LEMBRETE -> buildReminderTemplate(patientName, doctorName, doctorSpecialty, appointmentDate);
            case AGENDAMENTO -> buildAppointmentConfirmationTemplate(patientName, doctorName, doctorSpecialty, appointmentDate);
        };
    }

    private String buildReminderTemplate(
            String patientName,
            String doctorName,
            String doctorSpecialty,
            LocalDateTime appointmentDate
    ) {
        String date = appointmentDate.format(DATE_FORMATTER);
        String time = appointmentDate.format(TIME_FORMATTER);

        String template = """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Lembrete de Consulta</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #f4f7fa;
                            margin: 0;
                            padding: 0;
                        }
                        .container {
                            max-width: 600px;
                            margin: 40px auto;
                            background-color: #ffffff;
                            border-radius: 12px;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                            overflow: hidden;
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            padding: 30px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            font-weight: 600;
                        }
                        .header p {
                            margin: 10px 0 0 0;
                            font-size: 16px;
                            opacity: 0.9;
                        }
                        .content {
                            padding: 40px 30px;
                        }
                        .greeting {
                            font-size: 18px;
                            color: #333;
                            margin-bottom: 20px;
                        }
                        .info-box {
                            background-color: #f8f9fc;
                            border-left: 4px solid #667eea;
                            padding: 20px;
                            margin: 25px 0;
                            border-radius: 6px;
                        }
                        .info-box h2 {
                            margin: 0 0 15px 0;
                            color: #667eea;
                            font-size: 20px;
                        }
                        .info-item {
                            display: flex;
                            margin: 12px 0;
                            align-items: center;
                        }
                        .info-label {
                            font-weight: 600;
                            color: #555;
                            min-width: 120px;
                        }
                        .info-value {
                            color: #333;
                            font-size: 16px;
                        }
                        .reminder-icon {
                            font-size: 48px;
                            text-align: center;
                            margin: 20px 0;
                        }
                        .message {
                            color: #666;
                            line-height: 1.6;
                            font-size: 15px;
                        }
                        .footer {
                            background-color: #f8f9fc;
                            padding: 25px 30px;
                            text-align: center;
                            color: #888;
                            font-size: 13px;
                            border-top: 1px solid #e0e0e0;
                        }
                        .cta-button {
                            display: inline-block;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            padding: 14px 32px;
                            text-decoration: none;
                            border-radius: 6px;
                            margin: 20px 0;
                            font-weight: 600;
                            font-size: 16px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🔔 Lembrete de Consulta</h1>
                            <p>Sua consulta está chegando!</p>
                        </div>
                        <div class="content">
                            <p class="greeting">Olá, <strong>{{PATIENT_NAME}}</strong>!</p>
                            <p class="message">
                                Este é um lembrete amigável sobre sua consulta agendada. 
                                Não se esqueça de chegar com alguns minutos de antecedência.
                            </p>
                            
                            <div class="info-box">
                                <h2>📋 Detalhes da Consulta</h2>
                                <div class="info-item">
                                    <span class="info-label">📅 Data:</span>
                                    <span class="info-value">{{DATE}}</span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label">🕐 Horário:</span>
                                    <span class="info-value">{{TIME}}</span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label">👨‍⚕️ Médico(a):</span>
                                    <span class="info-value">{{DOCTOR_NAME}}</span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label">🏥 Especialidade:</span>
                                    <span class="info-value">{{SPECIALTY}}</span>
                                </div>
                            </div>
                            
                            <p class="message">
                                <strong>💡 Lembre-se de:</strong><br>
                                • Trazer documentos pessoais e cartão do convênio<br>
                                • Chegar 15 minutos antes do horário<br>
                                • Trazer exames anteriores, se houver
                            </p>
                        </div>
                        <div class="footer">
                            <p><strong>ClinicFIAPP</strong></p>
                            <p>Cuidando da sua saúde com tecnologia e carinho</p>
                            <p style="margin-top: 15px; font-size: 12px;">
                                Este é um e-mail automático, por favor não responda.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """;

        return template
                .replace("{{PATIENT_NAME}}", patientName)
                .replace("{{DATE}}", date)
                .replace("{{TIME}}", time)
                .replace("{{DOCTOR_NAME}}", doctorName)
                .replace("{{SPECIALTY}}", doctorSpecialty);
    }

    private String buildAppointmentConfirmationTemplate(
            String patientName,
            String doctorName,
            String doctorSpecialty,
            LocalDateTime appointmentDate
    ) {
        String date = appointmentDate.format(DATE_FORMATTER);
        String time = appointmentDate.format(TIME_FORMATTER);

        String template = """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Confirmação de Agendamento</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #f4f7fa;
                            margin: 0;
                            padding: 0;
                        }
                        .container {
                            max-width: 600px;
                            margin: 40px auto;
                            background-color: #ffffff;
                            border-radius: 12px;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                            overflow: hidden;
                        }
                        .header {
                            background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
                            color: white;
                            padding: 30px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            font-weight: 600;
                        }
                        .header p {
                            margin: 10px 0 0 0;
                            font-size: 16px;
                            opacity: 0.9;
                        }
                        .content {
                            padding: 40px 30px;
                        }
                        .greeting {
                            font-size: 18px;
                            color: #333;
                            margin-bottom: 20px;
                        }
                        .success-icon {
                            text-align: center;
                            font-size: 64px;
                            margin: 20px 0;
                        }
                        .info-box {
                            background-color: #f0fdf4;
                            border-left: 4px solid #11998e;
                            padding: 20px;
                            margin: 25px 0;
                            border-radius: 6px;
                        }
                        .info-box h2 {
                            margin: 0 0 15px 0;
                            color: #11998e;
                            font-size: 20px;
                        }
                        .info-item {
                            display: flex;
                            margin: 12px 0;
                            align-items: center;
                        }
                        .info-label {
                            font-weight: 600;
                            color: #555;
                            min-width: 120px;
                        }
                        .info-value {
                            color: #333;
                            font-size: 16px;
                        }
                        .message {
                            color: #666;
                            line-height: 1.6;
                            font-size: 15px;
                        }
                        .footer {
                            background-color: #f8f9fc;
                            padding: 25px 30px;
                            text-align: center;
                            color: #888;
                            font-size: 13px;
                            border-top: 1px solid #e0e0e0;
                        }
                        .highlight-box {
                            background-color: #fff7ed;
                            border: 2px solid #fb923c;
                            border-radius: 8px;
                            padding: 15px;
                            margin: 20px 0;
                            text-align: center;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>✅ Agendamento Confirmado!</h1>
                            <p>Sua consulta foi agendada com sucesso</p>
                        </div>
                        <div class="content">
                            <div class="success-icon">🎉</div>
                            
                            <p class="greeting">Parabéns, <strong>{{PATIENT_NAME}}</strong>!</p>
                            <p class="message">
                                Seu agendamento foi realizado com sucesso! Estamos felizes em poder atendê-lo(a).
                            </p>
                            
                            <div class="info-box">
                                <h2>📋 Informações do Agendamento</h2>
                                <div class="info-item">
                                    <span class="info-label">📅 Data:</span>
                                    <span class="info-value">{{DATE}}</span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label">🕐 Horário:</span>
                                    <span class="info-value">{{TIME}}</span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label">👨‍⚕️ Médico(a):</span>
                                    <span class="info-value">{{DOCTOR_NAME}}</span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label">🏥 Especialidade:</span>
                                    <span class="info-value">{{SPECIALTY}}</span>
                                </div>
                            </div>
                            
                            <div class="highlight-box">
                                <p style="margin: 0; color: #c2410c; font-weight: 600;">
                                    ⚡ Você receberá um lembrete automático antes da consulta
                                </p>
                            </div>
                            
                            <p class="message">
                                <strong>📝 Orientações importantes:</strong><br>
                                • Guarde esta confirmação para referência<br>
                                • Chegue com 15 minutos de antecedência<br>
                                • Traga documentos e cartão do convênio<br>
                                • Em caso de dúvidas, entre em contato conosco
                            </p>
                        </div>
                        <div class="footer">
                            <p><strong>ClinicFIAPP</strong></p>
                            <p>Cuidando da sua saúde com tecnologia e carinho</p>
                            <p style="margin-top: 15px; font-size: 12px;">
                                Este é um e-mail automático, por favor não responda.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """;

        return template
                .replace("{{PATIENT_NAME}}", patientName)
                .replace("{{DATE}}", date)
                .replace("{{TIME}}", time)
                .replace("{{DOCTOR_NAME}}", doctorName)
                .replace("{{SPECIALTY}}", doctorSpecialty);
    }

    public String getSubject(NotificationType type) {
        return switch (type) {
            case LEMBRETE -> "🔔 Lembrete: Sua consulta está próxima!";
            case AGENDAMENTO -> "✅ Consulta agendada com sucesso!";
        };
    }
}
