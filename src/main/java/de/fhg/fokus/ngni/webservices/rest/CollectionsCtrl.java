package de.fhg.fokus.ngni.webservices.rest;

import java.security.Principal;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
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
public class CollectionsCtrl extends BaseCtrl {

	protected static final Logger logger_c = Logger
			.getLogger(CollectionsCtrl.class);

	// create collection a long with an index in es
	@RequestMapping(value = "/app/{appName}/collections/{CollName}", method = RequestMethod.POST)
	public ModelAndView createColl(@PathVariable String appName,
			@PathVariable String CollName, Principal principal,
			@RequestBody String body) {
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have WRITE permission");
		}
		logger_c.debug("/app/" + appName + "/collections/" + CollName
				+ " : doPOST()");

		DB db = mongoDb.getDB(appName);
		if (db.getCollectionNames().contains(CollName))
			return response(false, null, "collection: " + CollName
					+ " is already created in app: " + appName);
		//if (body == null || body.compareTo("") == 0)
		//	return response(false, null, "you have to specify mapping in body");
		try {
			/*
			 * esClient.admin() .indices() .create(new
			 * CreateIndexRequest(appName + "_" + CollName) .mapping(CollName,
			 * body)).actionGet();
			 */
			esClient.admin().indices()
					.create(new CreateIndexRequest(appName + "_" + CollName))
					.actionGet();

			db.createCollection(CollName, null).save(new BasicDBObject());
		} catch (Exception ex) {
			return response(
					false,
					null,
					"error while creating the collection or the index: "
							+ ex.getMessage());
		}
		return response(true, null, "collection: " + CollName + " created");

	}

	// add a new document to a collection
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/app/{appName}/collections/{collName}/doc", method = RequestMethod.POST)
	public ModelAndView createDoc(@PathVariable String appName,
			@PathVariable String collName, Principal principal,
			@RequestBody String body) {
		if (!canRead(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have permission");
		}
		logger_c.debug("/app/" + appName + "/collections/" + collName
				+ "/doc : doPOST()");

		DB db = mongoDb.getDB(appName);
		if (!db.getCollectionNames().contains(collName))
			return response(false, null, "collection: " + collName
					+ " is no created in app: " + appName);
		if (body == null || body.compareTo("") == 0)
			return response(false, null, "html body can not be empty");
		try {
			HashMap<String, Object> jsonBody;
			try {
				jsonBody = new ObjectMapper().readValue(body, HashMap.class);
			} catch (Exception e) {
				return response(false, null,
						"jsonBody error: " + e.getMessage());
			}
			BasicDBObject bdo = new BasicDBObject(jsonBody);
			db.getCollection(collName).save(bdo);
			/*
			 * ClusterState cs = esClient.admin().cluster().prepareState()
			 * .setFilterIndices("myIndex").execute().actionGet() .getState();
			 * IndexMetaData imd = cs.getMetaData() .index(appName + "_" +
			 * collName); if (imd != null) { //only index to ES if mapping is
			 * defined MappingMetaData mdd = imd.mapping(collName);
			 * mdd.sourceAsMap();
			 */
			esClient.prepareIndex(appName + "_" + collName, collName,
					bdo.getObjectId("_id").toString()).setSource(jsonBody)
					.execute().actionGet();
			// }
		} catch (Exception ex) {
			return response(false, null, "error : " + ex.getMessage());
		}
		return response(true, null, "doc added succesfully created");

	}

	// get the index mapping for a collection
	@RequestMapping(value = "/app/{appName}/collections/{collName}/_mapping", method = RequestMethod.GET)
	public ModelAndView getCollStatus(@PathVariable String appName,
			@PathVariable String collName, Principal principal) {
		if (!canRead(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have READ permission");
		}
		logger_c.debug("/app/" + appName + "/collections/" + collName
				+ "/_mapping" + " : doGET()");

		if (!checkCollectionExist(appName, collName))
			return response(false, null, "collection: " + collName
					+ " not found in app: " + appName);

		ClusterState cs = esClient.admin().cluster().prepareState()
				.setFilterIndices(appName + "_" + collName).execute().actionGet().getState();
		IndexMetaData imd = cs.getMetaData().index(appName + "_" + collName);
		if (imd == null)
			return response(false, null, "no mapping found");
		MappingMetaData mdd = imd.mapping(collName);

		return response(true, mdd, null);
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
