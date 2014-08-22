package com.ninequintillion.services;

import com.ninequintillion.models.BracketModel;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BracketServiceImpl implements BracketService {

    private final Logger log = LoggerFactory.getLogger(BracketServiceImpl.class);

    @Override
    public BracketModel parseBracket(String year, boolean makePrediction) {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://espn.go.com/mens-college-basketball/tournament/bracket/_/id/"+year+"22/"+year+"-ncaa-mens-final-4-tournament");
        CloseableHttpResponse response = null;
        BufferedReader bufferedReader = null;
        BracketModel bracketModel = new BracketModel();

        try {
            response = client.execute(get);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent()));
                while (true) {
                    String line = bufferedReader.readLine();
//                    log.debug("Found a line: {}", line);
                    if (line == null)
                        break;
                    Pattern bracketPattern = Pattern.compile(".*<div id=\"bracket\".*");
                    Matcher bracketMatcher = bracketPattern.matcher(line);
                    if (bracketMatcher.matches()) {
                        // This gets the first two rounds of every region
                        Pattern gamePattern = Pattern.compile(".*?<dl id=\"match\\d+\".*?<dt><?b?>?(?<firstSeed>\\d+) <a href=\"(?<firstTeamUrl>.*?)\" title=\"(?<firstTeamName>.*?)\".*?<br/><?b?>?(?<secondSeed>\\d+) <a href=\"(?<secondTeamUrl>.*?)\" title=\"(?<secondTeamName>.*?)\".*?>(?<firstTeamScore>\\d+)<.*?>(?<secondTeamScore>\\d+)<");
                        Matcher gameMatcher = gamePattern.matcher(line);
                        while (gameMatcher.find()
                                && gameMatcher.group("firstSeed") != null
                                && gameMatcher.group("firstTeamUrl") != null
                                && gameMatcher.group("firstTeamName") != null
                                && gameMatcher.group("secondSeed") != null
                                && gameMatcher.group("secondTeamUrl") != null
                                && gameMatcher.group("secondTeamName") != null
                                && gameMatcher.group("firstTeamScore") != null
                                && gameMatcher.group("secondTeamScore") != null) {
                            log.debug("Found the first seed: {}", gameMatcher.group("firstSeed"));
                            log.debug("Found the first team URL: {}", gameMatcher.group("firstTeamUrl"));
                            log.debug("Found the first team name: {}", gameMatcher.group("firstTeamName"));
                            log.debug("Found the second seed: {}", gameMatcher.group("secondSeed"));
                            log.debug("Found the second team URL: {}", gameMatcher.group("secondTeamUrl"));
                            log.debug("Found the second team name: {}", gameMatcher.group("secondTeamName"));
                            log.debug("Found the first team score: {}", gameMatcher.group("firstTeamScore"));
                            log.debug("Found the second team score: {}\n", gameMatcher.group("secondTeamScore"));
                        }

                        // NOTE: At this point, if makePrediction is true, you can start making the prediction
                        if (makePrediction) {

                        } else {
                            // This gets the sweet sixteen?

                        }

                    }
                }
            }
        } catch (IOException e1) {
            log.debug("Exception in first try: {}", e1);
        } finally {
            if (response != null){
                try {
                    response.close();
                    bufferedReader.close();
                } catch (IOException e2) {
                    log.debug("Exception in second try: {}", e2);
                }
            }
        }

        return bracketModel;
    }

}
