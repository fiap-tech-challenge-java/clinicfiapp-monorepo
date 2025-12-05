CREATE TABLE processed_kafka_events (
                                        event_id UUID PRIMARY KEY,
                                        processed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);