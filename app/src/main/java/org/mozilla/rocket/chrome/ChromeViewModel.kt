package org.mozilla.rocket.chrome

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.os.Parcel
import android.os.Parcelable
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.persistence.BookmarkModel
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.invalidate
import org.mozilla.rocket.nightmode.AdjustBrightnessDialog
import org.mozilla.rocket.privately.PrivateMode
import org.mozilla.urlutils.UrlUtils

class ChromeViewModel(
    private val settings: Settings,
    private val bookmarkRepo: BookmarkRepository,
    private val privateMode: PrivateMode
) : ViewModel() {
    val isNightMode = MutableLiveData<Boolean>()
    val tabCount = MutableLiveData<Int>()
    val isTabRestoredComplete = MutableLiveData<Boolean>()
    val navigationState = MutableLiveData<ScreenNavigator.NavigationState>()
    val currentUrl = MutableLiveData<String>()
    val currentTitle = MutableLiveData<String>()
    var isCurrentUrlBookmarked: LiveData<Boolean>
    val isRefreshing = MutableLiveData<Boolean>()
    val canGoBack = MutableLiveData<Boolean>()
    val canGoForward = MutableLiveData<Boolean>()
    val isHomePageUrlInputShowing = MutableLiveData<Boolean>()
    val isMyShotOnBoardingPending = MutableLiveData<Boolean>()
    val isTurboModeEnabled = MutableLiveData<Boolean>()
    val isBlockImageEnabled = MutableLiveData<Boolean>()
    val hasUnreadScreenshot = MutableLiveData<Boolean>()
    val isPrivateBrowsingActive = MutableLiveData<Boolean>()

    val showToast = SingleLiveEvent<ToastMessage>()
    val openUrl = SingleLiveEvent<OpenUrlAction>()
    val showTabTray = SingleLiveEvent<Unit>()
    val showMenu = SingleLiveEvent<Unit>()
    val showNewTab = SingleLiveEvent<Unit>()
    val showUrlInput = SingleLiveEvent<String?>()
    val dismissUrlInput = SingleLiveEvent<Unit>()
    val doScreenshot = SingleLiveEvent<ScreenCaptureTelemetryData>()
    val pinShortcut = SingleLiveEvent<Unit>()
    val toggleBookmark = SingleLiveEvent<Unit>()
    // TODO: separate to startRefresh / stopLoading
    val refreshOrStop = SingleLiveEvent<Unit>()
    val share = SingleLiveEvent<Unit>()
    val goNext = SingleLiveEvent<Unit>()
    val showDownloadPanel = SingleLiveEvent<Unit>()
    val togglePrivateMode = SingleLiveEvent<Unit>()
    val dropCurrentPage = SingleLiveEvent<Unit>()
    val updateMenu = SingleLiveEvent<Unit>()
    val clearBrowsingHistory = SingleLiveEvent<Unit>()
    val driveDefaultBrowser = SingleLiveEvent<Unit>()
    val exitApp = SingleLiveEvent<Unit>()
    val showBookmarks = SingleLiveEvent<Unit>()
    val showHistory = SingleLiveEvent<Unit>()
    val showScreenshots = SingleLiveEvent<Unit>()
    val openPreference = SingleLiveEvent<Unit>()
    val showFindInPage = SingleLiveEvent<Unit>()
    val showAdjustBrightness = SingleLiveEvent<Unit>()
    val showNightModeOnBoarding = SingleLiveEvent<Unit>()

    init {
        settings.run {
            isNightMode.value = isNightModeEnable
            isTurboModeEnabled.value = shouldUseTurboMode()
            isBlockImageEnabled.value = shouldBlockImages()
            hasUnreadScreenshot.value = AppConfigWrapper.getMyshotUnreadEnabled() && hasUnreadMyShot()
            isPrivateBrowsingActive.value = privateMode.hasPrivateSession()
        }
        isRefreshing.value = false
        canGoBack.value = false
        canGoForward.value = false

        isCurrentUrlBookmarked = Transformations.switchMap<String, List<BookmarkModel>>(currentUrl, bookmarkRepo::getBookmarksByUrl)
                .let { urlBookmarksLiveData ->
                    Transformations.map<List<BookmarkModel>, Boolean>(urlBookmarksLiveData) { it.isNotEmpty() }
                }
    }

    fun invalidate() {
        isNightMode.invalidate()
        tabCount.invalidate()
        isTabRestoredComplete.invalidate()
        navigationState.invalidate()
        currentUrl.invalidate()
        currentTitle.invalidate()
        isRefreshing.invalidate()
        canGoBack.invalidate()
        canGoForward.invalidate()
        isHomePageUrlInputShowing.invalidate()
        isMyShotOnBoardingPending.invalidate()
        isTurboModeEnabled.invalidate()
        isBlockImageEnabled.invalidate()
    }

    fun adjustNightMode() {
        updateNightMode(true)
        showAdjustBrightness.call()
    }

    fun onNightModeToggled() {
        updateNightMode(!settings.isNightModeEnable)
        showAdjustBrightnessIfNeeded()
    }

    private fun updateNightMode(isEnabled: Boolean) {
        settings.setNightMode(isEnabled)
        if (isNightMode.value != isEnabled) {
            isNightMode.value = isEnabled
        }
        TelemetryWrapper.menuNightModeChangeTo(isEnabled)
    }

    private fun showAdjustBrightnessIfNeeded() {
        val currentBrightness = settings.nightModeBrightnessValue
        if (currentBrightness == BRIGHTNESS_OVERRIDE_NONE) {
            // First time turn on
            settings.nightModeBrightnessValue = AdjustBrightnessDialog.Constants.DEFAULT_BRIGHTNESS
            showAdjustBrightness.call()
            settings.setNightModeSpotlight(true)
        }
    }

    fun onRestoreTabCountStarted() {
        isTabRestoredComplete.value = false
    }

    fun onRestoreTabCountCompleted() {
        isTabRestoredComplete.value = true
    }

    fun onTabCountChanged(count: Int) {
        if (isTabRestoredComplete.value == true) {
            val currentCount = tabCount.value
            if (currentCount != count) {
                tabCount.value = count
            }
        }
    }

    fun onFocusedUrlChanged(url: String?) {
        if (url != currentUrl.value) {
            currentUrl.value = url
        }
    }

    fun onFocusedTitleChanged(title: String?) {
        if (title != currentTitle.value) {
            currentTitle.value = title
        }
    }

    fun onPageLoadingStarted() {
        if (isRefreshing.value != true) {
            isRefreshing.value = true
        }
    }

    fun onPageLoadingStopped() {
        if (isRefreshing.value == true) {
            isRefreshing.value = false
        }
        updateMenu()
    }

    fun onMenuShown() {
        updateMenu()
    }

    private fun updateMenu() {
        updateMenu.call()
        checkIfShowPrivateBrowsingOnBoarding()
        checkIfPrivateBrowsingActive()
    }

    private fun checkIfShowPrivateBrowsingOnBoarding() {
        if (settings.showNightModeSpotlight()) {
            settings.setNightModeSpotlight(false)
            showNightModeOnBoarding.call()
        }
    }

    // TODO: find a better way to observe if there is private session
    private fun checkIfPrivateBrowsingActive() {
        val hasPrivateSession = privateMode.hasPrivateSession()
        if (isPrivateBrowsingActive.value != hasPrivateSession) {
            isPrivateBrowsingActive.value = hasPrivateSession
        }
    }

    fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        if (this.canGoBack.value != canGoBack) {
            this.canGoBack.value = canGoBack
        }
        if (this.canGoForward.value != canGoForward) {
            this.canGoForward.value = canGoForward
        }
    }

    fun onShowHomePageUrlInput() {
        isHomePageUrlInputShowing.value = true
    }

    fun onDismissHomePageUrlInput() {
        isHomePageUrlInputShowing.value = false
    }

    fun showMyShotOnBoarding() {
        if (isMyShotOnBoardingPending.value != true) {
            isMyShotOnBoardingPending.value = true
        }
    }

    fun onMyShotOnBoardingDisplayed() {
        if (isMyShotOnBoardingPending.value != false) {
            isMyShotOnBoardingPending.value = false
        }
    }

    fun addBookmark(): String? {
        var bookmarkId: String? = null
        val url = currentUrl.value
        if (!url.isNullOrEmpty()) {
            val title = currentTitle.value.takeUnless { it.isNullOrEmpty() }
                    ?: UrlUtils.stripCommonSubdomains(UrlUtils.stripHttp(url))
            bookmarkId = bookmarkRepo.addBookmark(title, url)
        }

        return bookmarkId
    }

    fun deleteBookmark() {
        currentUrl.value?.let { url ->
            bookmarkRepo.deleteBookmarksByUrl(url)
        }
    }

    fun onTurboModeToggled() {
        val toEnable = !settings.shouldUseTurboMode()
        settings.setTurboMode(toEnable)
        if (isTurboModeEnabled.value != toEnable) {
            isTurboModeEnabled.value = toEnable
        }
        showToast.value = ToastMessage(if (toEnable) R.string.message_enable_turbo_mode else R.string.message_disable_turbo_mode)
        TelemetryWrapper.menuTurboChangeTo(toEnable)
    }

    fun onBlockImageToggled() {
        val toEnable = !settings.shouldBlockImages()
        settings.setBlockImages(toEnable)
        if (isBlockImageEnabled.value != toEnable) {
            isBlockImageEnabled.value = toEnable
        }
        showToast.value = ToastMessage(if (toEnable) R.string.message_enable_block_image else R.string.message_disable_block_image)
        TelemetryWrapper.menuBlockImageChangeTo(toEnable)
    }

    fun onDoScreenshot(telemetryData: ScreenCaptureTelemetryData) {
        doScreenshot.value = telemetryData
        val shouldShowUnread = AppConfigWrapper.getMyshotUnreadEnabled()
        if (hasUnreadScreenshot.value != shouldShowUnread) {
            hasUnreadScreenshot.value = shouldShowUnread
        }
    }

    fun showScreenshots() {
        settings.setHasUnreadMyShot(false)
        if (hasUnreadScreenshot.value != false) {
            hasUnreadScreenshot.value = false
        }
        showScreenshots.call()
        TelemetryWrapper.clickMenuCapture()
    }

    data class OpenUrlAction(
        val url: String,
        val withNewTab: Boolean,
        val isFromExternal: Boolean
    )

    data class ScreenCaptureTelemetryData(val mode: String, val position: Int) : Parcelable {
        constructor(source: Parcel) : this(
                source.readString()!!,
                source.readInt()
        )

        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
            writeString(mode)
            writeInt(position)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<ScreenCaptureTelemetryData> = object : Parcelable.Creator<ScreenCaptureTelemetryData> {
                override fun createFromParcel(source: Parcel): ScreenCaptureTelemetryData = ScreenCaptureTelemetryData(source)
                override fun newArray(size: Int): Array<ScreenCaptureTelemetryData?> = arrayOfNulls(size)
            }
        }
    }
}

class ToastMessage(
    val stringResId: Int,
    val duration: Int = LENGTH_SHORT,
    vararg val args: String
) {
    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1
    }
}