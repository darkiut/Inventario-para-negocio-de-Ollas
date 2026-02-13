from playwright.sync_api import sync_playwright

def verify_dashboard_features():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # 1. Login Page
        page.goto("http://localhost:8000/index.html")
        page.screenshot(path="verification/login_page.png")
        print("Login page screenshot taken.")

        # 2. Dashboard - We can't easily mock auth state or firebase data in this simple script without extensive mocking.
        # But we can load the dashboard and check if static elements like filters exist.
        page.goto("http://localhost:8000/dashboard.html")

        # Wait a bit to ensure elements render (even if empty or redirecting)
        # Note: Since auth fails, it will likely redirect to index.html immediately.
        # So checking specific elements might fail if we don't mock auth.
        # However, checking that the new JS/HTML is served is valuable.

        # In a real environment we would mock firebase auth or use a test account.
        # For now, let's just inspect the source or static structure if possible before redirect?
        # Actually, the redirect happens in window.onload.

        # Let's verify we can see the filters in the DOM if we disable JS or intercept?
        # No, easier to just verify the file content is correct via 'read_file', which we did.
        # But we can take a screenshot of the redirect action.

        page.wait_for_timeout(1000)
        page.screenshot(path="verification/dashboard_redirect_check.png")
        print("Dashboard redirect screenshot taken.")

        browser.close()

if __name__ == "__main__":
    verify_dashboard_features()
