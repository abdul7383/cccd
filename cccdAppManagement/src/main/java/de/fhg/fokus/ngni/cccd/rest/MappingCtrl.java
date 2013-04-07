package de.fhg.fokus.ngni.cccd.rest;

import java.io.IOException;
import java.security.Principal;

import org.apache.log4j.Logger;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * FundsController class will expose a series of RESTful endpoints
 */
@Controller
@RequestMapping(value = "/app/{appName}/collections/{collName}/mapping")
public class MappingCtrl extends BaseCtrl {

	protected static final Logger logger_c = Logger
			.getLogger(MappingCtrl.class);

	// add a new mapping
	@RequestMapping(method = RequestMethod.PUT)
	public ModelAndView updateMapping(@PathVariable String appName,
			@PathVariable String collName, Principal principal,
			@RequestBody String body) {
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have Write permission");
		}
		logger_c.debug("/app/" + appName + "/collections/" + collName
				+ "/mapping" + " : doPUT()");

		if (!checkCollectionExist(appName, collName))
			return response(false, null, "collection: " + collName
					+ " not found in app: " + appName);

		try {
			esClient.admin().indices() 
	        .preparePutMapping(appName+"_"+collName) 
	        .setType(collName) 
	        .setSource(body) 
	        .execute().actionGet(); 
		} catch (Exception ex) {
			return response(
					false,
					null,
					"error while updating the mapping of the collection"
							+ ex.getMessage());
		}
		return response(true, null, "collection: mapping of " + collName + " updated");
	}
	
	// get the index mapping for a collection
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView getMapping(@PathVariable String appName,
			@PathVariable String collName, Principal principal) throws IOException {

		if (!canRead(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have READ permission");
		}
		logger_c.debug("/app/" + appName + "/collections/" + collName
				+ "/mapping" + " : doGET()");

		if (!checkCollectionExist(appName, collName))
			return response(false, null, "collection: " + collName
					+ " not found in app: " + appName);

		ClusterState cs = esClient.admin().cluster().prepareState()
				.setFilterIndices(appName + "_" + collName).execute()
				.actionGet().getState();
		IndexMetaData imd = cs.getMetaData().index(appName + "_" + collName);
		if (imd == null)
			return response(false, null, "no mapping found");
		MappingMetaData mdd = imd.mapping(collName);
		
		if (mdd == null)
			return response(false, null, "no mapping found");
		return response(true, mdd.getSourceAsMap(), null);
	}

}
