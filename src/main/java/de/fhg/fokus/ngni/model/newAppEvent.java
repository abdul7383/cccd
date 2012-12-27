package de.fhg.fokus.ngni.model;

import java.sql.Timestamp;
import org.codehaus.jackson.annotate.JsonWriteNullProperties;

@JsonWriteNullProperties(false)
public class newAppEvent {

	private String appName;
	
	private String secret;
	
	private Timestamp timestamp;
	
	private String status;
	
	public newAppEvent(String appName, String secret, String status) {
		super();
		this.appName = appName;
		this.secret = secret;
		this.status = status;
		this.timestamp = new Timestamp(System.currentTimeMillis());
	}

	public newAppEvent(String appName, String status) {
		super();
		this.appName = appName;
		this.status = status;
		this.timestamp = new Timestamp(System.currentTimeMillis());
	}

	public newAppEvent(String appName, String secret, String status, Timestamp timestamp) {
		super();
		this.appName = appName;
		this.secret = secret;
		this.status = status;
		this.timestamp = timestamp;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
