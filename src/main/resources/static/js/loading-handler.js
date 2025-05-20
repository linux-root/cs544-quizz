const quotes = [
  "Code is like humor. When you have to explain it, it\'s bad. - Cory House",
  "Premature optimization is the root of all evil. - Donald Knuth",
  "Any fool can write code that a computer can understand. Good programmers write code that humans can understand. - Martin Fowler",
  "First, solve the problem. Then, write the code. - John Johnson",
  "The best error message is the one that never shows up. - Thomas Fuchs",
  "I never said half the crap people said I did - Albert Einstein",
];

function initLoadingEffect(formId, loadingOverlayId, quoteDisplayId) {
  const form = document.getElementById(formId);
  const loadingOverlay = document.getElementById(loadingOverlayId);
  const quoteDisplay = document.getElementById(quoteDisplayId);

  if (!form) {
    console.error(`Form with ID '${formId}' not found.`);
    return;
  }
  if (!loadingOverlay) {
    console.error(`Loading overlay with ID '${loadingOverlayId}' not found.`);
    return;
  }
  if (!quoteDisplay) {
    console.error(`Quote display element with ID '${quoteDisplayId}' not found.`);
    return;
  }

  form.addEventListener('submit', function (event) {
    loadingOverlay.style.display = 'flex';
    const randomIndex = Math.floor(Math.random() * quotes.length);
    quoteDisplay.textContent = quotes[randomIndex];
    // Optionally, you might want to hide the overlay if the form submission fails
    // or after a certain timeout if the page doesn't navigate away.
  });
} 
