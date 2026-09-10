package com.mamikos.kostapi.support;

import com.mamikos.kostapi.auth.web.dto.AuthResponse;
import com.mamikos.kostapi.auth.web.dto.RegisterRequest;
import com.mamikos.kostapi.common.response.ApiResponse;
import com.mamikos.kostapi.kost.entity.RoomType;
import com.mamikos.kostapi.kost.web.dto.request.AddressRequest;
import com.mamikos.kostapi.kost.web.dto.request.CreateKostRequest;
import com.mamikos.kostapi.kost.web.dto.response.OwnerKostResponse;
import com.mamikos.kostapi.user.entity.UserRole;
import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/** Small, shared helpers so every IT does not reimplement "register a user and grab the
 * token" from scratch. */
public final class TestApi {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private TestApi() {}

    public static String uniqueEmail(String label) {
        return "%s-%d-%d@example.test".formatted(label, System.nanoTime(), SEQUENCE.incrementAndGet());
    }

    public static AuthResponse register(TestRestTemplate restTemplate, String name, UserRole role) {
        RegisterRequest request = new RegisterRequest(
                name, uniqueEmail(role.name().toLowerCase()), "Password123", "Password123", null, role);
        ResponseEntity<ApiResponse<AuthResponse>> response = restTemplate.exchange(
                "/api/v1/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {});
        AuthResponse body =
                response.getBody() == null ? null : response.getBody().data();
        if (body == null) {
            throw new IllegalStateException("Registration failed: " + response);
        }
        return body;
    }

    public static OwnerKostResponse createKost(
            TestRestTemplate restTemplate, String ownerAccessToken, String name, BigDecimal price, String city) {
        CreateKostRequest request = new CreateKostRequest(
                name,
                "Test kost created by an integration test",
                new AddressRequest("Jl. Test No. 1", "Test District", city, "Test Province", null),
                null,
                null,
                price,
                RoomType.CAMPUR,
                10,
                4,
                true,
                Set.of("wifi"),
                java.util.List.of());
        ResponseEntity<ApiResponse<OwnerKostResponse>> response = restTemplate.exchange(
                "/api/v1/owner/kosts",
                HttpMethod.POST,
                new HttpEntity<>(request, bearer(ownerAccessToken)),
                new ParameterizedTypeReference<ApiResponse<OwnerKostResponse>>() {});
        OwnerKostResponse body =
                response.getBody() == null ? null : response.getBody().data();
        if (body == null) {
            throw new IllegalStateException("Kost creation failed: " + response);
        }
        return body;
    }

    public static HttpHeaders bearer(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
