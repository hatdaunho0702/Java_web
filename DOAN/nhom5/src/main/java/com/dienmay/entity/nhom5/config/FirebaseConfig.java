package com.dienmay.entity.nhom5.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@Slf4j
public class FirebaseConfig {

	private final FirebaseProperties firebaseProperties;

	public FirebaseConfig(FirebaseProperties firebaseProperties) {
		this.firebaseProperties = firebaseProperties;
	}

	@PostConstruct
	public void initFirebase() {
		if (!FirebaseApp.getApps().isEmpty()) {
			return;
		}

		try {
			String serviceAccountPath = firebaseProperties.getServiceAccountPath();
			log.info("Đang load service account từ: {}", serviceAccountPath);
			String classPathLocation = serviceAccountPath != null && serviceAccountPath.startsWith("classpath:")
					? serviceAccountPath.substring("classpath:".length())
					: serviceAccountPath;
			try (InputStream inputStream = new ClassPathResource(classPathLocation).getInputStream()) {
				FirebaseOptions options = FirebaseOptions.builder()
						.setCredentials(GoogleCredentials.fromStream(inputStream))
						.build();
				FirebaseApp.initializeApp(options);
				log.info("Đã khởi tạo Firebase Admin SDK");
			}
		} catch (Exception ex) {
			log.warn("Không thể khởi tạo Firebase Admin SDK khi dev: {}", ex.getMessage());
		}
	}
}
