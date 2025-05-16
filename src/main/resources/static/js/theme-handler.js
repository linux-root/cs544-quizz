document.addEventListener('DOMContentLoaded', () => {
    const themeToggle = document.getElementById('theme-toggle-checkbox');
    const htmlElement = document.documentElement;

    // 1. Determine initial theme
    let determinedTheme = localStorage.getItem('theme');
    if (!determinedTheme) {
        // If no theme in localStorage, check OS preference
        determinedTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    // 2. Apply initial theme to HTML attribute for immediate effect
    htmlElement.setAttribute('data-theme', determinedTheme);

    // 3. Set checkbox state to reflect initial theme
    // This allows theme-controller to be in sync and display correctly
    if (themeToggle) {
        themeToggle.checked = (determinedTheme === 'dark');
    }

    // 4. Ensure localStorage is consistent with the determined initial theme
    // This is important if determinedTheme came from OS preference and wasn't in localStorage yet,
    // or if localStorage was somehow cleared.
    localStorage.setItem('theme', determinedTheme);

    // Add event listener for the toggle
    if (themeToggle) {
        themeToggle.addEventListener('change', () => {
            // When the toggle is clicked, its 'checked' state changes.
            // DaisyUI's theme-controller (on the checkbox with value="dark")
            // will automatically update the <html> data-theme attribute:
            // - If checked (user wants dark): data-theme="dark"
            // - If unchecked (user wants light): theme-controller removes data-theme,
            //   and our page should default to "light" as per DaisyUI config.
            
            // Our JS only needs to persist this new state to localStorage.
            const newTheme = themeToggle.checked ? 'dark' : 'light';
            localStorage.setItem('theme', newTheme);

            // Note: We are now relying on DaisyUI's theme-controller to set/remove
            // the data-theme attribute on the <html> tag when the checkbox is interacted with.
            // We do not need to call htmlElement.setAttribute('data-theme', newTheme); here.
        });
    }

    // Listen for changes in OS preference (optional, but good for UX)
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        // Apply OS theme only if no theme is manually set in localStorage
        // or if the currently applied theme matches the previous OS preference
        const currentTheme = htmlElement.getAttribute('data-theme');
        const osPreferredTheme = e.matches ? 'dark' : 'light';
        if (!localStorage.getItem('themeOverride') || localStorage.getItem('theme') !== osPreferredTheme) {
            localStorage.setItem('theme', osPreferredTheme);
            // We clear themeOverride if OS changes, so user's manual selection is for that OS theme preference
            // However, we only do this if there wasn't already a user override for the new OS preference
            if(localStorage.getItem('themeOverride')){
                localStorage.removeItem('themeOverride');
            }
        }
    });

    // When user manually changes theme, set an override flag
    if (themeToggle) {
        themeToggle.addEventListener('click', () => {
            localStorage.setItem('themeOverride', 'true');
        });
    }

    // --- Logic to hide Login button on login page ---
    const currentPath = window.location.pathname;
    const loginButton = document.getElementById('navbar-login-button');

    if (loginButton && (currentPath === '/login' || currentPath.startsWith('/login/'))) { // .startsWith to catch /login and /login/professor etc.
        // Find the parent <li> element to hide it completely, as the <a> is inside an <li>
        const parentLi = loginButton.closest('li');
        if (parentLi) {
            parentLi.style.display = 'none';
        }
    }
    // --- End of Login button hiding logic ---
}); 