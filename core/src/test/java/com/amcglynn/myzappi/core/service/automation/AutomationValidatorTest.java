package com.amcglynn.myzappi.core.service.automation;

import com.amcglynn.myzappi.core.exception.AutomationValidationException;
import com.amcglynn.myzappi.core.exception.CapacityReachedException;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.model.Automation;
import com.amcglynn.myzappi.core.model.AutomationAction;
import com.amcglynn.myzappi.core.model.AutomationOperator;
import com.amcglynn.myzappi.core.model.AutomationPredicate;
import com.amcglynn.myzappi.core.model.EddiDevice;
import com.amcglynn.myzappi.core.model.LibbiDevice;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import com.amcglynn.myzappi.core.model.ZappiDevice;
import com.amcglynn.myzappi.core.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutomationValidatorTest {

    @Mock
    private LoginService loginService;

    private AutomationValidator validator;
    private final UserId userId = UserId.from("user-1");

    @BeforeEach
    void setUp() {
        validator = new AutomationValidator(loginService);
        when(loginService.readDevices(userId)).thenReturn(List.of(new ZappiDevice(SerialNumber.from("10000001")),
                new EddiDevice(SerialNumber.from("20000001"), "tank1", "tank2"),
                new LibbiDevice(SerialNumber.from("30000001"))));
    }

    @Test
    void createRejectsMoreThanTenAutomations() {
        assertThatThrownBy(() -> validator.validateForCreate(userId, validAutomation(), 10))
                .isInstanceOf(CapacityReachedException.class);
    }

    @Test
    void createRejectsNameLongerThanEightyCharacters() {
        var automation = validAutomation().toBuilder().name("a".repeat(81)).build();

        assertThatThrownBy(() -> validator.validateForCreate(userId, automation, 0))
                .isInstanceOf(AutomationValidationException.class);
    }

    @Test
    void createRejectsUnknownPredicateType() {
        var automation = validAutomation().toBuilder()
                .predicate(validPredicate().toBuilder().type("UNKNOWN").build())
                .build();

        assertThatThrownBy(() -> validator.validateForCreate(userId, automation, 0))
                .isInstanceOf(AutomationValidationException.class);
    }

    @Test
    void createRejectsTargetForAccountLevelPredicate() {
        var automation = validAutomation().toBuilder()
                .predicate(validPredicate().toBuilder().target("10000001").build())
                .build();

        assertThatThrownBy(() -> validator.validateForCreate(userId, automation, 0))
                .isInstanceOf(AutomationValidationException.class);
    }

    @Test
    void createRejectsMissingTargetForDevicePredicate() {
        var automation = validAutomation().toBuilder()
                .predicate(validPredicate().toBuilder().type("ZAPPI_EV_CHARGE_RATE_KW").build())
                .build();

        assertThatThrownBy(() -> validator.validateForCreate(userId, automation, 0))
                .isInstanceOf(AutomationValidationException.class);
    }

    @Test
    void createRejectsTargetedPredicateWhenUserDoesNotOwnTargetDevice() {
        var automation = validAutomation().toBuilder()
                .predicate(validPredicate().toBuilder()
                        .type("ZAPPI_EV_CHARGE_RATE_KW")
                        .target("99999999")
                        .build())
                .build();

        assertThatThrownBy(() -> validator.validateForCreate(userId, automation, 0))
                .isInstanceOf(MissingDeviceException.class);
    }

    @Test
    void createRejectsUnknownActionType() {
        var automation = validAutomation().toBuilder()
                .action(validAction().toBuilder().type("UNKNOWN").build())
                .build();

        assertThatThrownBy(() -> validator.validateForCreate(userId, automation, 0))
                .isInstanceOf(AutomationValidationException.class);
    }

    @Test
    void createRejectsActionTargetWhenUserDoesNotOwnTargetDevice() {
        var automation = validAutomation().toBuilder()
                .action(validAction().toBuilder().target("99999999").build())
                .build();

        assertThatThrownBy(() -> validator.validateForCreate(userId, automation, 0))
                .isInstanceOf(MissingDeviceException.class);
    }

    @Test
    void createRejectsActionTargetWithWrongDeviceClass() {
        var automation = validAutomation().toBuilder()
                .action(validAction().toBuilder().target("30000001").build())
                .build();

        assertThatThrownBy(() -> validator.validateForCreate(userId, automation, 0))
                .isInstanceOf(MissingDeviceException.class);
    }

    @Test
    void createRejectsInvalidActionValues() {
        var automation = validAutomation().toBuilder()
                .action(validAction().toBuilder().type("setZappiMgl").value("101").build())
                .build();

        assertThatThrownBy(() -> validator.validateForCreate(userId, automation, 0))
                .isInstanceOf(AutomationValidationException.class);
    }

    @Test
    void reorderRejectsDuplicateIds() {
        assertThatThrownBy(() -> validator.validatePriorityReorder(List.of(existing("a"), existing("b")),
                List.of("a", "a")))
                .isInstanceOf(AutomationValidationException.class);
    }

    @Test
    void reorderRejectsMissingIds() {
        assertThatThrownBy(() -> validator.validatePriorityReorder(List.of(existing("a"), existing("b")),
                List.of("a")))
                .isInstanceOf(AutomationValidationException.class);
    }

    @Test
    void reorderRejectsUnknownIds() {
        assertThatThrownBy(() -> validator.validatePriorityReorder(List.of(existing("a"), existing("b")),
                List.of("a", "c")))
                .isInstanceOf(AutomationValidationException.class);
    }

    private Automation existing(String automationId) {
        return Automation.builder().automationId(automationId).build();
    }

    private Automation validAutomation() {
        return Automation.builder()
                .name("Solar export")
                .predicate(validPredicate())
                .action(validAction())
                .build();
    }

    private AutomationPredicate validPredicate() {
        return AutomationPredicate.builder()
                .type("ENERGY_EXPORTING_KW")
                .operator(AutomationOperator.GREATER_THAN)
                .value("2.0")
                .build();
    }

    private AutomationAction validAction() {
        return AutomationAction.builder()
                .type("setChargeMode")
                .target("10000001")
                .value("ECO_PLUS")
                .build();
    }
}
