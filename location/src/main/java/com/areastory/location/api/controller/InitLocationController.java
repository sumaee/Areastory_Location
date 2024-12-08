package com.areastory.location.api.controller;

import com.areastory.location.db.repository.ArticleRepository;
import com.areastory.location.dto.common.LocationDto;
import com.areastory.location.dto.response.LocationResp;
import com.areastory.location.util.ObjectMapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class InitLocationController {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapperUtil objectMapperUtil;
    private final ArticleRepository articleRepository;
    private final AtomicInteger totalLocationCount = new AtomicInteger(1323);
    private final AtomicInteger currentLocationCount = new AtomicInteger(0);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @GetMapping("/progress")
    public ResponseEntity<String> getProgress() {
        return isRunning.get() ?
                ResponseEntity.ok(String.format("%.3f", (double) currentLocationCount.get() / totalLocationCount.get() * 100))
                : ResponseEntity.ok("init 진행 중이 아님");
    }


    @GetMapping("/init")
    public ResponseEntity<String> initMap() {
        if (isRunning.get()) {
            return ResponseEntity.ok("initializing");
        }
        isRunning.set(true);

        //data22.txt 에 있는 지역 옮겨담기
        Set<LocationDto> address;
        try {
            address = getAddress();
        } catch (IOException e) {
            throw new RuntimeException("data22 지역 옮겨담기 실패");
        }


        //for
        CountDownLatch cdl = new CountDownLatch(1323);
        for (LocationDto locationDto : address) {
            //대충 지역 한곳 데이터 불러오는 코드
            setMap(cdl, locationDto);
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        isRunning.set(false);
        return ResponseEntity.ok("init finish");
    }

    private Set<LocationDto> getAddress() throws IOException {
        Set<LocationDto> address = new HashSet<>();
        BufferedReader addressReader = new BufferedReader(new FileReader("data22.txt"));
        String line;
        while ((line = addressReader.readLine()) != null) {
            String[] add = line.split(",");
            address.add(new LocationDto(add[0], add[1], add[2]));
            address.add(new LocationDto(add[0], add[1]));
            address.add(new LocationDto(add[0]));
        }
        addressReader.close();

        return address;
    }

    @Async
    protected void setMap(CountDownLatch cdl, LocationDto address) {
        LocationResp locationResp = articleRepository.init(address.getDosi(), address.getSigungu(), address.getDongeupmyeon());
        //불러온 데이터 넣기
        if (locationResp != null) {
            LocationResp redisResp = new LocationResp(locationResp.getArticleId(), locationResp.getImage(), locationResp.getLikeCount(), address);
            try {
                String key = address.toString();
                String value = objectMapperUtil.toString(redisResp);
                redisTemplate.opsForValue().set(key, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
        currentLocationCount.set(currentLocationCount.get() + 1);
        cdl.countDown();
    }
}
