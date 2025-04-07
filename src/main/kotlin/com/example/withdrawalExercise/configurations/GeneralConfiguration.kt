package com.example.withdrawalExercise.configurations

import com.google.gson.Gson
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GeneralConfiguration {

    @Bean
    fun gson(): Gson {
        return Gson()
    }
}