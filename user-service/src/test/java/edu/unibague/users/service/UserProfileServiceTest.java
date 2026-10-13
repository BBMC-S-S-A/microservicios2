package edu.unibague.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.unibague.users.dto.CreateUserRequest;
import edu.unibague.users.exception.ConflictException;
import edu.unibague.users.exception.ResourceNotFoundException;
import edu.unibague.users.model.UserProfile;
import edu.unibague.users.repository.UserProfileRepository;

class UserProfileServiceTest {

    private final UserProfileRepository repository = mock(UserProfileRepository.class);
    private final UserProfileService service = new UserProfileService(repository);

    @Test
    void createsProfileForTokenSubject() {
        UUID id = UUID.randomUUID();
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(id, "student", "student@taller.local",
                new CreateUserRequest("Carlos", "Diaz", "3001234567"));

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.username()).isEqualTo("student");
        assertThat(response.firstName()).isEqualTo("Carlos");
    }

    @Test
    void rejectsDuplicateProfile() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        assertThatThrownBy(() -> service.create(id, "student", "student@taller.local",
                new CreateUserRequest("Carlos", "Diaz", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void throwsNotFoundForMissingProfile() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }
}
