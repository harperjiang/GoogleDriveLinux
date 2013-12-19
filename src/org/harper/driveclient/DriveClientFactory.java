package org.harper.driveclient;

import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

public class DriveClientFactory {

	private static final String APPLICATION_NAME = "Harper-GoogleDriveClient-1.0";

	/** Directory to store user credentials. */
	public static final java.io.File DATA_STORE_DIR = Configuration.getConfigFolder();

	/**
	 * Global instance of the {@link DataStoreFactory}.
	 */
	private static FileDataStoreFactory dataStoreFactory;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = JacksonFactory
			.getDefaultInstance();

	/** Global instance of the HTTP transport. */
	private static HttpTransport httpTransport;

	/**
	 * 
	 * @return @{Drive}
	 * @throws RuntimeException
	 *             if any error occurred during the creation of Drive client
	 */
	public static Drive createDrive() {
		try {
			// initialize the transport
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();

			// initialize the data store factory
			dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

			// authorization
			Credential credential = authorize();

			// set up global Drive instance
			Drive client = new Drive.Builder(httpTransport, JSON_FACTORY,
					credential).setApplicationName(APPLICATION_NAME).build();
			return client;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** Authorizes the installed application to access user's protected data. */
	private static Credential authorize() throws Exception {
		// load client secrets
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
				JSON_FACTORY,
				new InputStreamReader(DriveClientFactory.class
						.getResourceAsStream("/client_secrets.json")));
		if (clientSecrets.getDetails().getClientId().startsWith("Enter")
				|| clientSecrets.getDetails().getClientSecret()
						.startsWith("Enter ")) {
			throw new IllegalArgumentException("Invalid client_secrets info");
		}

		Set<String> scopes = new HashSet<String>();
		scopes.add(DriveScopes.DRIVE);
		scopes.add(DriveScopes.DRIVE_APPDATA);
		scopes.add(DriveScopes.DRIVE_APPS_READONLY);
		scopes.add(DriveScopes.DRIVE_FILE);

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, JSON_FACTORY, clientSecrets, scopes)
				.setDataStoreFactory(dataStoreFactory).build();
		// authorize
		return new AuthorizationCodeInstalledApp(flow,
				new LocalServerReceiver()).authorize("user");
	}
}
