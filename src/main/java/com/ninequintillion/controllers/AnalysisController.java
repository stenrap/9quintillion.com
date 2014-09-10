package com.ninequintillion.controllers;

import com.ninequintillion.exceptions.BracketAnalysisException;
import com.ninequintillion.models.BracketModel;
import com.ninequintillion.models.StartAnalysisModel;
import com.ninequintillion.services.BracketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@Controller
@RequestMapping(value = "/analysis")
public class AnalysisController {

    private final Logger log = LoggerFactory.getLogger(AnalysisController.class);
    private final String hashedPassword = "312b67e8bce3216c6280f0dd87e5f43b3fb2977831a612d55b3d6cd77b709a64";

    @Autowired
    private BracketService bracketService;

    @RequestMapping
    public String showAnalysisPage() {
        return "analysis/login";
    }

    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView startAnalysis(StartAnalysisModel startAnalysisModel, BindingResult result) throws BracketAnalysisException, IOException {
        if (result.hasErrors()) {
            return new ModelAndView("redirect:/analysis");
        }

        // TODO: Make sure the POSTed password matches the hashedPassword

        BracketModel bracketModel = bracketService.parseBracket(startAnalysisModel.getYear(), false);

        ModelAndView modelAndView = new ModelAndView("analysis/result");
        modelAndView.addObject("result", startAnalysisModel);
        return modelAndView;
    }

    @ExceptionHandler(BracketAnalysisException.class)
    public String handleException(Exception e) {
        return "analysis/error"; // TODO: Make this view
    }

    // TODO: Figure out what to do about IOException from bracket analysis, which are currently unhandled

}
