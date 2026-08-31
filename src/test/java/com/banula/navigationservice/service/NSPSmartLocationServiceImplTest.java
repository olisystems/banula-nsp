package com.banula.navigationservice.service;

import com.banula.navigationservice.config.ApplicationConfiguration;
import com.banula.navigationservice.repository.SmartLocationRepository;
import com.banula.openlib.mongodb.util.GenericMongoMapper;
import com.banula.openlib.ocpi.custom.smartlocations.SmartLocation;
import com.banula.openlib.ocpi.custom.smartlocations.SmartLocationState;
import com.banula.openlib.ocpi.custom.smartlocations.dto.SmartLocationDTO;
import com.banula.openlib.ocpi.custom.smartlocations.mongo.MongoSmartLocation;
import com.banula.openlib.ocpi.exception.OCPICustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NSPSmartLocationServiceImplTest {

    private static final String COUNTRY_CODE = "DE";
    private static final String PARTY_ID = "ABC";
    private static final String LOCATION_ID = "LOCTEST";

    @Mock
    private SmartLocationRepository smartLocationRepository;

    @Mock
    private GenericMongoMapper genericMongoMapper;

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    private NSPSmartLocationServiceImpl service;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new NSPSmartLocationServiceImpl(smartLocationRepository, genericMongoMapper,
                applicationConfiguration);
        when(applicationConfiguration.getZoneId()).thenReturn("Europe/Berlin");
    }

    // ---------- refreshActiveStates ----------

    @Test
    void refreshActiveStates_shouldPromoteVerified_whenWindowCoversToday() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation location = mongoLocation(SmartLocationState.VERIFIED, today.minusDays(1), today.plusDays(1));
        stubCandidates(location);
        stubToMongoIdentity();

        int changed = service.refreshActiveStates();

        assertEquals(1, changed);
        assertEquals(SmartLocationState.ACTIVE, location.getSmartLocationState());
        assertTrue(location.getPublish());
        verify(smartLocationRepository).save(any(MongoSmartLocation.class));
    }

    @Test
    void refreshActiveStates_shouldPromoteVerified_whenWindowIsExactlyToday() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation location = mongoLocation(SmartLocationState.VERIFIED, today, today);
        stubCandidates(location);
        stubToMongoIdentity();

        assertEquals(1, service.refreshActiveStates());
        assertEquals(SmartLocationState.ACTIVE, location.getSmartLocationState());
    }

    @Test
    void refreshActiveStates_shouldArchiveActive_whenBothDaysAreInThePast() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation location = mongoLocation(SmartLocationState.ACTIVE, today.minusDays(10),
                today.minusDays(5));
        location.setPublish(true);
        stubCandidates(location);
        stubToMongoIdentity();

        assertEquals(1, service.refreshActiveStates());
        assertEquals(SmartLocationState.ARCHIVED, location.getSmartLocationState());
        assertFalse(location.getPublish());
    }

    @Test
    void refreshActiveStates_shouldArchiveVerified_whenBothDaysAreInThePast() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation location = mongoLocation(SmartLocationState.VERIFIED, today.minusDays(10),
                today.minusDays(5));
        stubCandidates(location);
        stubToMongoIdentity();

        assertEquals(1, service.refreshActiveStates());
        assertEquals(SmartLocationState.ARCHIVED, location.getSmartLocationState());
    }

    @Test
    void refreshActiveStates_shouldActivateOpenEndedWindow_whenFirstDayHasPassed() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation location = mongoLocation(SmartLocationState.VERIFIED, today.minusDays(30), null);
        stubCandidates(location);
        stubToMongoIdentity();

        assertEquals(1, service.refreshActiveStates());
        assertEquals(SmartLocationState.ACTIVE, location.getSmartLocationState());
        assertTrue(location.getPublish());
    }

    @Test
    void refreshActiveStates_shouldReviveArchived_whenTheLastDayIsGone() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation location = mongoLocation(SmartLocationState.ARCHIVED, today.minusDays(30), null);
        stubCandidates(location);
        stubToMongoIdentity();

        assertEquals(1, service.refreshActiveStates());
        assertEquals(SmartLocationState.ACTIVE, location.getSmartLocationState());
    }

    @Test
    void refreshActiveStates_shouldKeepArchived_whenItHasNoWindow() {
        MongoSmartLocation location = mongoLocation(SmartLocationState.ARCHIVED, null, null);
        stubCandidates(location);

        assertEquals(0, service.refreshActiveStates());
        assertEquals(SmartLocationState.ARCHIVED, location.getSmartLocationState());
        verify(smartLocationRepository, never()).save(any(MongoSmartLocation.class));
    }

    @Test
    void refreshActiveStates_shouldNotSave_whenNothingChanged() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation unchangedVerified = mongoLocation(SmartLocationState.VERIFIED, today.plusDays(5),
                today.plusDays(10));
        MongoSmartLocation unchangedActive = mongoLocation(SmartLocationState.ACTIVE, today, today);
        stubCandidates(unchangedVerified, unchangedActive);

        assertEquals(0, service.refreshActiveStates());
        verify(smartLocationRepository, never()).save(any(MongoSmartLocation.class));
    }

    @Test
    void refreshActiveStates_shouldBeIdempotent() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation location = mongoLocation(SmartLocationState.VERIFIED, today, today);
        stubCandidates(location);
        stubToMongoIdentity();

        assertEquals(1, service.refreshActiveStates());
        assertEquals(0, service.refreshActiveStates());
    }

    @Test
    void refreshActiveStates_shouldIgnoreLocationsWithoutAWindow() {
        MongoSmartLocation location = mongoLocation(SmartLocationState.VERIFIED, null, null);
        stubCandidates(location);

        assertEquals(0, service.refreshActiveStates());
        assertEquals(SmartLocationState.VERIFIED, location.getSmartLocationState());
        verify(smartLocationRepository, never()).save(any(MongoSmartLocation.class));
    }

    // ---------- patchSmartLocation ----------

    @Test
    void patchSmartLocation_shouldRejectManuallySentActiveState() {
        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setSmartLocationState(SmartLocationState.ACTIVE);

        OCPICustomException exception = assertThrows(OCPICustomException.class,
                () -> service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto));

        assertTrue(exception.getMessage().contains("ACTIVE cannot be set directly"));
        verify(smartLocationRepository, never()).save(any(MongoSmartLocation.class));
    }

    @Test
    void patchSmartLocation_shouldActivateImmediately_whenWindowCoversToday() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.VERIFIED, null, null);
        stubExisting(existing);
        stubToMongoIdentity();
        stubToDto();

        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setActiveFirstDay(today);
        dto.setActiveLastDay(today.plusDays(3));
        stubFromDto(dto);

        SmartLocationDTO result = service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto);

        assertEquals(SmartLocationState.ACTIVE, result.getSmartLocationState());
        assertEquals(today, result.getActiveFirstDay());
        assertTrue(result.getPublish());
    }

    @Test
    void patchSmartLocation_shouldStayVerified_whenWindowIsInTheFuture() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.VERIFIED, null, null);
        stubExisting(existing);
        stubToMongoIdentity();
        stubToDto();

        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setActiveFirstDay(today.plusDays(5));
        dto.setActiveLastDay(today.plusDays(9));
        stubFromDto(dto);

        SmartLocationDTO result = service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto);

        assertEquals(SmartLocationState.VERIFIED, result.getSmartLocationState());
        assertFalse(result.getPublish());
    }

    @Test
    void patchSmartLocation_shouldClearWindow_whenAStateIsRequested() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.ACTIVE, today.minusDays(1), today.plusDays(1));
        stubExisting(existing);
        stubToMongoIdentity();
        stubToDto();

        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setSmartLocationState(SmartLocationState.VERIFIED);
        stubFromDto(dto);

        SmartLocationDTO result = service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto);

        assertNull(result.getActiveFirstDay());
        assertNull(result.getActiveLastDay());
        assertEquals(SmartLocationState.VERIFIED, result.getSmartLocationState());
        assertFalse(result.getPublish());
    }

    @Test
    void patchSmartLocation_shouldStoreWindowButStayInert_whenLocationIsNotVerified() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.ENRICHED, null, null);
        stubExisting(existing);
        stubToMongoIdentity();
        stubToDto();

        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setActiveFirstDay(today);
        dto.setActiveLastDay(today);
        stubFromDto(dto);

        SmartLocationDTO result = service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto);

        assertEquals(SmartLocationState.ENRICHED, result.getSmartLocationState());
        assertEquals(today, result.getActiveFirstDay());
        assertFalse(result.getPublish());
    }

    @Test
    void patchSmartLocation_shouldStampLastUpdated() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.VERIFIED, null, null);
        existing.setLastUpdated(LocalDateTime.of(2000, 1, 1, 0, 0));
        stubExisting(existing);
        stubToMongoIdentity();
        stubToDto();

        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setActiveFirstDay(today);
        dto.setActiveLastDay(today);
        stubFromDto(dto);

        service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto);

        ArgumentCaptor<MongoSmartLocation> saved = ArgumentCaptor.forClass(MongoSmartLocation.class);
        verify(smartLocationRepository).save(saved.capture());
        assertTrue(saved.getValue().getLastUpdated().isAfter(LocalDateTime.of(2020, 1, 1, 0, 0)));
    }

    @Test
    void patchSmartLocation_shouldActivate_whenOnlyTheFirstDayIsGiven() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.VERIFIED, null, null);
        stubExisting(existing);
        stubToMongoIdentity();
        stubToDto();

        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setActiveFirstDay(today.minusDays(2));
        stubFromDto(dto);

        SmartLocationDTO result = service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto);

        assertEquals(SmartLocationState.ACTIVE, result.getSmartLocationState());
        assertNull(result.getActiveLastDay());
        assertTrue(result.getPublish());
    }

    @Test
    void patchSmartLocation_shouldArchive_whenTheLastDayIsSetInThePast() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.ACTIVE, today.minusDays(10), null);
        existing.setPublish(true);
        stubExisting(existing);
        stubToMongoIdentity();
        stubToDto();

        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setActiveLastDay(today.minusDays(1));
        stubFromDto(dto);

        SmartLocationDTO result = service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto);

        assertEquals(SmartLocationState.ARCHIVED, result.getSmartLocationState());
        assertFalse(result.getPublish());
    }

    /**
     * The reversibility requirement: a last day entered by mistake archived the
     * location, and sending an explicit null clears it and brings it back to
     * ACTIVE, because the first day is still in the past.
     */
    @Test
    void patchSmartLocation_shouldReviveArchived_whenTheLastDayIsClearedExplicitly() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.ARCHIVED, today.minusDays(10),
                today.minusDays(1));
        stubExisting(existing);
        stubToMongoIdentity();
        stubToDto();

        SmartLocationDTO dto = new SmartLocationDTO();
        stubFromDto(dto);

        SmartLocationDTO result = service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto, false, true);

        assertNull(result.getActiveLastDay());
        assertEquals(today.minusDays(10), result.getActiveFirstDay());
        assertEquals(SmartLocationState.ACTIVE, result.getSmartLocationState());
        assertTrue(result.getPublish());
    }

    @Test
    void patchSmartLocation_shouldLeaveTheWindowAlone_whenNoDayKeyIsSent() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.ACTIVE, today.minusDays(1), today.plusDays(1));
        stubExisting(existing);
        stubToMongoIdentity();
        stubToDto();

        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setSmartMeterId("someMeter");
        stubFromDto(dto);

        SmartLocationDTO result = service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto);

        assertEquals(today.minusDays(1), result.getActiveFirstDay());
        assertEquals(today.plusDays(1), result.getActiveLastDay());
        assertEquals(SmartLocationState.ACTIVE, result.getSmartLocationState());
    }

    @Test
    void patchSmartLocation_shouldUpdateDates_onAnActiveLocation() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.ACTIVE, today.minusDays(5), today.plusDays(1));
        stubExisting(existing);
        stubToMongoIdentity();
        stubToDto();

        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setActiveLastDay(today.plusDays(30));
        stubFromDto(dto);

        SmartLocationDTO result = service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto);

        assertEquals(today.plusDays(30), result.getActiveLastDay());
        assertEquals(SmartLocationState.ACTIVE, result.getSmartLocationState());
    }

    @Test
    void patchSmartLocation_shouldUpdateDates_onAnArchivedLocation() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.ARCHIVED, today.minusDays(20),
                today.minusDays(10));
        stubExisting(existing);
        stubToMongoIdentity();
        stubToDto();

        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setActiveLastDay(today.plusDays(10));
        stubFromDto(dto);

        SmartLocationDTO result = service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto);

        assertEquals(today.plusDays(10), result.getActiveLastDay());
        assertEquals(SmartLocationState.ACTIVE, result.getSmartLocationState());
    }

    @Test
    void patchSmartLocation_shouldKeepTheWindow_whenAStateAndAWindowAreSentTogether() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.ARCHIVED, today.minusDays(20),
                today.minusDays(10));
        stubExisting(existing);
        stubToMongoIdentity();
        stubToDto();

        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setSmartLocationState(SmartLocationState.VERIFIED);
        dto.setActiveFirstDay(today.plusDays(1));
        dto.setActiveLastDay(today.plusDays(5));
        stubFromDto(dto);

        SmartLocationDTO result = service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto);

        assertEquals(today.plusDays(1), result.getActiveFirstDay());
        assertEquals(today.plusDays(5), result.getActiveLastDay());
        assertEquals(SmartLocationState.VERIFIED, result.getSmartLocationState());
    }

    @Test
    void patchSmartLocation_shouldRejectInvertedWindow() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.VERIFIED, null, null);
        stubExisting(existing);

        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setActiveFirstDay(today.plusDays(5));
        dto.setActiveLastDay(today.plusDays(1));
        stubFromDto(dto);

        OCPICustomException exception = assertThrows(OCPICustomException.class,
                () -> service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto));

        assertTrue(exception.getMessage().contains("active_last_day must not be before active_first_day"));
        verify(smartLocationRepository, never()).save(any(MongoSmartLocation.class));
    }

    @Test
    void patchSmartLocation_shouldRejectLastDayWithoutFirstDay() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.VERIFIED, null, null);
        stubExisting(existing);

        SmartLocationDTO dto = new SmartLocationDTO();
        dto.setActiveLastDay(today.plusDays(1));
        stubFromDto(dto);

        OCPICustomException exception = assertThrows(OCPICustomException.class,
                () -> service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto));

        assertTrue(exception.getMessage().contains("active_last_day requires active_first_day"));
        verify(smartLocationRepository, never()).save(any(MongoSmartLocation.class));
    }

    @Test
    void patchSmartLocation_shouldRejectClearingOnlyTheFirstDay() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.ACTIVE, today.minusDays(5), today.plusDays(5));
        stubExisting(existing);

        SmartLocationDTO dto = new SmartLocationDTO();
        stubFromDto(dto);

        assertThrows(OCPICustomException.class,
                () -> service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto, true, false));
    }

    @Test
    void patchSmartLocation_shouldDropToVerified_whenTheWholeWindowIsCleared() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        MongoSmartLocation existing = mongoLocation(SmartLocationState.ACTIVE, today.minusDays(5), today.plusDays(5));
        existing.setPublish(true);
        stubExisting(existing);
        stubToMongoIdentity();
        stubToDto();

        SmartLocationDTO dto = new SmartLocationDTO();
        stubFromDto(dto);

        SmartLocationDTO result = service.patchSmartLocation(COUNTRY_CODE, PARTY_ID, LOCATION_ID, dto, true, true);

        assertNull(result.getActiveFirstDay());
        assertNull(result.getActiveLastDay());
        assertEquals(SmartLocationState.VERIFIED, result.getSmartLocationState());
        assertFalse(result.getPublish());
    }

    // ---------- helpers ----------

    private void stubCandidates(MongoSmartLocation... locations) {
        when(smartLocationRepository.findBySmartLocationStateIn(any())).thenReturn(List.of(locations));
    }

    private void stubExisting(MongoSmartLocation existing) {
        when(smartLocationRepository.findByCompoundIndex(COUNTRY_CODE, PARTY_ID, LOCATION_ID))
                .thenReturn(Optional.of(existing));
    }

    private void stubToMongoIdentity() {
        when(genericMongoMapper.toMongo(any(SmartLocation.class), eq(MongoSmartLocation.class)))
                .thenAnswer(invocation -> {
                    SmartLocation source = invocation.getArgument(0);
                    if (source instanceof MongoSmartLocation mongo) {
                        return mongo;
                    }
                    MongoSmartLocation mongo = new MongoSmartLocation();
                    org.springframework.beans.BeanUtils.copyProperties(source, mongo);
                    return mongo;
                });
    }

    private void stubToDto() {
        when(genericMongoMapper.toDTO(any(SmartLocation.class), eq(SmartLocationDTO.class)))
                .thenAnswer(invocation -> {
                    SmartLocation source = invocation.getArgument(0);
                    SmartLocationDTO dto = new SmartLocationDTO();
                    org.springframework.beans.BeanUtils.copyProperties(source, dto);
                    return dto;
                });
    }

    private void stubFromDto(SmartLocationDTO dto) {
        when(genericMongoMapper.fromDTO(any(SmartLocationDTO.class), eq(SmartLocation.class)))
                .thenAnswer(invocation -> {
                    SmartLocationDTO source = invocation.getArgument(0);
                    SmartLocation entity = new SmartLocation();
                    org.springframework.beans.BeanUtils.copyProperties(source, entity);
                    // lastUpdated must not be copied in: the patcher owns that stamp.
                    entity.setLastUpdated(null);
                    return entity;
                });
    }

    private MongoSmartLocation mongoLocation(SmartLocationState state, LocalDate first, LocalDate last) {
        MongoSmartLocation location = new MongoSmartLocation();
        location.setCountryCode(COUNTRY_CODE);
        location.setPartyId(PARTY_ID);
        location.setId(LOCATION_ID);
        location.setSmartLocationState(state);
        location.setActiveFirstDay(first);
        location.setActiveLastDay(last);
        location.setPublish(false);
        return location;
    }
}
