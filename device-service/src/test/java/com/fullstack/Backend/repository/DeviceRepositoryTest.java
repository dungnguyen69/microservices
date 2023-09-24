package com.fullstack.Backend.repository;

import com.fullstack.Backend.BaseTestEntity;
import com.fullstack.Backend.enums.Origin;
import com.fullstack.Backend.enums.Project;
import com.fullstack.Backend.enums.Status;
import com.fullstack.Backend.models.Device;
import com.fullstack.Backend.repositories.interfaces.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectWriter;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*To make a Spring Boot application easily testable with real databases or other services your code depends on, you can use Test containers.
 * Test containers help to test the real database without affecting the database
 * Create a separate database just like the one I use in production, populate it with test data and use it only within my test class
 * Reference: https://programmingtechie.com/2020/10/21/spring-boot-testing-tutorial-database-testing-with-test-containers/*/
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DeviceRepositoryTest extends BaseTestEntity {
    @Autowired
    DeviceRepository deviceRepository;
    Device expectedDeviceObject;
    Device actualDeviceObject;

    @BeforeEach
    public void setUp() {
        expectedDeviceObject = Device
                .builder()
                .name("Air pod")
                .status(Status.VACANT)
                .platformId(2)
                .itemTypeId(2)
                .ramId(3)
                .screenId(1)
                .storageId(1)
                .ownerId(1)
                .inventoryNumber("ABC1b4")
                .serialNumber("12345X1TZ")
                .origin(Origin.External)
                .project(Project.BMW)
                .comments(null)
                .build();
        expectedDeviceObject.setCreatedDate(new Date());
        actualDeviceObject = deviceRepository.save(expectedDeviceObject);
    }

    @Test
    @DisplayName("Save Device")
    public void shouldSaveDevice() {
        assertThat(actualDeviceObject)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expectedDeviceObject);
    }

    @Test
    @DisplayName("Find Device By Serial Number")
    public void shouldFindBySerialNumber() {
        Device testDeviceObject = deviceRepository.findBySerialNumber("12345X1TZ");
        assertThat(testDeviceObject.getInventoryNumber()).isEqualTo("ABC1b4");
    }

    @Test
    @DisplayName("Find Device By Serial Number Except Updated Device")
    public void shouldFindBySerialNumberExceptUpdatedDevice() {
        Device testDeviceObject = deviceRepository.findBySerialNumberExceptUpdatedDevice(1, "12345X1TZ");
        assertThat(testDeviceObject.getInventoryNumber()).isEqualTo("ABC1b4");
    }

    @Test
    @DisplayName("Find Device By OwnerId")
    public void shouldFindByOwnerId() throws JsonProcessingException {
        Sort sort = Sort
                .by("id")
                .ascending();
        List<Device> testDeviceObject = deviceRepository.findByOwnerId(1, sort);
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(testDeviceObject);
        System.out.printf("data: " + json);
        assertThat(testDeviceObject
                .get(0)
                .getInventoryNumber()).isEqualTo("ABC1b4");
    }
}
