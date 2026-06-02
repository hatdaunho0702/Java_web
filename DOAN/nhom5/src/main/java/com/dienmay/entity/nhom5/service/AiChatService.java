package com.dienmay.entity.nhom5.service;

import com.dienmay.entity.nhom5.entity.Product;
import com.dienmay.entity.nhom5.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiChatService {

    private final ProductRepository productRepository;

    @Value("${app.ai.github-token}")
    private String githubToken;

    @Value("${app.ai.model:gpt-4o-mini}")
    private String modelName;

    @Value("${app.ai.endpoint:https://models.inference.ai.azure.com}")
    private String endpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateReply(List<Map<String, String>> chatHistory) {
        try {
            // 1. Build context from current products in DB
            String systemPrompt = buildSystemPrompt();

            // 2. Prepare payload
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            // Add history
            if (chatHistory != null) {
                for (Map<String, String> msg : chatHistory) {
                    String role = msg.get("role");
                    String content = msg.get("content");
                    if (role != null && content != null) {
                        messages.add(Map.of("role", role, "content", content));
                    }
                }
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);

            // 3. Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + githubToken);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 4. Invoke API
            String url = endpoint + "/chat/completions";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                List choices = (List) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map firstChoice = (Map) choices.get(0);
                    Map message = (Map) firstChoice.get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
            return "Xin lỗi, tôi gặp sự cố khi xử lý câu hỏi của bạn. Vui lòng thử lại sau!";
        } catch (Exception e) {
            log.error("Lỗi khi kết nối với GitHub Models AI API: ", e);
            return "Xin lỗi, hiện tại trợ lý ảo đang bận. Vui lòng liên hệ hỗ trợ hoặc thử lại sau!";
        }
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là ElectraBot - Trợ lý ảo cực kỳ dễ thương và thông minh của cửa hàng điện máy Electra Shop. ")
          .append("Nhiệm vụ của bạn là hỗ trợ khách hàng tìm kiếm sản phẩm, đọc thông số, giá cả, và kiểm tra tồn kho. ")
          .append("Hãy trả lời bằng tiếng Việt một cách tự nhiên, thân thiện, súc tích và chính xác dựa trên danh sách sản phẩm dưới đây. ")
          .append("Nếu khách hỏi sản phẩm không có trong danh sách, hãy khéo léo báo sản phẩm chưa có hàng hoặc giới thiệu sản phẩm tương tự. ")
          .append("Đặc biệt: Hãy dùng icon mặt cười hoặc biểu cảm dễ thương phù hợp để tăng tính gần gũi. ")
          .append("Dưới đây là danh sách sản phẩm hiện tại của cửa hàng được cập nhật trực tiếp từ cơ sở dữ liệu:\n\n");

        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            if (!Boolean.TRUE.equals(p.getIsActive())) {
                continue;
            }
            sb.append("- ").append(p.getName())
              .append(" (ID: #").append(p.getId()).append(")")
              .append(" | Danh mục: ").append(p.getCategory() != null ? p.getCategory().getName() : "Chưa phân loại")
              .append(" | Thương hiệu: ").append(p.getBrand() != null ? p.getBrand().getName() : "Chưa rõ")
              .append(" | Giá gốc: ").append(p.getOriginalPrice()).append("đ");

            if (p.getSalePrice() != null) {
                sb.append(" (Giá khuyến mãi: ").append(p.getSalePrice()).append("đ)");
            }

            sb.append(" | Tồn kho: ").append(p.getStockQty()).append(" sản phẩm");

            if (p.getSpecs() != null && !p.getSpecs().isEmpty()) {
                sb.append(" | Thông số: ");
                p.getSpecs().forEach(spec -> sb.append(spec.getSpecKey()).append(": ").append(spec.getSpecValue()).append(", "));
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
