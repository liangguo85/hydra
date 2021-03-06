/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.hydra.job.auth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;

import com.addthis.codec.annotations.Time;

import com.google.common.collect.ImmutableSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenCache implements Closeable {

    private static final ImmutableSet<PosixFilePermission> OWNER_READ_WRITE =
            ImmutableSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    public enum ExpirationPolicy {
        WRITE, ACCESS
    }

    private static final Logger log = LoggerFactory.getLogger(TokenCache.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Expiration policy. Default is {@code WRITE}
     */
    @Nonnull
    public final ExpirationPolicy policy;

    /**
     * Expiration time in seconds.
     */
    public final int timeout;

    /**
     * Map each username to a map of tokens to expiration timestamps.
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> cache;

    @Nullable
    private final Path outputPath;

    @JsonCreator
    public TokenCache(@JsonProperty(value = "policy", required = true) ExpirationPolicy policy,
                      @JsonProperty(value = "timeout", required = true) @Time(TimeUnit.SECONDS) int timeout,
                      @JsonProperty("outputPath") Path outputPath) throws IOException {
        this.policy = policy;
        this.timeout = timeout;
        this.outputPath = outputPath;
        if ((outputPath != null) && (Files.isReadable(outputPath))) {
            log.info("Loading authentication tokens from disk.");
            TypeReference<ConcurrentHashMap<String, ConcurrentHashMap<String, Long>>> typeReference =
                    new TypeReference<ConcurrentHashMap<String, ConcurrentHashMap<String, Long>>>() {};
            this.cache = mapper.readValue(outputPath.toFile(), typeReference);
        } else {
            this.cache = new ConcurrentHashMap<>();
        }
    }

    private ConcurrentHashMap<String, Long>  buildCache() {
        return new ConcurrentHashMap<>();
    }

    public boolean get(@Nullable String name, @Nullable String secret) {
        long now = System.currentTimeMillis();
        if ((name == null) || (secret == null)) {
            return false;
        }
        ConcurrentHashMap<String, Long> userTokens = cache.computeIfAbsent(name, (k) -> buildCache());
        Long expiration = userTokens.compute(secret, (key, prev) -> {
            if (prev == null) {
                return null;
            } else if (prev < now) {
                return null;
            } else if (policy == ExpirationPolicy.ACCESS) {
                return (now + TimeUnit.SECONDS.toMillis(timeout));
            } else {
                return prev;
            }
        });
        return (expiration != null);
    }

    public void put(@Nonnull String name, @Nonnull String secret) {
        ConcurrentHashMap<String, Long> userTokens = cache.computeIfAbsent(name, (k) -> buildCache());
        userTokens.put(secret, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeout));
    }

    public int remove(@Nonnull String name, @Nonnull String secret) {
        ConcurrentHashMap<String, Long> userTokens = cache.computeIfAbsent(name, (k) -> buildCache());
        userTokens.remove(secret);
        return userTokens.size();
    }

    public void evict(@Nonnull String name) {
        cache.remove(name);
    }

    @Override
    public void close() throws IOException {
        if (outputPath != null) {
            log.info("Persisting authentication tokens to disk.");
            if (!Files.exists(outputPath)) {
                Files.createFile(outputPath, PosixFilePermissions.asFileAttribute(OWNER_READ_WRITE));
            } else {
                Files.setPosixFilePermissions(outputPath, OWNER_READ_WRITE);
            }
            mapper.writeValue(outputPath.toFile(), cache);
        }
    }
}
