package com.react.spring.meta.metapack.dto;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class MetaPackVersionItemDto {
    private String id;
    private String metaCode; // Mapped from MetaSetVersion
    private String endpointAlias;
    private String returnType;
    private String parentId;
    private String parentField;
    private String childField;
    private String relationType; // ONE_TO_ONE, ONE_TO_MANY
    private String relationField;
    private Position position;
    private List<MetaPackFieldDto> selectedFields = new ArrayList<>();
    private List<MetaPackVersionItemDto> children = new ArrayList<>();

    public static class Position {
        private double x;
        private double y;
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMetaCode() { return metaCode; }
    public void setMetaCode(String metaCode) { this.metaCode = metaCode; }
    public String getEndpointAlias() { return endpointAlias; }
    public void setEndpointAlias(String endpointAlias) { this.endpointAlias = endpointAlias; }
    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getParentField() { return parentField; }
    public void setParentField(String parentField) { this.parentField = parentField; }
    public String getChildField() { return childField; }
    public void setChildField(String childField) { this.childField = childField; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public String getRelationField() { return relationField; }
    public void setRelationField(String relationField) { this.relationField = relationField; }
    public List<MetaPackFieldDto> getSelectedFields() { return selectedFields; }
    public void setSelectedFields(List<MetaPackFieldDto> selectedFields) { this.selectedFields = selectedFields; }
    public List<MetaPackVersionItemDto> getChildren() { return children; }
    public void setChildren(List<MetaPackVersionItemDto> children) { this.children = children; }
    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
}
