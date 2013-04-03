package de.fhg.fokus.ngni.cccd.rest;

import java.security.Principal;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.mongodb.DB;

/**
 * FundsController class will expose a series of RESTful endpoints
 */
@Controller
@RequestMapping(value = "/app/{appName}/collections/{collName}/search")
public class SearchCtrl extends BaseCtrl {

	protected static final Logger logger_c = Logger
			.getLogger(SearchCtrl.class);

	// search within a collection
	@RequestMapping(method = RequestMethod.POST)
	public ModelAndView search(@PathVariable String appName,
			@PathVariable String collName, Principal principal,
			@RequestBody String body) {
		if (!canRead(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have permission");
		}
		logger_c.debug("/app/" + appName + "/collections/" + collName
				+ "/search : doPOST()");

		DB db = mongoDb.getDB(appName);
		if (!db.getCollectionNames().contains(collName))
			return response(false, null, "collection: " + collName
					+ " is not found in app: " + appName);
		if (body == null || body.compareTo("") == 0)
			return response(false, null, "html body can not be empty");
		try {
			/*HashMap<String, Object> jsonBody;
			try {
				jsonBody = new ObjectMapper().readValue(body, HashMap.class);
			} catch (Exception e) {
				return response(false, null,
						"jsonBody error: " + e.getMessage());
			}
			BasicDBObject bdo = new BasicDBObject(jsonBody);
			db.getCollection(collName).save(bdo);*/
			/*
			 * ClusterState cs = esClient.admin().cluster().prepareState()
			 * .setFilterIndices("myIndex").execute().actionGet() .getState();
			 * IndexMetaData imd = cs.getMetaData() .index(appName + "_" +
			 * collName); if (imd != null) { //only index to ES if mapping is
			 * defined MappingMetaData mdd = imd.mapping(collName);
			 * mdd.sourceAsMap();
			 */
			SearchResponse resp = esClient.search(new SearchRequest(appName + "_" + collName).source(body.getBytes())).actionGet();
			XContentBuilder builder = XContentFactory.jsonBuilder();
			builder.startObject();
			resp.getHits().toXContent(builder, ToXContent.EMPTY_PARAMS);
			builder.endObject();
			builder.close();
//			logger_c.debug("builder1: "+builder.toString());
//			logger_c.debug("builder2: "+builder.prettyPrint());
//			logger_c.debug("builder3: "+builder.string());
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonResp = mapper.readTree(builder.string());
			return response(true, jsonResp, null);
			//prepareIndex(appName + "_" + collName, collName,
			//		bdo.getObjectId("_id").toString()).setSource(jsonBody)
			//		.execute().actionGet();
			// }
		} catch (Exception ex) {
			return response(false, null, "error : " + ex.getMessage());
		}
	}
}
