package com.example.demo;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder;
import com.amazonaws.services.route53.model.*;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class R53FailoverRtngApplication {

	public static void main(String[] args) {
		SpringApplication.run(R53FailoverRtngApplication.class, args);
		
		
		
		BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIAT3OBPQ7EEWFHCUI2", "452lfSGt9tV3HHecoxsAEViLLSu+BWFo1UkLICog");
		AmazonRoute53 route53 = AmazonRoute53ClientBuilder.standard()
		    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
		    .withRegion("ap-south-1")
		    .build();
//		AmazonRoute53 route53 = AmazonRoute53ClientBuilder.standard().withCredentials(new ProfileCredentialsProvider())
//				.withRegion("ap-south-1") // Set your AWS region
//				.build();

		// Create health check for primary resource
		String primaryHealthCheckId = createHealthCheck(route53,
				"k8s-eksapp-ingresse-feef13f5d5-18010782.ap-south-1.elb.amazonaws.com", "unique-primary-check");

		// Create health check for secondary resource
		String secondaryHealthCheckId = createHealthCheck(route53,
				"k8s-eksapp-ingresse-02198e2c0d-1488838075.us-east-1.elb.amazonaws.com", "unique-secondary-check");

		// Hosted zone ID
		String hostedZoneId = "Z05668886VMI0QDO3WAU"; 

		// Create DNS records with failover configuration
		createFailoverRecord(route53, hostedZoneId, "failoverroutingusingsdk.farukkadir.online",
				"k8s-eksapp-ingresse-feef13f5d5-18010782.ap-south-1.elb.amazonaws.com", "Primary", primaryHealthCheckId,
				"PRIMARY");
		createFailoverRecord(route53, hostedZoneId, "failoverroutingusingsdk.farukkadir.online",
				"k8s-eksapp-ingresse-02198e2c0d-1488838075.us-east-1.elb.amazonaws.com", "Secondary",
				secondaryHealthCheckId, "SECONDARY");
	}

	private static String createHealthCheck(AmazonRoute53 route53, String domainName, String callerReference) {
		HealthCheckConfig config = new HealthCheckConfig().withFullyQualifiedDomainName(domainName).withPort(80)
				.withType("HTTP").withResourcePath("/getallcrew").withRequestInterval(30).withFailureThreshold(3);

//				new HealthCheckConfig().withIPAddress(domainName).withPort(80).withType("HTTP")
//				.withResourcePath("/").withRequestInterval(30).withFailureThreshold(3);

		CreateHealthCheckRequest request = new CreateHealthCheckRequest().withCallerReference(callerReference)
				.withHealthCheckConfig(config);

		CreateHealthCheckResult response = route53.createHealthCheck(request);
		return response.getHealthCheck().getId();
	}

	private static void createFailoverRecord(AmazonRoute53 route53, String hostedZoneId, String domainName,
			String ipAddress, String identifier, String healthCheckId, String failoverType) {

		ResourceRecordSet recordSet = new ResourceRecordSet().withName(domainName).withType("CNAME").withTTL(300L)
				.withResourceRecords(new ResourceRecord(ipAddress)).withSetIdentifier(identifier)
				.withFailover(failoverType).withHealthCheckId(healthCheckId);

//		ResourceRecordSet recordSet = new ResourceRecordSet().withName(domainName).withType("A").withTTL(300L)
//				.withResourceRecords(new ResourceRecord(ipAddress)).withSetIdentifier(identifier)
//				.withFailover(failoverType).withHealthCheckId(healthCheckId);

		Change change = new Change().withAction(ChangeAction.CREATE).withResourceRecordSet(recordSet);

		ChangeResourceRecordSetsRequest request = new ChangeResourceRecordSetsRequest().withHostedZoneId(hostedZoneId)
				.withChangeBatch(new ChangeBatch().withChanges(change));

		route53.changeResourceRecordSets(request);

	}

}
