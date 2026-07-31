package com.skillenroll.enums;

/**
 * User roles in the SkillEnroll platform.
 * Exposed to Spring Security as {@code ROLE_<name>} authorities, enabling
 * {@code @PreAuthorize("hasRole('ADMIN')")}-style checks.
 */
public enum Role {
    ADMIN,
    INSTRUCTOR,
    STUDENT
}
