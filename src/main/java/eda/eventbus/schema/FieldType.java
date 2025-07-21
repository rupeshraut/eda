package eda.eventbus.schema;

/**
 * Supported field types in event schemas
 */
public enum FieldType {
    STRING,
    INTEGER,
    LONG,
    DOUBLE,
    BOOLEAN,
    TIMESTAMP,
    UUID,
    OBJECT,
    ARRAY,
    MAP,
    ANY
}