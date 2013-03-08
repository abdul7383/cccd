package de.fhg.fokus.ngni.cccd.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import de.fhg.fokus.ngni.cccd.model.User;
import de.fhg.fokus.ngni.cccd.rest.BaseCtrl;

public class CustomUserDetailsService implements UserDetailsService {

	protected static final Logger logger_c = Logger.getLogger(BaseCtrl.class);
	
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private SaltSource saltSource;

	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {
		logger_c.debug("loadUserByUsername");
		logger_c.debug(username);
		User user = getUserDetail(username);
		org.springframework.security.core.userdetails.User userDetail = new org.springframework.security.core.userdetails.User(
				user.getUsername(), user.getPassword(), true, true, true, true,
				getAuthorities(user.getRole()));
		logger_c.debug(userDetail);
		return userDetail;
	}
	
	public List<GrantedAuthority> getAuthorities(Integer role) {
		List<GrantedAuthority> authList = new ArrayList<GrantedAuthority>();
		if (role.intValue() == 1) {
			authList.add(new SimpleGrantedAuthority("ROLE_USER"));
			authList.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		} else if (role.intValue() == 2) {
			authList.add(new SimpleGrantedAuthority("ROLE_USER"));
		}
		return authList;
	}

	public User getUserDetail(String username) {
		MongoOperations mongoOperation = (MongoOperations) mongoTemplate;
		User user = mongoOperation.findOne(new Query(Criteria.where("username")
				.is(username)), User.class);
		
		return user;
	}
	
	public boolean deleteUserDetail(String username) {
		// TODO remove the user also from each app
		MongoOperations mongoOperation = (MongoOperations) mongoTemplate;
		User user = mongoOperation.findOne(new Query(Criteria.where("username")
				.is(username)), User.class);
		if (user == null)
			return false;
		mongoTemplate.remove(user,"user");
		return true;
	}
	
	public boolean addUserDetail(String username, String password,
			String firstName, String lastName, String email, int role) {
		MongoOperations mongoOperation = (MongoOperations) mongoTemplate;
		User user = mongoOperation.findOne(new Query(Criteria.where("username")
				.is(username)), User.class);
		if (user == null){
			String encodedPassword = passwordEncoder.encodePassword(password,
					username);
			mongoTemplate.save(new User(username, encodedPassword, firstName, lastName, email,role));
			return true;
		}
		else
			return false;
	}
	
	public List<User> listUserDetails() {
		// TODO maybe remove the filed password from the user list
		MongoOperations mongoOperation = (MongoOperations) mongoTemplate;
		return mongoOperation.findAll(User.class);
	}

	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	public void setSaltSource(SaltSource saltSource) {
		this.saltSource = saltSource;
	}

}