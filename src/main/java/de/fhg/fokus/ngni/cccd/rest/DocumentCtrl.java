package de.fhg.fokus.ngni.cccd.rest;

import java.security.Principal;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBObject;

/**
 * FundsController class will expose a series of RESTful endpoints
 */
@Controller
@RequestMapping(value = "/app/{appName}/collections/{collName}/doc")
public class DocumentCtrl extends BaseCtrl {

	protected static final Logger logger_c = Logger
			.getLogger(DocumentCtrl.class);

	// add a new document to a collection
	@SuppressWarnings("unchecked")
	@RequestMapping(method = RequestMethod.POST)
	public ModelAndView createDocument(@PathVariable String appName,
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
		return response(true, null, "doc created succesfully");

	}

	// get a document from a collection
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ModelAndView getDocument(@PathVariable String appName,
			@PathVariable String collName, Principal principal,
			@PathVariable String id,
			@RequestParam(value = "fields", required = false) String fields) {

		if (!canRead(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have permission");
		}
		logger_c.debug("/app/" + appName + "/collections/" + collName + "/doc/"
				+ id + ": doGET()");

		DB db = mongoDb.getDB(appName);
		if (!db.getCollectionNames().contains(collName))
			return response(false, null, "collection: " + collName
					+ " is no created in app: " + appName);
		DBObject bdo = null;
		if (fields == null || fields.compareTo("") == 0) {
			bdo = db.getCollection(collName).findOne(
					new BasicDBObject("_id", new ObjectId(id)));
		} else {
			BasicDBObjectBuilder fileds_obj = BasicDBObjectBuilder.start();
			for (String fi : fields.split(",")) {
				if (fi.contains(":"))
					fileds_obj.add(fi.split(":")[0], new Integer(
							fi.split(":")[1]));
				else
					fileds_obj.add(fi, 1);
			}
			try {
				bdo = db.getCollection(collName).findOne(
						new BasicDBObject("_id", new ObjectId(id)),
						fileds_obj.get());
			} catch (Exception ex) {
				return response(false, null, "error: " + ex.getMessage());
			}
		}
		if (bdo == null)
			return response(false, null, "no document found with id: " + id);
		return response(true, bdo, null);
	}

	// delete a document from a collection
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public ModelAndView deleteDoc(@PathVariable String appName,
			@PathVariable String collName, Principal principal,
			@PathVariable String id) {

		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have permission");
		}
		logger_c.debug("/app/" + appName + "/collections/" + collName + "/doc/"
				+ id + ": doDELETE()");

		DB db = mongoDb.getDB(appName);
		if (!db.getCollectionNames().contains(collName))
			return response(false, null, "collection: " + collName
					+ " is not found in app: " + appName);
		DBObject bdo = db.getCollection(collName).findOne(
				new BasicDBObject("_id", new ObjectId(id)));
		if (bdo == null)
			return response(false, null, "no document found with id: " + id);
		// delete it first from mongodb
		db.getCollection(collName).remove(bdo);
		// delete it then from ES
		esClient.prepareDelete(appName + "_" + collName, collName, id)
				.execute().actionGet();
		return response(true, null, "document deleted");
	}

}
