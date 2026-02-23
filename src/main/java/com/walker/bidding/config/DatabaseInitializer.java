package com.walker.bidding.config;

import com.walker.bidding.auction.Auction;
import com.walker.bidding.auction.AuctionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final AuctionRepository auctionRepository;
    private final Random random = new Random();

    private final List<String> DEMO_ITEMS = List.of(
            "Sony PlayStation 5 Pro",
            "Apple MacBook Pro M3 Max",
            "Vintage 1999 Holographic Charizard",
            "Herman Miller Aeron Chair",
            "NVIDIA RTX 4090 GPU",
            "Signed Michael Jordan Baseball",
            "Onewheel Onewheel+ XR",
            "Espresso Machine - La Marzocco"
    );

    @Override
    public void run(String... args) {
        log.info("🌱 Wiping old data and seeding database with realistic auctions...");

        Flux.fromIterable(DEMO_ITEMS)
                .flatMap(itemName -> {
                    // Generate a random starting price between $50 and $500
                    long startingPrice = 50 + random.nextInt(450);

                    Auction newAuction = new Auction(
                            "auction-" + UUID.randomUUID().toString().substring(0, 8), // Short readable ID
                            itemName,
                            BigDecimal.valueOf(startingPrice),
                            "System", // Starting bidder
                            Instant.now().plusSeconds(86400), // Ends in 24 hours
                            true,
                            1
                    );
                    return auctionRepository.save(newAuction);
                })
                .doOnNext(a -> log.info("✅ Seeded Storefront Item: {} starting at ${}", a.itemId(), a.currentPrice()))
                .doOnComplete(() -> log.info("🎉 Storefront fully stocked!"))
                .subscribe();
    }
}
