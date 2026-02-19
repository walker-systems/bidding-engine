package com.walker.bidding.config;

import com.walker.bidding.auction.Auction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    // Template = ConnectionFactory (database connection) + SerializationContext (rules for translating keys & values into bytes for Redis)
    public ReactiveRedisTemplate<String, Auction> auctionRedisTemplate(ReactiveRedisConnectionFactory factory) {

        // Define Key serializer (String <-> bytes)
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        // Define Value serializer (Auction <-> JSON <-> bytes)
        RedisSerializer<Auction> valueSerializer =
                // double cast avoids compiler error
                (RedisSerializer<Auction>) (RedisSerializer<?>) RedisSerializer.json();

        // Create context
        RedisSerializationContext<String, Auction> context = RedisSerializationContext
                .<String, Auction>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .build();

        // Create the template
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
