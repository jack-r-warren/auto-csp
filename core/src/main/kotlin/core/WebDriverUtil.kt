package core

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.ProfilesIni

sealed class Browser {
    abstract val webDriver: WebDriver
    fun load(url: String) = webDriver.get(url)
    fun quit() = webDriver::quit
    class Firefox(pathToDriver: String? = null) : Browser() {
        init {
            pathToDriver?.let { System.setProperty("webdriver.gecko.driver", pathToDriver) }
        }
        override val webDriver: WebDriver by lazy {
            with(FirefoxOptions()) {
                profile = ProfilesIni().getProfile("default").apply {
                    setPreference("network.cookie.cookieBehavior", 2)
                }
                FirefoxDriver(this)
            }.also {
                it.manage().deleteAllCookies()
            }
        }
    }
    class Chrome(pathToDriver: String? = null) : Browser() {
        init {
            pathToDriver?.let { System.setProperty("webdriver.chrome.driver", pathToDriver) }
        }
        override val webDriver: WebDriver by lazy {
            with(ChromeOptions()) {
                setProxy(null)
                addArguments("--disable-local-storage")
                ChromeDriver(this)
            }.also {
                it.manage().deleteAllCookies()
            }
        }
    }
}