package com.dienmay.entity.nhom5.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
@Slf4j
public class FirebaseConfig {

	private final ResourceLoader resourceLoader;

	@Value("${firebase.service-account-path:classpath:firebase-service-account.json}")
	private String serviceAccountPath;

	public FirebaseConfig(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@PostConstruct
	public void initFirebase() {
		if (!FirebaseApp.getApps().isEmpty()) {
			return;
		}

		try {
			Resource resource = resourceLoader.getResource(serviceAccountPath);
			if (!resource.exists()) {
				log.warn("Firebase service account không tồn tại tại {}. Bỏ qua init khi dev.", serviceAccountPath);
				return;
			}

			try (InputStream inputStream = resource.getInputStream()) {
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
