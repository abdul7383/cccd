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
import com.mongodb.DBObject;

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
			@RequestBody String body,
			@RequestParam(value = "op", required = true) String op,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "readOnly", required = false) boolean readOnly) {
		logger_c.debug("/app/" + appName + " : doPUT()");

		if (!mongoDb.getDatabaseNames().contains(appName))
			return response(false, null, "app: " + appName
					+ " not found, please create it first with POST");
		if (!canWrite(appName, principal.getName()))
			return response(false, null, "you don't have WRITE permission");
		DB db = mongoDb.getDB(appName);
		switch (op) {
		case "addUser":
			if (username == null)
				return response(
						false,
						null,
						"please spicify username to add, i.e. ?op=addUser&username=testuser&readOnly=false");
			if (customUserDetailsService.getUserDetail(username) == null)
				return response(
						false,
						null,
						"username: "
								+ username
								+ " is not created yet, please create it first thru API cccd/users");
			BasicDBObject user = new BasicDBObject("username", username)
					.append("readOnly", readOnly);
			db.getCollection("users").save(user);
			return response(true, null, "username: " + username + " added");
		case "deleteUser":
			if (username == null)
				return response(false, null,
						"please spicify username to delete, i.e. ?op=deleteUser&username=testuser");
			DBObject dbO = db.getCollection("users").findOne(
					new BasicDBObject("username", username));
			// check if username the last one who has write permission
			if (!(boolean) dbO.get("readOnly")){
				// the username has write access
				if (db.getCollection("users").count(
						new BasicDBObject("readOnly", false)) == 1){
					// last one
					return response(
							false,
							null,
							"username: "
									+ username
									+ " can not be deleted because it is the last one who has write access");
					}
				else{
					db.getCollection("users").remove(
							new BasicDBObject("username", username));
				}
			}else{
				// the username has only read access
				db.getCollection("users").remove(
						new BasicDBObject("username", username));
			}
			db.getCollection("users").remove(
					new BasicDBObject("username", username));
			return response(true, null, "username: " + username + " deleted");
		case "updateConf":
		default:
			break;
		}
		/*
		 * DB db = mongoDb.getDB(appName); HashMap<String, Object> jsonBody; try
		 * { jsonBody = new ObjectMapper().readValue(body, HashMap.class); }
		 * catch (Exception e) { return response(false, null, e.getMessage()); }
		 * BasicDBObject doc = new BasicDBObject(jsonBody);
		 * db.getCollection("conf").drop(); db.createCollection("conf", doc);
		 */
		return response(true, null, "app: " + appName + " created");
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

	// list collections OR check app status
	@RequestMapping(value = "/app/{appName}", method = RequestMethod.GET)
	public ModelAndView checkAppStatus(@PathVariable String appName,
			@RequestParam(value = "op", required = false) String op) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/app/" + appName + " : doGet()");

		if (!mongoDb.getDatabaseNames().contains(appName))
			return response(false, null, "app: " + appName + " is not found");

		DB db = mongoDb.getDB(appName);

		/*// list collections
		if (op == null){
			Set<String> list = db.getCollectionNames();
			list.remove("system.indexes");
			return response(true, list, null);
		}*/
		// check app status
		/*if (op.compareTo("stats") != 0)
			return response(false, null, "op: " + op + " is not supported");
		else*/
		return response(true, db.getStats(), null);
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
