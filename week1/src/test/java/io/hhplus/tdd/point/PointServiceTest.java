package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {
    @InjectMocks
    private PointService pointService;
    @Mock
    private UserPointTable userPointTable;
    @Mock
    private PointHistoryTable pointHistoryTable;
    @Mock
    private PointValidator pointValidator;

    @DisplayName("조회 기능 테스트")
    @Nested
    class getTest {
        @Test
        void 성공() {
            // given
            UserPoint userPoint = new UserPointFixture().create();
            given(userPointTable.selectById(userPoint.id()))
                .willReturn(userPoint);

            // when
            UserPoint result = pointService.get(userPoint.id());

            // then
            assertThat(result).isEqualTo(userPoint);
            verify(userPointTable).selectById(userPoint.id());
        }
    }

    @DisplayName("히스토리 조회 기능 테스트")
    @Nested
    class getHistoriesTest {
        @Test
        void 성공() {
            // given
            long userId = 1L;
            List<PointHistory> pointHistories = List.of(
                new PointHistoryFixture()
                    .setId(1L)
                    .setUserId(userId)
                    .setType(TransactionType.CHARGE)
                    .setAmount(1000)
                    .create(),
                new PointHistoryFixture()
                    .setId(2L)
                    .setUserId(userId)
                    .setType(TransactionType.CHARGE)
                    .setAmount(500)
                    .create()
            );
            given(pointHistoryTable.selectAllByUserId(userId))
                .willReturn(pointHistories);

            // when
            List<PointHistory> result = pointService.getHistories(userId);

            // then
            assertThat(result).isEqualTo(pointHistories);
            verify(pointHistoryTable).selectAllByUserId(userId);
        }
    }

    @DisplayName("충전 기능 테스트")
    @Nested
    class chargeTest {
        @Test
        void 성공() {
            // given
            UserPoint userPoint = new UserPointFixture().create();
            long amount = 100L;
            given(userPointTable.selectById(userPoint.id()))
                .willReturn(userPoint);

            // when
            Throwable throwable = catchThrowable(() -> pointService.charge(userPoint.id(), amount));

            // then
            assertThat(throwable).isNull();
            verify(pointValidator).validateCharge(userPoint, amount);
            verify(userPointTable).insertOrUpdate(userPoint.id(), userPoint.point() + amount);
            verify(pointHistoryTable).insert(eq(userPoint.id()), eq(amount), eq(TransactionType.CHARGE), anyLong());
        }
    }

    @DisplayName("사용 기능 테스트")
    @Nested
    class useTest {
        @Test
        void 성공() {
            // given
            UserPoint userPoint = new UserPointFixture().create();
            long amount = 100L;
            given(userPointTable.selectById(userPoint.id()))
                .willReturn(userPoint);

            // when
            Throwable throwable = catchThrowable(() -> pointService.use(userPoint.id(), amount));

            // then
            assertThat(throwable).isNull();
            verify(pointValidator).validateUse(userPoint, amount);
            verify(userPointTable).insertOrUpdate(userPoint.id(), userPoint.point() - amount);
            verify(pointHistoryTable).insert(eq(userPoint.id()), eq(amount), eq(TransactionType.USE), anyLong());
        }
    }
}