package dev.pehlivan.weatherapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pehlivan.weatherapi.constants.Constants;
import dev.pehlivan.weatherapi.dto.WeatherDto;
import dev.pehlivan.weatherapi.dto.WeatherResponse;
import dev.pehlivan.weatherapi.model.WeatherEntity;
import dev.pehlivan.weatherapi.repository.WeatherRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class WeatherService {

    /*private static final String API_URL = "http://api.weatherstack.com/current?access_key=99e07a88fe3ed9565c9226352de86585&query=";*/
    private final WeatherRepository weatherRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherService(WeatherRepository weatherRepository, RestTemplate restTemplate) {
        this.weatherRepository = weatherRepository;
        this.restTemplate = restTemplate;
    }

    public WeatherDto getWeatherByCityName(String city) {

        Optional<WeatherEntity> weatherEntityOptional = weatherRepository.findFirstByRequestedCityNameOrderByUpdatedTimeDesc(city);

        /*If-Else
        if (weatherEntityOptional.isEmpty()) {
            return WeatherDto.convert(getWeatherFromWeatherStack(city));
        }
        if (weatherEntityOptional.get().getUpdatedTime().isBefore(LocalDateTime.now().minusMinutes(30))) {
            return WeatherDto.convert(getWeatherFromWeatherStack(city));
        }
        return WeatherDto.convert(weatherEntityOptional.get());*/

        /* Functional */
        return weatherEntityOptional.map(weather -> {
            if (weatherEntityOptional.get().getUpdatedTime().isBefore(LocalDateTime.now().minusMinutes(30))) {
                return WeatherDto.convert(getWeatherFromWeatherStack(city));
            }
            return WeatherDto.convert(weather);
        }).orElseGet(() -> WeatherDto.convert(getWeatherFromWeatherStack(city)));
    }

    private WeatherEntity getWeatherFromWeatherStack(String city) {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(getWeatherStackUrl(city), String.class);

        try {
            WeatherResponse weatherResponse = objectMapper.readValue(responseEntity.getBody(), WeatherResponse.class);
            return saveWeatherEntity(city, weatherResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    /*private static final String API_URL = "http://api.weatherstack.com/current?access_key=99e07a88fe3ed9565c9226352de86585&query=";*/
    private String getWeatherStackUrl(String city) {
        return Constants.API_URL + Constants.ACCESS_KEY_PARAM + Constants.API_KEY + Constants.QUERY_KEY_PARAM + city;
    }

    private WeatherEntity saveWeatherEntity(String city, WeatherResponse weatherResponse) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        WeatherEntity weatherEntity = new WeatherEntity(city
                , weatherResponse.location().name()
                , weatherResponse.location().country()
                , weatherResponse.current().temperature()
                , LocalDateTime.now()
                , LocalDateTime.parse(weatherResponse.location().localtime(), dateTimeFormatter));

        return weatherRepository.save(weatherEntity);
    }
}
