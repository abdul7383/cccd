package de.fhg.fokus.ngni.webservices.rest;

import java.security.Principal;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.mongodb.DB;

/**
 * FundsController class will expose a series of RESTful endpoints
 */
@Controller
public class CollectionsCtrl extends BaseCtrl {

	protected static final Logger logger_c = Logger
			.getLogger(CollectionsCtrl.class);

	// create collection
	@RequestMapping(value = "/app/{appName}/collections/{CollName}", method = RequestMethod.POST)
	public ModelAndView createColl(@PathVariable String appName,
			@PathVariable String CollName,
			Principal principal,
			@RequestParam(value = "op", required = false) String op) {
		if (!canWrite(appName, principal.getName())){
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have WRITE permission");
			}
		logger_c.debug("/app/" + appName + "/" + CollName + " : doPOST()");

		DB db = mongoDb.getDB(appName);
		if (db.getCollectionNames().contains(CollName))
			return response(false, null, "collection: " + CollName
					+ " is already created in app: " + appName);
		else {
			db.createCollection(CollName, null);
			CreateIndexResponse response = esClient.admin().indices().prepareCreate(appName+"_"+CollName).execute().actionGet();
			return response(true, null, "collection: " + CollName + " created######"+response.toString());
		}
	}

	// check collection status
	@RequestMapping(value = "/db/{DBname}/{CollName}", method = RequestMethod.GET)
	public ModelAndView checkCollStatus(@PathVariable String DBname,
			@PathVariable String CollName,
			@RequestParam(value = "op", required = false) String op) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/db/" + DBname + "/" + CollName + " : doGet()");

		if (!mongoDb.getDatabaseNames().contains(DBname))
			return response(false, null, "database: " + DBname
					+ " is not found");

		if (op == null)
			return response(false, null, "please specify op");

		// check collection status
		if (op.compareTo("stats") != 0)
			return response(false, null, "op: " + op + " is not supported");
		else {
			DB db = mongoDb.getDB(DBname);
			if (!db.getCollectionNames().contains(CollName))
				return response(false, null, "collection: " + CollName
						+ " not found in database: " + DBname);
			else
				return response(true, db.getCollection(CollName).getStats(),
						null);
		}
	}

	// delete collection
	@RequestMapping(value = "/db/{DBname}/{CollName}", method = RequestMethod.DELETE)
	public ModelAndView deleteColl(@PathVariable String DBname,
			@PathVariable String CollName) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/db/" + DBname + "/" + CollName + " : doDelete()");

		if (!mongoDb.getDatabaseNames().contains(DBname))
			return response(false, null, "database: " + DBname
					+ " is not found");

		DB db = mongoDb.getDB(DBname);
		if (!db.getCollectionNames().contains(CollName))
			return response(false, null, "collection: " + CollName
					+ " not found in database: " + DBname);
		else {
			db.getCollection(CollName).drop();
			return response(true, null, "collection: " + CollName + " deleted");
		}
	}
}
