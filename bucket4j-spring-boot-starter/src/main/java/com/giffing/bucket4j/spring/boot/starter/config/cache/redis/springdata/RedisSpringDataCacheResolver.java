package com.giffing.bucket4j.spring.boot.starter.config.cache.redis.springdata;

import java.time.Duration;

import org.springframework.data.redis.connection.RedisCommands;

import com.giffing.bucket4j.spring.boot.starter.config.cache.CacheResolver;
import com.giffing.bucket4j.spring.boot.starter.config.cache.ProxyManagerWrapper;
import com.giffing.bucket4j.spring.boot.starter.config.cache.SyncCacheResolver;
import com.giffing.bucket4j.spring.boot.starter.context.ConsumptionProbeHolder;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.spring.cas.SpringDataRedisBasedProxyManager;

/**
 * This class is the Redis implementation of the {@link CacheResolver}.
 *
 */
public class RedisSpringDataCacheResolver implements SyncCacheResolver {
	
	private final RedisCommands redisCommands;
	
	public RedisSpringDataCacheResolver(RedisCommands  redisCommands) {
		this.redisCommands = redisCommands;
	}
	
	public ProxyManagerWrapper resolve(String cacheName) {
		final ProxyManager<byte[]> proxyManager =  SpringDataRedisBasedProxyManager.builderFor(redisCommands)
				.withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
				.build();
		
		return (key, numTokens, bucketConfiguration, metricsListener) -> {
			Bucket bucket = proxyManager.builder().build(key.getBytes(), bucketConfiguration).toListenable(metricsListener);
			return new ConsumptionProbeHolder(bucket.tryConsumeAndReturnRemaining(numTokens));
		};
			
	}
}
