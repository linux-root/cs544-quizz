let currentFocusedQuestionId = null;
const TYPING_DEBOUNCE_DELAY_MS = 2000; // 2 seconds

const typingStateCache = {}; // Stores { isTyping: boolean, typingTimer: TimeoutId | null, startTimestamp: string | null }

/**
 * Records a student action.
 *
 * @param {string} actionType - The type of action (e.g., 'COPY_PASTE', 'START_TYPING', 'STOP_TYPING').
 * @param {object} details - An object containing action-specific details.
 */
function recordAction(actionType, details) {
  // 'TYPING' is deprecated. All calls should use 'START_TYPING' or 'STOP_TYPING'.
  // If 'TYPING' is somehow passed, it will be sent as-is to the server.
  // The server-side enum StudentActionType should no longer have 'TYPING'.

  const originalActionData = {
    actionType: actionType,
    startTimestamp: details.startTimestamp || new Date().toISOString(),
    endTimestamp: details.endTimestamp || new Date().toISOString(),
    questionId: details.questionId !== undefined ? details.questionId : currentFocusedQuestionId,
    actionValue: details.actionValue !== undefined ? details.actionValue : null,
    sessionId: window.QUIZZ_SESSION_ID
  };

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

function handleInput(event) {
  const textarea = event.target;
  const questionId = textarea.id.replace('answer-', '');
  currentFocusedQuestionId = questionId;

  if (!typingStateCache[questionId]) {
    console.warn(`State for questionId ${questionId} not found in handleInput. Initializing.`);
    typingStateCache[questionId] = { isTyping: false, typingTimer: null, startTimestamp: null };
  }
  const state = typingStateCache[questionId];

  if (!state.isTyping) {
    state.startTimestamp = new Date().toISOString();
    recordAction('START_TYPING', {
      startTimestamp: state.startTimestamp,
      endTimestamp: state.startTimestamp,
      questionId: questionId,
      actionValue: String(textarea.value.length),
    });
    state.isTyping = true;
  }

  if (state.typingTimer) {
    clearTimeout(state.typingTimer);
  }

  state.typingTimer = setTimeout(() => {
    if (state.isTyping) {
      recordAction('STOP_TYPING', {
        startTimestamp: state.startTimestamp,
        endTimestamp: new Date().toISOString(),
        questionId: questionId,
        actionValue: String(textarea.value.length),
      });
      state.isTyping = false;
      state.startTimestamp = null;
    }
  }, TYPING_DEBOUNCE_DELAY_MS);
}

function forceStopTyping(questionId, reasonSuffix) {
  if (!typingStateCache[questionId] || !typingStateCache[questionId].isTyping) {
    return;
  }
  const state = typingStateCache[questionId];
  const textarea = document.getElementById('answer-' + questionId);
  const forcedEndTimestamp = new Date().toISOString();
  let stopActionValue;

  if (reasonSuffix === 'focus_change') stopActionValue = "typing ended due to focus out";
  else if (reasonSuffix === 'blur') stopActionValue = "typing ended due to blur";
  else if (reasonSuffix === 'tab_switch_away') stopActionValue = "typing ended due to tab switch away";
  else if (reasonSuffix === 'quiz_submission') stopActionValue = "typing ended due to quiz submission";
  else stopActionValue = textarea ? String(textarea.value.length) : "0";

  clearTimeout(state.typingTimer);
  recordAction('STOP_TYPING', {
    startTimestamp: state.startTimestamp || forcedEndTimestamp,
    endTimestamp: forcedEndTimestamp,
    questionId: questionId,
    actionValue: stopActionValue,
  });
  state.isTyping = false;
  state.startTimestamp = null;
}

function initActionTracking() {
  console.log('Initializing student action tracking...');
  Object.keys(typingStateCache).forEach(key => delete typingStateCache[key]);
  currentFocusedQuestionId = null;

  document.querySelectorAll("textarea[id^='answer-']").forEach(textarea => {
    const questionId = textarea.id.replace('answer-', '');

    typingStateCache[questionId] = {
      isTyping: false,
      typingTimer: null,
      startTimestamp: null
    };

    textarea.addEventListener('focus', () => {
      if (currentFocusedQuestionId && currentFocusedQuestionId !== questionId && typingStateCache[currentFocusedQuestionId]?.isTyping) {
        forceStopTyping(currentFocusedQuestionId, 'focus_change');
      }
      currentFocusedQuestionId = questionId;
    });

    textarea.addEventListener('copy', (event) => {
      recordAction('COPY_PASTE', { actionValue: 'copy', questionId: questionId });
    });

    textarea.addEventListener('paste', (event) => {
      recordAction('COPY_PASTE', { actionValue: 'paste', questionId: questionId });
    });

    textarea.addEventListener('input', handleInput); // THE ONLY WAY TYPING START/STOP IS NOW HANDLED

    textarea.addEventListener('blur', () => {
      forceStopTyping(questionId, 'blur');
    });
  });

  document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
      recordAction('TAB_SWITCH_AWAY', {});
      if (currentFocusedQuestionId && typingStateCache[currentFocusedQuestionId]?.isTyping) {
        forceStopTyping(currentFocusedQuestionId, 'tab_switch_away');
      }
    } else {
      recordAction('TAB_SWITCH_BACK', {});
    }
  });

  const quizForm = document.getElementById('quizAttemptForm');
  if (quizForm) {
    quizForm.addEventListener('submit', () => {
      Object.keys(typingStateCache).forEach(qId => {
        // No need to check isTyping here, forceStopTyping does it.
        forceStopTyping(qId, 'quiz_submission');
      });
      recordAction('SUBMIT_TEST', { questionId: null });
    });
  }

  const quizzSessionIdElement = document.evaluate("//p[contains(., 'Session ID:')]/span", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
  if (quizzSessionIdElement) {
    window.QUIZZ_SESSION_ID = quizzSessionIdElement.textContent.trim();
    console.log('Quizz Session ID found:', window.QUIZZ_SESSION_ID);
  } else {
    console.warn('Quizz Session ID span not found. Action tracking might be incomplete.');
    window.QUIZZ_SESSION_ID = 'UNKNOWN_SESSION';
  }

  console.log('Student action tracking initialized.');
}

// Initialize tracking when the DOM is ready, similar to the existing script in take-quizz.html
if (document.readyState === 'loading') { // Loading hasn't finished yet
  document.addEventListener('DOMContentLoaded', initActionTracking);
} else { // `DOMContentLoaded` has already fired
  initActionTracking();
} 
