package com.plataforma.renta.infrastructure.persistence.type;

import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

public class CustomPostgreSQLEnumJdbcType extends PostgreSQLEnumJdbcType {

    public static final CustomPostgreSQLEnumJdbcType INSTANCE = new CustomPostgreSQLEnumJdbcType();

    @Override
    public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
        @SuppressWarnings("unchecked")
        final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) javaType.getJavaType();
        final String typeName = toSnakeCase(enumClass.getSimpleName());
        return (appender, value, dialect, wrapperOptions) -> {
            appender.appendSql("'");
            appender.appendSql(((Enum<?>) value).name());
            appender.appendSql("'::");
            appender.appendSql(typeName);
        };
    }

    private static String toSnakeCase(String input) {
        return input.replaceAll("([a-z])([A-Z]+)", "_").toLowerCase();
    }
}
