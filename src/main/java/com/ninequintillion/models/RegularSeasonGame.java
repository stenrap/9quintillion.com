package com.ninequintillion.models;

import lombok.Data;

@Data
public class RegularSeasonGame {

    private final Team opponent;
    private final int pointsAllowed;
    private final boolean won;

}
