package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.dal.ScheduleDetailsRepository;
import com.amcglynn.myzappi.core.dal.UserScheduleRepository;
import com.amcglynn.myzappi.core.exception.MissingDeviceException;
import com.amcglynn.myzappi.core.model.MyEnergiDeployment;
import com.amcglynn.myzappi.core.model.Schedule;
import com.amcglynn.myzappi.core.model.ScheduleAction;
import com.amcglynn.myzappi.core.model.ScheduleDetails;
import com.amcglynn.myzappi.core.model.ScheduleRecurrence;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleResponse;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.SchedulerException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private UserScheduleRepository mockRepository;
    @Mock
    private ScheduleDetailsRepository mockScheduleDetailsRepository;
    private ScheduleService service;
    @Mock
    private SchedulerClient mockSchedulerClient;
    @Mock
    private LoginService mockLoginService;
    @Captor
    private ArgumentCaptor<CreateScheduleRequest> createScheduleRequestArgumentCaptor;
    private UserId userId = UserId.from("mockUserId");
    private final CreateScheduleResponse createScheduleResponse = CreateScheduleResponse.builder().scheduleArn("mockScheduleArn").build();
    @Captor
    private ArgumentCaptor<DeleteScheduleRequest> deleteScheduleRequestCapture;

    @BeforeEach
    void setUp() {
        when(mockSchedulerClient.createSchedule(any(CreateScheduleRequest.class))).thenReturn(createScheduleResponse);
        when(mockLoginService.readCredentials("mockUserId"))
                .thenReturn(Optional.of(new MyEnergiDeployment("mockUserId", SerialNumber.from("12345678"),
                        SerialNumber.from("09876543"), null, null)));
        service = new ScheduleService(mockRepository, mockScheduleDetailsRepository, mockSchedulerClient,
                mockLoginService, "mockExecutionArn", "mockLambdaArn");
    }

    @Test
    void create() {
        var schedule = Schedule.builder()
                .startDateTime(LocalDateTime.of(2023, 9, 10, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .action(ScheduleAction.builder()
                        .type("setChargeMode")
                        .value("ECO+")
                        .build())
                .build();
        schedule.getAction().toString();    // added for code coverage
        var response = service.createSchedule(UserId.from("mockUserId"), schedule);
        verify(mockRepository).write(UserId.from("mockUserId"), List.of(response));
        verify(mockScheduleDetailsRepository).write(response.getId(), UserId.from("mockUserId"));
        verify(mockSchedulerClient).createSchedule(createScheduleRequestArgumentCaptor.capture());
        assertThat(response.getId()).isNotNull();
        assertThat(createScheduleRequestArgumentCaptor.getValue().name()).isEqualTo(response.getId());
        assertThat(createScheduleRequestArgumentCaptor.getValue().scheduleExpression()).isEqualTo("at(2023-09-10T14:00)");
        assertThat(createScheduleRequestArgumentCaptor.getValue().target().input()).isEqualTo("{\n" +
                "\"scheduleId\": \"" + response.getId() + "\",\n" +
                "\"lwaUserId\": \"mockUserId\"\n" +
                "}");
    }

    @Test
    void createThrows409WhenCreatingEddiScheduleButEddiDoesNotExistForUser() {
        var schedule = Schedule.builder()
                .startDateTime(LocalDateTime.of(2023, 9, 10, 14, 0))
                .zoneId(ZoneId.of("Europe/Dublin"))
                .action(ScheduleAction.builder()
                        .type("setEddiMode")
                        .value("STOPPED")
                        .build())
                .build();
        var serverException = catchThrowableOfType(() -> service.createSchedule(UserId.from("mockUserId"), schedule), MissingDeviceException.class);
        assertThat(serverException).isNotNull();
    }

    @Test
    void createRecurringSchedule() {
        var schedule = Schedule.builder()
                .zoneId(ZoneId.of("Europe/Dublin"))
                .action(ScheduleAction.builder()
                        .type("setChargeMode")
                        .value("ECO+")
                        .build())
                .recurrence(ScheduleRecurrence.builder()
                        .timeOfDay(LocalTime.of(14, 0))
                        .daysOfWeek(Set.of(1, 4, 6))
                        .build())
                .build();
        var response = service.createSchedule(UserId.from("mockUserId"), schedule);
        verify(mockRepository).write(UserId.from("mockUserId"), List.of(response));
        verify(mockScheduleDetailsRepository).write(response.getId(), UserId.from("mockUserId"));
        verify(mockSchedulerClient).createSchedule(createScheduleRequestArgumentCaptor.capture());
        assertThat(response.getId()).isNotNull();
        assertThat(createScheduleRequestArgumentCaptor.getValue().name()).isEqualTo(response.getId());
        assertThat(createScheduleRequestArgumentCaptor.getValue().scheduleExpression()).startsWith("cron(0 14 ? * 1,4,6 *)");

        assertThat(createScheduleRequestArgumentCaptor.getValue().target().input()).isEqualTo("{\n" +
                "\"scheduleId\": \"" + response.getId() + "\",\n" +
                "\"lwaUserId\": \"mockUserId\"\n" +
                "}");
    }

    @Test
    void getScheduleReturnsEmptyOptionalWhenScheduleNotFoundForUser() {
        when(mockScheduleDetailsRepository.read("mockScheduleId")).thenReturn(java.util.Optional.empty());
        var result = service.getSchedule("mockScheduleId");
        assertThat(result).isEmpty();
    }

    @Test
    void getScheduleReturnsEmptyOptionalWhenScheduleIsFoundInScheduleDetailsTableButNoSchedulesFoundInUserScheduleTable() {
        when(mockScheduleDetailsRepository.read("mockScheduleId"))
                .thenReturn(Optional.of(new ScheduleDetails("mockScheduleId", UserId.from("mockUserId"))));
        when(mockRepository.read(UserId.from("mockUserId"))).thenReturn(List.of());
        var result = service.getSchedule("mockScheduleId");
        assertThat(result).isEmpty();
    }

    @Test
    void getScheduleReturnsEmptyOptionalWhenScheduleIsFoundInScheduleDetailsTableButNoCorrespondingScheduleFoundInUserScheduleTable() {
        when(mockScheduleDetailsRepository.read("mockScheduleId"))
                .thenReturn(Optional.of(new ScheduleDetails("mockScheduleId", UserId.from("mockUserId"))));
        when(mockRepository.read(UserId.from("mockUserId"))).thenReturn(List.of(Schedule.builder().id("unknown").build()));
        var result = service.getSchedule("mockScheduleId");
        assertThat(result).isEmpty();
    }

    @Test
    void getScheduleReturnsScheduleWhenThereIsAnAssociateValueInBothScheduleDetailsTableAndUserScheduleTable() {
        var schedule = Schedule.builder().id("mockScheduleId").build();
        when(mockScheduleDetailsRepository.read("mockScheduleId"))
                .thenReturn(Optional.of(new ScheduleDetails("mockScheduleId", UserId.from("mockUserId"))));
        when(mockRepository.read(UserId.from("mockUserId"))).thenReturn(List.of(schedule));
        var result = service.getSchedule("mockScheduleId");
        assertThat(result).contains(schedule);
    }

    @Test
    void deleteLocalScheduleWhenNoSchedulesExistInUserScheduleRepository() {
        when(mockScheduleDetailsRepository.read("mockScheduleId"))
                .thenReturn(Optional.of(new ScheduleDetails("mockScheduleId", userId)));

        service.deleteLocalSchedule("mockScheduleId");
        verify(mockRepository).update(userId, List.of());
        verify(mockScheduleDetailsRepository).delete("mockScheduleId");
    }

    @Test
    void deleteLocalSchedule() {
        when(mockScheduleDetailsRepository.read("mockScheduleId"))
                .thenReturn(Optional.of(new ScheduleDetails("mockScheduleId", userId)));
        var schedule1 = Schedule.builder().id("mockScheduleId").build();
        var schedule2 = Schedule.builder().id("mockScheduleId2").build();
        when(mockRepository.read(userId)).thenReturn(List.of(
                schedule1,
                schedule2));

        service.deleteLocalSchedule("mockScheduleId");
        verify(mockRepository).update(userId, List.of(schedule2));
        verify(mockScheduleDetailsRepository).delete("mockScheduleId");
    }

    @Test
    void deleteLocalScheduleWhenScheduleDoesNotExist() {
        when(mockScheduleDetailsRepository.read("mockScheduleId"))
                .thenReturn(Optional.empty());

        service.deleteLocalSchedule("mockScheduleId");
        verify(mockRepository, never()).write(any(), any());
        verify(mockScheduleDetailsRepository, never()).delete(anyString());
    }

    @Test
    void deleteSchedule() {
        when(mockScheduleDetailsRepository.read("mockScheduleId"))
                .thenReturn(Optional.of(new ScheduleDetails("mockScheduleId", userId)));
        var schedule1 = Schedule.builder().id("mockScheduleId").build();
        var schedule2 = Schedule.builder().id("mockScheduleId2").build();
        when(mockRepository.read(userId)).thenReturn(List.of(
                schedule1,
                schedule2));

        service.deleteSchedule(userId, "mockScheduleId");
        verify(mockRepository).update(userId, List.of(schedule2));
        verify(mockScheduleDetailsRepository).delete("mockScheduleId");
        verify(mockSchedulerClient).deleteSchedule(deleteScheduleRequestCapture.capture());
        assertThat(deleteScheduleRequestCapture.getValue().name()).isEqualTo("mockScheduleId");
    }

    @Test
    void deleteScheduleDeletesFromDbWhenAwsThrowsException() {
        when(mockScheduleDetailsRepository.read("mockScheduleId"))
                .thenReturn(Optional.of(new ScheduleDetails("mockScheduleId", userId)));
        var schedule1 = Schedule.builder().id("mockScheduleId").build();
        var schedule2 = Schedule.builder().id("mockScheduleId2").build();
        when(mockRepository.read(userId)).thenReturn(List.of(
                schedule1,
                schedule2));

        when(mockSchedulerClient.deleteSchedule(any(DeleteScheduleRequest.class)))
                .thenThrow(mock(SchedulerException.class));

        service.deleteSchedule(userId, "mockScheduleId");
        verify(mockRepository).update(userId, List.of(schedule2));
        verify(mockScheduleDetailsRepository).delete("mockScheduleId");
        verify(mockSchedulerClient).deleteSchedule(deleteScheduleRequestCapture.capture());
        assertThat(deleteScheduleRequestCapture.getValue().name()).isEqualTo("mockScheduleId");
    }

    @Test
    void scheduleIsNotDeletedWhenUserDoesNotOwnTheSchedule() {
        when(mockScheduleDetailsRepository.read("mockScheduleId"))
                .thenReturn(Optional.of(new ScheduleDetails("mockScheduleId", UserId.from("unknownUserId"))));

        service.deleteSchedule(userId, "mockScheduleId");
        verify(mockRepository, never()).update(eq(userId), any());
        verify(mockScheduleDetailsRepository, never()).delete(anyString());
        verify(mockSchedulerClient, never()).deleteSchedule(any(DeleteScheduleRequest.class));
    }

    @Test
    void deleteScheduleWhenScheduleDoesNotExist() {
        when(mockScheduleDetailsRepository.read("mockScheduleId"))
                .thenReturn(Optional.empty());

        service.deleteSchedule(userId, "mockScheduleId");
        verify(mockRepository, never()).write(any(), any());
        verify(mockScheduleDetailsRepository, never()).delete(anyString());
        verify(mockSchedulerClient, never()).deleteSchedule(any(DeleteScheduleRequest.class));
    }

    @Test
    void testClock() {
        var clock = new Clock();
        clock.localDateTime(ZoneId.of("Europe/Dublin"));
        clock.localDate(ZoneId.of("Europe/Dublin"));
    }
}
