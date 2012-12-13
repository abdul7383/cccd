package de.fhg.fokus.ngni.services;

import java.net.UnknownHostException;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import com.mongodb.Mongo;

import de.fhg.fokus.ngni.model.User;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PasswordEncoder passwordEncoder = new org.springframework.security.authentication.encoding.Md5PasswordEncoder();
		Mongo mongo=null;
		try {
			mongo = new Mongo("localhost:27017");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String encodedPassword = passwordEncoder.encodePassword("cccd1",
				"cccd1");
		User user = new User("cccd1",encodedPassword,"Abdul","Hamood");
		user.setRole(1);
		MongoTemplate mongoTemplate = new MongoTemplate(mongo,"users");
		mongoTemplate.save(user);

	}

}
