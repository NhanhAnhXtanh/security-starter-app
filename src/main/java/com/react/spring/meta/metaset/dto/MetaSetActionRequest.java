package com.react.spring.meta.metaset.dto;

import jakarta.validation.constraints.Size;

public record MetaSetActionRequest(
        @Size(max = 200) String actor,
        @Size(max = 1000) String comment
) {}
