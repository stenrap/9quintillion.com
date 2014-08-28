package com.ninequintillion.services;

import com.ninequintillion.exceptions.BracketAnalysisException;
import com.ninequintillion.models.BracketModel;

import java.io.IOException;

public interface BracketService {

    public BracketModel parseBracket(String year, boolean makePrediction) throws BracketAnalysisException, IOException;

}
