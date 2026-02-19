package com.walker.bidding.auction;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuctionRepository {

    private final ReactiveRedisTemplate<String, Auction> template;

    private String getKey(String auctionId) {
        return "auctions:" + auctionId;
    }

    public Mono<Auction> save(Auction auction) {
        return template.opsForValue()
                .set(getKey(auction.id()), auction)
                .thenReturn(auction);
    }

    public Mono<Auction> findById(String id) {
        return template.opsForValue().get(getKey(id));
    }

    // Atomically updates auction only if version matches.
    public Mono<Boolean> updateWithVersion(Auction newAuction) {
        String lua = """
                local key = KEYS[1]
                local newObjJson = ARGV[1]
                
                -- Get the current object as a JSON string
                local currentJson = redis.call('GET', key)
                if not currentJson then return false end
                
                -- Parse both JSON strings
                local currentObj = cjson.decode(currentJson)
                local newObj = cjson.decode(newObjJson)
                
                -- Optimistic lock: to save, current DB version must == (new version - 1)
                if currentObj.version == (newObj.version - 1) then
                    redis.call('SET', key, newObjJson)
                    return true
                else
                    return false
                end
                """;

        return template.execute(
                RedisScript.of(lua, Boolean.class),
                List.of(getKey(newAuction.id())), // KEYS[1]
                List.of(newAuction) // ARGV[1]
        ).next();
    }
}
