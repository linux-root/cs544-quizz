let currentFocusedQuestionId = null;
let typingTimer = null;
const TYPING_DEBOUNCE_DELAY_MS = 2000; // 2 seconds
let lastTypedActionTimestamp = null;

/**
 * Records a student action.
 * For now, it logs to the console. Later, this will send data to the server.
 *
 * @param {string} actionType - The type of action (e.g., 'COPY_PASTE', 'TYPING').
 * @param {object} details - An object containing action-specific details.
 *                           Expected properties:
 *                           - startTimestamp (ISO string)
 *                           - endTimestamp (ISO string, can be same as start for point-in-time actions)
 *                           - questionId (string|null) - Optional ID of the related question.
 *                           - actionValue (string|null) - Optional, any specific value related to the action.
 */
function recordAction(actionType, details) {
  // Original actionData structure based on client-side logic
  const originalActionData = {
    actionType: actionType,
    startTimestamp: details.startTimestamp || new Date().toISOString(),
    endTimestamp: details.endTimestamp || new Date().toISOString(),
    questionId: details.questionId !== undefined ? details.questionId : currentFocusedQuestionId, // Will be string or null
    actionValue: details.actionValue !== undefined ? details.actionValue : null,
    sessionId: window.QUIZZ_SESSION_ID // Will be string from textContent
  };

  // Prepare payload for the server, parsing IDs
  let parsedSessionId = null;
  if (originalActionData.sessionId && originalActionData.sessionId !== 'UNKNOWN_SESSION') {
    const numSessionId = parseInt(originalActionData.sessionId, 10);
    if (!isNaN(numSessionId)) {
      parsedSessionId = numSessionId;
    } else {
      console.warn('Could not parse sessionId to number:', originalActionData.sessionId);
    }
  }

  let parsedQuestionId = null;
  if (originalActionData.questionId) { // If it's null or undefined, it remains null
    const numQuestionId = parseInt(originalActionData.questionId, 10);
    if (!isNaN(numQuestionId)) {
      parsedQuestionId = numQuestionId;
    } else {
      console.warn('Could not parse questionId to number:', originalActionData.questionId);
    }
  }

  const payloadForServer = {
    actionType: originalActionData.actionType,
    startTimestamp: originalActionData.startTimestamp,
    endTimestamp: originalActionData.endTimestamp,
    questionId: parsedQuestionId, // Parsed to number or null
    actionValue: originalActionData.actionValue,
    sessionId: parsedSessionId // Parsed to number or null
    // studentId is no longer sent from the client
  };

  if (payloadForServer.sessionId === null) {
    console.warn('Session ID is unknown or invalid. Action not sent. Original sessionId:', originalActionData.sessionId);
    return;
  }

  // Function to get CSRF token from cookies
  function getCsrfToken() {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
      const [name, value] = cookie.trim().split('=');
      if (name === 'XSRF-TOKEN') { // Default cookie name used by Spring Security
        return decodeURIComponent(value);
      }
    }
    return null;
  }

  const csrfToken = getCsrfToken();
  const headers = {
    'Content-Type': 'application/json'
  };
  if (csrfToken) {
    headers['X-XSRF-TOKEN'] = csrfToken; // Default header name expected by Spring Security
  } else {
    console.warn('CSRF token (XSRF-TOKEN) not found in cookies. Request might be rejected by server.');
  }

  fetch('/api/v1/student-actions', {
    method: 'POST',
    headers: headers,
    body: JSON.stringify(payloadForServer)
  })
    .then(response => {
      if (!response.ok) {
        response.text().then(text => {
          console.error('Failed to record action. Status:', response.status, 'Response Text:', text, 'Payload Sent:', payloadForServer);
        }).catch(() => {
          console.error('Failed to record action. Status:', response.status, 'Could not parse response text.', 'Payload Sent:', payloadForServer);
        });
      }
    })
    .catch(error => {
      console.error('Error recording action:', error, 'Payload Sent:', payloadForServer);
    });
}

function handleTypingStart(event) {
  const questionId = event.target.id.replace('answer-', '');
  currentFocusedQuestionId = questionId; // Update focused question

  if (!lastTypedActionTimestamp) { // If it's the first key press or after a debounced action
    lastTypedActionTimestamp = new Date().toISOString();
  }
  clearTimeout(typingTimer);
}

function handleTypingEnd(event) {
  const questionId = event.target.id.replace('answer-', '');
  // Ensure currentFocusedQuestionId is set, even if focusout happens before input for some reason.
  currentFocusedQuestionId = questionId;


  clearTimeout(typingTimer);
  typingTimer = setTimeout(() => {
    if (lastTypedActionTimestamp) {
      recordAction('TYPING', {
        startTimestamp: lastTypedActionTimestamp,
        endTimestamp: new Date().toISOString(),
        questionId: questionId,
        actionValue: event.target.value.length // Record current length as an example value
      });
      lastTypedActionTimestamp = null; // Reset for the next typing burst
    }
  }, TYPING_DEBOUNCE_DELAY_MS);
}


function initActionTracking() {
  console.log('Initializing student action tracking...');

  // 1. Track Copy/Paste
  document.querySelectorAll("textarea[id^='answer-']").forEach(textarea => {
    const questionId = textarea.id.replace('answer-', '');

    textarea.addEventListener('focus', () => {
      currentFocusedQuestionId = questionId;
      // If there was an ongoing typing timer for another textarea, log it before switching focus.
      if (typingTimer && lastTypedActionTimestamp) {
        clearTimeout(typingTimer); // Clear existing timer
        recordAction('TYPING', {
          startTimestamp: lastTypedActionTimestamp,
          endTimestamp: new Date().toISOString(), // Log with current time as end
          // questionId will be the one from the *previous* textarea (closure)
          // This uses currentFocusedQuestionId by way of recordAction's fallback
          actionValue: "typing ended due to focus out"
        });
        lastTypedActionTimestamp = null; // Reset
      }
    });

    textarea.addEventListener('copy', (event) => {
      recordAction('COPY_PASTE', {
        actionValue: 'copy',
        questionId: questionId
      });
    });

    textarea.addEventListener('paste', (event) => {
      recordAction('COPY_PASTE', {
        actionValue: 'paste',
        questionId: questionId
      });
    });

    // 2. Track Typing (debounced)
    textarea.addEventListener('input', handleTypingStart); // Use 'input' for more robust typing detection
    textarea.addEventListener('input', handleTypingEnd); // Debounce from the last input

    // If user leaves the textarea, force the typing action to be recorded if one was pending
    textarea.addEventListener('blur', () => {
      clearTimeout(typingTimer);
      if (lastTypedActionTimestamp) {
        recordAction('TYPING', {
          startTimestamp: lastTypedActionTimestamp,
          endTimestamp: new Date().toISOString(),
          questionId: questionId, // use questionId from the closure
          actionValue: "typing ended due to blur"
        });
        lastTypedActionTimestamp = null;
      }
      // currentFocusedQuestionId = null; // Optionally clear when no textarea is focused
    });
  });

  // 3. Track Tab Switching
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
      recordAction('TAB_SWITCH_AWAY', {});
      // If there was an ongoing typing timer, log it
      if (typingTimer && lastTypedActionTimestamp) {
        clearTimeout(typingTimer);
        recordAction('TYPING', {
          startTimestamp: lastTypedActionTimestamp,
          endTimestamp: new Date().toISOString(),
          // currentFocusedQuestionId should still be set from the last interaction
          actionValue: "typing ended due to tab switch away"
        });
        lastTypedActionTimestamp = null;
      }
    } else {
      recordAction('TAB_SWITCH_BACK', {});
    }
  });

  // 4. Track Quiz Submission
  const quizForm = document.getElementById('quizAttemptForm');
  if (quizForm) {
    quizForm.addEventListener('submit', () => {
      // Ensure any pending typing action is recorded before submitting
      clearTimeout(typingTimer);
      if (lastTypedActionTimestamp) {
        recordAction('TYPING', {
          startTimestamp: lastTypedActionTimestamp,
          endTimestamp: new Date().toISOString(),
          // currentFocusedQuestionId should still be set
          actionValue: "typing ended due to quiz submission"
        });
        lastTypedActionTimestamp = null;
      }
      recordAction('SUBMIT_TEST', {
        // No specific questionId for submit test, it's a global action for the session.
        questionId: null
      });
      // Note: The actual submission will proceed after this.
      // If sending to server, ensure it's synchronous or use `navigator.sendBeacon` if possible for reliability.
    });
  }

  // Set a global variable for QUIZZ_SESSION_ID if it's available in the HTML
  // This is a placeholder, actual mechanism might involve parsing from a data attribute or a script tag.
  const quizzSessionIdElement = document.evaluate("//p[contains(., 'Session ID:')]/span", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
  if (quizzSessionIdElement) {
    window.QUIZZ_SESSION_ID = quizzSessionIdElement.textContent.trim();
    console.log('Quizz Session ID found:', window.QUIZZ_SESSION_ID);
  } else {
    console.warn('Quizz Session ID span not found. Action tracking might be incomplete.');
    window.QUIZZ_SESSION_ID = 'UNKNOWN_SESSION'; // Fallback
  }

  console.log('Student action tracking initialized.');
}

// Initialize tracking when the DOM is ready, similar to the existing script in take-quizz.html
if (document.readyState === 'loading') { // Loading hasn't finished yet
  document.addEventListener('DOMContentLoaded', initActionTracking);
} else { // `DOMContentLoaded` has already fired
  initActionTracking();
} 
