package de.fhg.fokus.ngni.cccd.rest;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.gridfs.GridFS;

/**
 * FundsController class will expose a series of RESTful endpoints
 */
@Controller
@RequestMapping(value = "/app/{appName}/buckets")
public class BucketCtrl extends BaseCtrl {

	protected static final Logger logger_c = Logger.getLogger(BucketCtrl.class);

	// create bucket
	@RequestMapping(value = "/{bucketName}", method = RequestMethod.POST)
	public ModelAndView createBucket(@PathVariable String appName,
			@PathVariable String bucketName, Principal principal) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have WRITE permission");
		}
		logger_c.debug("/app/" + appName + "/buckets/" + bucketName
				+ " : doPOST()");

		DB db = mongoDb.getDB(appName);
		if (db.getCollectionNames().contains(bucketName+".files"))
			return response(false, null, "bucket: " + bucketName
					+ " is already created in app: " + appName);
		// if bucket does not exist, it will be created
		new GridFS(db, bucketName);
		return response(true, null, "bucket: " + bucketName + " created");

	}
	
	// list all buckets in a app
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView listBuckets(@PathVariable String appName,
			Principal principal) {
		if (!canRead(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have WRITE permission");
		}
		logger_c.debug("/app/" + appName + "/buckets" + " : doGet()");

		DB db = mongoDb.getDB(appName);
		// TODO alternative to LinkedList 
		List<String> buckets = new LinkedList<String>();
		for(String c : db.getCollectionNames()){
			if(c.endsWith(".chunks"))
				//buckets.add(c.substring(0, c.indexOf(".chunks")));
				buckets.add(c.replace(".chunks",""));
		}
		return response(true, buckets, null);

	}
	
	// list all files in bucket
	@RequestMapping(value = "/{bucketName}", method = RequestMethod.GET)
	public ModelAndView listAllFilesInBucket(@PathVariable String appName,
			@PathVariable String bucketName, Principal principal) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		if (!canRead(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have permission");
		}
		logger_c.debug("/app/" + appName + "/buckets/" + bucketName + " : doGet()");
		
		DB db = mongoDb.getDB(appName);
		// check if bucket exist
		if(!db.getCollectionNames().contains(bucketName+".chunks"))
			return response(false, null, "bucket: " + bucketName
					+ " does not exist in app: " + appName);
		BasicDBObject fields = new BasicDBObject();
		fields.append("_id",0);
		fields.append("length",0);
		fields.append("md5",0);
		fields.append("aliases",0);
		fields.append("chunkSize",0);
		//DBCursor cursor = db.getCollection(bucketName+".files").find(new BasicDBObject(), fields).toArray();
		GridFS gfs = new GridFS(db, bucketName);
		List<String> fi = new ArrayList<String>();
		DBCursor dbc = gfs.getFileList();
		while(dbc.hasNext()){
			fi.add(dbc.next().get("filename").toString());
		}
		return response(true, fi, null);
	}
	
	// delete a bucket
	@RequestMapping(value = "/{bucketName}", method = RequestMethod.DELETE)
	public ModelAndView deleteBucket(@PathVariable String appName,
			@PathVariable String bucketName, Principal principal) {
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have permission");
		}

		logger_c.debug("/app/" + appName + "/buckets/" + bucketName
				+ " : doDELETE()");

		if (!checkCollectionExist(appName, bucketName+".chunks"))
			return response(false, null, "bucket: " + bucketName
					+ " not found in app: " + appName);
		DB db = mongoDb.getDB(appName);
		db.getCollection(bucketName+".chunks").drop();
		db.getCollection(bucketName+".files").drop();
		return response(true, null, "bucket: " + bucketName + " deleted");
	}
	/*
	 * //check collection status
	 * 
	 * @RequestMapping(value = "/db/{DBname}/{CollName}", method =
	 * RequestMethod.GET) public ModelAndView checkCollStatus(@PathVariable
	 * String DBname, @PathVariable String CollName, @RequestParam(value = "op",
	 * required = false) String op) { if (!isAuthorized()) return
	 * response(false, null, "Invalid username or password");
	 * logger_c.debug("/db/"+DBname+"/"+CollName+" : doGet()");
	 * 
	 * if(!mongoDb.getDatabaseNames().contains(DBname)) return response(false,
	 * null, "database: "+DBname+" is not found");
	 * 
	 * if(op == null) return response(false, null, "please specify op");
	 * 
	 * //check collection status if(op.compareTo("stats")!=0) return
	 * response(false, null, "op: "+op+" is not supported"); else{ DB db =
	 * mongoDb.getDB( DBname ); if(!db.getCollectionNames().contains(CollName))
	 * return response(false, null, "collection: "+
	 * CollName+" not found in database: "+DBname); else return response(true,
	 * db.getCollection(CollName).getStats(), null); } }
	 * 
	 * //delete collection
	 * 
	 * @RequestMapping(value = "/db/{DBname}/{CollName}", method =
	 * RequestMethod.DELETE) public ModelAndView deleteColl(@PathVariable String
	 * DBname, @PathVariable String CollName) { if (!isAuthorized()) return
	 * response(false, null, "Invalid username or password");
	 * logger_c.debug("/db/"+DBname+"/"+CollName+" : doDelete()");
	 * 
	 * if(!mongoDb.getDatabaseNames().contains(DBname)) return response(false,
	 * null, "database: "+DBname+" is not found");
	 * 
	 * DB db = mongoDb.getDB( DBname );
	 * if(!db.getCollectionNames().contains(CollName)) return response(false,
	 * null, "collection: "+ CollName+" not found in database: "+DBname); else{
	 * db.getCollection(CollName).drop(); return response(true, null,
	 * "collection: "+ CollName+" deleted"); } }
	 */
}
