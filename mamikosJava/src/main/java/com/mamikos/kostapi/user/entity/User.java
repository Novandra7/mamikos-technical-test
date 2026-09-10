package com.mamikos.kostapi.user.entity;

import com.mamikos.kostapi.common.audit.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    @Setter
    private String name;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    /** Always a BCrypt hash. Never serialised: no DTO exposes this field. */
    @Column(nullable = false)
    @Setter
    private String password;

    /**
     * Stored as text, never as an ordinal. An ordinal silently remaps every existing row
     * the moment someone reorders the enum constants.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(length = 20)
    @Setter
    private String phone;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Normalises the address so uniqueness is not defeated by letter case. */
    public static String normaliseEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isOwner() {
        return role == UserRole.OWNER;
    }

    public boolean hasCreditWallet() {
        return role.hasCreditWallet();
    }
}
