package com.react.spring.meta.metapack.dto;

public class MetaPackFieldDto {
    private String fieldName;
    private String alias;
    private boolean included = true;
    private String type;
    private String dataType;
    private boolean isVirtual;

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public boolean isIncluded() { return included; }
    public void setIncluded(boolean included) { this.included = included; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public boolean isVirtual() { return isVirtual; }
    public void setVirtual(boolean virtual) { isVirtual = virtual; }
}
