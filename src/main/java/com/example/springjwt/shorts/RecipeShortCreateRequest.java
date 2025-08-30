package com.example.springjwt.shorts;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RecipeShortCreateRequest {
    private String title;
    private String videoUrl;
    private boolean isPublic = true;
}
