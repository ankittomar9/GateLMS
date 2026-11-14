package com.gatelms.anatomy.dto;

import java.util.List;

public record CurationQuestionConceptsPatchRequest(
    List<String> tags,
    List<String> addTags,
    List<String> removeTags
) {
    public CurationQuestionConceptsPatchRequest() {
        this(null, List.of(), List.of());
    }
}
