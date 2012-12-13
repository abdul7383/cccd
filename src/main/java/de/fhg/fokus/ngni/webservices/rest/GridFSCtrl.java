package de.fhg.fokus.ngni.webservices.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

/**
 * FundsController class will expose a series of RESTful endpoints
 */
@Controller
public class GridFSCtrl extends BaseCtrl {

	protected static final Logger logger_c = Logger.getLogger(GridFSCtrl.class);

	// create bucket
	@RequestMapping(value = "/gridfs/{DBname}/{bucket}", method = RequestMethod.PUT)
	public ModelAndView createBucket(@PathVariable String DBname,
			@PathVariable String bucket) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/gridfs/" + DBname + "/" + bucket + " : doPut()");

		if (!mongoDb.getDatabaseNames().contains(DBname))
			return response(false, null, "database: " + DBname
					+ " is not found");

		DB db = mongoDb.getDB(DBname);
		// if bucket does not exist, it will be created
		new GridFS(db, bucket);
		return response(true, null, "GridFS bucket: " + bucket + " created");

	}
	
	// put a file in bucket
	@RequestMapping(value = "/gridfs/{DBname}/{bucket}/put", method = RequestMethod.POST)
	public ModelAndView putFileInBucket(@PathVariable String DBname,
			@PathVariable String bucket, @RequestParam(value = "filename",
					  required = false) String filename, @RequestParam(value="file",
					  required = false) MultipartFile file) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/gridfs/" + DBname + "/" + bucket + "/put" + " : doPut()");
		
		if(filename==null || filename=="")
			return response(false, null, "required String parameter 'filename' is not present");
		
		if(file==null || file.isEmpty())
			return response(false, null, "file is empty");
		
		if (!mongoDb.getDatabaseNames().contains(DBname))
			return response(false, null, "database: " + DBname
					+ " is not found");

		DB db = mongoDb.getDB(DBname);
		// check if bucket exist
		if(!db.getCollectionNames().contains(bucket+".chunks"))
			return response(false, null, "bucket: " + bucket
					+ " does not exist in databse: " + DBname);
		GridFS gfs = new GridFS(db, bucket);
		GridFSDBFile old_file = gfs.findOne( filename );
		if( old_file!=null ){
			return response(false, null, "file: " + filename
					+ " already exists, use POST to override or change the filename if new");
	    }
		InputStream in=null;
		try {
			in=file.getInputStream();
		} catch (IOException e) {
			return response(false, null, "error while reading file: " + filename);
		}
		if(in==null)
			return response(false, null, "error while reading file: " + filename);
		
		GridFSInputFile newfile = gfs.createFile(in,filename);
		newfile.setContentType(file.getContentType());
		newfile.save();
		return response(true, null, "put file: "+ filename + " in bucket: " + bucket + " succeded");

	}

	// list buckets in a db
	@RequestMapping(value = "/gridfs/{DBname}", method = RequestMethod.GET)
	public ModelAndView listBuckets(@PathVariable String DBname) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/gridfs/" + DBname + " : doGet()");

		if (!mongoDb.getDatabaseNames().contains(DBname))
			return response(false, null, "database: " + DBname
					+ " is not found");

		
		DB db = mongoDb.getDB(DBname);
		// TODO alternative to LinkedList 
		List<String> buckets = new LinkedList<String>();
		for(String c : db.getCollectionNames()){
			if(c.endsWith(".chunks"))
				buckets.add(c.substring(0, c.indexOf(".chunks")));
		}
		return response(true, buckets, null);

	}
	
	// get file
	@RequestMapping(value = "/gridfs/{DBname}/{bucket}/get", method = RequestMethod.GET)
	public ModelAndView getFileFromBucket(@PathVariable String DBname,
			@PathVariable String bucket, @RequestParam(value = "filename",
			  required = false) String filename , HttpServletResponse response) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/gridfs/" + DBname + "/" + bucket + "/get : doGet()");
		
		if (!mongoDb.getDatabaseNames().contains(DBname))
			return response(false, null, "database: " + DBname
					+ " is not found");

		DB db = mongoDb.getDB(DBname);
		// check if bucket exist
		if(!db.getCollectionNames().contains(bucket+".chunks"))
			return response(false, null, "bucket: " + bucket
					+ " does not exist in databse: " + DBname);
		GridFS gfs = new GridFS(db, bucket);
		GridFSDBFile file = gfs.findOne( filename );
		if(file==null)
			return response(false, null, "file: " + filename
					+ " does not exist in bucket: " + bucket);
		//InputStream in = new FileInputStream(file);
		response.setContentType(file.getContentType());
		InputStream is = file.getInputStream();
		int read = 0;  
		byte[] bytes = new byte[1024];
		OutputStream os = null;
		try {
			 os=response.getOutputStream();
			while((read = is.read(bytes)) != -1) {  
				os.write(bytes, 0, read);  
			}
			os.flush();  
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response(true, null, "");
	}
	
	// list all files in bucket
	@RequestMapping(value = "/gridfs/{DBname}/{bucket}", method = RequestMethod.GET)
	public ModelAndView listFilesInBucket(@PathVariable String DBname,
			@PathVariable String bucket) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/gridfs/" + DBname + "/" + bucket + " : doGet()");
		
		if (!mongoDb.getDatabaseNames().contains(DBname))
			return response(false, null, "database: " + DBname
					+ " is not found");

		DB db = mongoDb.getDB(DBname);
		// check if bucket exist
		if(!db.getCollectionNames().contains(bucket+".chunks"))
			return response(false, null, "bucket: " + bucket
					+ " does not exist in databse: " + DBname);
		GridFS gfs = new GridFS(db, bucket);
		List<String> fl = new LinkedList<String>();
		DBCursor dbc = gfs.getFileList();
		
		while(dbc.hasNext()){
			fl.add(dbc.next().get("filename").toString());
		}
		return response(true, fl, null);
	}
	
	// delete file
	@RequestMapping(value = "/gridfs/{DBname}/{bucket}/delete", method = RequestMethod.DELETE)
	public ModelAndView deleteFileFromBucket(@PathVariable String DBname,
			@PathVariable String bucket, @RequestParam(value = "filename",
			  required = false) String filename) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		logger_c.debug("/gridfs/" + DBname + "/" + bucket + "/delete : doDelete()");
		
		if (!mongoDb.getDatabaseNames().contains(DBname))
			return response(false, null, "database: " + DBname
					+ " is not found");

		DB db = mongoDb.getDB(DBname);
		// check if bucket exist
		if(!db.getCollectionNames().contains(bucket+".chunks"))
			return response(false, null, "bucket: " + bucket
					+ " does not exist in databse: " + DBname);
		GridFS gfs = new GridFS(db, bucket);
		GridFSDBFile file = gfs.findOne( filename );
		if(file==null)
			return response(false, null, "file: " + filename
					+ " does not exist in bucket: " + bucket);
		
		gfs.remove(filename);
		return response(true, null, "file: " + filename + " deleted");
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
