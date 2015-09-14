/**
 * This file is part of ExportMail library (check README).
 * Copyright (C) 2012-2013 Stanislav Nepochatov
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
**/

package ExportModules;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import Utils.IOControl;

/**
 * Mail message export class.
 * @author Stanislav Nepochatov <spoilt.exile@gmail.com>
 */
@Utils.RibbonIOModule(type="MAIL", property="EXPORT_MAIL", api_version=2)
public class Mail extends Export.Exporter {
    
    /**
     * SMTP session object.
     */
    private Session exportSession;
    
    /**
     * Constructor redirect.
     * @param givenMessage message to export;
     * @param givenSchema export scheme reference;
     * @param givenSwitch message index updater switch;
     * @param givenDir dir which message came from;
     */
    public Mail(MessageClasses.Message givenMessage, Export.Schema givenSchema, Export.ReleaseSwitch givenSwitch, String givenDir) {
        super(givenMessage, givenSchema, givenSwitch, givenDir);
    }

    @Override
    protected void doExport() throws Exception {
        final Properties mailInit = new Properties();
        mailInit.put("mail.smtp.host", this.currSchema.currConfig.getProperty("mail_smtp_address"));
        if (this.currSchema.currConfig.getProperty("mail_smtp_con_port") != null) {
            mailInit.put("mail.smtp.port", this.currSchema.currConfig.getProperty("mail_smtp_con_port"));
        }
        if (this.currSchema.currConfig.getProperty("mail_smtp_con_security") != null) {
            String sec = this.currSchema.currConfig.getProperty("mail_smtp_con_security");
            switch (sec) {
                case "ssl":
                    mailInit.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    mailInit.put("mail.smtp.socketFactory.port", this.currSchema.currConfig.getProperty("mail_smtp_con_port"));
                    mailInit.put("mail.smtp.ssl.enable", true);
                    break;
            }
        }
        if (this.currSchema.currConfig.getProperty("mail_smtp_login") != null && this.currSchema.currConfig.getProperty("mail_smtp_pass") != null) {
            mailInit.put("mail.user", this.currSchema.currConfig.getProperty("mail_smtp_login"));
            mailInit.put("mail.password", this.currSchema.currConfig.getProperty("mail_smtp_pass"));
            mailInit.put("mail.smtp.auth", "true");
        }
        exportSession = Session.getDefaultInstance(mailInit, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(mailInit.getProperty("mail.user"), mailInit.getProperty("mail.password"));
				}
			});
        String[] rcpList = null;
        if (this.currSchema.currConfig.getProperty("mail_rcp_list") != null) {
            rcpList = this.readRcpList(this.currSchema.currConfig.getProperty("mail_rcp_list"));
        }
        if (rcpList != null) {
            for (String rcp_address: rcpList) {
                sendMessageAsMail(rcp_address);
            }
        } else {
            sendMessageAsMail(this.currSchema.currConfig.getProperty("mail_to"));
        }
    }
    
    /**
     * Send current message as mail to the specified address.
     * @param mailProps mail initiation properties;
     * @param to current recipient address;
     * @throws Exception all exception during sending.
     */
    private void sendMessageAsMail(String to) throws Exception {
        MimeMessage message = new MimeMessage(exportSession);
        message.setFrom(new InternetAddress(this.currSchema.currConfig.getProperty("mail_from")));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        if (this.currSchema.currConfig.getProperty("mail_subject") != null) {
            if ("1".equals(this.currSchema.currConfig.getProperty("mail_subject_allow_template"))) {
                Export.Formater headerFormater = new Export.Formater(this.currSchema.currConfig, this.currSchema.currConfig.getProperty("mail_subject"));
                message.setSubject(headerFormater.format(exportedMessage, calledDir), exportedCharset);
            } else {
                message.setSubject(this.currSchema.currConfig.getProperty("mail_subject"), exportedCharset);
            }
        } else {
            message.setSubject(this.exportedMessage.HEADER, exportedCharset);
        }
        message.setHeader("X-Mailer", "Ribbon System ExportMail module");
        message.setContent(this.exportedContent.getBytes(exportedCharset), "text/plain; charset=" + exportedCharset);
        Transport.send(message);
    }
    
    /**
     * Read recipient lists and return its lines.
     * @return string array with recipients;
     */
    private String[] readRcpList(String name) {
        String[] returned = null;
        try {
            returned = new String(java.nio.file.Files.readAllBytes(new java.io.File(IOControl.EXPORT_DIR + "/" + name).toPath())).split("\n");
        } catch (java.io.IOException ex) {
            IOControl.serverWrapper.log(IOControl.EXPORT_LOGID + ":" + this.currSchema.name, 1, 
            "неможливо прочитати список розсилки - експорт буде проведено до адреси у параметрі mail_to\n"
            + "Шлях до файлу списку розсилки:" + IOControl.EXPORT_DIR + "/" + name);
        }
        return returned;
    }
}
