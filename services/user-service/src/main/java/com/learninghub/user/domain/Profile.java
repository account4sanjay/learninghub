package com.learninghub.user.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Application profile of a Keycloak user (keyed by preferred_username). */
@Entity
@Table(name = "profile")
@Getter
@Setter
@NoArgsConstructor
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    /** Ask 46: logical reference to enrollment-service grade_level.id ('junior'); names come from there. */
    @Column(name = "grade_level")
    private String gradeLevel;
    @Column(nullable = false)
    private String role = "Student";
    private String email;
    private String phone;
    private String location;
    private String bio;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    private String school;
    private String address;
    @Column(name = "guardian_name")
    private String guardianName;
    @Column(name = "guardian_relation")
    private String guardianRelation;
    @Column(name = "guardian_email")
    private String guardianEmail;
    @Column(name = "guardian_phone")
    private String guardianPhone;
    @Column(name = "pref_timezone", nullable = false)
    private String prefTimezone = "Pacific Time (PT)";
    @Column(name = "pref_notifications", nullable = false)
    private String prefNotifications = "Email & Push";
    /** Mr | Ms | Dr | null */
    private String title;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    /** male | female | null — selects the default avatar */
    private String gender;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "avatar")
    private byte[] avatar;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "avatar_content_type")
    private String avatarContentType;

    /** "Dr John Anderson" — title (when set) + first + last; falls back to the stored full name / username. */
    public String displayName() {
        StringBuilder b = new StringBuilder();
        if (title != null && !title.isBlank()) b.append(title.trim()).append(' ');
        if (firstName != null && !firstName.isBlank()) b.append(firstName.trim()).append(' ');
        if (lastName != null && !lastName.isBlank()) b.append(lastName.trim());
        String n = b.toString().trim();
        if (!n.isEmpty()) return n;
        return fullName != null && !fullName.isBlank() ? fullName : username;
    }

    /** Keep full_name in step with title/first/last. */
    public void recomputeFullName() {
        fullName = displayName();
    }

    public boolean hasAvatar() {
        return avatar != null && avatar.length > 0;
    }
}
