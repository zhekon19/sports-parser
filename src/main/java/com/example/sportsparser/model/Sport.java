package com.example.sportsparser.model;

import lombok.Data;
import java.util.List;

@Data
public class Sport {
    private Long id;
    private String name;
    private String family;
    private List<League> leagues;
}
