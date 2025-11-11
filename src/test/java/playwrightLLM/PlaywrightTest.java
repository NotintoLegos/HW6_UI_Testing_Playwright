package playwrightLLM;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PlaywrightTest {

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
		// record videos into target/test-videos; Playwright will store video files there
		Path videoDir = Paths.get("target/test-videos");
		context = browser.newContext(new Browser.NewContextOptions().setRecordVideoDir(videoDir));
		context.setDefaultTimeout(30000);
		page = context.newPage();
		page.navigate("https://depaul.bncollege.com/");

		// Ensure cart is cleared before each test
		clearShoppingCart();
	}

	@AfterEach
	void closeContext() {
		// Closing context will finalize and save the recorded video
		if (context != null) {
			context.close();
		}
	}

	@AfterAll
	void closeBrowser() {
		if (playwright != null) {
			playwright.close();
		}
	}

	void clearShoppingCart() {
		try {
			// Navigate to cart page and remove any items
			page.navigate("https://depaul.bncollege.com/cart");
			page.waitForLoadState(LoadState.NETWORKIDLE);
			page.waitForTimeout(1000);

			Locator removeButtons = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Remove"));
			int itemCount = 0;
			try {
				itemCount = Math.toIntExact(removeButtons.count());
			} catch (Exception ignored) {
			}

			for (int i = 0; i < itemCount; i++) {
				Locator firstRemove = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Remove")).first();
				firstRemove.click();
				page.waitForTimeout(500);
			}

			// Return to home for tests
			page.navigate("https://depaul.bncollege.com/");
		} catch (PlaywrightException e) {
			// If anything goes wrong, continue; tests will start from a consistent home page
			System.out.println("clearShoppingCart: " + e.getMessage());
			page.navigate("https://depaul.bncollege.com/");
		}
	}

	@Test
	void testSearchFilterAddToCart_andVerifyCartCount() {
		// 1) Search for "earbuds"
		page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Search")).click();
		page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Search")).fill("earbuds");
		page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Search")).press("Enter");
		page.waitForLoadState(LoadState.NETWORKIDLE);

		// Verify search results page contains the query
		assertThat(page.locator("body")).containsText("earbuds");

		// 2) Filter by color: Black (use same steps as the other tests)
		Locator colorButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Color"));
		if (colorButton.count() > 0) {
			colorButton.first().click();
			page.waitForTimeout(1000);
			Locator blackFilter = page.getByText("Black", new Page.GetByTextOptions().setExact(false)).first();
			if (blackFilter.count() > 0) {
				blackFilter.click();
			}
		}

		// 3) Click the first product found (navigate to product details)
		// Try several heuristics in order to pick a product link
		Locator productLink = page.getByTitle("JBL Quantum True Wireless").first();
		if (productLink.count() == 0) {
			productLink = page.locator("a").filter(new Locator.FilterOptions().setHasText("JBL"));
		}
		if (productLink.count() == 0) {
			// fallback: click first product card link
			productLink = page.locator("a").first();
		}
		productLink.first().click();
		page.waitForLoadState(LoadState.NETWORKIDLE);

		// 4) Add to cart
		Locator addToCart = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add to cart"));
		if (addToCart.count() == 0) {
			addToCart = page.getByText("Add to cart").first();
		}
		addToCart.first().click();

		// allow cart to update briefly
		page.waitForTimeout(3000);

		// 5) Verify cart shows 1 item (look for cart count or cart link text)
		Locator cartCount = page.locator(".cart-count").or(page.locator(".nav-items-total")).or(page.locator("[class*='cart-items']")).first();
		if (cartCount.count() > 0) {
			assertThat(cartCount).containsText("1");
		} else {
			// fallback: open cart and check quantity
			page.locator("[href*='cart']").first().click();
			page.waitForLoadState(LoadState.NETWORKIDLE);
			Locator quantityField = page.locator("input[type='number']").or(page.locator("[name='quantity']")).first();
			assertThat(quantityField).hasValue("1");
		}
	}
}
