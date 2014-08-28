package com.ninequintillion.services;

import com.ninequintillion.exceptions.BracketAnalysisException;
import com.ninequintillion.models.BracketModel;
import com.ninequintillion.models.Game;
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
    private List<Game> gameList = new ArrayList<>();
    private Map<Integer, Team> teamMap = new HashMap<>();

    @Override
    public BracketModel parseBracket(String year, boolean makePrediction) throws BracketAnalysisException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://espn.go.com/mens-college-basketball/tournament/bracket/_/id/"+year+"22/"+year+"-ncaa-mens-final-4-tournament");
        CloseableHttpResponse response = null;
        HttpEntity entity = null;
        BufferedReader bufferedReader = null;
        String line = null;
        BracketModel bracketModel = new BracketModel();

        try {
            response = client.execute(get);
            entity = response.getEntity();
            if (entity != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent()));
                int gameCount = 0;
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

                            int seed = Integer.parseInt(gameMatcher.group("firstSeed"));
                            int id = Integer.parseInt(firstUrlMatcher.group("id"));
                            String name = gameMatcher.group("firstName");
                            Team firstTeam  = new Team(seed, id, name);

//                            log.debug("Found the first id: {}", id);

                            seed = Integer.parseInt(gameMatcher.group("secondSeed"));
                            id = Integer.parseInt(secondUrlMatcher.group("id"));
                            name = gameMatcher.group("secondName");
                            Team secondTeam = new Team(seed, id, name);

//                            log.debug("Found the second id: {}\n", id);

                            if (teamMap.get(firstTeam.getId()) == null) {
                                teamMap.put(firstTeam.getId(), firstTeam);
                            }

                            if (teamMap.get(secondTeam.getId()) == null) {
                                teamMap.put(secondTeam.getId(), secondTeam);
                            }

                            Team winningTeam = Integer.parseInt(gameMatcher.group("firstScore")) > Integer.parseInt(gameMatcher.group("secondScore")) ? firstTeam : secondTeam;

                            gameList.add(new Game(firstTeam, secondTeam, winningTeam));

                            gameCount++;
                        }

                        break;
                    }
                }

                if (gameCount != 63) {
                    throw new BracketAnalysisException("Error finding team.");
                }
            } else {
                throw new BracketAnalysisException("Error getting bracket response entity.");
            }

            // WYLO ... The code below (looping over the map) works. However, it can't help you find the record against the current opponent in the tournament.
            //          In other words, it might be best to loop over the gameList...but you'll somehow need to avoid multiple GET requests for every team
            //          that won multiple games in the tournament...(Should each team instance store its entire regular season?)

            // Load each team's "Schedule" page and gather the stats available thereon
            for (Team team : teamMap.values()) {
                log.debug("Loading {}'s schedule page", team.getName());
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
                        Pattern gameRowPattern = Pattern.compile(".*?tr class=\"oddrow .*");
                        Matcher gameRowMatcher = gameRowPattern.matcher(line);
                        if (gameRowMatcher.matches()) {
                            // This regex gets every game row
                            Pattern scheduleGamePattern = Pattern.compile("class=\"team-name\">#(?<opponentRank>\\d+) ");
                            Matcher scheduleGameMatcher = scheduleGamePattern.matcher(line);

                            while (scheduleGameMatcher.find()) {
                                if (scheduleGameMatcher.group("opponentRank") != null) {
                                    log.debug("{} played a ranked opponent: {}", team.getName(), scheduleGameMatcher.group("opponentRank"));
                                }
                            }
                        }
                    }
                } else {
                    throw new BracketAnalysisException("Error getting bracket response entity.");
                }
                log.debug("\n\n ===== Next Team ===== \n");
            }
        } finally {
            if (response != null) {
                response.close();
                bufferedReader.close();
            }
        }

        return bracketModel;
    }

}
