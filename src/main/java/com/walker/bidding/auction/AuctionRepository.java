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
                local auctionKey = KEYS[1]
                local proposedAuctionJson = ARGV[1]
                
                local databaseAuctionJson = redis.call('GET', auctionKey)
                if not databaseAuctionJson then return false end
                
                local databaseAuction = cjson.decode(databaseAuctionJson)
                local proposedAuction = cjson.decode(proposedAuctionJson)
                
                if databaseAuction.version == (proposedAuction.version - 1) then
                    redis.call('SET', auctionKey, proposedAuctionJson)
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
