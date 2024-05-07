/*
 * This file is generated by jOOQ.
 */
package golf.handicap.generated.tables.records


import golf.handicap.generated.tables.Golfer

import java.math.BigDecimal

import org.jooq.Record1
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class GolferRecord() : UpdatableRecordImpl<GolferRecord>(Golfer.GOLFER) {

    open var pin: String?
        set(value): Unit = set(0, value)
        get(): String? = get(0) as String?

    open var firstName: String?
        set(value): Unit = set(1, value)
        get(): String? = get(1) as String?

    open var lastName: String?
        set(value): Unit = set(2, value)
        get(): String? = get(2) as String?

    open var handicap: Float?
        set(value): Unit = set(3, value)
        get(): Float? = get(3) as Float?

    open var country: String?
        set(value): Unit = set(4, value)
        get(): String? = get(4) as String?

    open var state: String?
        set(value): Unit = set(5, value)
        get(): String? = get(5) as String?

    open var overlapYears: Int?
        set(value): Unit = set(6, value)
        get(): Int? = get(6) as Int?

    open var `public`: Int?
        set(value): Unit = set(7, value)
        get(): Int? = get(7) as Int?

    open var lastLogin: BigDecimal?
        set(value): Unit = set(8, value)
        get(): BigDecimal? = get(8) as BigDecimal?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<String?> = super.key() as Record1<String?>

    /**
     * Create a detached, initialised GolferRecord
     */
    constructor(pin: String? = null, firstName: String? = null, lastName: String? = null, handicap: Float? = null, country: String? = null, state: String? = null, overlapYears: Int? = null, `public`: Int? = null, lastLogin: BigDecimal? = null): this() {
        this.pin = pin
        this.firstName = firstName
        this.lastName = lastName
        this.handicap = handicap
        this.country = country
        this.state = state
        this.overlapYears = overlapYears
        this.`public` = `public`
        this.lastLogin = lastLogin
        resetChangedOnNotNull()
    }
}