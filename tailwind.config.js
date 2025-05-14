/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/main/resources/templates/**/*.html",
    // "./src/main/resources/static/**/*.js" // Keep this commented for now or add it back if you use JS to add classes
  ],
  theme: {
    extend: {},
  },
  plugins: [
    // require('daisyui'), // Removed for Tailwind CSS v4 compatibility with DaisyUI v5
  ],
  // daisyui: { // Removed for Tailwind CSS v4 compatibility with DaisyUI v5
  //   themes: true, 
  //   darkTheme: "dark", 
  //   base: true, 
  //   styled: true, 
  //   utils: true, 
  //   prefix: "", 
  //   logs: true, 
  // }
} 