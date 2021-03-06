package de.fhg.fokus.ngni.cccd.rest;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * FundsController class will expose a series of RESTful endpoints
 */
@RequestMapping(value = "/app/{appName}/users")
@Controller
public class AppUserCtrl extends BaseCtrl {

	protected static final Logger logger_c = Logger
			.getLogger(AppUserCtrl.class);
	
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView listUsers(
			@PathVariable String appName,
			Principal principal) {
		logger_c.debug("/app/" + appName + "/" + "users : doGET()");
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have WRITE permission");
		}
		DB db = mongoDb.getDB(appName);
		//BasicDBObject keys = new BasicDBObject("id","0");
		DBCursor cursor = db.getCollection("users").find(new BasicDBObject(), new BasicDBObject("_id",0));
		List<DBObject> docs = new ArrayList<DBObject>();
		while (cursor.hasNext()) {
			docs.add(cursor.next());
		}
		return response(true, docs, null);
	}

	// add a user to an app
	@RequestMapping(method = RequestMethod.PUT)
	public ModelAndView addUser(
			@PathVariable String appName,
			Principal principal,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "readonly", required = false) boolean readonly) {
		logger_c.debug("/app/" + appName + "/" + "users : doPUT()");
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have WRITE permission");
		}
		DB db = mongoDb.getDB(appName);
		if (username == null)
			return response(false, null,
					"please spicify username to add, i.e. ?username=testuser&readOnly=false");
		if (customUserDetailsService.getUserDetail(username) == null)
			return response(
					false,
					null,
					"username: "
							+ username
							+ " is not created yet, please create it first thru API cccd/users");
		if(db.getCollection("users").find(new BasicDBObject("username",username)).hasNext())
			return response(false, null,
					"username: "+username+ " is already in the app user list");
		BasicDBObject user = new BasicDBObject("username", username).append(
				"readOnly", readonly);
		db.getCollection("users").save(user);
		return response(true, null, "username: " + username + " added");
	}

	// delete a user from an app
	@RequestMapping(method = RequestMethod.DELETE)
	public ModelAndView deleteUser(@PathVariable String appName,
			Principal principal,
			@RequestParam(value = "username", required = false) String username) {
		logger_c.debug("/app/" + appName + "/" + "users : doDELETE()");
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have WRITE permission");
		}
		DB db = mongoDb.getDB(appName);

		if (username == null)
			return response(false, null,
					"please spicify username to delete, i.e. ?op=deleteUser&username=testuser");
		DBObject dbO = db.getCollection("users").findOne(
				new BasicDBObject("username", username));
		// check if username the last one who has write permission
		if (!(boolean) dbO.get("readOnly")) {
			// the username has write access
			if (db.getCollection("users").count(
					new BasicDBObject("readOnly", false)) == 1) {
				// last one
				return response(
						false,
						null,
						"username: "
								+ username
								+ " can not be deleted because it is the last one who has write access");
			} else {
				db.getCollection("users").remove(
						new BasicDBObject("username", username));
			}
		} else {
			// the username has only read access
			db.getCollection("users").remove(
					new BasicDBObject("username", username));
		}
		db.getCollection("users").remove(
				new BasicDBObject("username", username));
		return response(true, null, "username: " + username + " deleted");
	}
}
