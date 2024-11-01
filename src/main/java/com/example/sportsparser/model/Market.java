package com.example.sportsparser.model;

import lombok.Data;
import java.util.List;

@Data
public class Market {
    private Long id;
    private String name;
    private String typeTag;
    private Boolean open;
    private List<BetOption> betOptions;
}
