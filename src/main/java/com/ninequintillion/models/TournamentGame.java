package com.ninequintillion.models;

import lombok.Data;

@Data
public class TournamentGame {

    private final Team firstTeam;
    private final Team secondTeam;
    private final Team winningTeam;

    // Regular season wins (if they played each other in the regular season)
    private int firstTeamWinsAgainstSecond;
    private int secondTeamWinsAgainstFirst;

    private String firstTeamCssClass;
    private String secondTeamCssClass;

}
