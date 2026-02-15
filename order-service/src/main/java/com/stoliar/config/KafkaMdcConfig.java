package com.stoliar.config;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.RecordInterceptor;

@Configuration
public class KafkaMdcConfig {

    public static final String TRACE_ID = "trace_id";
    public static final String SPAN_ID = "span_id";

    public static class TraceMdcRecordInterceptor<K, V> implements RecordInterceptor<K, V> {

        public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record) {
            Span span = Span.current();
            SpanContext context = span.getSpanContext();

            if (context != null && context.isValid()) {
                MDC.put(TRACE_ID, context.getTraceId());
                MDC.put(SPAN_ID, context.getSpanId());
            }

            return record;
        }

        public void success(ConsumerRecord<K, V> record) {
            MDC.clear();
        }

        public void failure(ConsumerRecord<K, V> record, Exception exception) {
            MDC.clear();
        }

        @Override
        public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
            return null;
        }

        @Override
        public void success(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
            RecordInterceptor.super.success(record, consumer);
        }

        @Override
        public void failure(ConsumerRecord<K, V> record, Exception exception, Consumer<K, V> consumer) {
            RecordInterceptor.super.failure(record, exception, consumer);
        }

        @Override
        public void afterRecord(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
            RecordInterceptor.super.afterRecord(record, consumer);
        }
    }
}
