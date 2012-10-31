package de.fhg.fokus.ngni.webservices.rest;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;

/**
 * FundsController class will expose a series of RESTful endpoints
 */
@Controller
public class DBCtrl extends BaseCtrl{
	
	protected static final Logger logger_c = Logger.getLogger(DBCtrl.class);	
	
	//create database
	@RequestMapping(value = "/db/{DBname}", method = RequestMethod.PUT)
	public ModelAndView createDatabase(@PathVariable String DBname) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/db/"+DBname+" : doPut()");
		
		if(mongoDb.getDatabaseNames().contains(DBname))
			return response(false, null, "database: "+DBname+" is already created");
		
		DB db = mongoDb.getDB(DBname);
		BasicDBObject doc = new BasicDBObject();
		doc.put("dbName", DBname);
		db.createCollection("settingsCollection", doc);
		return response(true, null,"database: "+DBname+" created");
	}
	
	//create database with setting
	/*@RequestMapping(value = "/db/{DBname}", method = RequestMethod.PUT)
	public ModelAndView createDatabaseWithSetting(@PathVariable String DBname,@RequestParam(value = "op", required = false) String op) {
		// TODO impl. createDatabaseWithSetting()
		return null;
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/db/"+DBname+" : doPut()");
		
		if(mongoDb.getDatabaseNames().contains(DBname))
			return response(false, null, "database: "+DBname+" is already created");
		
		DB db = mongoDb.getDB(DBname);
		BasicDBObject doc = new BasicDBObject();
		doc.put("dbName", DBname);
		db.createCollection("settingsCollection", doc);
		return response(true, null,"database: "+DBname+" created");
	}*/
	
	//list databases
	@RequestMapping(value = "/db", method = RequestMethod.GET)
	public ModelAndView listDatabases() {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/db : doGet()");
		return response(true, mongoDb.getDatabaseNames(), null);
	}
	
	//list collections OR check database status
	@RequestMapping(value = "/db/{DBname}", method = RequestMethod.GET)
	public ModelAndView checkDBStatus(@PathVariable String DBname,@RequestParam(value = "op", required = false) String op) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/db/"+DBname+" : doGet()");
		
		if(!mongoDb.getDatabaseNames().contains(DBname))
			return response(false, null, "database: "+DBname+" is not found");
		
		DB db = mongoDb.getDB( DBname );
		
		//list collections
		if(op == null)
			return response(true, db.getCollectionNames(), null);
		
		//check database status
		if(op.compareTo("stats")!=0)
			return response(false, null, "op: "+op+" is not supported");
		else
			return response(true, db.getStats(), null);
	}
	
	//drop database
	@RequestMapping(value = "/db/{DBname}", method = RequestMethod.DELETE)
	public ModelAndView DeleteDatabase(@PathVariable String DBname) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/db/"+DBname+" : doDelete()");
		
		if(!mongoDb.getDatabaseNames().contains(DBname))
			return response(false, null, "database: "+DBname+" is not found");
		
		mongoDb.dropDatabase(DBname);
		return response(true, null,"database: "+DBname+" deleted");
	}
}
