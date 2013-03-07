package de.fhg.fokus.ngni.webservices.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
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
@RequestMapping(value = "/app/{appName}/buckets/{filename:.*}")
public class FilesCtrl extends BaseCtrl {

	protected static final Logger logger_c = Logger.getLogger(FilesCtrl.class);

	// add a file to bucket
	@RequestMapping( method = RequestMethod.POST)
	public ModelAndView addFileToBucket(@PathVariable String appName,
			@PathVariable String bucketName, @PathVariable String filename, @RequestParam(value="file",
					  required = false) MultipartFile file, Principal principal) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have WRITE permission");
		}
		
		logger_c.debug("/app/" + appName + "/buckets/" + bucketName + "/"+ filename
				+ " : doPOST()");
		
		//if(filename==null || filename=="")
		//	return response(false, null, "required String parameter 'filename' is not present");
		
		if(file==null || file.isEmpty())
			return response(false, null, "file is empty");
		
		DB db = mongoDb.getDB(appName);
		// check if bucket exist
		if(!db.getCollectionNames().contains(bucketName+".chunks"))
			return response(false, null, "bucket: " + bucketName
					+ " does not exist in app: " + appName);
		
		GridFS gfs = new GridFS(db, bucketName);
		GridFSDBFile old_file = gfs.findOne( filename );
		if( old_file!=null ){
			return response(false, null, "file: " + filename
					+ " already exists, use PUT to update the file");
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
		return response(true, null, "add file: "+ filename + " in bucket: " + bucketName + " succeded");

	}
	
	// get file
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView getFileFromBucket(@PathVariable String appName,
			@PathVariable String bucketName, @PathVariable String filename, 
			HttpServletResponse response, Principal principal) {
		
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have WRITE permission");
		}
		
		logger_c.debug("/app/" + appName + "/buckets/" + bucketName + "/"+ filename
				+ " : doGET()");

		DB db = mongoDb.getDB(appName);
		// check if bucket exist
		if(!db.getCollectionNames().contains(bucketName+".chunks"))
			return response(false, null, "bucket: " + bucketName
					+ " does not exist in app: " + appName);
		GridFS gfs = new GridFS(db, bucketName);
		GridFSDBFile file = gfs.findOne( filename );
		if(file==null)
			return response(false, null, "file: " + filename
					+ " does not exist in bucket: " + bucketName);
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
	
	// delete file
	@RequestMapping(method = RequestMethod.DELETE)
	public ModelAndView deleteFileFromBucket(@PathVariable String appName,
			@PathVariable String bucketName, @PathVariable String filename
			, Principal principal) {
		if (!isAuthorized())
			return response(false, null, "Invalid username or password");
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have WRITE permission");
		}
		
		logger_c.debug("/app/" + appName + "/buckets/" + bucketName + "/"+ filename
				+ " : doDELETE()");

		DB db = mongoDb.getDB(appName);
		// check if bucket exist
		if(!db.getCollectionNames().contains(bucketName+".chunks"))
			return response(false, null, "bucket: " + bucketName
					+ " does not exist in app: " + appName);
		GridFS gfs = new GridFS(db, bucketName);
		GridFSDBFile file = gfs.findOne( filename );
		if(file==null)
			return response(false, null, "file: " + filename
					+ " does not exist in bucket: " + bucketName);
		
		gfs.remove(filename);
		return response(true, null, "file: " + filename + " deleted");
	}

}
