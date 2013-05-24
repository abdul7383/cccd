package de.fhg.fokus.ngni.cccd.model;

import java.sql.Timestamp;
import org.codehaus.jackson.annotate.JsonWriteNullProperties;

@JsonWriteNullProperties(false)
public class DocEvent {

	private String appName;
	
	private String collName;
	
	private String docId;
	
	private String bucketName;
	
	private String objectId;
	
	private String profiles;
	
	private Timestamp timestamp;
	
	private String status;

	
	public DocEvent(String appName, String collName, String docId, String bucketName, String objectId,
			 String status, String profiles) {
		super();
		this.appName = appName;
		this.collName = collName;
		this.docId = docId;
		this.bucketName = bucketName;
		this.objectId = objectId;
		this.timestamp = new Timestamp(System.currentTimeMillis());;
		this.status = status;
		this.profiles = profiles;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getCollName() {
		return collName;
	}

	public void setCollName(String collName) {
		this.collName = collName;
	}

	public String getDocId() {
		return docId;
	}

	public void setDocId(String docId) {
		this.docId = docId;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
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

	public String getProfiles() {
		return profiles;
	}

	public void setProfiles(String profiles) {
		this.profiles = profiles;
	}
}
