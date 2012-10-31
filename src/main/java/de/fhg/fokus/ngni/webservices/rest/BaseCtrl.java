package de.fhg.fokus.ngni.webservices.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import com.mongodb.Mongo;


public class BaseCtrl {
	
	@Autowired
	protected Mongo mongoDb;
	
	@Autowired
	protected View jsonView_i;
	
	@Autowired
	protected Boolean debugResponse;
	
	@Autowired 
	protected String settingsCollection;

	protected static final boolean SUCCESS = true;
	protected static final boolean ERROR = false;

	public boolean isAuthorized(){
		// TODO implement isAuthorized()
		return true;
	}
	
	/**
	 * response in json format for nosql quries
	 * The return will be either the response of a query such SELECT
	 * or a confirmation in case of INSERT.
	 *
	 * 
	 */
	
	protected ModelAndView response(boolean sucess, Object result, String debug){
		if(debugResponse){
			if(sucess)
				if(debug!=null)
					return new ModelAndView(jsonView_i, "debug",debug).addObject("ok", "1");
				else
					return new ModelAndView(jsonView_i, "data", result).addObject("ok", "1");
			else
				return new ModelAndView(jsonView_i, "debug",debug).addObject("ok", "0");
		}else{
			if(sucess)
				if(result!=null)
					return new ModelAndView(jsonView_i, "data", result).addObject("ok", "1");
				else
					return new ModelAndView(jsonView_i, "ok", "1");
			else
				return new ModelAndView(jsonView_i, "ok", "0");
		}
	}
		
	/**
	 * Injector methods.
	 *
	 * @param view
	 *            the new json view
	 */
	public void setJsonView(View view) {
		jsonView_i = view;
	}
	
	public Mongo getMongoDb() {
		return mongoDb;
	}

	public void setMongoDb(Mongo mongoDb) {
		this.mongoDb = mongoDb;
	}

	public Boolean getDebugResponse() {
		return debugResponse;
	}

	public void setDebugResponse(Boolean debugResponse) {
		this.debugResponse = debugResponse;
	}

	public String getSettingsCollection() {
		return settingsCollection;
	}

	public void setSettingsCollection(String settingsCollection) {
		this.settingsCollection = settingsCollection;
	}
}