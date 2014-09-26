package com.ninequintillion.models;

import lombok.Data;

@Data
public class TournamentGame {

    private final Team firstTeam;
    private final Team secondTeam;
    private final Team winningTeam;

}
