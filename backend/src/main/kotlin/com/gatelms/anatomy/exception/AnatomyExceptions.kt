package com.gatelms.anatomy.exception

class AnatomyValidationException(message: String) : RuntimeException(message)

class AnatomyLockedException(
    val code: String = "ANATOMY_LOCKED_UNTIL_ATTEMPT",
    message: String = "Anatomy dossier is locked until you submit at least one attempt on this question."
) : RuntimeException(message)

class NoSourceQuestionsException(
    val code: String = "NO_SOURCE_QUESTIONS",
    message: String = "At least one tagged question is required to generate a concept unit."
) : RuntimeException(message)

class AnatomyNotFoundException(message: String) : RuntimeException(message)
