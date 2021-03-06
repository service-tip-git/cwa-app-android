package de.rki.coronawarnapp.main

import de.rki.coronawarnapp.contactdiary.ui.ContactDiarySettings
import de.rki.coronawarnapp.covidcertificate.test.core.TestCertificateRepository
import de.rki.coronawarnapp.covidcertificate.vaccination.core.VaccinationSettings
import de.rki.coronawarnapp.environment.EnvironmentSetup
import de.rki.coronawarnapp.playbook.BackgroundNoise
import de.rki.coronawarnapp.presencetracing.TraceLocationSettings
import de.rki.coronawarnapp.presencetracing.checkins.CheckInRepository
import de.rki.coronawarnapp.storage.OnboardingSettings
import de.rki.coronawarnapp.ui.main.MainActivityViewModel
import de.rki.coronawarnapp.util.CWADebug
import de.rki.coronawarnapp.util.device.BackgroundModeStatus
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import testhelpers.BaseTest
import testhelpers.TestDispatcherProvider
import testhelpers.extensions.CoroutinesTestExtension
import testhelpers.extensions.InstantExecutorExtension
import testhelpers.preferences.mockFlowPreference

@ExtendWith(InstantExecutorExtension::class, CoroutinesTestExtension::class)
class MainActivityViewModelTest : BaseTest() {

    @MockK lateinit var environmentSetup: EnvironmentSetup
    @MockK lateinit var backgroundModeStatus: BackgroundModeStatus
    @MockK lateinit var diarySettings: ContactDiarySettings
    @MockK lateinit var backgroundNoise: BackgroundNoise
    @MockK lateinit var onboardingSettings: OnboardingSettings
    @MockK lateinit var traceLocationSettings: TraceLocationSettings
    @MockK lateinit var checkInRepository: CheckInRepository
    @MockK lateinit var vaccinationSettings: VaccinationSettings
    @MockK lateinit var testCertificateRepository: TestCertificateRepository

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)

        mockkObject(CWADebug)

        every { onboardingSettings.isOnboarded } returns true
        every { environmentSetup.currentEnvironment } returns EnvironmentSetup.Type.WRU
        every { traceLocationSettings.onboardingStatus } returns mockFlowPreference(
            TraceLocationSettings.OnboardingStatus.NOT_ONBOARDED
        )
        every { onboardingSettings.isBackgroundCheckDone } returns true
        every { checkInRepository.checkInsWithinRetention } returns MutableStateFlow(listOf())
        every { testCertificateRepository.certificates } returns emptyFlow()
    }

    private fun createInstance(): MainActivityViewModel = MainActivityViewModel(
        dispatcherProvider = TestDispatcherProvider(),
        environmentSetup = environmentSetup,
        backgroundModeStatus = backgroundModeStatus,
        contactDiarySettings = diarySettings,
        backgroundNoise = backgroundNoise,
        onboardingSettings = onboardingSettings,
        checkInRepository = checkInRepository,
        traceLocationSettings = traceLocationSettings,
        vaccinationSettings = vaccinationSettings,
        testCertificateRepository = testCertificateRepository,
    )

    @Test
    fun `environment toast is visible test environments`() {
        every { CWADebug.isDeviceForTestersBuild } returns true
        every { environmentSetup.currentEnvironment } returns EnvironmentSetup.Type.DEV

        val vm = createInstance()
        vm.showEnvironmentHint.value shouldBe EnvironmentSetup.Type.DEV.rawKey
    }

    @Test
    fun `environment toast is only visible in deviceForTesters flavor`() {
        every { CWADebug.isDeviceForTestersBuild } returns false
        every { environmentSetup.currentEnvironment } returns EnvironmentSetup.Type.DEV

        val vm = createInstance()
        vm.showEnvironmentHint.value shouldBe null
    }

    @Test
    fun `environment toast is not visible in production`() {
        every { CWADebug.isDeviceForTestersBuild } returns true
        every { environmentSetup.currentEnvironment } returns EnvironmentSetup.Type.PRODUCTION

        val vm = createInstance()
        vm.showEnvironmentHint.value shouldBe null
    }

    @Test
    fun `User is not onboarded when settings returns NOT_ONBOARDED `() {
        every { diarySettings.onboardingStatus } returns ContactDiarySettings.OnboardingStatus.NOT_ONBOARDED
        every { vaccinationSettings.registrationAcknowledged } returns true
        val vm = createInstance()
        vm.onBottomNavSelected()
        vm.isContactDiaryOnboardingDone.value shouldBe false
    }

    @Test
    fun `User is onboarded when settings returns RISK_STATUS_1_12 `() {
        every { diarySettings.onboardingStatus } returns ContactDiarySettings.OnboardingStatus.RISK_STATUS_1_12
        every { vaccinationSettings.registrationAcknowledged } returns true
        val vm = createInstance()
        vm.onBottomNavSelected()
        vm.isContactDiaryOnboardingDone.value shouldBe true
    }

    @Test
    fun `Vaccination is not acknowledged when settings returns false `() {
        every { diarySettings.onboardingStatus } returns ContactDiarySettings.OnboardingStatus.RISK_STATUS_1_12
        every { vaccinationSettings.registrationAcknowledged } returns false
        val vm = createInstance()
        vm.onBottomNavSelected()
        vm.isVaccinationConsentGiven.value shouldBe false
    }

    @Test
    fun `Vaccination is acknowledged  when settings returns true `() {
        every { diarySettings.onboardingStatus } returns ContactDiarySettings.OnboardingStatus.RISK_STATUS_1_12
        every { vaccinationSettings.registrationAcknowledged } returns true
        val vm = createInstance()
        vm.onBottomNavSelected()
        vm.isVaccinationConsentGiven.value shouldBe true
    }
}
