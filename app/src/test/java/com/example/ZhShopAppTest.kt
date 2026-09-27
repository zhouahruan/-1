package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.zhshop.data.repository.ZhShopRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ZhShopAppTest {

    @Test
    fun testRepositoryInitialData() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ZhShopRepository(context)

        // Verify categories
        val categories = repo.categories.value
        assertTrue("Categories should not be empty", categories.isNotEmpty())
        assertTrue("Should contain wear category", categories.any { it.id == "c_wear" })

        // Verify apps list
        val apps = repo.appsList.value
        assertTrue("Apps list should contain items", apps.isNotEmpty())

        // Verify pulse data
        val pulse = repo.getPulseData()
        assertNotNull(pulse)
        assertTrue("Pulse should have banners", pulse.banners.isNotEmpty())
        assertTrue("Pulse should have announcements", pulse.announcements.isNotEmpty())

        // Verify check-in
        val (success, msg) = repo.checkin()
        assertTrue("Checkin should succeed", success)
        assertTrue(msg.contains("签到成功"))

        // Verify search
        val searchResults = repo.searchApps("表盘")
        assertTrue("Search for 表盘 should return results", searchResults.isNotEmpty())

        // Verify developer apps
        val devApps = repo.developerApps.value
        assertTrue("Developer apps should exist", devApps.isNotEmpty())

        // Verify APK parser
        val parsed = repo.parseApk("https://backend.appmiaoda.com/files/test.apk")
        assertNotNull(parsed)
        assertEquals(24, parsed.minSdk)

        // Verify Agreement Security Gate & Anti-Bypass Checksum
        // 1. Initial or reset state should not be agreed
        repo.revokeAgreementConsent()
        assertFalse("Agreement should not be valid before user consents", repo.hasAgreedAgreement.value)
        assertFalse(repo.tokenManager.isAgreementValid())

        // 2. User consents: state transitions to true with valid checksum
        repo.saveAgreementConsent()
        assertTrue("Agreement should be valid after consent", repo.hasAgreedAgreement.value)
        assertTrue(repo.tokenManager.isAgreementValid())

        // 3. User revokes: resets to false
        repo.revokeAgreementConsent()
        assertFalse("Agreement should be invalid after revocation", repo.hasAgreedAgreement.value)
    }
}
