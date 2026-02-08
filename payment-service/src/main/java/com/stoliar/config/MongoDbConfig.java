package com.stoliar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

@Profile("!test")
@Configuration
public class MongoDbConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(Arrays.asList(
            new BigDecimalToDoubleConverter(),
            new DoubleToBigDecimalConverter()
        ));
    }

    public static class BigDecimalToDoubleConverter implements org.springframework.core.convert.converter.Converter<BigDecimal, Double> {
        @Override
        public Double convert(BigDecimal source) {
            return source != null ? source.doubleValue() : null;
        }
    }

    public static class DoubleToBigDecimalConverter implements org.springframework.core.convert.converter.Converter<Double, BigDecimal> {
        @Override
        public BigDecimal convert(Double source) {
            return source != null ? BigDecimal.valueOf(source) : null;
        }
    }

    public static class DateToLocalDateTimeConverter implements org.springframework.core.convert.converter.Converter<Date, LocalDateTime> {
        @Override
        public LocalDateTime convert(Date source) {
            return source.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
    }

    public static class LocalDateTimeToDateConverter implements org.springframework.core.convert.converter.Converter<LocalDateTime, Date> {
        @Override
        public Date convert(LocalDateTime source) {
            return Date.from(source.atZone(ZoneId.systemDefault()).toInstant());
        }
    }
}