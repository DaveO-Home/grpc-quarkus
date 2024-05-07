/*
 * This file is generated by jOOQ.
 */
package golf.handicap.generated.tables.records


import golf.handicap.generated.tables.Course

import org.jooq.Record1
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class CourseRecord() : UpdatableRecordImpl<CourseRecord>(Course.COURSE) {

    open var courseSeq: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var courseName: String?
        set(value): Unit = set(1, value)
        get(): String? = get(1) as String?

    open var courseCountry: String?
        set(value): Unit = set(2, value)
        get(): String? = get(2) as String?

    open var courseState: String?
        set(value): Unit = set(3, value)
        get(): String? = get(3) as String?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    /**
     * Create a detached, initialised CourseRecord
     */
    constructor(courseSeq: Int? = null, courseName: String? = null, courseCountry: String? = null, courseState: String? = null): this() {
        this.courseSeq = courseSeq
        this.courseName = courseName
        this.courseCountry = courseCountry
        this.courseState = courseState
        resetChangedOnNotNull()
    }
}