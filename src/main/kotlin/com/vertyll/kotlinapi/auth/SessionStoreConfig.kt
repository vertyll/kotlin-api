package com.vertyll.kotlinapi.auth

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.security.jackson.SecurityJacksonModules
import org.springframework.security.web.savedrequest.SimpleSavedRequest
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator

/**
 * How the attributes of a session are written to Redis: as JSON, never as Java serialization.
 *
 * A session holds the security context, the `OAuth2AuthorizedClient` with the user's tokens, the
 * sign-in in progress and the page it returns to. Java serialization would tie each stored session
 * to the exact classes of the release that wrote it, and reading it back would instantiate whatever
 * classes its bytes name. Spring Security's Jackson modules map its own types and allow only those
 * — and the collections they hold — as polymorphic types, so a stored value naming any other class
 * is refused. Spring Session picks the serializer up by its bean name.
 */
@Configuration(proxyBeanMethods = false)
class SessionStoreConfig {
    @Bean
    fun springSessionDefaultRedisSerializer(): RedisSerializer<Any> = sessionSerializer()

    /** The page a sign-in returns to, which Spring Security's modules leave out, read and written field by field. */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
    )
    internal interface SimpleSavedRequestMixin

    companion object {
        // The collections List.of, Set.of and Map.of return, which the claims of a sign-in's ID token can be held in;
        // Spring Security's own list allows only the collections it creates itself.
        private const val JDK_IMMUTABLE_COLLECTIONS = "java.util.ImmutableCollections$"

        fun sessionSerializer(): RedisSerializer<Any> =
            GenericJacksonJsonRedisSerializer(
                JsonMapper
                    .builder()
                    .addModules(
                        SecurityJacksonModules.getModules(
                            SessionStoreConfig::class.java.classLoader,
                            BasicPolymorphicTypeValidator
                                .builder()
                                .allowIfSubType(JDK_IMMUTABLE_COLLECTIONS)
                                .allowIfSubType(SimpleSavedRequest::class.java),
                        ),
                    ).addMixIn(SimpleSavedRequest::class.java, SimpleSavedRequestMixin::class.java)
                    .build(),
            )
    }
}
