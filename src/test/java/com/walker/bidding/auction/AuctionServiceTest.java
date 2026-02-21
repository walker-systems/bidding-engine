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

        String auctionId = "testAuction";
        Auction auctionStateA = new Auction(
                auctionId,
                "testItem",
                new BigDecimal("100.00"),
                "testUserA",
                Instant.now().plusSeconds(3600),
                true,
                1
        );
        Auction auctionStateB = new Auction(
                auctionId,
                "testItem",
                new BigDecimal("120.00"),
                "testUserB",
                Instant.now().plusSeconds(3600),
                true,
                2
        );
        AtomicInteger attemptCount = new AtomicInteger(1);

        Mockito.when(auctionRepository.findById(auctionId))
                .thenReturn(Mono.defer(() -> {
                    if (attemptCount.getAndIncrement() == 1) {
                        return Mono.just(auctionStateA);
                    } else {
                        return Mono.just(auctionStateB);
                    }
                }));
        Mockito.when(auctionRepository.updateWithVersion(any(Auction.class)))
                .thenReturn(Mono.just(false)) // State A -> False
                .thenReturn(Mono.just(true)); // State B -> True

        Mono<Auction> resultMono = auctionService.placeBid(
                auctionId,
                "testUserC",
                new BigDecimal("150.00")
        );

        StepVerifier.create(resultMono)
                .assertNext(auction -> {
                    Assertions.assertEquals(new BigDecimal("150.00"), auction.currentPrice());
                    Assertions.assertEquals("testUserC", auction.highBidder());
                    Assertions.assertEquals(3, auction.version());
                })
                .verifyComplete();
    }
}
