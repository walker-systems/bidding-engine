package com.walker.bidding.auction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ConcurrentModificationException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private final AuctionRepository auctionRepository;


    public Mono<Auction> placeBid(String auctionId, String bidder, BigDecimal bidAmount) {
        return auctionRepository.findById(auctionId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Auction not found: " + auctionId)))

                // Begin stream
                .flatMap(auction -> {
                    if (!auction.active()) {
                        return Mono.error(new IllegalStateException("Auction is closed."));
                    }
                    if (bidAmount.compareTo(auction.currentPrice()) <= 0) {
                        return Mono.error(new IllegalArgumentException("Bid must be higher than the current price of "
                                + auction.currentPrice()));
                    }

                    // If above checks are passed, this auction will replace the old auction
                    var newAuction = new Auction(
                            auction.id(),
                            auction.itemId(),
                            bidAmount,            // New price
                            bidder,               // New highest bidder
                            auction.endsAt(),
                            auction.active(),     // Always true - checked upstream
                            auction.version() + 1 // **Increment version**
                    );

                    return auctionRepository.updateWithVersion(newAuction)
                            .flatMap(success -> {
                                if (success) {
                                    log.info("✅ Bid placed successfully by {} for ${}", bidder, bidAmount);
                                    return Mono.just(newAuction);
                                } else {
                                    log.warn("⚠️ Collision detected for auction "
                                                + "{}. Someone else bid at the exact same time!", auctionId);
                                    return Mono.error(new ConcurrentModificationException("Bid collision"));
                                }
                            });
                })

                // Retry from the beginning (findById...) thrice in case of bid collision error
                .retryWhen(Retry.backoff(3, Duration.ofMillis(50))
                        .filter(throwable -> throwable instanceof ConcurrentModificationException)
                );
    }

    // Temporary exception class
    public static class ConcurrentModificationException extends RuntimeException {
        public ConcurrentModificationException(String message) {
            super(message);
        }
    }
}
