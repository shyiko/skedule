package com.github.shyiko.skedule

/**
 * Thrown in case of invalid schedule (during parsing, programmatic construction, etc).
 */
class InvalidScheduleException(msg: String) : RuntimeException(msg)
