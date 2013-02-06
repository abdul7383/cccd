package de.fhg.fokus.ngni.services;

import java.io.IOException;

import org.springframework.security.authentication.encoding.PasswordEncoder;


public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		PasswordEncoder passwordEncoder = new org.springframework.security.authentication.encoding.Md5PasswordEncoder();
//		Mongo mongo=null;
//		try {
//			mongo = new Mongo("localhost:27017");
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		String encodedPassword = passwordEncoder.encodePassword("abdul",
				"abdul");
		System.out.println(encodedPassword);
//		User user = new User("cccd1",encodedPassword,"Abdul","Hamood");
//		user.setRole(1);
//		MongoTemplate mongoTemplate = new MongoTemplate(mongo,"users");
//		mongoTemplate.save(user);
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
