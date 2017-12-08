package org.pdb;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoogleController {

	@Autowired
	OAuth2RestOperations template;

	@RequestMapping("/user/driveInfo")
	public Map<String, Object> driveInfo() {
		@SuppressWarnings("unchecked")
		Map<String, Object> user = template.getForObject(
				"https://www.googleapis.com/drive/v3/about?fields=storageQuota/limit&key=AIzaSyB1qJ27jg8WVlNvMJlnrnMZandHi8fHArI",
				Map.class);
		return user;
	}

}
