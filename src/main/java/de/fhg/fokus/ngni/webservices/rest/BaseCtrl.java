package de.fhg.fokus.ngni.webservices.rest;

import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import de.fhg.fokus.ngni.services.CustomUserDetailsService;



public class BaseCtrl {

	protected static final Logger logger_c = Logger.getLogger(BaseCtrl.class);
	

	@Autowired
	protected Mongo mongoDb;
	
	@Autowired
	Client esClient;
	
	@Autowired
	protected CustomUserDetailsService customUserDetailsService;
	
	@Autowired
	protected View jsonView_i;

	@Autowired
	protected Boolean debugResponse;

	protected static final boolean SUCCESS = true;
	protected static final boolean ERROR = false;

	protected boolean isAuthorized() {
		// TODO implement isAuthorized()
		return true;
	}

	protected boolean canRead(String db, String user) {
		if (mongoDb.getDB(db).getCollectionNames().contains("users")) {
			DBCollection col = mongoDb.getDB(db).getCollection("users");
			col.find();
			DBCursor cursor = col.find();
			while (cursor.hasNext()) {
				DBObject doc = cursor.next();
				if (doc.get("username").toString().compareTo(user) == 0)
					return true;
			}
		}
		return false;
	}

	protected boolean canWrite(String db, String user) {
		if (mongoDb.getDB(db).getCollectionNames().contains("users")) {
			DBCollection col = mongoDb.getDB(db).getCollection("users");
			col.find();
			DBCursor cursor = col.find();
			while (cursor.hasNext()) {
				DBObject doc = cursor.next();
				if (doc.get("username").toString().compareTo(user) == 0 && !(Boolean)doc.get("readOnly"))
					return true;
			}
		}
		return false;
	}

	/*public String MD5(String md5) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest
					.getInstance("MD5");
			byte[] array = md.digest(md5.getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100)
						.substring(1, 3));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
		}
		return null;
	}*/

	/**
	 * response in json format for nosql quries The return will be either the
	 * response of a query such SELECT or a confirmation in case of INSERT.
	 * 
	 * 
	 */

	protected ModelAndView response(boolean sucess, Object result, String debug) {
		if (debugResponse) {
			if (sucess)
				if (debug != null)
					return new ModelAndView(jsonView_i, "debug", debug)
							.addObject("ok", "1");
				else
					return new ModelAndView(jsonView_i, "data", result)
							.addObject("ok", "1");
			else
				return new ModelAndView(jsonView_i, "debug", debug).addObject(
						"ok", "0");
		} else {
			if (sucess)
				if (result != null)
					return new ModelAndView(jsonView_i, "data", result)
							.addObject("ok", "1");
				else
					return new ModelAndView(jsonView_i, "ok", "1");
			else
				return new ModelAndView(jsonView_i, "ok", "0");
		}
	}
	
	protected boolean checkCollectionExist(String app,String collName){
		DB db = mongoDb.getDB(app);
		if (db.getCollectionNames().contains(collName))
			return true;
		return false;
	}

	/**
	 * Injector methods.
	 * 
	 * @param view
	 *            the new json view
	 */
	public void setJsonView(View view) {
		jsonView_i = view;
	}

	public void setCustomUserDetailsService(CustomUserDetailsService customUserDetailsService) {
		this.customUserDetailsService = customUserDetailsService;
	}

	public void setMongoDb(Mongo mongoDb) {
		this.mongoDb = mongoDb;
	}

	public void setEsClient(Client esClient) {
		this.esClient = esClient;
	}

	public void setDebugResponse(Boolean debugResponse) {
		this.debugResponse = debugResponse;
	}
}