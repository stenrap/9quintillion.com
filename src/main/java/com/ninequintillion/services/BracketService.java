package com.ninequintillion.services;

import com.ninequintillion.models.BracketModel;

public interface BracketService {

    public BracketModel parseBracket(String year, boolean makePrediction);

}
