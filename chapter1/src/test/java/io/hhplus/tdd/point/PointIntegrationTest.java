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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("포인트 조회 테스트")
    class GetPointTest {
        @Test
        void 포인트_조회_성공() throws Exception {
            // given
            UserPoint savedPoint = saveUserPoint();

            // when & then
            mockMvc.perform(get("/point/{id}", savedPoint.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedPoint.id()))
                .andExpect(jsonPath("$.point").value(savedPoint.point()))
                .andExpect(jsonPath("$.updateMillis").value(savedPoint.updateMillis()));
        }

        @Test
        void 신규_사용자의_경우_0원으로_반환() throws Exception {
            // given
            long userId = 1L;

            // when & then
            mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(0L))
                .andExpect(jsonPath("$.updateMillis").isNumber());
        }

        @Test
        void 포인트_충전후_조회시_충전_내용_반영되어_조회() throws Exception {
            // given
            UserPoint savedPoint = saveUserPoint();
            long chargeAmount = 10000L;
            long expectedAmount = savedPoint.point() + chargeAmount;

            // when
            mockMvc.perform(patch("/point/{id}/charge", savedPoint.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(Long.toString(chargeAmount)));

            // then
            mockMvc.perform(get("/point/{id}", savedPoint.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedPoint.id()))
                .andExpect(jsonPath("$.point").value(expectedAmount))
                .andExpect(jsonPath("$.updateMillis").isNumber());
        }
    }

    @Nested
    @DisplayName("포인트 이력 조회 테스트")
    class GetHistoriesTest {
        @Test
        void 포인트_이력_조회_성공() throws Exception {
            // given
            UserPoint savedPoint = saveUserPoint();

            // when then
            mockMvc.perform(get("/point/{id}/histories", savedPoint.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].userId").value(savedPoint.id()))
                .andExpect(jsonPath("$[0].amount").value(savedPoint.point()))
                .andExpect(jsonPath("$[0].type").value(TransactionType.CHARGE.name()))
                .andExpect(jsonPath("$[0].updateMillis").value(savedPoint.updateMillis()));
        }

        @Test
        void 신규_사용자의_경우_이력_없음() throws Exception {
            // given
            long userId = 1L;

            // when then
            mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void 포인트_충전_and_사용후_조회시_충전_and_사용_이력_확인_가능() throws Exception {
            // given
            UserPoint savedPoint = saveUserPoint();
            long chargeAmount = 10000L;
            long useAmount = 3000L;

            // when
            mockMvc.perform(patch("/point/{id}/charge", savedPoint.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(Long.toString(chargeAmount)));

            mockMvc.perform(patch("/point/{id}/use", savedPoint.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(Long.toString(useAmount)));

            // then
            mockMvc.perform(get("/point/{id}/histories", savedPoint.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].id").isNumber())
                .andExpect(jsonPath("$[1].userId").value(savedPoint.id()))
                .andExpect(jsonPath("$[1].amount").value(chargeAmount))
                .andExpect(jsonPath("$[1].type").value(TransactionType.CHARGE.name()))
                .andExpect(jsonPath("$[1].updateMillis").isNumber())
                .andExpect(jsonPath("$[2].id").isNumber())
                .andExpect(jsonPath("$[2].userId").value(savedPoint.id()))
                .andExpect(jsonPath("$[2].amount").value(useAmount))
                .andExpect(jsonPath("$[2].type").value(TransactionType.USE.name()))
                .andExpect(jsonPath("$[2].updateMillis").isNumber());
        }
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
        var saved = userPointTable.insertOrUpdate(1L, 100L);
        pointHistoryTable.insert(saved.id(), saved.point(), TransactionType.CHARGE, saved.updateMillis());
        return saved;
    }

    private UserPoint saveUserPoint(UserPoint userPoint) {
        var saved = userPointTable.insertOrUpdate(userPoint.id(), userPoint.point());
        pointHistoryTable.insert(saved.id(), userPoint.point(), TransactionType.CHARGE, saved.updateMillis());
        return saved;
    }

    @Nested
    @DisplayName("포인트 사용 테스트")
    class UsePointTest {
        @Test
        void 포인트_사용_성공() throws Exception {
            // given
            UserPoint userPoint = new UserPointFixture().setPoint(10000L).create();
            UserPoint savedPoint = userPointTable.insertOrUpdate(userPoint.id(), userPoint.point());
            long useAmount = 3000L;
            long expectedAmount = savedPoint.point() - useAmount;

            // when
            mockMvc.perform(patch("/point/{id}/use", savedPoint.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(Long.toString(useAmount)));

            //then
            UserPoint result = userPointTable.selectById(savedPoint.id());
            assertThat(result.id()).isEqualTo(savedPoint.id());
            assertThat(result.point()).isEqualTo(expectedAmount);

            List<PointHistory> resultHistories = pointHistoryTable.selectAllByUserId(savedPoint.id());
            PointHistory latestHistory = resultHistories.get(resultHistories.size() - 1);
            assertThat(latestHistory.userId()).isEqualTo(savedPoint.id());
            assertThat(latestHistory.amount()).isEqualTo(useAmount);
            assertThat(latestHistory.type()).isEqualTo(TransactionType.USE);
            assertThat(latestHistory.updateMillis()).isEqualTo(result.updateMillis());
        }

        @Test
        void 포인트_여러번_사용_케이스() throws Exception {
            // given
            UserPoint userPoint = new UserPointFixture().setPoint(10000L).create();
            UserPoint savedPoint = userPointTable.insertOrUpdate(userPoint.id(), userPoint.point());
            List<Long> useAmounts = List.of(1000L, 500L);
            long expectedAmount = savedPoint.point() - useAmounts.stream().reduce(0L, Long::sum);

            // when
            mockMvc.perform(patch("/point/{id}/use", savedPoint.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(useAmounts.get(0))));

            mockMvc.perform(patch("/point/{id}/use", savedPoint.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(useAmounts.get(1))));

            //then
            UserPoint resultPoint = userPointTable.selectById(savedPoint.id());
            assertThat(resultPoint.id()).isEqualTo(savedPoint.id());
            assertThat(resultPoint.point()).isEqualTo(expectedAmount);

            List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(savedPoint.id());
            List<PointHistory> latestHistories = pointHistories.subList(pointHistories.size() - 2, pointHistories.size());
            assertThat(latestHistories.get(0).userId()).isEqualTo(savedPoint.id());
            assertThat(latestHistories.get(0).amount()).isEqualTo(useAmounts.get(0));
            assertThat(latestHistories.get(0).type()).isEqualTo(TransactionType.USE);
            assertThat(latestHistories.get(1).userId()).isEqualTo(savedPoint.id());
            assertThat(latestHistories.get(1).amount()).isEqualTo(useAmounts.get(1));
            assertThat(latestHistories.get(1).type()).isEqualTo(TransactionType.USE);
            assertThat(latestHistories.get(1).updateMillis()).isEqualTo(resultPoint.updateMillis());
        }

        @Test
        void 포인트_부족시_사용_실패() throws Exception {
            // given
            long initialPoint = 1000L;
            UserPoint userPoint = new UserPointFixture().setPoint(initialPoint).create();
            UserPoint savedPoint = userPointTable.insertOrUpdate(userPoint.id(), userPoint.point());
            long useAmount = 2000L; // 보유 포인트보다 큰 금액

            // when & then
            mockMvc.perform(
                    patch("/point/{id}/use", savedPoint.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Long.toString(useAmount))
                )
                .andExpect(status().isBadRequest());

            // 포인트와 이력이 변경되지 않았는지 확인
            UserPoint resultPoint = userPointTable.selectById(savedPoint.id());
            assertThat(resultPoint.point()).isEqualTo(initialPoint);
            List<PointHistory> resultHistories = pointHistoryTable.selectAllByUserId(savedPoint.id());
            assertThat(resultHistories).isEmpty();
        }

        @Test
        void 사용_포인트가_1보다_작을_경우_실패() throws Exception {
            // given
            UserPoint savedPoint = saveUserPoint();
            long amount = 0L;
            String expectedMessage = "사용 포인트는 1 보다 작을 수 없습니다. 사용하시려는 포인트: %d"
                .formatted(amount);

            // when then
            mockMvc.perform(patch("/point/{id}/use", savedPoint.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(amount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
            UserPoint current = userPointTable.selectById(savedPoint.id());
            assertThat(current.point()).isEqualTo(savedPoint.point());
        }

        @Test
        void 잔고가_부족할_경우_실패() throws Exception {
            // given
            UserPoint userPoint = new UserPointFixture().setPoint(10000L).create();
            UserPoint savedPoint = userPointTable.insertOrUpdate(userPoint.id(), userPoint.point());
            long amount = savedPoint.point() + 100L;
            String expectedMessage = "포인트가 부족합니다. 현재 포인트: %d, 사용하려는 포인트: %d"
                .formatted(userPoint.point(), amount);

            // when then
            mockMvc.perform(patch("/point/{id}/use", savedPoint.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(amount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
            UserPoint current = userPointTable.selectById(savedPoint.id());
            assertThat(current.point()).isEqualTo(savedPoint.point());
        }
    }
}