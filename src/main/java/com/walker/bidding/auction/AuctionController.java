package com.walker.bidding.auction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
@Slf4j
public class AuctionController {

    private final AuctionService auctionService;

    // DTO to specify expected JSON from frontend: {"bidder": "user1", "amount": 150.00}
    public record BidRequest(String bidder, BigDecimal amount) {}

    @PostMapping("/{id}/bids")
    public Mono<Auction> placeBid(@PathVariable String id,
                                  @RequestBody BidRequest request) {
        log.info("Received HTTP POST to place bid on auction {} for ${}", id, request.amount());

        return auctionService.placeBid(id, request.bidder(), request.amount());
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Auction> streamLiveBids(@PathVariable String id) {
        log.info("Client connected to live stream for auction {}", id);
        return auctionService.streamAuctionUpdates(id);
    }

    @GetMapping
    public Flux<Auction> getAllAuctions() {
        log.info("Fetching all active auctions for storefront");
        return auctionService.getAllAuctions();
    }
}
