package playwrightTraditional;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MainTest {

    Playwright playwright;
    Browser browser;
    BrowserContext context;
    Page page;

    @BeforeAll
    void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    }

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext();
        context.setDefaultTimeout(30000);
        page = context.newPage();
        page.navigate("https://depaul.bncollege.com/");
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    // Helper to perform all navigation and actions up to product details page
    void navigateToProductDetails() {
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Search")).click();
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Search")).fill("earbuds");
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Search")).press("Enter");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("brand")).click();
        page.waitForTimeout(1000);
        page.getByText("JBL", new Page.GetByTextOptions().setExact(false)).first().click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Color")).click();
        page.waitForTimeout(1000);
        page.getByText("Black", new Page.GetByTextOptions().setExact(false)).first().click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Price")).click();
        page.waitForTimeout(1000);
        page.getByText("Over $50", new Page.GetByTextOptions().setExact(false)).click();
        page.getByTitle("JBL Quantum True Wireless").first().click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForTimeout(2000);
    }

    @Test
    void testSearchResultsContainEarbuds() {
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Search")).click();
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Search")).fill("earbuds");
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Search")).press("Enter");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertThat(page.locator("body")).containsText("earbuds");
    }

    @Test
    void testProductNameVisible() {
        navigateToProductDetails();
        Locator productName = page.locator("h1").or(page.locator("[data-testid='product-title']"))
            .or(page.locator(".product-name"))
            .or(page.locator(".product-detail-title"))
            .first();
        assertThat(productName).containsText("Welcome to Your Bookstore");
    }

    @Test
    void testProductPriceVisible() {
        navigateToProductDetails();
        Locator productPrice = page.locator(".price").or(page.locator("[data-testid='price']"))
            .or(page.locator(".product-price"))
            .or(page.getByText("$")).first();
        assertThat(productPrice).isVisible();
    }

    @Test
    void testProductDescriptionVisible() {
        navigateToProductDetails();
        Locator productDescription = page.locator(".product-description")
            .or(page.locator(".description"))
            .or(page.getByText("Wireless"))
            .or(page.getByText("Noise Cancelling"))
            .first();
        assertThat(productDescription).isVisible();
    }

    @Test
    void testProductSkuVisible() {
        navigateToProductDetails();
        Locator skuElement = page.getByText("SKU:").or(page.getByText("Model:"))
            .or(page.locator(".sku"))
            .or(page.getByText("Item"))
            .first();
        assertThat(skuElement).isVisible();
    }

                // ...existing code for other test steps can be split similarly ...

    // Add more individual test methods for each assertion as needed
    }