package com.ninequintillion.services;

import com.ninequintillion.exceptions.BracketAnalysisException;
import com.ninequintillion.models.BracketModel;
import com.ninequintillion.models.Game;
import com.ninequintillion.models.RegularSeasonGame;
import com.ninequintillion.models.Team;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BracketServiceImpl implements BracketService {

    private final Logger log = LoggerFactory.getLogger(BracketServiceImpl.class);

    private String year;
    private CloseableHttpClient client;
    private HttpGet get;
    private CloseableHttpResponse response;
    private HttpEntity entity;
    private BufferedReader bufferedReader;
    private String line;
    private List<Game> gameList;
    private Map<Integer, Team> teamMap;

    @Override
    public BracketModel parseBracket(String year, boolean makePrediction) throws BracketAnalysisException, IOException {
        gameList = new ArrayList<>();
        teamMap = new HashMap<>();
        this.year = year;
        client = HttpClients.createDefault();
        get = new HttpGet("http://espn.go.com/mens-college-basketball/tournament/bracket/_/id/"+year+"22/"+year+"-ncaa-mens-final-4-tournament");
        BracketModel bracketModel = new BracketModel();

        try {
            response = client.execute(get);
            entity = response.getEntity();
            if (entity != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent()));
                while (true) {
                    line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    Pattern bracketPattern = Pattern.compile(".*?<div id=\"bracket\".*");
                    Matcher bracketMatcher = bracketPattern.matcher(line);
                    if (bracketMatcher.matches()) {
                        // This regex gets every game in the entire bracket!
                        Pattern gamePattern = Pattern.compile(".*?<(dl|div) id=\"match\\d+\".*?<dt><?b?>?(?<firstSeed>\\d+) <a href=\"(?<firstUrl>.*?)\" title=\"(?<firstName>.*?)\".*?<br/><?b?>?(?<secondSeed>\\d+) <a href=\"(?<secondUrl>.*?)\" title=\"(?<secondName>.*?)\".*?>(?<firstScore>\\d+)<.*?>(?<secondScore>\\d+)<");
                        Matcher gameMatcher = gamePattern.matcher(line);

                        while (gameMatcher.find()) {
                            if (gameMatcher.group("firstSeed")          == null
                                    || gameMatcher.group("firstUrl")    == null
                                    || gameMatcher.group("firstName")   == null
                                    || gameMatcher.group("secondSeed")  == null
                                    || gameMatcher.group("secondUrl")   == null
                                    || gameMatcher.group("secondName")  == null
                                    || gameMatcher.group("firstScore")  == null
                                    || gameMatcher.group("secondScore") == null) {
                                throw new BracketAnalysisException("Error finding team property.");
                            }

//                            log.debug("Found the first seed: {}",        gameMatcher.group("firstSeed"));
//                            log.debug("Found the first URL: {}",         gameMatcher.group("firstUrl"));
//                            log.debug("Found the first team name: {}",   gameMatcher.group("firstName"));
//                            log.debug("Found the second seed: {}",       gameMatcher.group("secondSeed"));
//                            log.debug("Found the second URL: {}",        gameMatcher.group("secondUrl"));
//                            log.debug("Found the second team name: {}",  gameMatcher.group("secondName"));
//                            log.debug("Found the first team score: {}",  gameMatcher.group("firstScore"));
//                            log.debug("Found the second team score: {}", gameMatcher.group("secondScore"));

                            Pattern urlPattern = Pattern.compile(".*?\\/id\\/(?<id>\\d+)");
                            Matcher firstUrlMatcher = urlPattern.matcher(gameMatcher.group("firstUrl"));
                            Matcher secondUrlMatcher = urlPattern.matcher(gameMatcher.group("secondUrl"));

                            if (!firstUrlMatcher.find() || !secondUrlMatcher.find()) {
                                throw new BracketAnalysisException("Error finding team id.");
                            }

                            int firstTeamSeed    = Integer.parseInt(gameMatcher.group("firstSeed"));
                            int firstTeamId      = Integer.parseInt(firstUrlMatcher.group("id"));
                            String firstTeamName = gameMatcher.group("firstName");

//                            log.debug("Found the first id: {}", id);

                            int secondTeamSeed    = Integer.parseInt(gameMatcher.group("secondSeed"));
                            int secondTeamId      = Integer.parseInt(secondUrlMatcher.group("id"));
                            String secondTeamName = gameMatcher.group("secondName");

                            Team firstTeam;
                            Team secondTeam;

//                            log.debug("Found the second id: {}\n", id);

                            if (teamMap.get(firstTeamId) == null) {
                                firstTeam = new Team(firstTeamSeed, firstTeamId, firstTeamName);
                                teamMap.put(firstTeamId, firstTeam);
                            } else {
                                firstTeam = teamMap.get(firstTeamId);
                            }

                            if (teamMap.get(secondTeamId) == null) {
                                secondTeam = new Team(secondTeamSeed, secondTeamId, secondTeamName);
                                teamMap.put(secondTeamId, secondTeam);
                            } else {
                                secondTeam = teamMap.get(secondTeamId);
                            }

                            Team winningTeam = Integer.parseInt(gameMatcher.group("firstScore")) > Integer.parseInt(gameMatcher.group("secondScore")) ? firstTeam : secondTeam;

                            gameList.add(new Game(firstTeam, secondTeam, winningTeam));
                        }

                        break;
                    }
                }

                if (gameList.size() != 63) {
                    throw new BracketAnalysisException("Error finding team.");
                }
            } else {
                throw new BracketAnalysisException("Error getting bracket response entity.");
            }

            // Load each team's "Schedule" page and gather the stats available thereon
            for (Game tournamentGame : gameList) {
                if (tournamentGame.getFirstTeam().getSchedule() == null) {
                    getSchedulePage(tournamentGame.getFirstTeam());
                }
                if (tournamentGame.getSecondTeam().getSchedule() == null) {
                    getSchedulePage(tournamentGame.getSecondTeam());
                }
            }
        } finally {
            if (response != null) {
                response.close();
                bufferedReader.close();
            }
        }

        return bracketModel;
    }

    private void getSchedulePage(Team team) throws BracketAnalysisException, IOException {
        team.setSchedule(new HashMap<Integer, RegularSeasonGame>());
//        log.debug("Loading {}'s schedule page", team.getName());
        response.close();
        bufferedReader.close();
        get = new HttpGet("http://espn.go.com/mens-college-basketball/team/schedule?id="+team.getId()+"&year="+year);
        response = client.execute(get);
        entity = response.getEntity();
        if (entity != null) {
            bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent()));
            while (true) {
                line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                // This regex finds the very first odd row (but all the games are in a single HTML line, so this is the beginning of the schedule)
                Pattern gameRowPattern = Pattern.compile(".*?tr class=\"oddrow .*");
                Matcher gameRowMatcher = gameRowPattern.matcher(line);
                if (gameRowMatcher.matches()) {
                    // This regex gets every game row
                    Pattern regularSeasonGamePattern = Pattern.compile("class=\"team-name\">#?(?<opponentRank>\\d+)? ?.*?_/id/(?<opponentId>\\d+).*?font\">(?<outcome>W|L)<.*?\">(?<winningScore>\\d+)-(?<losingScore>\\d+)<? ?\\d?(?<overtime>OT)?");
                    Matcher regularSeasonGameMatcher = regularSeasonGamePattern.matcher(line);

                    while (regularSeasonGameMatcher.find()) {
                        if (regularSeasonGameMatcher.group("opponentId")          == null
                                || regularSeasonGameMatcher.group("outcome")      == null
                                || regularSeasonGameMatcher.group("winningScore") == null
                                || regularSeasonGameMatcher.group("losingScore")  == null) {
                            throw new BracketAnalysisException("Error loading team schedule.");
                        }

                        int opponentId   = Integer.parseInt(regularSeasonGameMatcher.group("opponentId"));
                        boolean won      = regularSeasonGameMatcher.group("outcome").equalsIgnoreCase("w");
                        int winningScore = Integer.parseInt(regularSeasonGameMatcher.group("winningScore"));
                        int losingScore  = Integer.parseInt(regularSeasonGameMatcher.group("losingScore"));

                        // Victories Against Ranked
                        if (regularSeasonGameMatcher.group("opponentRank") != null) {
                            team.setGamesAgainstRanked(team.getGamesAgainstRanked() + 1);
                            if (won) {
                                team.setWinsAgainstRanked(team.getWinsAgainstRanked() + 1);
                            }
                        }

                        // 1-Point Victories
                        if (winningScore - losingScore == 1) {
                            team.setOnePointGamesPlayed(team.getOnePointGamesPlayed() + 1);
                            if (won) {
                                team.setOnePointGamesWon(team.getOnePointGamesWon() + 1);
                            }
                        }

                        if (regularSeasonGameMatcher.group("overtime") != null) {
//                            log.debug("Game was an overtime game");
                        }
                    }
                    log.debug("{} played {} 1-point games and won {} of them", team.getName(), team.getOnePointGamesPlayed(), team.getOnePointGamesWon());
                }
            }
        } else {
            throw new BracketAnalysisException("Error getting bracket response entity.");
        }
        log.debug("\n\n ===== Next Team ===== \n");
    }

}
