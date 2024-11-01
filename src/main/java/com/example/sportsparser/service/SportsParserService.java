package com.example.sportsparser.service;

import com.example.sportsparser.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@EnableAsync
public class SportsParserService {

    private final RestTemplate restTemplate;

    // TODO replace
    private final String API_URL = "https://leonbets.com/api-2/betline/sports?ctag=en-US&flags=urlv2";
    private final String LEAGUE_MATCHES_API_URL = "https://leonbets.com/api-2/betline/changes/all?ctag=en-US&vtag=9c2cd386-31e1-4ce9-a140-28e9b63a9300&hideClosed=true&flags=reg,urlv2,mm2,rrc,nodup";
    private final String MATCH_DETAILS_API_URL = "https://leonbets.com/api-2/betline/event/all?ctag=en-US&flags=reg,urlv2,mm2,rrc,nodup,smg,outv2";

    public SportsParserService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Async
    public CompletableFuture<Match> fetchMatchDetailsByEventId(Long eventId) {
        String url = MATCH_DETAILS_API_URL + "&eventId=" + eventId;
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode matchNode = response.getBody();
            if (matchNode != null) {
                Match match = new Match();
                match.setId(matchNode.get("id").asLong());
                match.setName(matchNode.get("name").asText());
                match.setKickoff(Instant.ofEpochMilli(matchNode.get("kickoff").asLong()));

                // parse competitors TODO
                List<Competitor> competitors = new ArrayList<>();
                JsonNode competitorsNode = matchNode.get("competitors");
                if (competitorsNode != null && competitorsNode.isArray()) {
                    for (JsonNode competitorNode : competitorsNode) {
                        Competitor competitor = new Competitor();
                        competitor.setId(competitorNode.get("id").asLong());
                        competitor.setName(competitorNode.get("name").asText());
                        competitor.setHomeAway(competitorNode.get("homeAway").asText());
                        competitors.add(competitor);
                    }
                }
                match.setCompetitors(competitors);

                // Parse league
                JsonNode leagueNode = matchNode.get("league");
                if (leagueNode != null) {
                    League league = new League();
                    league.setId(leagueNode.get("id").asLong());
                    league.setName(leagueNode.get("name").asText());
                    JsonNode regionNode = leagueNode.get("region");
                    if (regionNode != null) {
                        league.setRegion(regionNode.get("name").asText());
                    }
                    match.setLeague(league);
                }

                // sport TODO
                JsonNode sportNode = matchNode.get("league").get("sport");
                if (sportNode != null) {
                    Sport sport = new Sport();
                    sport.setId(sportNode.get("id").asLong());
                    sport.setName(sportNode.get("name").asText());
                    sport.setFamily(sportNode.get("family").asText());
                    match.setSport(sport);
                }

                // Parse market
                List<Market> markets = new ArrayList<>();
                JsonNode marketsNode = matchNode.get("markets");
                if (marketsNode != null && marketsNode.isArray()) {
                    for (JsonNode marketNode : marketsNode) {
                        Market market = new Market();
                        market.setId(marketNode.get("id").asLong());
                        market.setName(marketNode.get("name").asText());
                        market.setTypeTag(marketNode.get("typeTag").asText());
                        market.setOpen(marketNode.get("open").asBoolean());

                        // parse bet
                        List<BetOption> betOptions = new ArrayList<>();
                        JsonNode runnersNode = marketNode.get("runners");
                        if (runnersNode != null && runnersNode.isArray()) {
                            for (JsonNode runnerNode : runnersNode) {
                                BetOption betOption = new BetOption();
                                betOption.setId(runnerNode.get("id").asLong());
                                betOption.setName(runnerNode.get("name").asText());
                                betOption.setPrice(runnerNode.get("price").asDouble());
                                betOptions.add(betOption);
                            }
                        }
                        market.setBetOptions(betOptions);
                        markets.add(market);
                    }
                }
                match.setMarkets(markets);
                return CompletableFuture.completedFuture(match);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Cacheable("leagues")
    @Async
    public CompletableFuture<List<Long>> fetchMatchIdsByLeagueId(Long leagueId, int limit) {
        String url = LEAGUE_MATCHES_API_URL + "&league_id=" + leagueId;
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        List<Long> matchIds = new ArrayList<>();

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode rootNode = response.getBody();
            if (rootNode != null && rootNode.has("data")) {
                JsonNode dataNode = rootNode.get("data");

                int count = 0;
                for (JsonNode matchNode : dataNode) {
                    if (count >= limit) break;

                    Long matchId = matchNode.get("id").asLong();
                    matchIds.add(matchId);
                    count++;
                }
            }
        }

        return CompletableFuture.completedFuture(matchIds);
    }

    @Cacheable("sports")
    @Async
    public CompletableFuture<List<Sport>> fetchSportsWithTopLeagues(List<String> requiredSports) {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(API_URL, JsonNode.class);

        List<Sport> sports = new ArrayList<>();

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode rootNode = response.getBody();
            if (rootNode != null && rootNode.isArray()) {
                for (JsonNode sportNode : rootNode) {
                    String sportName = sportNode.get("name").asText();
                    if (!requiredSports.contains(sportName)) {
                        continue;
                    }

                    Sport sport = new Sport();
                    sport.setId(sportNode.get("id").asLong());
                    sport.setName(sportName);
                    sport.setFamily(sportNode.get("family").asText());

                    List<League> topLeagues = new ArrayList<>();
                    JsonNode regionsNode = sportNode.get("regions");

                    if (regionsNode != null && regionsNode.isArray()) {
                        for (JsonNode regionNode : regionsNode) {
                            JsonNode leaguesNode = regionNode.get("leagues");

                            if (leaguesNode != null && leaguesNode.isArray()) {
                                for (JsonNode leagueNode : leaguesNode) {
                                    if (leagueNode.get("top").asBoolean()) {
                                        League league = new League();
                                        league.setId(leagueNode.get("id").asLong());
                                        league.setName(leagueNode.get("name").asText());
                                        league.setRegion(regionNode.get("name").asText());
                                        topLeagues.add(league);
                                    }
                                }
                            }
                        }
                    }

                    sport.setLeagues(topLeagues);
                    sports.add(sport);
                }
            }
        }
        return CompletableFuture.completedFuture(sports);
    }
    public void fetchAndDisplayMatchData() {
        List<String> requiredSports = List.of("Football", "Tennis", "Ice Hockey", "Basketball");

        CompletableFuture<List<Sport>> sportsFuture = fetchSportsWithTopLeagues(requiredSports);
        List<Sport> sports = sportsFuture.join();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        List<CompletableFuture<List<Long>>> matchIdsFutures = new ArrayList<>();

        for (Sport sport : sports) {
            for (League league : sport.getLeagues()) {
                CompletableFuture<List<Long>> matchIdsFuture = fetchMatchIdsByLeagueId(league.getId(), 2);
                matchIdsFutures.add(matchIdsFuture);
            }
        }

        CompletableFuture<Void> allMatchIdsFuture = CompletableFuture.allOf(matchIdsFutures.toArray(new CompletableFuture[0]));

        allMatchIdsFuture.thenRun(() -> {
            for (CompletableFuture<List<Long>> matchIdsFuture : matchIdsFutures) {
                try {
                    List<Long> matchIds = matchIdsFuture.join();

                    for (Long matchId : matchIds) {
                        CompletableFuture<Match> matchFuture = fetchMatchDetailsByEventId(matchId);

                        futures.add(matchFuture.thenAccept(match -> {
                            if (match != null) {
                                System.out.println(match);
                            }
                        }));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

}

