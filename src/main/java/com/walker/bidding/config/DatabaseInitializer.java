package com.walker.bidding.config;

import com.walker.bidding.auction.Auction;
import com.walker.bidding.auction.AuctionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final AuctionRepository auctionRepository;

    @Override
    public void run(String... args) {
        log.info("🌱 Seeding database with test auction...");

        Auction testAuction = new Auction(
                "auction-1",
                "item-tesla-model-s",               // Item ID
                new BigDecimal("100.00"),           // Current Price
                "user-1",                           // High Bidder
                Instant.now().plusSeconds(3600),    // Ends in 1 hr
                true                                // Active
        );

        auctionRepository.save(testAuction)
                .doOnSuccess(a -> log.info("✅ Auction created: {}", a))
                .doOnError(e -> log.error("❌ Failed to create auction", e))
                .subscribe();
    }
}
