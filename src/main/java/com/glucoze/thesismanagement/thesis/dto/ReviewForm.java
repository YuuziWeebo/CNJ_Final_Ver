package com.glucoze.thesismanagement.thesis.dto;

import jakarta.validation.constraints.Size;

public class ReviewForm {

    @Size(max = 2000)
    private String feedback;

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}