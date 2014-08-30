package com.ninequintillion.models;

import lombok.Data;

import java.net.URL;
import java.util.Map;

@Data
public class Team {

    /* Derived From Bracket */
    private final int seed;
    private final int id;
    private final String name;

    /* Derived From Schedule Page */
    private Map<Integer, RegularSeasonGame> schedule;
    private int onePointGamesPlayed;
    private int onePointGamesWon;
    private int twoPointGamesPlayed;
    private int twoPointGamesWon;
    private int threePointGamesPlayed;
    private int threePointGamesWon;
    private int overtimeGamesPlayed;
    private int overtimeGamesWon;
    private int winsAgainstCurrent;
    private int lossesAgainstCurrent;
    private int tiesAgainstCurrent;
    private int gamesAgainstRanked;
    private int winsAgainstRanked;
    private double averagePointsAllowed;
    private String conference;

    /* Derived From Statistics Page */
    private double pointsPerGame;
    private double assistsPerGame;
    private double stealsPerGame;
    private double blocksPerGame;
    private double turnoversPerGame;
    private double fieldGoalPercentage;
    private double freeThrowPercentage;
    private double threePointPercentage;
    private int totalPoints;
    private int totalOffensiveRebounds;
    private int totalDefensiveRebounds;

}
