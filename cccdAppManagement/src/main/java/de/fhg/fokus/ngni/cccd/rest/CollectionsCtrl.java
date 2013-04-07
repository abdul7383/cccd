package de.fhg.fokus.ngni.cccd.rest;

import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;

/**
 * FundsController class will expose a series of RESTful endpoints
 */
@Controller
@RequestMapping(value = "/app/{appName}/collections")
public class CollectionsCtrl extends BaseCtrl {

	protected static final Logger logger_c = Logger
			.getLogger(CollectionsCtrl.class);

	// create collection a long with an index in es
	@RequestMapping(value = "/{collName}", method = RequestMethod.POST)
	public ModelAndView createCollection(@PathVariable String appName,
			@PathVariable String collName, Principal principal,
			@RequestBody String body) {
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have WRITE permission");
		}
		logger_c.debug("/app/" + appName + "/collections/" + collName
				+ " : doPOST()");

		DB db = mongoDb.getDB(appName);
		if (db.getCollectionNames().contains(collName))
			return response(false, null, "collection: " + collName
					+ " is already created in app: " + appName);
		// if (body == null || body.compareTo("") == 0)
		// return response(false, null, "you have to specify mapping in body");
		try {
			/*
			 * esClient.admin() .indices() .create(new
			 * CreateIndexRequest(appName + "_" + CollName) .mapping(CollName,
			 * body)).actionGet();
			 */
			esClient.admin().indices()
					.create(new CreateIndexRequest(appName + "_" + collName))
					.actionGet();

			db.createCollection(collName, null).save(new BasicDBObject());
		} catch (Exception ex) {
			return response(
					false,
					null,
					"error while creating the collection or the index: "
							+ ex.getMessage());
		}
		return response(true, null, "collection: " + collName + " and an empty index created");

	}

	// list all collections within an app
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView listCollections(@PathVariable String appName,
			Principal principal) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have permission");
		}
		logger_c.debug("/app/" + appName + "/collections" + " : doGet()");

		DB db = mongoDb.getDB(appName);

		// list collections
		Set<String> list = db.getCollectionNames();
		list.remove("system.indexes");
		list.remove("conf");
		list.remove("users");
		List<String> buckets = new LinkedList<String>();
		for(String c:list)
			if(c.endsWith(".chunks")){
				buckets.add(c);
				buckets.add(c.replace(".chunks", ".files"));
				//list.remove(c);
				//list.remove(c.replace(".chunks", ".files"));
		}
		for (String b:buckets)
			list.remove(b);
		return response(true, list, null);
	}

	// delete collection
	@RequestMapping(value = "/{collName}", method = RequestMethod.DELETE)
	public ModelAndView deleteColl(@PathVariable String appName,
			@PathVariable String collName, Principal principal) {
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have permission");
		}

		logger_c.debug("/app/" + appName + "/collections/" + collName
				+ " : doDELETE()");

		if (!checkCollectionExist(appName, collName))
			return response(false, null, "collection: " + collName
					+ " not found in app: " + appName);
		DB db = mongoDb.getDB(appName);
		db.getCollection(collName).drop();
		esClient.admin().indices()
				.delete(new DeleteIndexRequest(appName + "_" + collName))
				.actionGet();
		return response(true, null, "collection: " + collName + " deleted");
	}
}
