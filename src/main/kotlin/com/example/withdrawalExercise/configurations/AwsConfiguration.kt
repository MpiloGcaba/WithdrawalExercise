package com.example.withdrawalExercise.configurations

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient

@Configuration
class AwsConfiguration {

    @Value("\${aws.region}")
    private lateinit var awsRegion: String

    @Value("\${aws.sns.withdrawal-topic-arn}")
    private lateinit var withdrawalTopicArn: String

    @Bean
    fun snsClient(): SnsClient {
        return SnsClient.builder()
            .region(Region.of(awsRegion))
            .build()
    }

    @Bean
    fun withdrawalTopicArn(): String {
        return withdrawalTopicArn
    }
}