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
public class Mail extends Export.Exporter {
    
    public static String type = "MAIL";
    
    public static String propertyType = "EXPORT_MAIL";
    
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
    protected void doExport() {
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
            }
        }
        if (this.currSchema.currConfig.getProperty("mail_smtp_login") != null && this.currSchema.currConfig.getProperty("mail_smtp_pass") != null) {
            mailInit.put("mail.user", this.currSchema.currConfig.getProperty("mail_smtp_login"));
            mailInit.put("mail.password", this.currSchema.currConfig.getProperty("mail_smtp_pass"));
            mailInit.put("mail.smtp.auth", "true");
        }
        String defCharset;
        if (this.currSchema.currConfig.getProperty("opt_charset") != null) {
            defCharset = this.currSchema.currConfig.getProperty("opt_charset");
        } else {
            defCharset = "UTF-8";
        }
        Session exportSes = Session.getDefaultInstance(mailInit, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(mailInit.getProperty("mail.user"), mailInit.getProperty("mail.password"));
				}
			});
        try {
            MimeMessage message = new MimeMessage(exportSes);
            message.setFrom(new InternetAddress(this.currSchema.currConfig.getProperty("mail_from")));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(this.currSchema.currConfig.getProperty("mail_to")));
            if (this.currSchema.currConfig.getProperty("mail_subject") != null) {
                message.setSubject(this.currSchema.currConfig.getProperty("mail_subject"), defCharset);
            } else {
                message.setSubject(this.exportedMessage.HEADER, defCharset);
            }
            message.setHeader("X-Mailer", "Ribbon System ExportMail module vx.1");
            message.setContent(this.exportedContent.getBytes(defCharset), "text/plain; charset=" + defCharset);
            Transport.send(message);
            if ("1".equals(this.currSchema.currConfig.getProperty("opt_log"))) {
                IOControl.serverWrapper.log(IOControl.EXPORT_LOGID + ":" + this.currSchema.name, 3, "прозведено експорт повідомлення " + this.exportedMessage.INDEX);
            }
            exportedMessage.PROPERTIES.add(new MessageClasses.MessageProperty(this.propertyType, "root", this.currSchema.currConfig.getProperty("export_print"), IOControl.serverWrapper.getDate()));
        } catch (MessagingException ex) {
            IOControl.serverWrapper.log(IOControl.EXPORT_LOGID + ":" + this.currSchema.name, 1, "неможливо відправити повідомлення");
            ex.printStackTrace();
        } catch (java.io.UnsupportedEncodingException ex) {
            IOControl.serverWrapper.log(IOControl.EXPORT_LOGID + ":" + this.currSchema.name, 1, "неможливо відправити повідомлення");
            ex.printStackTrace();
        }
    }

    @Override
    public void tryRecovery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void resetState() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
