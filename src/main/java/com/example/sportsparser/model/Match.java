package com.example.sportsparser.model;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class Match {
    private Long id;
    private String name;
    private List<Competitor> competitors;
    private Instant kickoff;
    private League league;
    private Sport sport;
    private List<Market> markets;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(sport.getName()).append(", ").append(league.getName()).append("\n");
        sb.append(name).append(", ").append(kickoff).append(" UTC, ").append(id).append("\n");

        for (Market market : markets) {
            sb.append(market.getName()).append("\n");
            for (BetOption option : market.getBetOptions()) {
                sb.append(option.getName()).append(", ").append(option.getPrice()).append(", ").append(option.getId()).append("\n");
            }
        }
        return sb.toString();
    }
}
