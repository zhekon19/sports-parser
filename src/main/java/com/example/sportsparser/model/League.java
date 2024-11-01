package com.example.sportsparser.model;

import lombok.Data;
import java.util.List;

@Data
public class League {
    private Long id;
    private String name;
    private String region;
    private List<Match> matches;
}
