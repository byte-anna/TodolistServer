import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtUtils {
    private const val SECRET = "your-secret-key-change-in-prod"
    private const val EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000 // 7 дней

    fun generateToken(userId: String, email: String): String {
        return JWT.create()
            .withSubject(userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + EXPIRATION_MS))
            .sign(Algorithm.HMAC256(SECRET))
    }
}