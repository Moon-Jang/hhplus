package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class PointIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserPointTable userPointTable;
    @Autowired
    private PointHistoryTable pointHistoryTable;

    @AfterEach
    void setUp() {
        ReflectionTestUtils.setField(userPointTable, "table", new HashMap<>());
        ReflectionTestUtils.setField(pointHistoryTable, "table", new ArrayList<>());
        ReflectionTestUtils.setField(pointHistoryTable, "cursor", 1);
    }
    @Nested
    @DisplayName("포인트 충전 테스트")
    class ChargePointTest {
        @Test
        void 포인트_충전_성공() throws Exception {
            // given
            UserPoint savedPoint = saveUserPoint();
            long chargeAmount = 10000L;
            long expectedAmount = savedPoint.point() + chargeAmount;

            // when
            mockMvc.perform(patch("/point/{id}/charge", savedPoint.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(Long.toString(chargeAmount)));

            // then
            UserPoint resultPoint = userPointTable.selectById(savedPoint.id());
            assertThat(resultPoint.id()).isEqualTo(savedPoint.id());
            assertThat(resultPoint.point()).isEqualTo(expectedAmount);

            List<PointHistory> resultHistories = pointHistoryTable.selectAllByUserId(savedPoint.id());
            PointHistory latestHistory = resultHistories.get(resultHistories.size() - 1);
            assertThat(latestHistory.id()).isPositive();
            assertThat(latestHistory.userId()).isEqualTo(savedPoint.id());
            assertThat(latestHistory.amount()).isEqualTo(chargeAmount);
            assertThat(latestHistory.type()).isEqualTo(TransactionType.CHARGE);
        }

        @Test
        void 포인트_여러번_충전_케이스() throws Exception {
            // given
            UserPoint saved = saveUserPoint();
            List<Long> chargeAmounts = List.of(1000L, 500L);
            long expectedAmount = saved.point() + chargeAmounts.stream().reduce(0L, Long::sum);

            // when
            mockMvc.perform(patch("/point/{id}/charge", saved.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargeAmounts.get(0))));

            mockMvc.perform(patch("/point/{id}/charge", saved.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargeAmounts.get(1))));

            //then
            UserPoint result = userPointTable.selectById(saved.id());
            assertThat(result.id()).isEqualTo(saved.id());
            assertThat(result.point()).isEqualTo(expectedAmount);
            List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(saved.id());
            List<PointHistory> latestHistories = pointHistories.subList(pointHistories.size() - 2, pointHistories.size());
            assertThat(latestHistories.get(0).amount()).isEqualTo(chargeAmounts.get(0));
            assertThat(latestHistories.get(1).amount()).isEqualTo(chargeAmounts.get(1));
        }

        @Test
        void 포인트_충전_최대_금액_초과시_실패() throws Exception {
            // given
            UserPoint saved = saveUserPoint();
            long chargeAmount = UserPoint.MAX_CHARGE_AMOUNT + 1;
            String expectedMessage = "1회 충전 가능한 최대 포인트는 %d 입니다. 충전하시려는 포인트: %d".formatted(UserPoint.MAX_CHARGE_AMOUNT, chargeAmount);

            // when then
            mockMvc.perform(patch("/point/{id}/charge", saved.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargeAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
            UserPoint current = userPointTable.selectById(saved.id());
            assertThat(current.point()).isEqualTo(saved.point());
        }

        @Test
        void 포인트_충전_최소_금액_미만시_실패() throws Exception {
            // given
            UserPoint saved = saveUserPoint();
            long chargeAmount = UserPoint.MIN_CHARGE_AMOUNT - 1;
            String expectedMessage = "1회 충전 가능한 최소 포인트는 %d 입니다. 충전하시려는 포인트: %d".formatted(UserPoint.MIN_CHARGE_AMOUNT, chargeAmount);

            // when then
            mockMvc.perform(patch("/point/{id}/charge", saved.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargeAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
            UserPoint current = userPointTable.selectById(saved.id());
            assertThat(current.point()).isEqualTo(saved.point());
        }

        @Test
        void 포인트_충전_최대_잔액_초과시_실패() throws Exception {
            // given
            UserPoint userPoint = new UserPointFixture().setPoint(UserPoint.MAX_BALANCE).create();
            UserPoint saved = saveUserPoint(userPoint);
            long chargeAmount = 100;
            String expectedMessage = "포인트 잔액은 %d 보다 클 수 없습니다. 현재 포인트: %d, 충전 후 포인트: %d"
                .formatted(UserPoint.MAX_BALANCE, saved.point(), saved.point() + chargeAmount);

            // when then
            mockMvc.perform(patch("/point/{id}/charge", saved.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargeAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
            UserPoint current = userPointTable.selectById(saved.id());
            assertThat(current.point()).isEqualTo(saved.point());
        }
    }

    private UserPoint saveUserPoint() {
        return userPointTable.insertOrUpdate(1L, 100L);
    }

    private UserPoint saveUserPoint(UserPoint userPoint) {
        var saved = userPointTable.insertOrUpdate(userPoint.id(), userPoint.point());
        pointHistoryTable.insert(saved.id(), userPoint.point(), TransactionType.CHARGE, saved.updateMillis());
        return saved;
    }
}