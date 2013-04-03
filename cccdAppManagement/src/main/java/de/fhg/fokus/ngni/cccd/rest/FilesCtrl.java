package de.fhg.fokus.ngni.cccd.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.mongodb.DB;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

/**
 * FundsController class will expose a series of RESTful endpoints
 */
@Controller
// @RequestMapping(value = "/app/{appName}/buckets/{filename:.*}")
@RequestMapping(value = "/app/{appName}/buckets/{bucketName}/files")
public class FilesCtrl extends BaseCtrl {

	protected static final Logger logger_c = Logger.getLogger(FilesCtrl.class);

	// add a file to bucket
	@RequestMapping(method = RequestMethod.POST)
	public ModelAndView addFileToBucket(@PathVariable String appName,
			@PathVariable String bucketName,
			@RequestParam(value = "file", required = false) MultipartFile file,
			Principal principal) {
		if (!canRead(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have permission");
		}

		logger_c.debug("/app/" + appName + "/buckets/" + bucketName + "/"
				+ "/files : doPOST()");

		// if(filename==null || filename=="")
		// return response(false, null,
		// "required String parameter 'filename' is not present");

		if (file == null || file.isEmpty())
			return response(false, null, "file is empty");

		DB db = mongoDb.getDB(appName);
		// check if bucket exist
		if (!db.getCollectionNames().contains(bucketName + ".chunks"))
			return response(false, null, "bucket: " + bucketName
					+ " does not exist in app: " + appName);

		GridFS gfs = new GridFS(db, bucketName);
		/*
		 * GridFSDBFile old_file = gfs.findOne( filename ); if( old_file!=null
		 * ){ return response(false, null, "file: " + filename +
		 * " already exists, use PUT to update the file"); }
		 */
		InputStream in = null;
		try {
			in = file.getInputStream();
		} catch (IOException e) {
			return response(false, null,
					"error while reading the uploaded file");
		}
		if (in == null)
			return response(false, null,
					"error while reading the uploaded file");

		GridFSInputFile newfile = gfs.createFile(in, file.getOriginalFilename());
		newfile.setContentType(file.getContentType());
		newfile.save();
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HashMap<String,String> map = new HashMap<String,String>();
		map.put("app", appName);
		map.put("bucket",bucketName);
		map.put("objectid", newfile.getId().toString());
		return response(true, map, null);

	}

	// get file
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView getFileFromBucket(
			@PathVariable String appName,
			@PathVariable String bucketName,
			@RequestParam(value = "objectid", required = true) String objectid,
			HttpServletResponse response, Principal principal) {

		if (!canRead(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have permission");
		}

		logger_c.debug("/app/" + appName + "/buckets/" + bucketName + "/"
				+ "/files : doGET()");

		DB db = mongoDb.getDB(appName);
		// check if bucket exist
		if (!db.getCollectionNames().contains(bucketName + ".chunks"))
			return response(false, null, "bucket: " + bucketName
					+ " does not exist in app: " + appName);
		GridFS gfs = new GridFS(db, bucketName);
		GridFSDBFile file = gfs.findOne(new ObjectId(objectid));
		if (file == null)
			return response(false, null, "file with ObejectID=" + objectid
					+ " does not exist in bucket: " + bucketName);
		// InputStream in = new FileInputStream(file);
		response.setContentType(file.getContentType());
		InputStream is = file.getInputStream();
		int read = 0;
		byte[] bytes = new byte[1024];
		OutputStream os = null;
		try {
			os = response.getOutputStream();
			while ((read = is.read(bytes)) != -1) {
				os.write(bytes, 0, read);
			}
			os.flush();
			os.close();
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response(true, null, "");
	}

	// delete file
	@RequestMapping(method = RequestMethod.DELETE)
	public ModelAndView deleteFileFromBucket(
			@PathVariable String appName,
			@PathVariable String bucketName,
			@RequestParam(value = "objectid", required = false) String objectid,
			Principal principal) {
		
		if (!canWrite(appName, principal.getName())) {
			if (!mongoDb.getDatabaseNames().contains(appName))
				return response(false, null, "app: " + appName
						+ " is not found");
			return response(false, null, "you don't have WRITE permission");
		}

		logger_c.debug("/app/" + appName + "/buckets/" + bucketName
				+ "/files : doDELETE()");

		DB db = mongoDb.getDB(appName);
		// check if bucket exist
		if (!db.getCollectionNames().contains(bucketName + ".chunks"))
			return response(false, null, "bucket: " + bucketName
					+ " does not exist in app: " + appName);
		GridFS gfs = new GridFS(db, bucketName);
		GridFSDBFile file = gfs.findOne(new ObjectId(objectid));
		if (file == null)
			return response(false, null, "file with ObjectID=" + objectid
					+ " does not exist in bucket: " + bucketName);

		gfs.remove(new ObjectId(objectid));
		return response(true, null, "file with ObjectID=" + objectid
				+ " deleted");
	}

}
