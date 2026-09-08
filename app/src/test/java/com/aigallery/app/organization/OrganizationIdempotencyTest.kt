package com.aigallery.app.organization

import com.aigallery.app.data.database.OrganizedMediaEntity
import com.aigallery.app.data.database.OrganizationStatus
import com.aigallery.app.domain.organization.model.ScreenshotCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OrganizationIdempotencyTest {

    @Test
    fun testOrganizationEntity_versioningAndIdempotency() {
        // Initial pending entity
        val initial = OrganizedMediaEntity(
            sourceMediaId = 555L,
            sourceUri = "content://media/external/images/media/555",
            destinationUri = null,
            category = ScreenshotCategory.OTHER.id,
            subcategory = null,
            organizationVersion = 1,
            createdAt = 1000L,
            updatedAt = 1000L,
            status = OrganizationStatus.PENDING
        )

        assertEquals(OrganizationStatus.PENDING, initial.status)
        assertNull(initial.destinationUri)

        // First organization pass
        val organized = initial.copy(
            category = ScreenshotCategory.WORK.id,
            subcategory = "Hackathon",
            destinationUri = "content://media/external/images/media/999",
            status = OrganizationStatus.ORGANIZED,
            updatedAt = 2000L
        )

        assertEquals(OrganizationStatus.ORGANIZED, organized.status)
        assertEquals("content://media/external/images/media/999", organized.destinationUri)
        assertEquals(ScreenshotCategory.WORK.id, organized.category)

        // Idempotent re-run: when destination exists, destinationUri is preserved
        val reRun = organized.copy(
            updatedAt = 3000L
        )

        assertEquals(organized.sourceMediaId, reRun.sourceMediaId)
        assertEquals(organized.destinationUri, reRun.destinationUri)
        assertEquals(organized.status, reRun.status)

        // Stale detection: when user deletes file externally
        val stale = organized.copy(
            status = OrganizationStatus.STALE,
            updatedAt = 4000L
        )

        assertEquals(OrganizationStatus.STALE, stale.status)
        assertNotEquals(organized.status, stale.status)
    }

    @Test
    fun testReorganizationVersionBump() {
        val original = OrganizedMediaEntity(
            sourceMediaId = 777L,
            sourceUri = "content://media/external/images/media/777",
            destinationUri = "content://media/external/images/media/888",
            category = ScreenshotCategory.TECHNOLOGY.id,
            subcategory = null,
            organizationVersion = 1,
            createdAt = 1000L,
            updatedAt = 1000L,
            status = OrganizationStatus.ORGANIZED
        )

        // Reorganize marks pending with incremented version
        val pendingReorganize = original.copy(
            organizationVersion = original.organizationVersion + 1,
            status = OrganizationStatus.PENDING,
            updatedAt = 5000L
        )

        assertEquals(2, pendingReorganize.organizationVersion)
        assertEquals(OrganizationStatus.PENDING, pendingReorganize.status)
    }
}
