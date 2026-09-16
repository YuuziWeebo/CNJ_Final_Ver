package com.glucoze.thesismanagement.thesis.dto;

public record ThesisView(Long id, String title, String description, String academicYear, String semester,
                         String supervisor) {
}