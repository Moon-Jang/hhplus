package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class ConcurrencyControlTest {
    @Autowired
    private PointService pointService;
    @Autowired
    private UserPointTable userPointTable;
    @Autowired
    private PointHistoryTable pointHistoryTable;
    @SpyBean
    private PointValidator pointValidator;

    @AfterEach
    void setUp() {
        ReflectionTestUtils.setField(userPointTable, "table", new HashMap<>());
        ReflectionTestUtils.setField(pointHistoryTable, "table", new ArrayList<>());
        ReflectionTestUtils.setField(pointHistoryTable, "cursor", 1);
    }

    @Test
    public void 포인트를_여러번_충전해도_데이터_정합성이_유지된다() throws InterruptedException {
        // given
        UserPoint userPoint = saveUserPoint();
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        long chargeAmount = 100L;
        long expectedPoint = userPoint.point() + chargeAmount * threadCount;

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                pointService.charge(userPoint.id(), chargeAmount);
                latch.countDown();
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        var updatedPoint = userPointTable.selectById(userPoint.id());
        assertThat(updatedPoint.point()).isEqualTo(expectedPoint);
    }

    @Test
    public void 사용과_충전을_동시에_진행해도_데이터_정합성이_유지된다() throws InterruptedException {
        // given
        UserPoint userPoint = saveUserPoint(
            new UserPointFixture().setPoint(100_000L).create()
        );
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        executorService.submit(() -> {
            pointService.use(userPoint.id(), 500L);
            latch.countDown();
        });
        executorService.submit(() -> {
            pointService.use(userPoint.id(), 5000L);
            latch.countDown();
        });
        executorService.submit(() -> {
            pointService.charge(userPoint.id(), 2000L);
            latch.countDown();
        });
        executorService.submit(() -> {
            pointService.use(userPoint.id(), 100L);
            latch.countDown();
        });
        executorService.submit(() -> {
            pointService.charge(userPoint.id(), 333L);
            latch.countDown();
        });

        latch.await();
        executorService.shutdown();

        // then
        var updatedPoint = userPointTable.selectById(userPoint.id());
        long expectedPoint = 100_000L - 500L - 5000L + 2000L - 100L + 333L;
        assertThat(updatedPoint.point()).isEqualTo(expectedPoint);
    }

    @Test
    void 동시에_여러번_요청이왔을때_중간에_익셉션이_발생해도_락이_해제되면서_정상_동작한다() throws InterruptedException {
        // given
        UserPoint userPoint = saveUserPoint();
        int threadCount = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Long> chargeAmount = List.of(100L, 200L, 300L);
        long expectedPoint = userPoint.point() + 100L + 300L;

        doThrow(new RuntimeException("TEST EXCEPTION"))
            .when(pointValidator).validateCharge(any(UserPoint.class), eq(chargeAmount.get(1)));

        // when
        for (int i = 0; i < threadCount; i++) {
            long amount = chargeAmount.get(i);
            executorService.submit(() -> {
                try {
                    pointService.charge(userPoint.id(), amount);
                } catch (Exception e) {
                    log.error("error", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        var updatedPoint = userPointTable.selectById(userPoint.id());
        assertThat(updatedPoint.point()).isEqualTo(expectedPoint);
    }

    /*
     * 시나리오 설명
     * 1. 아이디가 다른 여러 유저가 각각의 방식으로 동시에 충전을 시도한다.
     * 2. 각 방식별로 수행한 시간을 기록한다.
     * 3. ReentrantLock 방식이 synchronized 방식보다 빠르다는 것을 확인한다.
     */
    @Test
    void synchronized와_ConcurrentHashMap_ReentrantLock의_성능을_비교한다() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * 2);
        long chargeAmount = 100L;
        long[] synchronizedTimes = new long[threadCount];
        long[] reentrantLockTimes = new long[threadCount];

        // when
        // synchronized 방식 테스트
        for (int i = 0; i < threadCount; i++) {
            int index = i;
            long userId = index + 1;
            executorService.submit(() -> {
                long startTime = System.currentTimeMillis();
                pointService.synchronizedCharge(userId, chargeAmount);
                long endTime = System.currentTimeMillis();
                synchronizedTimes[index] = endTime - startTime;
                latch.countDown();
            });
        }

        // ReentrantLock 방식 테스트
        for (int i = 0; i < threadCount; i++) {
            int index = i;
            long userId = index + 1;
            executorService.submit(() -> {
                long startTime = System.currentTimeMillis();
                pointService.charge(userId, chargeAmount);
                long endTime = System.currentTimeMillis();
                reentrantLockTimes[index] = endTime - startTime;
                latch.countDown();
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        long synchronizedAvg = calculateAverage(synchronizedTimes);
        long reentrantLockAvg = calculateAverage(reentrantLockTimes);

        log.info("Synchronized 평균 실행 시간: {}ms", synchronizedAvg);
        log.info("ReentrantLock 평균 실행 시간: {}ms", reentrantLockAvg);

        assertThat(reentrantLockAvg).isLessThan(synchronizedAvg);
    }

    private long calculateAverage(long[] times) {
        long sum = 0;
        for (long time : times) {
            sum += time;
        }
        return sum / times.length;
    }

    private UserPoint saveUserPoint() {
        var saved = userPointTable.insertOrUpdate(1L, 100L);
        pointHistoryTable.insert(saved.id(), saved.point(), TransactionType.CHARGE, saved.updateMillis());
        return saved;
    }

    private UserPoint saveUserPoint(UserPoint userPoint) {
        var saved = userPointTable.insertOrUpdate(userPoint.id(), userPoint.point());
        pointHistoryTable.insert(saved.id(), userPoint.point(), TransactionType.CHARGE, saved.updateMillis());
        return saved;
    }
}
