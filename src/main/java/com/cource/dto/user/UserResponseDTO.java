package com.cource.dto.user;

public class UserResponseDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String idCard;
    private String role;
    private boolean isActive;

    private UserResponseDTO() {}

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getIdCard() {
        return idCard;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return isActive;
    }

    public static class Builder {
        private UserResponseDTO dto = new UserResponseDTO();

        public Builder id(Long id) {
            dto.id = id;
            return this;
        }

        public Builder email(String email) {
            dto.email = email;
            return this;
        }

        public Builder firstName(String firstName) {
            dto.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            dto.lastName = lastName;
            return this;
        }

        public Builder idCard(String idCard) {
            dto.idCard = idCard;
            return this;
        }

        public Builder role(String role) {
            dto.role = role;
            return this;
        }

        public Builder isActive(boolean isActive) {
            dto.isActive = isActive;
            return this;
        }

        public UserResponseDTO build() {
            return dto;
        }
    }
}
