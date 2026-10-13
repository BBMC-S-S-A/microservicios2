package edu.unibague.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import edu.unibague.users.dto.CreateUserRequest;
import edu.unibague.users.exception.BadRequestException;
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

    @Test
    void listsProfilesPage() {
        var pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt"));
        UserProfile profile = new UserProfile();
        profile.setId(UUID.randomUUID());
        profile.setUsername("student");
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(profile), pageable, 3));

        var page = service.findAll(pageable);

        assertThat(page.getContent()).extracting("username").containsExactly("student");
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void rejectsUnknownSortField() {
        var pageable = PageRequest.of(0, 20, Sort.by("passwordHash"));

        assertThatThrownBy(() -> service.findAll(pageable)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository);
    }
}
