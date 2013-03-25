package de.fhg.fokus.ngni.cccd.rest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import de.fhg.fokus.ngni.cccd.services.CustomUserDetailsService;

/**
 * FundsController class will expose a series of RESTful endpoints
 */
//@SuppressWarnings("unchecked")
@Controller
public class UserCtrl extends BaseCtrl {

	protected static final Logger logger_c = Logger
			.getLogger(UserCtrl.class);
	@Autowired
	protected CustomUserDetailsService customUserDetailsService;

	// create App, addUser, settingCollection
	@RequestMapping(value = "/users", method = RequestMethod.POST)
	public ModelAndView addUser(
			@RequestParam(value = "username", required = true) String username,
			@RequestParam(value = "password", required = true) String password,
			@RequestParam(value = "firstName", required = false) String firstName,
			@RequestParam(value = "lastName", required = false) String lastName,
			@RequestParam(value = "email", required = true) String email,
			@RequestParam(value = "role", required = false) int role) {

		logger_c.debug("/addUser" + " : doPOST()");
		
		if (customUserDetailsService.addUserDetail(username, password, firstName, lastName, email, role))
			return response(true, null, "user: " + username + " created");
		else
			return response(false, null, "user: " + username
					+ " is already created, please choose another one");
	}

/*	// update users
	@RequestMapping(value = "/users", method = RequestMethod.PUT)
	public ModelAndView updateUser(@PathVariable String appName,
			Authentication auth, @RequestBody String body) {
		logger_c.debug("/app/" + appName + " : doPUT()");

		if (!mongoDb.getDatabaseNames().contains(appName))
			return response(false, null, "app: " + appName
					+ " not found, please create it first with POST");
		//if (!canWrite(appName, auth))
		//	return response(false, null, "Invalid username or password");

		DB db = mongoDb.getDB(appName);
		HashMap<String, Object> jsonBody;
		try {
			jsonBody = new ObjectMapper().readValue(body, HashMap.class);
		} catch (Exception e) {
			return response(false, null, e.getMessage());
		}
		BasicDBObject doc = new BasicDBObject(jsonBody);
		db.getCollection("conf").drop();
		db.createCollection("conf", doc);
		return response(true, null, "app: " + appName + " created");
	}
*/
	// list users
	@RequestMapping(value = "/users", method = RequestMethod.GET)
	public ModelAndView listUsers() {
		logger_c.debug("/users : doGet()");
		return response(true, customUserDetailsService.listUserDetails(), null);
	}

	// delete user
	@RequestMapping(value = "/users", method = RequestMethod.DELETE)
	public ModelAndView deleteUser(@RequestParam(value = "username", required = true) String username) {
		logger_c.debug("/users" + " : doDELETE()");
		if (customUserDetailsService.deleteUserDetail(username))
			return response(true, null, "user: " + username + " deleted");
		else
			return response(false, null, "user: " + username
					+ " not found");
	}

	public void setCustomUserDetailsService(
			CustomUserDetailsService customUserDetailsService) {
		this.customUserDetailsService = customUserDetailsService;
	}
}
