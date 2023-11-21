package com.example.config;
import com.example.constance.FailedMessage;
import com.example.constance.Notification;
import com.example.repository.FailedMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;
import java.util.HashMap;
import java.util.Map;


@Configuration
@Slf4j
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    private final FailedMessageRepository failedMessageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KafkaConsumerConfig(FailedMessageRepository failedMessageRepository) {
        this.failedMessageRepository = failedMessageRepository;
    }


    @Bean
//    public ConsumerFactory<Notification, Notification> consumerFactory() {
//        HashMap<String, Object> props = new HashMap<>();
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
//        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 10);
//        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 60000);
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-group");
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        ErrorHandlingDeserializer<Notification> errorHandlingDeserializer
//                = new ErrorHandlingDeserializer<>(new JsonDeserializer<>(Notification.class));
//        return new DefaultKafkaConsumerFactory<>(props, new JsonDeserializer<>(Notification.class), errorHandlingDeserializer);
//    }
    public ConsumerFactory<String, Notification> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "message-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*"); // You can restrict trusted packages for security
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Notification.class);

        return new DefaultKafkaConsumerFactory<>(props);
    }


    @Bean("kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Notification> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Notification> concurrentKafkaListenerContainerFactory
                = new ConcurrentKafkaListenerContainerFactory<>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(consumerFactory());
        concurrentKafkaListenerContainerFactory.setCommonErrorHandler(getDefaultErrorHandler());
        return concurrentKafkaListenerContainerFactory;
    }
    private DefaultErrorHandler getDefaultErrorHandler() {
        return new DefaultErrorHandler((record, exception) -> {
            FailedMessage failedMessageEntity = new FailedMessage();
            try {
                failedMessageEntity.setMessage(objectMapper.writeValueAsString(record.value()));
                failedMessageEntity.setException(exception.getClass().toString());
                failedMessageEntity.setTopic(record.topic());
                failedMessageEntity.setConsumerOffset(record.offset());
                failedMessageRepository.save(failedMessageEntity);
                log.error("Saved the failed message to db {}", failedMessageEntity);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }, new FixedBackOff(10000L, 2L));
    }
}
