package com.react.spring.common.enums;

public enum ConnectorType {
    POSTGRES(SourceType.DATABASE),
    MONGODB(SourceType.DATABASE),
    MYSQL(SourceType.DATABASE),
    REST(SourceType.API),
    GRAPHQL(SourceType.API),
    CSV(SourceType.FILE),
    JSON(SourceType.FILE);

    private final SourceType sourceType;

    ConnectorType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public SourceType getSourceType() {
        return sourceType;
    }
}
