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
//                        log.debug("Found the bracket: {}", line);
                        Pattern gamePattern = Pattern.compile(".*?<dl id=\"match\\d+\".*?<dt><?b?>?(?<firstSeed>\\d+) <a href=\"(?<firstTeamUrl>.+?)\" title=");
                        Matcher gameMatcher = gamePattern.matcher(line);
                        // TODO: Obviously the call to find() must be in a loop. (Should it execute 63 times, or will some of the games have a different pattern?)
                        if (gameMatcher.find()
                                && gameMatcher.group("firstSeed") != null
                                && gameMatcher.group("firstTeamUrl") != null) {
                            log.debug("Found the first seed: {}", gameMatcher.group("firstSeed"));
                            log.debug("Found the first team URL: {}", gameMatcher.group("firstTeamUrl"));
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
