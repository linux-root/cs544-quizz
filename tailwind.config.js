/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/main/resources/templates/**/*.html",
    // "./src/main/resources/static/**/*.js" // Keep this commented for now or add it back if you use JS to add classes
  ],
  theme: {
    extend: {},
  },
  // DaisyUI plugin and config removed for Tailwind CSS v4
  // It will be configured in input.css
} 
