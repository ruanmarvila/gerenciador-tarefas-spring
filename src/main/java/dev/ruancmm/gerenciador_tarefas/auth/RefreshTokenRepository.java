package dev.ruancmm.gerenciador_tarefas.auth;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = "UPDATE refresh_tokens SET revoked_at = :now WHERE id = :id AND revoked_at IS NULL",
        nativeQuery = true
    )
    int revokeIfActive(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = "UPDATE refresh_tokens SET revoked_at = :now WHERE family_id = :familyId and revoked_at IS NULL",
        nativeQuery = true
    )
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = "UPDATE refresh_tokens SET revoked_at = :now WHERE user_id = :userId and revoked_at IS NULL",
        nativeQuery = true
    )
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = "DELETE FROM refresh_tokens WHERE expires_at < :limit",
        nativeQuery = true
    )
    int deleteExpiredBefore(@Param("limit") LocalDateTime limit);
}
