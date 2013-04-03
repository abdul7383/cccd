package de.fhg.fokus.ngni.cccd.rest;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;

import de.fhg.fokus.ngni.cccd.model.AppEvent;

/**
 * FundsController class will expose a series of RESTful endpoints
 */
@SuppressWarnings("unchecked")
@Controller
public class AppCtrl extends BaseCtrl {

	protected static final Logger logger_c = Logger.getLogger(AppCtrl.class);

	// create App, addUser, settingCollection
	@RequestMapping(value = "/app/{appName}", method = RequestMethod.POST)
	public ModelAndView createApp(@PathVariable String appName,
			Principal principal, @RequestBody String body) {
		logger_c.debug("/app/" + appName + " : doPOST()");

		if (mongoDb.getDatabaseNames().contains(appName))
			return response(false, null, "app: " + appName
					+ " is already created");
		if(body == null || body.compareTo("") == 0)
			return response(false, null, "you have to specify the settings for this app, at least the secret setting i.e {\"secret\": \"simpleSecret\"}");
		DB db = mongoDb.getDB(appName);
		HashMap<String, Object> jsonBody;
		try {
			jsonBody = new ObjectMapper().readValue(body, HashMap.class);
		} catch (Exception e) {
			return response(false, null, "jsonBody error: " + e.getMessage());
		}
		BasicDBObject doc = new BasicDBObject(jsonBody);
		db.createCollection("conf", doc).save(doc);
		BasicDBObject user = new BasicDBObject();
		user.append("username", principal.getName());
		user.append("readOnly", false);
		DBCollection coll = db.createCollection("users", user);
		coll.ensureIndex(new BasicDBObject("username", 1), null, true);
		coll.save(user);
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			amqpTemplate.convertAndSend("cccdApp",mapper.writeValueAsString(new AppEvent(appName,jsonBody.get("secret").toString(),"created")));
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AmqpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return response(true, null, "app: " + appName + " created");
	}

	// update App
	@RequestMapping(value = "/app/{appName}", method = RequestMethod.PUT)
	public ModelAndView updateApp(
			@PathVariable String appName,
			Principal principal,
			@RequestBody String body) {
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have WRITE permission");
		}
		logger_c.debug("/app/" + appName + " : doPUT()");

		
		 DB db = mongoDb.getDB(appName); 
		 HashMap<String, Object> jsonBody;
		 try{ 
			 jsonBody = new ObjectMapper().readValue(body, HashMap.class);
		 }
		 catch (Exception e) { 
			 return response(false, null, e.getMessage());
		 }
		 BasicDBObject doc = new BasicDBObject("$set",jsonBody);
		 // TODO update the secret in content distribution if needed!!
		 db.getCollection("conf").update(new BasicDBObject(), doc, true, false);
		return response(true, null, "app: updated succecfully");
	}

	// create App with setting
	/*
	 * @RequestMapping(value = "/app/{appName}", method = RequestMethod.PUT)
	 * public ModelAndView createAppWithSetting(@PathVariable String
	 * appName,@RequestParam(value = "op", required = false) String op) { //
	 * TODO impl. createAppWithSetting() return null; if (!isAuthorized())
	 * return response(false, null, "Invalid username or password");
	 * logger_c.debug("/app/"+appName+" : doPut()");
	 * 
	 * if(mongoDb.getDatabaseNames().contains(appName)) return response(false,
	 * null, "app: "+appName+" is already created");
	 * 
	 * DB db = mongoDb.getDB(appName); BasicDBObject doc = new BasicDBObject();
	 * doc.put("appName", appName); db.createCollection("conf", doc); return
	 * response(true, null,"app: "+appName+" created"); }
	 */

	// list Apps
	@RequestMapping(value = "/app", method = RequestMethod.GET)
	public ModelAndView listApps(Principal principal) {
		logger_c.debug("/app : doGet()");
		ArrayList<String> arr = new ArrayList<String>();
		for (String db : mongoDb.getDatabaseNames()) {
			if (db.compareTo("admin") == 0)
				continue;
			if (canRead(db, principal.getName()))
				arr.add(db);
		}
		return response(true, arr, null);
	}

	// list configuration OR check app status
	@RequestMapping(value = "/app/{appName}", method = RequestMethod.GET)
	public ModelAndView checkAppStatus(@PathVariable String appName,
			Principal principal,
			@RequestParam(value = "op", required = false) String op) {
		
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have permission");
		}
		
		DB db = mongoDb.getDB(appName);

		// list config
		if (op == null){
			//DBObject bdo = db.getCollection("conf").findOne();
			return response(true, db.getCollection("conf").findOne(new BasicDBObject(), new BasicDBObject("_id",0)), null);
		}else{
			// check app status
			if (op.compareTo("stats") != 0)
				return response(false, null, "op: " + op + " is not supported");
			return response(true, db.getStats(), null);
		}
	}

	// drop app
	@RequestMapping(value = "/app/{appName}", method = RequestMethod.DELETE)
	public ModelAndView deleteApp(@PathVariable String appName,
			Principal principal) {
		logger_c.debug("/app/" + appName + " : doDELETE()");

		if (!mongoDb.getDatabaseNames().contains(appName))
			return response(false, null, "app: " + appName + " is not found");

		if (!canWrite(appName, principal.getName()))
			return response(false, null, "you don't have WRITE permission");
		mongoDb.dropDatabase(appName);
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			amqpTemplate.convertAndSend("cccdApp",mapper.writeValueAsString(new AppEvent(appName,"deleted")));
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AmqpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return response(true, null, "app: " + appName + " deleted");
	}
}
