package com.ninequintillion.models;

import lombok.Data;

import java.util.List;

@Data
public class Team {

    /* Derived From Bracket */
    private final int seed;
    private final int id;
    private final String name;

    /* Derived From Schedule Page */
    private List<RegularSeasonGame> schedule;
    private int gamesAgainstRanked;
    private int winsAgainstRanked;
    private int onePointGamesPlayed;
    private int onePointGamesWon;
    private int twoPointGamesPlayed;
    private int twoPointGamesWon;
    private int threePointGamesPlayed;
    private int threePointGamesWon;
    private int overtimeGamesPlayed;
    private int overtimeGamesWon;
    private double averagePointsAllowed;
    private int wins;
    private int losses;
    private String conference;

    /* Derived From Statistics Page */
    private int gamesPlayed;  // Sometimes this differs from the schedule page, and all the numbers on the statistics page are based off this, so you can't use schedule.size()
    private double fieldGoalPercentage;
    private double freeThrowPercentage;
    private double threePointPercentage;
    private int totalPoints;
    private double pointsPerGame;
    private int totalOffensiveRebounds;
    private int totalDefensiveRebounds;
    private double assistsPerGame;
    private double turnoversPerGame;
    private double stealsPerGame;
    private double blocksPerGame;

}
