package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.hhplus.tdd.point.UserPoint.MIN_CHARGE_AMOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PointValidatorTest {
    private PointValidator pointValidator;

    @BeforeEach
    void setUp() {
        pointValidator = new PointValidator();
    }

    @DisplayName("포인트 충전 검증 테스트")
    @Nested
    class validateChargeTest {
        @Test
        void 성공() {
            // given
            UserPoint userPoint = new UserPointFixture()
                .setPoint(0L)
                .create();
            long amount = 100L;

            // when
            Throwable throwable = catchThrowable(() -> pointValidator.validateCharge(userPoint, amount));

            // then
            assertThat(throwable).isNull();
        }

        @Test
        void 충전하려는_포인트가_최대_충전_포인트를_넘었을_경우_실패() {
            // given
            UserPoint userPoint = new UserPointFixture().create();
            long amount = UserPoint.MAX_CHARGE_AMOUNT + 1;
            String expectedMessage = "1회 충전 가능한 최대 포인트는 %d 입니다. 충전하시려는 포인트: %d"
                .formatted(UserPoint.MAX_CHARGE_AMOUNT, amount);

            // when
            Throwable throwable = catchThrowable(() -> pointValidator.validateCharge(userPoint, amount));

            // then
            assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
        }

        @Test
        void 충전하려는_포인트가_최소_충전_포인트보다_작을_경우_실패() {
            // given
            UserPoint userPoint = new UserPointFixture().create();
            int amount = 0;
            String expectedMessage = "1회 충전 가능한 최소 포인트는 %d 입니다. 충전하시려는 포인트: %d"
                .formatted(MIN_CHARGE_AMOUNT, amount);

            // when
            Throwable throwable = catchThrowable(() -> pointValidator.validateCharge(userPoint, amount));

            // then
            assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);


        }

        @Test
        void 포인트_잔액이_최대_한도를_넘을_경우_실패() {
            // given
            UserPoint userPoint = new UserPointFixture()
                .setPoint(UserPoint.MAX_BALANCE)
                .create();
            long amount = 1L;
            String expectedMessage = "포인트 잔액은 %d 보다 클 수 없습니다. 현재 포인트: %d, 충전 후 포인트: %d"
                .formatted(UserPoint.MAX_BALANCE, userPoint.point(), userPoint.point() + amount);

            // when
            Throwable throwable = catchThrowable(() -> pointValidator.validateCharge(userPoint, amount));

            // then
            assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
        }

        @Test
        void 포인트_잔액이_최소_한도보다_낮을_경우_실패() {
            // given
            UserPoint userPoint = new UserPointFixture()
                .setPoint(UserPoint.MIN_BALANCE - 100)
                .create();
            long amount = 1L;
            String expectedMessage = "포인트 잔액은 %d 보다 작을 수 없습니다. 현재 포인트: %d, 충전 후 포인트: %d"
                .formatted(UserPoint.MIN_BALANCE, userPoint.point(), userPoint.point() + amount);

            // when
            Throwable throwable = catchThrowable(() -> pointValidator.validateCharge(userPoint, amount));

            // then
            assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
        }
    }

    @DisplayName("포인트 사용 검증 테스트")
    @Nested
    class validateUseTest {
        @Test
        void 성공() {
            // given
            UserPoint userPoint = new UserPointFixture()
                .setPoint(100L)
                .create();
            long amount = 50L;

            // when
            Throwable throwable = catchThrowable(() -> pointValidator.validateUse(userPoint, amount));

            // then
            assertThat(throwable).isNull();
        }

        @Test
        void 사용_포인트가_1보다_작을_경우_실패() {
            // given
            UserPoint userPoint = new UserPointFixture().create();
            long amount = 0L;
            String expectedMessage = "사용 포인트는 1 보다 작을 수 없습니다. 사용하시려는 포인트: %d"
                .formatted(amount);

            // when
            Throwable throwable = catchThrowable(() -> pointValidator.validateUse(userPoint, amount));

            // then
            assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
        }

        @Test
        void 잔고가_부족할_경우_실패() {
            // given
            UserPoint userPoint = new UserPointFixture()
                .setPoint(100L)
                .create();
            long amount = 200L;
            String expectedMessage = "포인트가 부족합니다. 현재 포인트: %d, 사용하려는 포인트: %d"
                .formatted(userPoint.point(), amount);

            // when
            Throwable throwable = catchThrowable(() -> pointValidator.validateUse(userPoint, amount));

            // then
            assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
        }
    }
}