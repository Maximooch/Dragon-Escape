import vinextWorker from "../dist/server/index.js";

const executionContext = {
  passThroughOnException() {},
  waitUntil(promise) {
    promise.catch((error) => {
      console.error("[vercel] Background task failed", error);
    });
  },
};

export default {
  fetch(request) {
    return vinextWorker.fetch(request, {}, executionContext);
  },
};
