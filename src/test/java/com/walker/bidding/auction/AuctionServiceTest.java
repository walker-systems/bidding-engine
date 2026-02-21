package com.walker.bidding.auction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private AuctionService auctionService;

    @Test
    void placeBid_whenValid_shouldUpdateAndReturnNewAuction() {

        String auctionId = "testAuction";
        Auction auction = new Auction(
                auctionId,
                "testItem",
                new BigDecimal("100.00"),
                "testUserA",
                Instant.now().plusSeconds(3600),
                true,
                1
        );

        Mockito.when(auctionRepository.findById(auctionId)).thenReturn(Mono.just(auction));
        Mockito.when(auctionRepository.updateWithVersion(any(Auction.class))).thenReturn(Mono.just(true));

        Mono<Auction> validBidMono = auctionService.placeBid(
                auctionId,
                "testUserB",
                new BigDecimal("150.00")
        );

        StepVerifier.create(validBidMono)
                .assertNext(updatedAuction -> {
                    assert updatedAuction.currentPrice().equals(new BigDecimal("150.00"));
                    assert updatedAuction.highBidder().equals("testUserB");
                    assert updatedAuction.version() == 2;
                })
                .verifyComplete();
    }

    @Test
    void placeBid_whenBidTooLow_shouldThrowError() {

        String auctionId = "testAuction";
        Auction auction = new Auction(
                auctionId,
                "testItem",
                new BigDecimal("100.00"),
                "testUserA",
                Instant.now().plusSeconds(3600),
                true,
                1
        );

        Mockito.when(auctionRepository.findById(auctionId)).thenReturn(Mono.just(auction));

        Mono<Auction> lowBidMono = auctionService.placeBid(
                auctionId,
                "testUserB",
                new BigDecimal("50.00")
        );

        StepVerifier.create(lowBidMono)
                .verifyError(IllegalArgumentException.class);
    }

    @Test
    void placeBid_whenCollisionOccurs_shouldRetryAndSucceed() {

        String auctionId = "auctionId";
        Auction originalAuction = new Auction(
                auctionId,
                "testItem",
                new BigDecimal("100.00"),
                "originalUser",
                Instant.now().plusSeconds(3600),
                true,
                1
        );

        Auction updatedAuction = new Auction(
                auctionId,
                "testItem",
                new BigDecimal("120.00"),
                "updatedBidUser",
                Instant.now().plusSeconds(3600),
                true,
                2
        );
        AtomicInteger retryCount = new AtomicInteger(0);

        Mockito.when(auctionRepository.findById(auctionId))
                .thenReturn(Mono.defer(() -> {
                    if (retryCount.getAndIncrement() == 0) {
                        return Mono.just(originalAuction);
                    } else {
                        return Mono.just(updatedAuction);
                    }
                }));
        Mockito.when(auctionRepository.updateWithVersion(any(Auction.class)))
                .thenReturn(Mono.just(false))
                .thenReturn(Mono.just(true));


        Mono<Auction> finalBidMono = auctionService.placeBid(
                auctionId,
                "finalBidUser",
                new BigDecimal("150.00")
        );

        StepVerifier.create(finalBidMono)
                .assertNext(finalAuction -> {
                    Assertions.assertEquals(new BigDecimal("150.00"), finalAuction.currentPrice());
                    Assertions.assertEquals("finalBidUser", finalAuction.highBidder());
                    Assertions.assertEquals(3, finalAuction.version());
                })
                .verifyComplete();
    }
}
