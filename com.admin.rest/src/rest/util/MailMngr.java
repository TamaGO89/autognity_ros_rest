package rest.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

/*==============================================================================================================================
 * MAIL MANAGER * Compile and send emails
 *============================================================================================================================*/
public class MailMngr {
	
	private static Map<String,MailMngr> mail_mngr_map = new HashMap<String,MailMngr>();
	private static Map<String,Object> mail_map;
	private static Properties mail_props = new Properties();
	private static Map<String,MailTemplate> templates = new HashMap<String,MailTemplate>();
	private Map<String,Object> session_map;
	private Address from_addr, bcc_addr;
	private Session mail_session;
	
	/*--------------------------------------------------------------------------------------------------------------------------
	 * MAIL AUTHENTICATOR and TEMPLATE * Needed by the session to retrieve user and password and store message templates
	 *------------------------------------------------------------------------------------------------------------------------*/
	private static class MailAuthenticator extends Authenticator {
		private String username, password;
		public MailAuthenticator(String username, String password) { this.username = username; this.password = password; }
        protected PasswordAuthentication getPasswordAuthentication() { return new PasswordAuthentication(username, password); }
	}

	private static class MailTemplate {
		// Instance methods
		private String subject, text;
		// Constructor
		public MailTemplate(String subject, String text) { this.subject = subject; this.text = text; }
		// Public setters
		public String getSubject(Map<String,String> args) { return this.format(new String(this.subject), args); }
		public String getText(Map<String,String> args) { return this.format(new String(this.text), args); }
		public String getSubject(List<String> args) { return this.format(new String(this.subject), args); }
		public String getText(List<String> args) { return this.format(new String(this.text), args); }
		public String getSubject(String[] args) { return this.format(new String(this.subject), args); }
		public String getText(String[] args) { return this.format(new String(this.text), args); }
		// Private utilities
		private String format(String str, Map<String,String> args) {
			for (String key : args.keySet()) str = str.replaceAll("\\$\\{"+key+"\\}", args.get(key));
			return str; }
		private String format(String str, List<String> args) {
			for (int i = 0; i < args.size(); i++) str = str.replaceAll("\\$\\{"+i+"\\}", args.get(i));
			return str; }
		private String format(String str, String[] args) {
			for (int i = 0; i < args.length; i++) str = str.replaceAll("\\$\\{"+i+"\\}", args[i]);
			return str; }
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * CONSTRUCTOR * Define a new Mail Manager, with user, password, address, etc...
	 *------------------------------------------------------------------------------------------------------------------------*/
	public MailMngr (Map<String,Object> map) {
		this.session_map = map;
		this.mail_session = Session.getInstance(mail_props, new MailAuthenticator(this.getStr("username"),
																				  this.getStr("password")));
		try { 
			from_addr = new InternetAddress(this.getStr("addr"));
			if (this.getStr("bcc") != null) bcc_addr = new InternetAddress(this.getStr("bcc"));
		} catch (MessagingException e) { System.out.println(e); }
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * UTILITIES * Instantiate and retrieve a specified Mail Manager, setup the properties
	 *------------------------------------------------------------------------------------------------------------------------*/
	public static MailMngr getMailMngr(String key) { return getMailMngr().get(key); }
	// If the list of Mail Manager is empty, fill it
	@SuppressWarnings("unchecked") public static Map<String,MailMngr> getMailMngr() {
		if (!mail_mngr_map.isEmpty()) return mail_mngr_map;
		for (Map<String,Object> mngr_map : (List<Map<String,Object>>) mail_map.get("managers"))
			mail_mngr_map.putIfAbsent((String) mngr_map.get("name"), new MailMngr(mngr_map));
		return mail_mngr_map;
	}
	// Set the MAP for the MailManager and get INT, STRING and MAP from it
	private String getStr(String key) {
		return (this.session_map.containsKey(key)) ? (String) this.session_map.get(key) : (String) mail_map.get(key); }
	private int getInt(String key) {
		return (this.session_map.containsKey(key)) ? (int) this.session_map.get(key) : (int) mail_map.get(key); }
	@SuppressWarnings("unchecked") public static void setMailMap(Map<String,Object> m_map) {
		mail_map = m_map;
		mail_props.put("mail.smtp.host", (String) mail_map.get("host"));
		mail_props.put("mail.smtp.port", (int) mail_map.get("port"));
		mail_props.put("mail.transport.protocol", (String) mail_map.get("protocol"));
        mail_props.put("mail.smtp.starttls.enable", true);
		mail_props.put("mail.smtp.auth", true);
		for (Map<String,Object> template : (List<Map<String,Object>>) mail_map.get("templates")) {
			templates.put((String)template.get("name"), new MailTemplate((String) template.get("subject"),
																		 String.join("\n",(List<String>)template.get("text"))));
		}
			
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * PUBLIC METHODS * Senders and other public methods
	 *------------------------------------------------------------------------------------------------------------------------*/
	public void send(String recipient, String template, Map<String,String> args) throws MessagingException {
		MimeMessage message = this.getMime(recipient);
		message.setSubject(templates.get(template).getSubject(args));
		message.setText(templates.get(template).getText(args));
		Transport.send(message);
	}
	public void send(String recipient, String template, List<String> args) throws MessagingException {
		MimeMessage message = this.getMime(recipient);
		message.setSubject(templates.get(template).getSubject(args));
		message.setText(templates.get(template).getText(args));
		Transport.send(message);
	}
	public void send(String recipient, String template, String[] args) throws MessagingException {
		MimeMessage message = this.getMime(recipient);
		message.setSubject(templates.get(template).getSubject(args));
		message.setText(templates.get(template).getText(args));
		Transport.send(message);
	}
	private MimeMessage getMime(String recipient) throws MessagingException {
		MimeMessage mime_message = new MimeMessage(this.mail_session);
		mime_message.setFrom(this.from_addr);
		if (this.bcc_addr != null) mime_message.setRecipient(RecipientType.BCC, this.bcc_addr);
		mime_message.setRecipient(RecipientType.TO, new InternetAddress(recipient));
		return mime_message;
	}
}
