package com.walker.bidding.auction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
@Slf4j
public class BiddingController {

    private final AuctionService auctionService;

    // DTO to specify expected JSON from frontend: {"bidder": "user1", "amount": 150.00}
    public record BidRequest(String bidder, BigDecimal amount) {}

    @PostMapping("/{id}/bids")
    public Mono<Auction> placeBid(@PathVariable String id,
                                  @RequestBody BidRequest request) {
        log.info("Received HTTP POST to place bid on auction {} for ${}", id, request.amount());

        return auctionService.placeBid(id, request.bidder(), request.amount())
                // TODO: Error handling will be replaced with global exception handler
                .onErrorMap(IllegalArgumentException.class, e ->
                        new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()))
                .onErrorMap(IllegalStateException.class, e ->
                        new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()))
                .onErrorMap(AuctionService.ConcurrentModificationException.class, e ->
                        new ResponseStatusException(HttpStatus.CONFLICT, "High traffic. Please try again."));

    }
}
