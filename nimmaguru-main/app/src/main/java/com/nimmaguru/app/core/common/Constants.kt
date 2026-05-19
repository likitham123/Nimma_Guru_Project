package com.nimmaguru.app.core.common

/**
 * App-wide constants.
 * R-KT-05: No magic numbers or strings — all constants here.
 */
object Constants {

    // Firestore Collections
    const val COLLECTION_GURUS = "gurus"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_SESSIONS = "sessions"
    const val SUBCOLLECTION_SESSIONS = "sessions"
    const val SUBCOLLECTION_APPRECIATIONS = "appreciations"
    const val COLLECTION_ALL_APPRECIATIONS = "all_appreciations"

    // Firestore Fields
    const val FIELD_SKILLS = "skills"
    const val FIELD_APPRECIATION_COUNT = "appreciationCount"
    const val FIELD_TOTAL_SESSIONS = "totalSessions"
    const val FIELD_AVG_RATING = "avgRating"
    const val FIELD_FAME_SCORE = "fameScore"
    const val FIELD_IS_PUBLIC = "isPublic"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_UPDATED_AT = "updatedAt"
    const val FIELD_STATUS = "status"
    const val FIELD_ATTENDEES = "attendees"
    const val FIELD_GURU_ID = "guruId"
    const val FIELD_DATE = "date"
    const val FIELD_OWNER_ID = "ownerId"
    const val FIELD_IS_APPROVED = "isApproved"

    // User roles
    const val ROLE_STUDENT = "student"
    const val ROLE_GURU = "guru"
    const val ROLE_ADMIN = "admin"

    // Pagination
    const val PAGE_SIZE = 20

    // Search
    const val SEARCH_DEBOUNCE_MS = 300L
    const val SEARCH_FETCH_LIMIT = 100

    // Image Upload (compression target)
    const val MAX_IMAGE_SIZE_BYTES = 500 * 1024 // 500KB per R-PERF-02
    const val MAX_IMAGE_DIMENSION = 600 // 600px longest side after resize
    const val IMAGE_JPEG_QUALITY = 75 // 0-100, JPEG compression quality

    // Session Status
    const val SESSION_UPCOMING = "upcoming"
    const val SESSION_COMPLETED = "completed"
    const val SESSION_CANCELLED = "cancelled"

    // Rate Limiting
    const val MAX_APPRECIATIONS_PER_DAY = 5

    // Wall of Fame
    const val WALL_OF_FAME_COUNT = 5
    const val WALL_OF_FAME_FETCH_LIMIT = 10

    // Touch Targets (per R-COMP-03)
    const val MIN_TOUCH_TARGET_DP = 48

    // Firestore Cache
    const val FIRESTORE_CACHE_SIZE_BYTES = 50L * 1024 * 1024 // 50MB per P-NICHE-05

    // OTP / Phone Auth
    const val OTP_LENGTH = 6
    const val PHONE_NUMBER_LENGTH = 10
    const val OTP_TIMEOUT_SECONDS = 60L

    // Validation lengths
    const val NAME_MIN_LENGTH = 2
    const val NAME_MAX_LENGTH = 50
    const val VILLAGE_MIN_LENGTH = 2
    const val VILLAGE_MAX_LENGTH = 60
    const val BIO_MIN_LENGTH = 30
    const val BIO_MAX_LENGTH = 600
    const val APPRECIATION_MIN_LENGTH = 10
    const val APPRECIATION_MAX_LENGTH = 500
    const val SESSION_SUBJECT_MAX_LENGTH = 80
    const val SESSION_DESCRIPTION_MAX_LENGTH = 500

    // Deep links
    const val DEEP_LINK_SCHEME = "nimmaguru"
    const val DEEP_LINK_HOST_GURU = "guru"
}
