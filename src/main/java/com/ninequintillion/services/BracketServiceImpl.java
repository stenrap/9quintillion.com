package com.ninequintillion.services;

import com.ninequintillion.exceptions.BracketAnalysisException;
import com.ninequintillion.models.BracketModel;
import com.ninequintillion.models.TournamentGame;
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
    private List<TournamentGame> gameList;
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

                            gameList.add(new TournamentGame(firstTeam, secondTeam, winningTeam));
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

            // Load each team's "Schedule" and "Statistics" pages, and gather the stats available thereon
            for (TournamentGame tournamentGame : gameList) {
                if (tournamentGame.getFirstTeam().getSchedule() == null) {
                    getSchedulePage(tournamentGame.getFirstTeam());
                }
                if (tournamentGame.getSecondTeam().getSchedule() == null) {
                    getSchedulePage(tournamentGame.getSecondTeam());
                }

                // Record Against Current
                for (RegularSeasonGame regularSeasonGame : tournamentGame.getFirstTeam().getSchedule()) {
                    if (regularSeasonGame.getOpponent().getId() == tournamentGame.getSecondTeam().getId()) {
                        if (regularSeasonGame.isWon()) {
                            tournamentGame.setFirstTeamWinsAgainstSecond(tournamentGame.getFirstTeamWinsAgainstSecond() + 1);
                        } else {
                            tournamentGame.setSecondTeamWinsAgainstFirst(tournamentGame.getSecondTeamWinsAgainstFirst() + 1);
                        }
                    }
                }

                if (tournamentGame.getFirstTeam().getGamesPlayed() == 0) {
                    getStatisticsPage(tournamentGame.getFirstTeam());
                }
                if (tournamentGame.getSecondTeam().getGamesPlayed() == 0) {
                    getStatisticsPage(tournamentGame.getSecondTeam());
                }

                if (tournamentGame.getWinningTeam() == tournamentGame.getFirstTeam()) {
                    tournamentGame.setFirstTeamCssClass("analysis-winner");
                    tournamentGame.setSecondTeamCssClass("analysis-loser");
                } else {
                    tournamentGame.setFirstTeamCssClass("analysis-loser");
                    tournamentGame.setSecondTeamCssClass("analysis-winner");
                }
            }
        } finally {
            if (response != null) {
                response.close();
                bufferedReader.close();
            }
        }

        bracketModel.setGameList(gameList);
        return bracketModel;
    }

    private void getSchedulePage(Team team) throws BracketAnalysisException, IOException {
        team.setSchedule(new ArrayList<RegularSeasonGame>());
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
                    // This regex gets every game row (except postseason games, which are explicitly skipped)
                    Pattern regularSeasonGamePattern = Pattern.compile("class=\"team-name\">(?<tourneySeed>\\(\\d+\\) )?#?(?<opponentRank>\\d+)? ?.*?_/id/(?<opponentId>\\d+).*?font\">(?<outcome>W|L)<.*?\">(?<winningScore>\\d+)-(?<losingScore>\\d+).*?(?<overtime>OT)?<\\/a");
                    Matcher regularSeasonGameMatcher = regularSeasonGamePattern.matcher(line);

                    int totalPointsAllowed = 0;

                    while (regularSeasonGameMatcher.find()) {
                        if (regularSeasonGameMatcher.group("tourneySeed") != null) {
                            break;
                        }
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

                        // 2-Point Victories
                        if (winningScore - losingScore == 2) {
                            team.setTwoPointGamesPlayed(team.getTwoPointGamesPlayed() + 1);
                            if (won) {
                                team.setTwoPointGamesWon(team.getTwoPointGamesWon() + 1);
                            }
                        }

                        // 3-Point Victories
                        if (winningScore - losingScore == 3) {
                            team.setThreePointGamesPlayed(team.getThreePointGamesPlayed() + 1);
                            if (won) {
                                team.setThreePointGamesWon(team.getThreePointGamesWon() + 1);
                            }
                        }

                        // Prepare for Record Against Current and Average Points Allowed
                        Team opponent = teamMap.get(opponentId) != null ? teamMap.get(opponentId) : new Team(-1, opponentId, "");
                        int pointsAllowed = won ? losingScore : winningScore;
                        team.getSchedule().add(new RegularSeasonGame(opponent, pointsAllowed, won));

                        // Overtime Record
                        if (regularSeasonGameMatcher.group("overtime") != null) {
                            team.setOvertimeGamesPlayed(team.getOvertimeGamesPlayed() + 1);
                            if (won) {
                                team.setOvertimeGamesWon(team.getOvertimeGamesWon() + 1);
                            }
                        }

                        // Prepare for Average Points Allowed
                        totalPointsAllowed += pointsAllowed;

                        // Wins and Losses
                        if (won) {
                            team.setWins(team.getWins() + 1);
                        } else {
                            team.setLosses(team.getLosses() + 1);
                        }
                    }

                    // Average Points Allowed
                    team.setAveragePointsAllowed((double) totalPointsAllowed / team.getSchedule().size());
//                    log.debug("{} allowed an average of {} points per game", team.getName(), team.getAveragePointsAllowed());
                }

                // Conference
                if (team.getConference() == null) {
                    Pattern conferencePattern = Pattern.compile(".*?div class=\"sub-title\">(?<conference>.*?)</div.*");
                    Matcher conferenceMatcher = conferencePattern.matcher(line);
                    if (conferenceMatcher.matches() && conferenceMatcher.group("conference") != null) {
                        team.setConference(conferenceMatcher.group("conference"));
//                        log.debug("{} is in the {}", team.getName(), team.getConference());
                    }
                }
            }
        } else {
            throw new BracketAnalysisException("Error loading schedule page.");
        }
    }

    private void getStatisticsPage(Team team) throws BracketAnalysisException, IOException {
//        log.debug("Loading statistics page for {}", team.getName());
        response.close();
        bufferedReader.close();
        get = new HttpGet("http://espn.go.com/mens-college-basketball/team/stats/_/id/"+team.getId()+"/year/"+year);
        response = client.execute(get);
        entity = response.getEntity();
        if (entity != null) {
            bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent()));
            while (true) {
                line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                // This regex finds the row containing the total number of games played
                Pattern gamesPlayedPattern = Pattern.compile(".*?Totals<\\/td><td align=\"right\">(?<gamesPlayed>\\d+)<.*");
                Matcher gamesPlayedMatcher = gamesPlayedPattern.matcher(line);
                if (gamesPlayedMatcher.matches()) {
                    team.setGamesPlayed(Integer.parseInt(gamesPlayedMatcher.group("gamesPlayed")));
//                    log.debug("{} played {} games", team.getName(), team.getGamesPlayed());
                }

                // WYLO .... This regex finds the row containing the rest of the statistics
                Pattern statsPattern = Pattern.compile(".*?Totals<\\/td><td align=\"right\">--<.*?>(?<FGM>\\d+)<.*?>(?<FGA>\\d+)<.*?>(?<FTM>\\d+)<.*?>(?<FTA>\\d+)<.*?>(?<TPM>\\d+)<.*?>(?<TPA>\\d+)<.*?>(?<PTS>\\d+)<.*?>(?<OFR>\\d+)<.*?>(?<DFR>\\d+)<.*?>\\d+<.*?>(?<AST>\\d+)<.*?>(?<TO>\\d+)<.*?>(?<STL>\\d+)<.*?>(?<BLK>\\d+)<.*");
                Matcher statsMatcher = statsPattern.matcher(line);
                if (statsMatcher.matches()) {
                    if (statsMatcher.group("FGM") == null
                            || statsMatcher.group("FGA") == null
                            || statsMatcher.group("FTM") == null
                            || statsMatcher.group("FTA") == null
                            || statsMatcher.group("TPM") == null
                            || statsMatcher.group("TPA") == null
                            || statsMatcher.group("PTS") == null
                            || statsMatcher.group("OFR") == null
                            || statsMatcher.group("DFR") == null
                            || statsMatcher.group("AST") == null
                            || statsMatcher.group("TO") == null
                            || statsMatcher.group("STL") == null
                            || statsMatcher.group("BLK") == null) {
                        throw new BracketAnalysisException("Error parsing statistics page.");
                    }
                    team.setFieldGoalPercentage((double) Integer.parseInt(statsMatcher.group("FGM")) / Integer.parseInt(statsMatcher.group("FGA")));
                    team.setFreeThrowPercentage((double) Integer.parseInt(statsMatcher.group("FTM")) / Integer.parseInt(statsMatcher.group("FTA")));
                    team.setThreePointPercentage((double) Integer.parseInt(statsMatcher.group("TPM")) / Integer.parseInt(statsMatcher.group("TPA")));
                    team.setTotalPoints(Integer.parseInt(statsMatcher.group("PTS")));
                    team.setPointsPerGame((double) team.getTotalPoints() / team.getGamesPlayed());
                    team.setTotalOffensiveRebounds(Integer.parseInt(statsMatcher.group("OFR")));
                    team.setTotalDefensiveRebounds(Integer.parseInt(statsMatcher.group("DFR")));
                    team.setAssistsPerGame((double) Integer.parseInt(statsMatcher.group("AST")) / team.getGamesPlayed());
                    team.setTurnoversPerGame((double) Integer.parseInt(statsMatcher.group("TO")) / team.getGamesPlayed());
                    team.setStealsPerGame((double) Integer.parseInt(statsMatcher.group("STL")) / team.getGamesPlayed());
                    team.setBlocksPerGame((double) Integer.parseInt(statsMatcher.group("BLK")) / team.getGamesPlayed());
//                    log.debug("{} had {} STL and {} BLK", team.getName(), team.getStealsPerGame(), team.getBlocksPerGame());
                }
            }
        } else {
            throw new BracketAnalysisException("Error loading statistics page.");
        }
    }

}
