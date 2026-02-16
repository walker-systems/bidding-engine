package com.walker.bidding.auction;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

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
}
