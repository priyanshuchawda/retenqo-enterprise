package com.mneme.app.data

/**
 * Structured memory card kinds stored on [NoteEntity].
 * Notes remain the Room row; these fields make the product a card system.
 */
enum class CardType(val storage: String, val label: String) {
    DUE("due", "Due"),
    PROMISE("promise", "Promise"),
    WAITING("waiting", "Waiting"),
    FACT("fact", "Fact"),
    ASK_FOR_YOU("ask_for_you", "Ask"),
    ;

    companion object {
        fun fromStorage(value: String?): CardType =
            entries.firstOrNull { it.storage == value } ?: FACT
    }
}

enum class CardStatus(val storage: String, val label: String) {
    OPEN("open", "Open"),
    DONE("done", "Done"),
    SNOOZED("snoozed", "Snoozed"),
    EXPIRED("expired", "Expired"),
    ;

    companion object {
        fun fromStorage(value: String?): CardStatus =
            entries.firstOrNull { it.storage == value } ?: OPEN
    }
}

enum class CardReviewState(val storage: String, val label: String) {
    PENDING("pending", "Needs review"),
    CONFIRMED("confirmed", "Confirmed"),
    DISMISSED("dismissed", "Dismissed");

    companion object {
        fun fromStorage(value: String?): CardReviewState =
            entries.firstOrNull { it.storage == value } ?: CONFIRMED
    }
}
