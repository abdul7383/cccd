package de.fhg.fokus.ngni.cccd.services;

import java.io.Console;
import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import de.fhg.fokus.ngni.cccd.model.User;


@SuppressWarnings("deprecation")
public class AddAdmin {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Console console = System.console();
		String enteredUsername = new String(console.readLine("Please enter the admin username to be created: "));
		String enteredPassword = new String(console.readPassword("Please enter the password: "));

		PasswordEncoder passwordEncoder = new org.springframework.security.authentication.encoding.Md5PasswordEncoder();
		String encodedPassword = passwordEncoder.encodePassword(enteredPassword,
				enteredUsername);
		System.out.println(encodedPassword);
		User user = new User(enteredUsername,encodedPassword,"Abdul","Hamood");
		user.setRole(1);
		ApplicationContext context= new ClassPathXmlApplicationContext("cccd-config.xml");
		MongoTemplate mongoTemplate  = (MongoTemplate) context.getBean("mongoTemplate");
		mongoTemplate.save(user);
		System.exit(0);
//		TransportClient esClient = new 
//				TransportClient().addTransportAddress(new 
//				InetSocketTransportAddress("localhost", 9300));
//		ClusterState cs = esClient.admin().cluster().prepareState()
//				.setFilterIndices("db1_test3").execute().actionGet()
//				.getState();
//		IndexMetaData imd = cs.getMetaData().index("db1_test3");
//		MappingMetaData mdd = imd.mapping("test3");
//		System.out.println("###################");
//		System.out.println(mdd.sourceAsMap());
//		for(String fi : "id,s".split(","))
//			System.out.println('#'+fi+'#');
	}

}
